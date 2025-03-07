package kgg.translator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import kgg.translator.Translate;
import kgg.translator.TranslatorConfig;
import kgg.translator.TranslatorManager;
import kgg.translator.screen.ConfigJsonScreen;
import kgg.translator.translator.Translator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;

public class TranslateConfigCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("transconfig");

        // /trans-config language <from> [<to>]
        root.then(ClientCommandManager.literal("language")
                .executes(TranslateConfigCommand::queryLanguage)
                .then(ClientCommandManager.argument("from", LangArgumentType.lang())
                        .executes(context -> {
                            String from = LangArgumentType.getLanguage(context, "from");
                            TranslatorManager.setFrom(from);
                            TranslatorConfig.writeFile();
                            return queryLanguage(context);
                        })
                        .then(ClientCommandManager.argument("to", LangArgumentType.lang())
                                .executes(context -> {
                                    String from = LangArgumentType.getLanguage(context, "from");
                                    String to = LangArgumentType.getLanguage(context, "to");
                                    TranslatorManager.setFrom(from);
                                    TranslatorManager.setTo(to);
                                    TranslatorConfig.writeFile();
                                    return queryLanguage(context);
                                }))));
        // /trans-config translator
        LiteralArgumentBuilder<FabricClientCommandSource> selectNode = ClientCommandManager.literal("translator")
                .executes(TranslateConfigCommand::queryTranslator);
        // /trans-config translator <translator> ...
        TranslatorManager.getTranslators().forEach(translator -> {
            LiteralArgumentBuilder<FabricClientCommandSource> subNode = ClientCommandManager.literal(translator.getName())
                    .executes(context -> {
                        boolean b = TranslatorManager.setTranslator(translator);
                        TranslatorConfig.writeFile();
                        int a = queryTranslator(context);
                        if (!b) {
                            context.getSource().sendError(Text.literal("未能自动切换语言，需要手动修改语言"));
                        }
                        return a;
                    });
            translator.register(subNode);
            selectNode.then(subNode);
        });
        root.then(selectNode);

        // /trans-config translator <llm-translator>

        // /trans-config clearcache
        root.then(ClientCommandManager.literal("clearcache")
                .executes(context -> {
                    Translate.clearCache();
                    kgg.translator.handler.TranslateHelper.clearCache();
                    context.getSource().sendFeedback(Text.literal("OK"));
                    return 0;
                }));

        // /trans-config config
        root.then(ClientCommandManager.literal("config").executes(context -> {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new ConfigJsonScreen()));
            return 0;
        }));

        dispatcher.register(root);
    }

    private static int queryLanguage(CommandContext<FabricClientCommandSource> context) {
        Text message = Text.translatable("commands.transconfig.querylanguage", TranslatorManager.getFrom(), TranslatorManager.getTo());
        context.getSource().sendFeedback(message);
        return 0;
    }

    private static int queryTranslator(CommandContext<FabricClientCommandSource> context) {
        Translator translator = TranslatorManager.getCurrent();
        Text message = Text.translatable("commands.transconfig.querytranslator", translator);
        context.getSource().sendFeedback(message);
        if (translator.isConfigured()) {
                message = Text.translatable("commands.transconfig.querytranslator.configed", translator).withColor(0x00ff00);
            } else {
                message = Text.translatable("commands.transconfig.querytranslator.unconfiged", translator).withColor(0xff0000);
            }
            context.getSource().sendFeedback(message);
        return 0;
    }

}
