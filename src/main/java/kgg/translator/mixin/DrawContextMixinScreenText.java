package kgg.translator.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 用于工具栏翻译
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixinScreenText {

//    @Shadow public abstract void drawOrderedTooltip(TextRenderer textRenderer, List<? extends OrderedText> text, int x, int y);
//    @Unique
//    private static boolean wrapped;
//
//    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("RETURN"))
//    public void drawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
//        if (Options.screenTranslate.getValue()) {
//            if (!wrapped) {
//                if (text != null) {
//                    int w = textRenderer.getWidth(text);
//                    int h = textRenderer.fontHeight;
//                    handle(textRenderer, List.of(text), x, y, w, h);
//                }
//            }
//        }
//    }
//
//    @Inject(method = "drawTextWrapped", at = @At("HEAD"))
//    public void drawTextWrappedHead(TextRenderer textRenderer, StringVisitable text, int x, int y, int width, int color, CallbackInfo ci) {
//        if (Options.screenTranslate.getValue()) {
//            wrapped = true;
//        }
//    }
//
//    @Inject(method = "drawTextWrapped", at = @At("TAIL"))
//    public void drawTextWrappedTAIL(TextRenderer textRenderer, StringVisitable text, int x, int y, int width, int color, CallbackInfo ci) {
//        if (Options.screenTranslate.getValue()) {
//            wrapped = false;
//            int w = 0;
//            int h = 0;
//            List<OrderedText> texts = textRenderer.wrapLines(text, width);
//            for (OrderedText orderedText : texts) {
//                w = Math.max(w, textRenderer.getWidth(orderedText));
//                h += textRenderer.fontHeight;
//                y += textRenderer.fontHeight;
//            }
//            handle(textRenderer, texts, x, y, w, h);
//        }
//    }
//
//    /**
//     * 如果鼠标选中，与物品一样，在旁边显示译文
//     */
//    @Unique
//    private void handle(TextRenderer textRenderer, List<OrderedText> texts, int x, int y, int w, int h) {
//        List<Text> t = texts.stream().map(text -> (Text) Text.literal(TextUtil.getString(text))).toList();
//
//        MinecraftClient client = MinecraftClient.getInstance();
//        int mouseX = (int)(client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth());
//        int mouseY = (int)(client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight());
//        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
//            TipHandler.handle((DrawContext)(Object)this, t, mouseX, mouseY, 0.2f);
//            if (TipHandler.drawTranslateText) {
//                TipHandler.drawTranslateText = false;  // 防止触发DrawContextMixinTooltip的方法
//                drawOrderedTooltip(textRenderer, List.of(TipHandler.getTranslatedOrderedText()), mouseX, mouseY);
//            }
//        }
//    }
}
