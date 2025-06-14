package kgg.translator.translator;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kgg.translator.LLMManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class LLMTranslatorImpl extends LLMTranslator {


    public LLMTranslatorImpl(LLMManager.Model model) {
        super(model);
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> node) {
    }
}
