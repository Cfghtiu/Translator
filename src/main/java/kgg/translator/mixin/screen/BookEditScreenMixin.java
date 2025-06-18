package kgg.translator.mixin.screen;

import kgg.translator.TranslateService;
import kgg.translator.exception.TranslateException;
import kgg.translator.translator.Source;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;


@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow private boolean signing;
    @Shadow @Final private List<String> pages;
    @Shadow private int currentPage;
    @Shadow @Final private static int MAX_TEXT_WIDTH;
    @Unique
    ButtonWidget translateButton;
    @Unique
    String translateText = null;


    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void init(CallbackInfo ci) {
        // 翻译按钮放到done按钮和cancel按钮的中间的下面
        translateButton = addDrawableChild(ButtonWidget.builder(Text.literal("翻译"), button -> {
            if (!signing) {
                CompletableFuture.runAsync(() -> {
                    try {
                        translateText = TranslateService.cachedTranslate(pages.get(currentPage), Source.BOOK);
                    } catch (TranslateException e) {
                        translateText = e.getMessage();
                    }
                });
            }
        }).dimensions(this.width / 2 + 2 - (98 / 2), 196 + 20 + 4, 98, 20).build());
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!signing && translateText != null) {
            AtomicInteger y = new AtomicInteger(32);
            textRenderer.getTextHandler().wrapLines(translateText, MAX_TEXT_WIDTH, Style.EMPTY, false, (style, start, end) -> {
                Text text = Text.literal(translateText.substring(start, end));
                context.drawText(textRenderer, text, (width + MAX_TEXT_WIDTH) / 2 + 20, y.get(), Colors.WHITE, false);
                y.addAndGet(textRenderer.fontHeight);
            });
        }
    }

    @Inject(method = "changePage", at = @At("HEAD"))
    public void changePage(CallbackInfo ci) {
        translateText = null;
    }
}
