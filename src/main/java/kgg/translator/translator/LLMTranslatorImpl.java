package kgg.translator.translator;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class LLMTranslatorImpl extends LLMTranslator {

    public LLMTranslatorImpl(String name, String url) {
        super(name, url);
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> node) {
        node.then(ClientCommandManager.literal("model").then(ClientCommandManager.argument("model", StringArgumentType.word()).then(node)))
            .then(ClientCommandManager.literal("apiKey").then(ClientCommandManager.argument("apiKey", StringArgumentType.word()).then(node)))
            .executes(context -> {
                setConfig(StringArgumentType.getString(context, "apiKey"), StringArgumentType.getString(context, "model"));
                context.getSource().sendFeedback(Text.of("ok"));
                return 0;
            });
    }
}
