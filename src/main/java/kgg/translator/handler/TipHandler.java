package kgg.translator.handler;

import kgg.translator.TranslateService;
import kgg.translator.exception.TranslateException;
import kgg.translator.translator.Source;
import kgg.translator.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;


public class TipHandler {
    private static final Logger LOGGER = LogManager.getLogger(TipHandler.class);

    private static boolean drawTranslateText = false;
    private static boolean needTranslate = false;
    private static boolean handleAfter = false;
    private static OrderedText[] translatedOrderedText;
    private static List<Text> lastText;
    private static long time = 0;
    private static boolean isTranslated = false;

    public static boolean isDrawTranslateText() {
        return drawTranslateText;
    }

    public static boolean isNeedTranslate() {  // 必在调用
        return needTranslate;
    }

    public static boolean isHandleAfter() {
        if (handleAfter) {
            handleAfter = false;
            return true;
        }
        return false;
    }

    public static void handle(DrawContext drawContext, List<Text> text, int mouseX, int mouseY, float delayTime) {
        handleAfter = true;
        if (!text.equals(lastText)) {  // 文本改变，重置状态
            resetState(text);
            return;
        }
        lastText = text;
        if (System.currentTimeMillis() > time + (int) (delayTime * 1000)) {  // 开始翻译
            if (!isTranslated) {
                isTranslated = true;
                if (text.stream().filter(t -> TextUtil.isSystemText(t) || TranslateService.shouldSkipTranslation(t.getString())).count() == text.size()) {  // 全是系统文本，不用翻译
                    return;
                }
                needTranslate = true;
                startTranslation(text);
            }
        }
    }

    private static void resetState(List<Text> text) {
        time = System.currentTimeMillis();
        lastText = text;
        isTranslated = false;
        drawTranslateText = false;
        needTranslate = false;
    }

    private static void startTranslation(List<Text> texts) {
        // 拼接文本，一次翻译
        StringJoiner joiner = new StringJoiner("\n");
        texts.forEach(text -> {joiner.add(TextUtil.getString(text));});
        String text = joiner.toString();

        CompletableFuture.supplyAsync(() -> {
            try {
                return TranslateService.cachedTranslate(text, Source.TOOLTIP);
            } catch (TranslateException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Translation failed", throwable);
            return "翻译失败";
        }).thenAccept(translated -> {
            if (translated.equals(text)) {
                // 一模一样就不用翻译
                needTranslate = false;
                return;
            }
            // 拆开文本存放
            String[] translatedLines = translated.split("\n");
            if (translatedLines.length == 1 && texts.size() != 1) {  // 谁家翻译器不换行
                translatedOrderedText = MinecraftClient.getInstance().textRenderer.wrapLines(Text.of(translated), 120).toArray(OrderedText[]::new);
            } else {
                translatedOrderedText =  new OrderedText[translatedLines.length];
                for (int i = 0; i < translatedLines.length; i++) {
                    translatedOrderedText[i] = TextUtil.toText(translatedLines[i], texts.get(i)).asOrderedText();
                }
            }
            drawTranslateText = true;
        });
    }

    public static OrderedText[] getTranslatedOrderedText() {
        return translatedOrderedText;
    }

    public record SidebarEntry(Text name, Text score, int scoreWidth) {
    }
}