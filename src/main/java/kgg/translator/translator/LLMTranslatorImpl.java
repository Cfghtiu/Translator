package kgg.translator.translator;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class LLMTranslatorImpl extends LLMTranslator {

    public LLMTranslatorImpl(String name, String url) {
        super(name, url);
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> node) {
    }
}
