package net.kamkeyke.selective_vanish.command.argumenttype;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerListArgument implements ArgumentType<List<String>> {
    public static PlayerListArgument players(){
        return new PlayerListArgument();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPlayerList(CommandContext<CommandSourceStack> context, String name){
        return (List<String>) context.getArgument(name, List.class);
    }

    @Override
    public List<String> parse(StringReader stringReader) throws CommandSyntaxException {
        if (!stringReader.canRead() || stringReader.peek() != '{') {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol()
                    .createWithContext(stringReader, "{");
        }
        stringReader.skip();

        List<String> names = new ArrayList<>();

        while (stringReader.canRead()) {
            stringReader.skipWhitespace();

            if (stringReader.canRead() && stringReader.peek() == '}') {
                stringReader.skip();
                if (names.isEmpty()) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()
                            .createWithContext(stringReader);
                }
                return names;
            }

            int startPos = stringReader.getCursor();
            StringBuilder sb = new StringBuilder();
            while (stringReader.canRead()) {
                char c = stringReader.peek();
                if (c == ',' || c == '}' ) {
                    break;
                }
                sb.append(c);
                stringReader.skip();
            }

            String name = sb.toString().trim();

            if (stringReader.getCursor() == startPos || name.isEmpty()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidEscape()
                        .createWithContext(stringReader, "Something is wrong!");
            }

            names.add(name);

            stringReader.skipWhitespace();

            if (stringReader.canRead() && stringReader.peek() == ',') {
                stringReader.skip();
                continue;
            }

            if (!stringReader.canRead()) {
                break;
            }

            if (stringReader.peek() == '}') {
                continue;
            }

            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol()
                    .createWithContext(stringReader, ", or }");
        }

        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol()
                .createWithContext(stringReader, "}");
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining();

        // 1. Identificar onde começar a sugerir (após '{' ou após a última ',')
        int lastDelimiter = Math.max(input.lastIndexOf('{'), input.lastIndexOf(','));

        // O offset define onde a palavra sugerida será inserida no chat
        int startOffset = builder.getStart() + lastDelimiter + 1;
        SuggestionsBuilder subBuilder = builder.createOffset(startOffset);

        // 2. Extrair apenas o pedaço que o jogador está digitando agora
        String partial = input.substring(lastDelimiter + 1).trim().toLowerCase();

        // 3. Obter os nomes dos jogadores de forma segura
        Collection<String> playerNames;
        if (context.getSource() instanceof SharedSuggestionProvider provider) {
            playerNames = provider.getOnlinePlayerNames();
        } else {
            return Suggestions.empty();
        }

        // 4. Filtrar e sugerir
        for (String name : playerNames) {
            if (name.toLowerCase().startsWith(partial)) {
                subBuilder.suggest(name);
            }
        }

        return subBuilder.buildFuture();
    }
}
