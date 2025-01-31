package kgg.translator;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import kgg.translator.event.TranslateEvent;
import kgg.translator.exception.NoTranslatorException;
import kgg.translator.exception.NotConfiguredException;
import kgg.translator.exception.TranslateException;
import kgg.translator.ocrtrans.ResRegion;
import kgg.translator.option.Options;
import kgg.translator.translator.Source;
import kgg.translator.translator.Translator;
import kgg.translator.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import static kgg.translator.TranslatorManager.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translate {
    private static final Logger LOGGER = LogManager.getLogger(Translate.class);
    private record Text(String text, String source) {}

    private static final LoadingCache<Text, String> cache = CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<>() {
        @Override
        public @NotNull String load(@NotNull Text key) throws Exception {
            return translate(key.text, key.source);
        }
    });

    private static Text createText(String text, String source) {
        if (!Options.markSources.getValue()) {
            source = Source.UNKNOWN;
        }
        return new Text(text, source);
    }

    private static final Pattern compile = Pattern.compile("[-+]?\\d*\\.?\\d+");
    private static String cacheTranslate(Text text) throws TranslateException {
        try {
            return cache.get(text);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TranslateException c) {
                throw c;
            } else {
                throw new TranslateException(e.getCause());
            }
        }
    }

    public static String cachedTranslate(String text, String source) throws TranslateException {
        boolean isNumber = compile.matcher(text).find();
        Text key = createText(text, source);
        if (!isNumber) {  // 没有数字，直接翻译
            return cacheTranslate(key);
        }
        String result = getFromCache(text, source);
        if (result != null) {  // 结构缓存或直接缓存，直接返回
            return result;
        }
        // 创建一个id，这个字符不会出现在原字符串中，虽然字符串中出现\0的概率极低
        char id = 0;
        while (text.indexOf(id) >= 0) {
            id++;
        }
        // 举例：第3场还有15秒开始，匹配数字得到结果为：[3, 15]
        String replaced = compile.matcher(text).replaceAll(String.valueOf(id));  // 替换为 第\0场还有\0秒开始
        // 翻译
        result = translate(key.text, key.source);  // the 3 ... 15 s
        // 判断原文匹配结果是否和翻译匹配结果一致
        List<String> originalNumbers = compile.matcher(text).results().map(MatchResult::group).toList();
        List<String> translatedNumbers = compile.matcher(result).results().map(MatchResult::group).toList();
        if (!originalNumbers.equals(translatedNumbers)) {
            // 不一致，代表无法缓存这个结构，直接放进缓存
            cache.put(key, result);
            return result;
        } else {
            // 如果一致，创建数字缓存
            Text newKey = createText(replaced, source);  // 第\0场还有\0秒开始
            String value = compile.matcher(result).replaceAll(String.valueOf(id));
            // value = the 0 ... 0 s
            cache.put(newKey, value);
            // 返回翻译结果
            return result;
        }
    }

    public static String getFromCache(String text, String source) {
        boolean isNumber = compile.matcher(text).find();
        if (!isNumber) {  // 不是数字，直接返回
            return cache.getIfPresent(createText(text, source));
        } else {
            // 获得id
            char id = 0;
            while (text.indexOf(id) >= 0) {
                id++;
            }
            // 举例：第3场还有15秒开始，替换为 第\0场还有\0秒开始
            String replaced = compile.matcher(text).replaceAll(String.valueOf(id));
            // 判断替换文是否有 "the \0 ... \0 s" 有这个缓存
            String result = cache.getIfPresent(createText(replaced, source));
            if (result == null) {  // 没有
                return null;
            } else {  // 有
                Matcher matcher = compile.matcher(text);  // 匹配数字得到结果为：[3, 15]
                // 将\0替换回数字 "the \0 ... \0 s" -> "the 3 ... 15 s"
                while (matcher.find()) result = result.replaceFirst(String.valueOf(id), matcher.group());
                return result;
            }
        }
    }

    public static String translate(String text, String source) throws TranslateException {
        return translate(text, getCurrent(), getFrom(), getTo(), source);
    }

    public static String translate(String text, Translator translator, String from, String to, String source) throws TranslateException {
        if (StringUtil.isBlank(text)) return text;
        if (StringUtils.isNumeric(text)) return text;
        if (Language.getPredicate(to).test(text)) return text;
        checkTranslator(translator);

        try {
            boolean begin = TranslateEvent.BEGIN.invoker().begin(text, from, to, source);
            if (!begin) {
                throw new TranslateException("未翻译");
            }
            String translate = translator.translate(text, from, to, source);
            translate = TranslateEvent.AFTER.invoker().after(text, translate, from, to, source);
            LOGGER.info("{} translate from {} to {}: ({})\"{}\" -> \"{}\"", translator, from, to, source, StringUtil.getOutString(text), StringUtil.getOutString(translate));
            return translate;
        } catch (Exception e) {
            LOGGER.error("{} translate from {} to {} failed: \"{}\"", translator, from, to, StringUtil.getOutString(text), e);
            if (e instanceof TranslateException c) {
                throw c;
            } else {
                throw new TranslateException(e);
            }
        }
    }

    public static ResRegion[] ocrtrans(byte[] img) throws TranslateException {
        return ocrtrans(getCurrent(), img, getFrom(), getTo());
    }

    public static ResRegion[] ocrtrans(Translator translator, byte[] img, String from, String to) throws TranslateException {
        checkTranslator(translator);
        LOGGER.info("{} ocrtrans, from {} to {}", translator, from, to);
        try {
            return translator.ocrtrans(img, from, to);
        } catch (Exception e) {
            LOGGER.error("{} ocrtrans, from {} to {} failed:", translator, from, to, e);
            if (e instanceof TranslateException c) {
                throw c;
            } else {
                throw new TranslateException(e);
            }
        }
    }

    private static void checkTranslator(Translator translator) throws TranslateException {
        if (translator == null) {
            throw new NoTranslatorException();
        }
        if (!translator.isConfigured()) {
            throw new NotConfiguredException(translator);
        }
    }

    public static void clearCache() {
        LOGGER.info("Clear cache");
        cache.invalidateAll();
    }
}
