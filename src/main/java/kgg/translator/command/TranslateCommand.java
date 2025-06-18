package kgg.translator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import kgg.translator.TranslateService;
import kgg.translator.TranslatorManager;
import kgg.translator.exception.TranslateException;
import kgg.translator.translator.Source;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public class TranslateCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("translate")
                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(context -> {
                            translate(context, StringArgumentType.getString(context, "text"), TranslatorManager.getFrom(), TranslatorManager.getTo());
                            return 0;
                        })));
        // 反向翻译
        dispatcher.register(ClientCommandManager.literal("translate-re")
                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(context -> {
                            translate(context, StringArgumentType.getString(context, "text"), TranslatorManager.getTo(), TranslatorManager.getFrom());
                            return 0;
                        })));
    }

    private static void translate(CommandContext<FabricClientCommandSource> context, String text, String from, String to) {
        CompletableFuture.runAsync(() -> {
            Text message;
            try {
                String result = TranslateService.translate(text, TranslatorManager.getCurrent(), from, to, Source.CHAT);
                message = Text.literal("[结果] " +result).setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, result))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击复制"))));
            } catch (TranslateException e) {
                message = Text.literal("[错误] " + e.getMessage()).formatted(Formatting.RED);
            }
            Text finalMessage = message;
            MinecraftClient.getInstance().execute(() -> {
                try {
                    context.getSource().sendFeedback(finalMessage);
                } catch (Exception ignore) {}
            });
        });
    }
}
