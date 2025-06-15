package kgg.translator.mixin;

import kgg.translator.handler.TipHandler;
import kgg.translator.option.Options;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
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

    @Shadow protected abstract void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, @Nullable Identifier texture);

    @Unique
    @Deprecated public abstract void draw(Runnable drawCallback);

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    public void drawTooltip(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y, Identifier texture, CallbackInfo ci) {
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

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    public void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, @Nullable Identifier texture, CallbackInfo ci) {
        DrawContextMixinTooltip.textRenderer = textRenderer;
        DrawContextMixinTooltip.positioner = positioner;
    }


    @Unique
    private boolean firstCall = true;  // 防止重复调用

    /**
     * 重定向 getPosition 以在原文 Tooltip 旁边显示翻译内容
     */
    @Redirect(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/tooltip/TooltipPositioner;getPosition(IIIIII)Lorg/joml/Vector2ic;"))
    public Vector2ic redirectGetPosition(TooltipPositioner instance, int screenWidth, int screenHeight, int x, int y, int width, int height) {
        // 原 Tooltip 的位置
        Vector2ic position = instance.getPosition(screenWidth, screenHeight, x, y, width, height);

        // 如果不需要翻译或非首次调用，直接返回原位置
        if (!firstCall || !TipHandler.isNeedTranslate()) {
            return position;
        }

        // 获取翻译文本组件
        List<TooltipComponent> components;
        if (TipHandler.isDrawTranslateText()) {
            components = Arrays.stream(TipHandler.getTranslatedOrderedText())
                .map(TooltipComponent::of)
                .toList();
        } else {
            components = List.of(TooltipComponent.of(Text.literal("...").asOrderedText()));
        }

        firstCall = false;

        try {
            // 计算翻译 Tooltip 尺寸
            int translatedWidth = 0;
            int translatedHeight = 0;
            for (TooltipComponent comp : components) {
                int w = comp.getWidth(textRenderer);
                int h = comp.getHeight(textRenderer);
                translatedWidth = Math.max(translatedWidth, w);
                translatedHeight += h;
            }

            // 尝试放置的位置
            int newX;
            int newY;

            // 优先尝试右侧
            if (position.x() + width + translatedWidth + 12 <= screenWidth) {
                newX = position.x() + width + 1;
                newY = position.y();
            }
            // 其次尝试左侧
            else if (position.x() - translatedWidth - 12 >= 0) {
                newX = position.x() - translatedWidth - 12;
                newY = position.y();
            }
            // 再尝试下方
            else if (position.y() + height + translatedHeight + 5 <= screenHeight) {
                newX = position.x();
                newY = position.y() + height + 5;
            }
            // 最后尝试上方
            else if (position.y() - translatedHeight - 5 >= 0) {
                newX = position.x();
                newY = position.y() - translatedHeight - 5;
            }
            // 实在没空间就随便放一个地方，优先下方
            else {
                newX = position.x();
                newY = Math.min(position.y() + height + 5, screenHeight - translatedHeight - 10);
            }

            // 绘制裁译文 Tooltip
            drawTooltip(textRenderer, components, newX, newY, positioner, null);

            // 返回原文 Tooltip 的位置
            return position;
        } finally {
            firstCall = true;
        }
    }
}
