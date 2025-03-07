package kgg.translator.translator;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import kgg.translator.translator.Translator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class LLMTranslatorImpl extends LLMTranslator {
    public LLMTranslatorImpl(String prompt, String name, String url) {
        super(prompt, name, url);
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> node) {
        // [model] [url] <appKey>
//        node.then(ClientCommandManager.argument("model", StringArgumentType.word())
//            .executes((context) -> {
//                setConfig(apiKey, StringArgumentType.getString(context, "model"));
//                context.getSource().sendFeedback(Text.of("OK"));
//                return 0;
//            })
//            .then(ClientCommandManager.argument("url", StringArgumentType.word())
//                    .then(ClientCommandManager.argument("appKey", StringArgumentType.word())
//                        .executes((context) -> {
//                            setConfig(StringArgumentType.getString(context, "appKey"), StringArgumentType.getString(context, "url"), StringArgumentType.getString(context, "model"));
//                            context.getSource().sendFeedback(Text.of("OK"));
//                            return 0;
//                        }))));
        // <ChatGPT>
//        node.then(ClientCommandManager.literal("ChatGPT")
//            .then(createArgument(, "gpt-3.5-turbo")));
        // <质谱>
//        node.then(ClientCommandManager.literal("质谱")
//            .then(createArgument("https://open.bigmodel.cn/api/paas/v4/", "glm-4-flash")));
        // <kimi>
//        node.then(ClientCommandManager.literal("kimi")
//            .then(createArgument("https://api.moonshot.cn", "moonshot-v1-8k")));
        // <千问>
//        node.then(ClientCommandManager.literal("千问")
//            .then(createArgument("https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen2.5-1.5b-instruct")));
    }

//    private RequiredArgumentBuilder<FabricClientCommandSource, String> createArgument(String url, String model) {
//        return createArgument(url, model, Text.of("OK"));
//    }
//
//    private RequiredArgumentBuilder<FabricClientCommandSource, String> createArgument(String url, String model, Text tip) {
//        return ClientCommandManager.argument("apiKey", StringArgumentType.word())
//            .executes((context) -> {
//                setConfig(StringArgumentType.getString(context, "apiKey"), url, model);
//                context.getSource().sendFeedback(tip);
//                return 0;
//            })
//            .then(ClientCommandManager.argument("model", StringArgumentType.word())
//                .executes((context) -> {
//                    setConfig(StringArgumentType.getString(context, "apiKey"), url, StringArgumentType.getString(context, "model"));
//                    context.getSource().sendFeedback(Text.of("OK"));
//                    return 0;
//                }));
//    }
}
