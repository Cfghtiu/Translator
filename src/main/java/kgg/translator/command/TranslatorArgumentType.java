package kgg.translator.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import kgg.translator.TranslatorManager;
import kgg.translator.translator.LLMTranslator;
import kgg.translator.translator.Translator;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TranslatorArgumentType implements ArgumentType<String> {
    public static TranslatorArgumentType translator() {
        return new TranslatorArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readQuotedString();
    }

    public static Translator getTranslator(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        String string = context.getArgument(name, String.class);
        List<Translator> translators = TranslatorManager.getTranslators();
        for (Translator translator : translators) {
            if (translator.getName().equals(string)) {
                return translator;
            }
        }
        throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            List<Translator> translators = TranslatorManager.getTranslators();
            return CommandSource.suggestMatching(translators.stream().map(t -> "\"" + t.getName() + "\""), builder);
        }
        return Suggestions.empty();
    }
}
