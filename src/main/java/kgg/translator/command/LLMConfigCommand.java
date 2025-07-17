package kgg.translator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import kgg.translator.LLMManager;
import kgg.translator.TranslatorConfig;
import kgg.translator.TranslatorManager;
import kgg.translator.translator.Translator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LLMConfigCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal("llm")
            .then(ClientCommandManager.literal("list")
                .executes(LLMConfigCommand::listModels))
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .then(ClientCommandManager.argument("url", StringArgumentType.string())
                        .then(ClientCommandManager.argument("model", StringArgumentType.string())
                            .then(ClientCommandManager.argument("apikey", StringArgumentType.string())
                                .executes(LLMConfigCommand::addModel))))))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("name", LLMModelArgumentType.llmModel())
                    .executes(LLMConfigCommand::removeModel)))
            .then(ClientCommandManager.literal("edit")
                .then(ClientCommandManager.argument("name", LLMModelArgumentType.llmModel())
                    .then(ClientCommandManager.literal("url")
                        .then(ClientCommandManager.argument("value", StringArgumentType.string())
                            .executes(ctx -> editModel(ctx, "url"))))
                    .then(ClientCommandManager.literal("model")
                        .then(ClientCommandManager.argument("value", StringArgumentType.string())
                            .executes(ctx -> editModel(ctx, "model"))))
                    .then(ClientCommandManager.literal("apikey")
                        .then(ClientCommandManager.argument("value", StringArgumentType.string())
                            .executes(ctx -> editModel(ctx, "apikey"))))))
            .then(ClientCommandManager.literal("use")
                .then(ClientCommandManager.argument("name", LLMModelArgumentType.llmModel())
                    .executes(LLMConfigCommand::useModel)))
            .then(ClientCommandManager.literal("builtin")
                .executes(LLMConfigCommand::showBuiltinModels))
            .then(ClientCommandManager.literal("test")
                .then(ClientCommandManager.argument("name", LLMModelArgumentType.llmModel())
                    .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(LLMConfigCommand::testModel))));
        
        dispatcher.register(command);
        
        // 注册到主命令系统
        dispatcher.register(ClientCommandManager.literal("transconfig")
            .then(ClientCommandManager.literal("llm")
                .redirect(command.build())));
    }
    
    private static int listModels(CommandContext<FabricClientCommandSource> context) {
        Map<String, LLMManager.Model> models = LLMManager.getModels();
        
        if (models.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("没有配置任何LLM模型").formatted(Formatting.YELLOW));
            context.getSource().sendFeedback(Text.literal("使用 /llm builtin 查看内置模型").formatted(Formatting.GRAY));
            return 0;
        }
        
        context.getSource().sendFeedback(Text.literal("已配置的LLM模型:").formatted(Formatting.GREEN));
        
        for (Map.Entry<String, LLMManager.Model> entry : models.entrySet()) {
            LLMManager.Model model = entry.getValue();
            boolean isCurrentModel = TranslatorManager.getCurrent() != null && 
                                   TranslatorManager.getCurrent().getName().equals(model.name);
            
            Text modelText = Text.literal("- " + model.name)
                .formatted(isCurrentModel ? Formatting.GOLD : Formatting.YELLOW)
                .append(isCurrentModel ? Text.literal(" [当前使用]").formatted(Formatting.GREEN) : Text.empty());
            
            Text detailsText = Text.literal("\n  URL: ").formatted(Formatting.GRAY)
                .append(Text.literal(model.url).formatted(Formatting.WHITE))
                .append(Text.literal("\n  Model: ").formatted(Formatting.GRAY))
                .append(Text.literal(model.model).formatted(Formatting.WHITE))
                .append(Text.literal("\n  API Key: ").formatted(Formatting.GRAY))
                .append(Text.literal(model.apiKey.isEmpty() ? "[未设置]" : "[已设置]")
                    .formatted(model.apiKey.isEmpty() ? Formatting.RED : Formatting.GREEN));
            
            modelText = modelText.setStyle(Style.EMPTY
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, detailsText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/llm use " + model.name)));
            
            context.getSource().sendFeedback(modelText);
        }
        
        return models.size();
    }
    
    private static int addModel(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        String url = StringArgumentType.getString(context, "url");
        String model = StringArgumentType.getString(context, "model");
        String apiKey = StringArgumentType.getString(context, "apikey");
        
        LLMManager.Model newModel = new LLMManager.Model(name, url, model, apiKey);
        LLMManager.addModel(newModel);
        TranslatorConfig.writeFile();
        
        context.getSource().sendFeedback(
            Text.literal("成功添加LLM模型: " + name).formatted(Formatting.GREEN)
        );
        
        context.getSource().sendFeedback(
            Text.literal("使用 ").formatted(Formatting.GRAY)
                .append(Text.literal("/llm use " + name).formatted(Formatting.AQUA)
                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/llm use " + name))))
                .append(Text.literal(" 切换到此模型").formatted(Formatting.GRAY))
        );
        
        return 1;
    }
    
    private static int removeModel(CommandContext<FabricClientCommandSource> context) {
        String name = LLMModelArgumentType.getLLMModel(context, "name");
        
        if (LLMManager.removeModel(name)) {
            TranslatorConfig.writeFile();
            context.getSource().sendFeedback(
                Text.literal("成功删除LLM模型: " + name).formatted(Formatting.GREEN)
            );
            return 1;
        } else {
            context.getSource().sendError(
                Text.literal("删除失败: 未找到模型 " + name).formatted(Formatting.RED)
            );
            return 0;
        }
    }
    
    private static int editModel(CommandContext<FabricClientCommandSource> context, String field) {
        String name = LLMModelArgumentType.getLLMModel(context, "name");
        String value = StringArgumentType.getString(context, "value");
        
        LLMManager.Model model = LLMManager.getModels().get(name);
        if (model == null) {
            context.getSource().sendError(Text.literal("未找到模型: " + name));
            return 0;
        }
        
        switch (field) {
            case "url":
                model.url = value;
                break;
            case "model":
                model.model = value;
                break;
            case "apikey":
                model.apiKey = value;
                break;
        }
        
        // 重新添加以更新翻译器
        LLMManager.addModel(model);
        TranslatorConfig.writeFile();
        
        context.getSource().sendFeedback(
            Text.literal("成功更新 " + name + " 的 " + field + " 为: " + value).formatted(Formatting.GREEN)
        );
        
        return 1;
    }
    
    private static int useModel(CommandContext<FabricClientCommandSource> context) {
        String name = LLMModelArgumentType.getLLMModel(context, "name");
        
        // 查找对应的翻译器
        for (Translator translator : TranslatorManager.getTranslators()) {
            if (translator.getName().equals(name)) {
                TranslatorManager.setTranslator(translator);
                TranslatorConfig.writeFile();
                
                context.getSource().sendFeedback(
                    Text.literal("已切换到LLM模型: " + name).formatted(Formatting.GREEN)
                );
                
                if (!translator.isConfigured()) {
                    context.getSource().sendFeedback(
                        Text.literal("警告: 该模型未完全配置，请检查API Key等设置").formatted(Formatting.YELLOW)
                    );
                }
                
                return 1;
            }
        }
        
        context.getSource().sendError(Text.literal("未找到对应的翻译器: " + name));
        return 0;
    }
    
    private static int showBuiltinModels(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("内置LLM模型模板:").formatted(Formatting.GREEN));
        
        for (LLMManager.Model model : LLMManager.geBuiltInModels()) {
            Text modelText = Text.literal("- " + model.name).formatted(Formatting.YELLOW)
                .append(Text.literal(" (点击添加)").formatted(Formatting.GRAY));
            
            String addCommand = String.format("/llm add %s \"%s\" \"%s\" YOUR_API_KEY", 
                model.name.replace(" ", "_"), model.url, model.model);
            
            Text detailsText = Text.literal("URL: " + model.url + "\n")
                .append("Model: " + model.model + "\n")
                .append("点击后需要替换 YOUR_API_KEY 为实际的API密钥");
            
            modelText = modelText.setStyle(Style.EMPTY
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, detailsText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, addCommand)));
            
            context.getSource().sendFeedback(modelText);
        }
        
        return 1;
    }
    
    private static int testModel(CommandContext<FabricClientCommandSource> context) {
        String name = LLMModelArgumentType.getLLMModel(context, "name");
        String text = StringArgumentType.getString(context, "text");
        
        // 查找对应的翻译器
        Translator translator = null;
        for (Translator t : TranslatorManager.getTranslators()) {
            if (t.getName().equals(name)) {
                translator = t;
                break;
            }
        }
        
        if (translator == null) {
            context.getSource().sendError(Text.literal("未找到对应的翻译器: " + name));
            return 0;
        }
        
        if (!translator.isConfigured()) {
            context.getSource().sendError(Text.literal("该模型未完全配置"));
            return 0;
        }
        
        context.getSource().sendFeedback(Text.literal("正在测试模型 " + name + "...").formatted(Formatting.YELLOW));
        
        Translator finalTranslator = translator;
        CompletableFuture.runAsync(() -> {
            try {
                String result = finalTranslator.translate(text, "auto", "zh-cn", "test");
                context.getSource().sendFeedback(
                    Text.literal("测试成功！").formatted(Formatting.GREEN)
                );
                context.getSource().sendFeedback(
                    Text.literal("原文: ").formatted(Formatting.GRAY)
                        .append(Text.literal(text).formatted(Formatting.WHITE))
                );
                context.getSource().sendFeedback(
                    Text.literal("译文: ").formatted(Formatting.GRAY)
                        .append(Text.literal(result).formatted(Formatting.AQUA))
                );
            } catch (Exception e) {
                context.getSource().sendError(
                    Text.literal("测试失败: " + e.getMessage()).formatted(Formatting.RED)
                );
            }
        });
        
        return 1;
    }
    
    /**
     * LLM模型参数类型
     */
    public static class LLMModelArgumentType implements com.mojang.brigadier.arguments.ArgumentType<String> {
        
        public static LLMModelArgumentType llmModel() {
            return new LLMModelArgumentType();
        }
        
        @Override
        public String parse(com.mojang.brigadier.StringReader reader) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            return reader.readQuotedString();
        }
        
        public static String getLLMModel(CommandContext<FabricClientCommandSource> context, String name) {
            return context.getArgument(name, String.class);
        }
        
        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            if (context.getSource() instanceof CommandSource) {
                List<String> modelNames = LLMManager.getModels().keySet().stream()
                    .map(name -> "\"" + name + "\"")
                    .toList();
                return CommandSource.suggestMatching(modelNames, builder);
            }
            return Suggestions.empty();
        }
    }
}