package kgg.translator.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import kgg.translator.Language;
import kgg.translator.TranslatorManager;
import kgg.translator.translator.LLMTranslator;
import kgg.translator.translator.Translator;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LLMTranslatorArgumentType implements ArgumentType<String> {
    public static LLMTranslatorArgumentType translator() {
        return new LLMTranslatorArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readQuotedString();
    }

    public static LLMTranslator getTranslator(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        String string = context.getArgument(name, String.class);
        List<LLMTranslator> llmTranslators = TranslatorManager.getTranslators().stream().filter(translator -> translator instanceof LLMTranslator).map(translator -> (LLMTranslator) translator).toList();
        for (LLMTranslator llmTranslator : llmTranslators) {
            if (llmTranslator.getName().equals(string)) {
                return llmTranslator;
            }
        }
        throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            List<LLMTranslator> llmTranslators = TranslatorManager.getTranslators().stream().filter(translator -> translator instanceof LLMTranslator).map(translator -> (LLMTranslator) translator).toList();
            return CommandSource.suggestMatching(llmTranslators.stream().map(t -> "\"" + t.getName() + "\""), builder);
        }
        return Suggestions.empty();
    }

}
