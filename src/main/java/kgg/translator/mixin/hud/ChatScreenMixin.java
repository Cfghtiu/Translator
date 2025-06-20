package kgg.translator.mixin.hud;

import kgg.translator.handler.ChatHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow protected TextFieldWidget chatField;

    @Shadow public abstract void sendMessage(String chatText, boolean addToHistory);

    @Inject(method = "init", at = @At(value = "HEAD"))
    public void init(CallbackInfo ci) {
        ChatHandler.addTip();
    }


    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
//        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hasShiftDown()) {
//            // 翻译选中内容
//            String text = chatField.getText();
//            client.setScreen(null);
//
//            CompletableFuture.runAsync(() -> {
//                try {
//                    String result = TranslateService.cachedTranslate(text, Source.CHAT);
//                    Text msg = Text.empty().setStyle(Style.EMPTY.withInsertion(text)
//                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, text))
//                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击输出"))))
//                        .append(
//                            Text.literal("[翻译结果] ")
//                                .setStyle(Style.EMPTY
//                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(text)))
//                                    .withColor(Formatting.GREEN))
//                        )
//                        .append(Text.literal(result)
//                            .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
//                    client.player.sendMessage(msg, false);
//                } catch (TranslateException e) {
//                    client.player.sendMessage(Text.literal("[翻译失败] ").setStyle(Style.EMPTY.withColor(Formatting.RED))
//                        .append(Text.literal(e.getMessage())), false);
//                }
//            });
//            cir.setReturnValue(true);
//        }
    }


    @Override
    public void removed() {
        ChatHandler.removeTip();
        super.removed();
    }
}
