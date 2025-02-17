package kgg.translator.mixin;

import kgg.translator.handler.TipHandler;
import kgg.translator.option.Options;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用于工具栏翻译
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixinTooltip {
    @Shadow protected abstract void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner);

    @Unique
    @Deprecated public abstract void draw(Runnable drawCallback);

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"))
    public void drawTooltip(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y, CallbackInfo ci) {
        if (Options.autoTooltip.getValue()) {
            TipHandler.handle((DrawContext) (Object) this, text, x, y, 0.4f);
        }
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"))
    public void drawTooltip(TextRenderer textRenderer, List<Text> text, int x, int y, CallbackInfo ci) {
        if (Options.autoTooltip.getValue()) {
            TipHandler.handle((DrawContext) (Object) this, text, x, y, 0.4f);
        }
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;II)V", at = @At("RETURN"))
    public void drawTooltip(TextRenderer textRenderer, Text text, int x, int y, CallbackInfo ci) {
        if (Options.autoTooltip.getValue()) {
            TipHandler.handle((DrawContext) (Object) this, List.of(text), x, y, 0.4f);
        }
    }


    @Unique
    private static TextRenderer textRenderer;
    @Unique
    private static TooltipPositioner positioner;

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At("HEAD"))
    public void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
        DrawContextMixinTooltip.textRenderer = textRenderer;
        DrawContextMixinTooltip.positioner = positioner;
    }

    /**
     * 重定向getPosition可以方便的获得屏幕大小
     */
    @Redirect(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/tooltip/TooltipPositioner;getPosition(IIIIII)Lorg/joml/Vector2ic;"))
    public Vector2ic getPosition(TooltipPositioner instance, int screenWidth, int screenHeight, int x, int y, int width, int height) {
        // 原位置
        Vector2ic position = instance.getPosition(screenWidth, screenHeight, x, y, width, height);
        // 如果没有翻译文本，则直接返回原位置
        if (!TipHandler.drawTranslateText) {
            return position;
        } else {
            List<TooltipComponent> components = Arrays.stream(TipHandler.getTranslatedOrderedText()).map(TooltipComponent::of).toList();
            TipHandler.drawTranslateText = false;  // 反正下面重新调用此方法再次执行到这里
            if (!components.isEmpty()) {
                // 计算翻译文本的矩阵大小
                int translatedRectWidth = 0;
                int translatedRectHeight = components.size() == 1 ? -2 : 0;
                for (TooltipComponent tooltipComponent : components) {
                    int k = tooltipComponent.getWidth(textRenderer);
                    if (k > translatedRectWidth) {
                        translatedRectWidth = k;
                    }
                    translatedRectHeight += tooltipComponent.getHeight();
                }

                /*显示工具栏逻辑如下
                * 尝试让原文和译文保存在同一行
                * 但是如果译文的宽度+超过屏幕宽度，则会自动变到左边
                * 所以要让译文在上下行
                * */

                if (position.x() + width + translatedRectWidth + 12 > screenWidth) {
                    if (y + 12 + height + 3 > screenHeight) {
                        // -12是与原文x对称
                        drawTooltip(textRenderer, components, position.x() - 12, y + height + 1 + 12, positioner);
                    } else {
                        drawTooltip(textRenderer, components, position.x() - 12, y + height + 1 + 12, positioner);
                    }
                    return position;
                } else {
                    // 返回加上翻译文本的总体尺寸
                    Vector2ic newPosition = instance.getPosition(screenWidth, screenHeight, x, y, width + translatedRectWidth + 1, Math.max(translatedRectHeight, height));
                    // 渲染
                    drawTooltip(textRenderer, components, newPosition.x() + width + 1, y, positioner);
                    return newPosition;
                }
            } else {
                return position;
            }
        }
    }
}
