package kgg.translator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import kgg.translator.LLMManager;
import kgg.translator.Translate;
import kgg.translator.TranslatorConfig;
import kgg.translator.TranslatorManager;
import kgg.translator.screen.ConfigJsonScreen;
import kgg.translator.translator.LLMTranslator;
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

        // /trans-config ai-model <add|remove|list>
        root.then(ClientCommandManager.literal("ai-model")
            .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .then(ClientCommandManager.argument("url", StringArgumentType.string())
                            .executes(context -> {
                                LLMManager.Model model = new LLMManager.Model(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "url"), null);
                                LLMManager.addModel(model);
                                context.getSource().sendFeedback(Text.translatable("commands.transconfig.addmodel.add"));
                                return 0;
                            })
                            .then(ClientCommandManager.argument("model", StringArgumentType.string())
                                .executes(context -> {
                                    LLMManager.Model model = new LLMManager.Model(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "url"), StringArgumentType.getString(context, "model"));
                                    LLMManager.addModel(model);
                                    context.getSource().sendFeedback(Text.translatable("commands.transconfig.addmodel.add"));
                                    return 0;
                                })))))
            .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("name", LLMTranslatorArgumentType.translator())
                            .executes(context -> {
                                if (LLMManager.removeModel(LLMTranslatorArgumentType.getTranslator(context, "name").getName())) {
                                    context.getSource().sendFeedback(Text.literal("OK"));
                                } else {
                                    context.getSource().sendFeedback(Text.translatable("commands.transconfig.addmodel.remove.fail"));
                                }
                                return 0;
                            })))
           .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        TranslatorManager.getTranslators().forEach(translator -> {
                            if (translator instanceof LLMTranslator llmTranslator) {
                                // name(url) model
                                context.getSource().sendFeedback(Text.literal("%s(%s) %s".formatted(llmTranslator.getName(), llmTranslator.getUrl(), llmTranslator.getModel())));
                            }
                        });
                        return 0;
                    }))
            .then(ClientCommandManager.literal("reload-prompt").executes(context -> {
                LLMManager.init();
                context.getSource().sendFeedback(Text.literal("OK"));
                return 0;
            }))
        );

        // /trans-config ai-translator <translator> ...
        LiteralArgumentBuilder<FabricClientCommandSource> ai = ClientCommandManager.literal("ai-translator");
        buildLLMTranslatorCommand(ai);
        root.then(ai);

        // /trans-config translator
        LiteralArgumentBuilder<FabricClientCommandSource> selectNode = ClientCommandManager.literal("translator")
            .executes(TranslateConfigCommand::queryTranslator);

        // /trans-config translator <translator> ...
        TranslatorManager.getTranslators().forEach(translator -> {
            if (translator instanceof LLMTranslator) {
                return;
            }
            LiteralArgumentBuilder<FabricClientCommandSource> subNode = ClientCommandManager.literal(translator.getName())
                    .executes(context -> selectTranslator(context, translator));
            translator.register(subNode);
            selectNode.then(subNode);
        });

        root.then(selectNode);
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

    private static void buildLLMTranslatorCommand(LiteralArgumentBuilder<FabricClientCommandSource> ai) {
        ai.executes(TranslateConfigCommand::queryTranslator)
            .then(ClientCommandManager.argument("translator", LLMTranslatorArgumentType.translator())
                .executes(context -> {
                    selectTranslator(context, LLMTranslatorArgumentType.getTranslator(context, "translator"));
                    return 0;
                })
                .then(ClientCommandManager.argument("model", StringArgumentType.string())
                    .executes(context -> {
                        LLMTranslator translator = LLMTranslatorArgumentType.getTranslator(context, "translator");
                        translator.setConfig(null, StringArgumentType.getString(context, "model"));
                        context.getSource().sendFeedback(Text.literal("OK"));
                        return 0;
                    })
                    .then(ClientCommandManager.argument("api-key", StringArgumentType.string())
                        .executes(context -> {
                            LLMTranslator translator = LLMTranslatorArgumentType.getTranslator(context, "translator");
                            translator.setConfig(StringArgumentType.getString(context, "api-key"), StringArgumentType.getString(context, "model"));
                            context.getSource().sendFeedback(Text.literal("OK"));
                            return 0;
                        }))
                ));
    }

    private static int selectTranslator(CommandContext<FabricClientCommandSource> context, Translator translator) {
        boolean b = TranslatorManager.setTranslator(translator);
        TranslatorConfig.writeFile();
        int a = queryTranslator(context);
        if (!b) {
            context.getSource().sendError(Text.translatable("commands.transconfig.querytranslator.unsupported", translator));
        }
        return a;
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
