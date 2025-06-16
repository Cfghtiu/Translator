package kgg.translator.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import kgg.translator.ChatFormat;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.concurrent.CompletableFuture;

public class ChatFormatArgumentType implements ArgumentType<String> {
    public static ChatFormatArgumentType chatFormat() {
        return new ChatFormatArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readQuotedString();
    }

    public static String getChatFormat(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        String formatName = context.getArgument(name, String.class);
        if (ChatFormat.listFormats().contains(formatName)) {
            return formatName;
        }
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            return CommandSource.suggestMatching(ChatFormat.listFormats().stream().map(f -> "\"" + f + "\""), builder);
        }
        return Suggestions.empty();
    }
}