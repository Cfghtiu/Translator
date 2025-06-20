package kgg.translator;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import kgg.translator.event.TranslateEvent;
import kgg.translator.exception.NoTranslatorException;
import kgg.translator.exception.NotConfiguredException;
import kgg.translator.exception.TranslateException;
import kgg.translator.ocrtrans.ResRegion;
import kgg.translator.translator.Source;
import kgg.translator.translator.Translator;
import kgg.translator.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static kgg.translator.TranslatorManager.*;

public class TranslateService {
    private static final Logger LOGGER = LogManager.getLogger(TranslateService.class);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+");
    private static final CacheManager CACHE_MANAGER = new CacheManager();

    // ======================== 公共接口 ========================
    public static String translate(String text, String source) throws TranslateException {
        return translate(text, getCurrent(), getFrom(), getTo(), source);
    }

    public static String translate(String text, Translator translator, String from, String to, String source) throws TranslateException {
        if (shouldSkipTranslation(translator, text, to)) {
            return text;
        }
        checkTranslator(translator);
        return performTranslation(text, translator, from, to, source);
    }

    public static String cachedTranslate(String text, String source) throws TranslateException {
        if (NUMBER_PATTERN.matcher(text).find()) {
            return CACHE_MANAGER.handleNumericTranslation(text, source);
        }
        return CACHE_MANAGER.getOrLoadTranslation(text, source);
    }

    public static ResRegion[] ocrtrans(byte[] img) throws TranslateException {
        return ocrtrans(getCurrent(), img, getFrom(), getTo());
    }

    public static ResRegion[] ocrtrans(Translator translator, byte[] img, String from, String to) throws TranslateException {
        checkTranslator(translator);
        LOGGER.info("{} ocrtrans, from {} to {}", translator, from, to);
        return performOcrTranslation(translator, img, from, to);
    }

    public static void clearCache() {
        LOGGER.info("Clearing translation cache");
        CACHE_MANAGER.clearCache();
    }

    @Nullable
    public static String getCache(String text, String source) {
        return CACHE_MANAGER.getCache(text, source);
    }

    public static boolean shouldSkipTranslation(String text) {
        return shouldSkipTranslation(getCurrent(), text, getTo());
    }

    public static boolean shouldSkipTranslation(Translator translator, String text, String to) {
        Predicate<String> predicate = Language.getPredicate(Language.getLeftLang(translator.getLanguageType(), to));
        return StringUtil.isBlank(text) ||
            StringUtils.isNumeric(text) ||
            predicate.test(text);
    }
    // ======================== 私有方法 ========================

    private static void checkTranslator(Translator translator) throws TranslateException {
        if (translator == null) {
            throw new NoTranslatorException();
        }
        if (!translator.isConfigured()) {
            throw new NotConfiguredException(translator);
        }
    }

    private static String performTranslation(String text, Translator translator,
                                             String from, String to, String source) throws TranslateException {
        try {
            boolean shouldProceed = TranslateEvent.BEGIN.invoker().begin(text, from, to, source);
            if (!shouldProceed) {
                throw new TranslateException("Translation aborted by event handler");
            }

            String translated = translator.translate(text, from, to, source);
            translated = TranslateEvent.AFTER.invoker().after(text, translated, from, to, source);

            logTranslationSuccess(translator, from, to, source, text, translated);
            return translated;
        } catch (Exception e) {
            logTranslationError(translator, from, to, text, e);
            if (e instanceof TranslateException) {
                throw (TranslateException) e;
            }
            throw new TranslateException(e);
        }
    }

    private static ResRegion[] performOcrTranslation(Translator translator, byte[] img,
                                                     String from, String to) throws TranslateException {
        try {
            return translator.ocrtrans(img, from, to);
        } catch (Exception e) {
            LOGGER.error("{} ocrtrans from {} to {} failed:", translator, from, to, e);
            if (e instanceof TranslateException) {
                throw (TranslateException) e;
            }
            throw new TranslateException(e);
        }
    }

    private static void logTranslationSuccess(Translator translator, String from, String to,
                                              String source, String original, String translated) {
        LOGGER.info("{} translated from {} to {} (source: {}): \"{}\" -> \"{}\"",
            translator.getName(), from, to, source,
            StringUtil.getOutString(original),
            StringUtil.getOutString(translated));
    }

    private static void logTranslationError(Translator translator, String from,
                                            String to, String text, Exception e) {
        LOGGER.error("{} translation from {} to {} failed for text: \"{}\"",
            translator.getName(), from, to,
            StringUtil.getOutString(text), e);
    }

    // ======================== 缓存管理内部类 ========================
    private static class CacheManager {
        private record TextKey(String text, String source) {}

        private final LoadingCache<TextKey, String> translationCache =
            CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull String load(@NotNull TextKey key) throws TranslateException {
                        return TranslateService.translate(
                            key.text(),
                            getCurrent(),
                            getFrom(),
                            getTo(),
                            key.source()
                        );
                    }
                });

        String getOrLoadTranslation(String text, String source) throws TranslateException {
            try {
                return translationCache.get(new TextKey(text, source));
            } catch (ExecutionException e) {
                handleCacheException(e);
                return ""; // 不会执行到此处
            }
        }

        String handleNumericTranslation(String text, String source) throws TranslateException {
            TextKey key = new TextKey(text, source);
            String cached = getStructuredCachedResult(text, source);
            if (cached != null) return cached;

            return processNewNumericTranslation(text, source, key);
        }

        @Nullable
        String getCache(String text, String source) {
            return translationCache.getIfPresent(new TextKey(text, source));
        }

        private String getStructuredCachedResult(String text, String source) {
            char placeholder = findPlaceholder(text);
            String maskedText = NUMBER_PATTERN.matcher(text).replaceAll(String.valueOf(placeholder));

            String cachedTemplate = translationCache.getIfPresent(new TextKey(maskedText, source));
            if (cachedTemplate == null) return null;

            return restoreNumbersFromTemplate(text, cachedTemplate, placeholder);
        }

        private String processNewNumericTranslation(String text, String source, TextKey key)
            throws TranslateException {
            try {
                char placeholder = findPlaceholder(text);
                String maskedText = NUMBER_PATTERN.matcher(text).replaceAll(String.valueOf(placeholder));

                String translated = translationCache.get(key);
                cacheValidStructure(text, source, maskedText, translated, placeholder);

                return translated;
            } catch (ExecutionException e) {
                handleCacheException(e);
                return ""; // 不会执行到此处
            }
        }

        private void cacheValidStructure(String original, String source, String masked,
                                         String translated, char placeholder) {
            List<String> originalNumbers = extractNumbers(original);
            List<String> translatedNumbers = extractNumbers(translated);

            if (originalNumbers.equals(translatedNumbers)) {
                String maskedTranslation = NUMBER_PATTERN.matcher(translated)
                    .replaceAll(String.valueOf(placeholder));
                translationCache.put(new TextKey(masked, source), maskedTranslation);
            }
        }

        private char findPlaceholder(String text) {
            char placeholder = 0;
            while (text.indexOf(placeholder) >= 0) placeholder++;
            return placeholder;
        }

        private List<String> extractNumbers(String text) {
            return NUMBER_PATTERN.matcher(text)
                .results()
                .map(MatchResult::group)
                .toList();
        }

        private String restoreNumbersFromTemplate(String original, String template, char placeholder) {
            List<String> numbers = extractNumbers(original);
            String result = template;
            for (String number : numbers) {
                result = result.replaceFirst(String.valueOf(placeholder), number);
            }
            return result;
        }

        private void handleCacheException(ExecutionException e) throws TranslateException {
            Throwable cause = e.getCause();
            if (cause instanceof TranslateException) {
                throw (TranslateException) cause;
            }
            throw new TranslateException(cause);
        }

        void clearCache() {
            translationCache.invalidateAll();
        }
    }
}