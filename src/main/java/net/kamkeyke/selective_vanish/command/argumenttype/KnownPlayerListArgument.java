package net.kamkeyke.selective_vanish.command.argumenttype;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kamkeyke.selective_vanish.data.KnownPlayer;
import net.kamkeyke.selective_vanish.saveddata.KnownPlayersData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command argument capable of accepting either a known player name,
 * a group of known player names enclosed in curly braces, or a
 * Minecraft entity selector.
 *
 * <p>Supported formats:</p>
 *
 * <pre>{@code
 * Kamui
 * {Kamui,Haehae,Darius}
 * @a
 * @e[type=player]
 * }</pre>
 *
 * <p>Plain names and groups are resolved through {@link KnownPlayersData},
 * allowing previously known offline players to be selected. Selectors
 * retain Minecraft's normal behavior and therefore only resolve players
 * currently available to the server.</p>
 *
 * <p>Groups intentionally accept only player names. Mixed expressions such
 * as {@code {Kamui,@a}} are not supported.</p>
 *
 * <p>This argument returns {@link KnownPlayer} records rather than
 * {@link ServerPlayer} instances. Code that requires an online player is
 * responsible for resolving the {@link KnownPlayer#uuid()} itself.</p>
 */
public class KnownPlayerListArgument implements ArgumentType<KnownPlayerListArgument.Result> {

    private static final Collection<String> EXAMPLES = Arrays.asList(
            "Player",
            "{Player1,Player2}",
            "@a",
            "@e[type=player]"
    );

    public static final SimpleCommandExceptionType NO_PLAYERS_FOUND =
            new SimpleCommandExceptionType(
                    Component.translatable("argument.entity.notfound.player")
            );

    public static final SimpleCommandExceptionType INVALID_GROUP =
            new SimpleCommandExceptionType(
                    Component.translatable("argument.entity.invalid")
            );

    public static final SimpleCommandExceptionType UNKNOWN_PLAYER =
            new SimpleCommandExceptionType(
                    Component.translatable("argument.entity.notfound.player")
            );

    /**
     * Creates a known-player argument.
     *
     * @return a new known-player argument
     */
    public static KnownPlayerListArgument knownPlayers() {
        return new KnownPlayerListArgument();
    }

    /**
     * Retrieves the parsed argument and resolves it into known players.
     *
     * <p>Names and groups are resolved through {@link KnownPlayersData},
     * while selectors are resolved against the current command source.</p>
     *
     * @param context command context
     * @param name argument name
     * @return resolved known players
     * @throws CommandSyntaxException if a player cannot be resolved or a
     * selector does not select any players
     */
    public static Collection<KnownPlayer> getKnownPlayers(
            CommandContext<CommandSourceStack> context,
            String name
    ) throws CommandSyntaxException {

        Result result = context.getArgument(name, Result.class);

        KnownPlayersData data =
                KnownPlayersData.get(context.getSource().getServer());

        List<KnownPlayer> players = new ArrayList<>();

        switch (result.type()) {

            case NAME -> {
                KnownPlayer player =
                        resolveName(data, result.values().get(0));

                if (player == null) {
                    throw UNKNOWN_PLAYER.create();
                }

                players.add(player);
            }

            case GROUP -> {
                for (String playerName : result.values()) {
                    KnownPlayer player =
                            resolveName(data, playerName);

                    if (player == null) {
                        throw UNKNOWN_PLAYER.create();
                    }

                    players.add(player);
                }
            }

            case SELECTOR -> {
                Collection<ServerPlayer> selected =
                        result.selector().findPlayers(context.getSource());

                if (selected.isEmpty()) {
                    throw NO_PLAYERS_FOUND.create();
                }

                for (ServerPlayer player : selected) {
                    players.add(
                            new KnownPlayer(
                                    player.getUUID(),
                                    player.getGameProfile().getName()
                            )
                    );
                }
            }
        }

        return players;
    }

    /**
     * Resolves a player name through the persistent known-player cache.
     */
    private static KnownPlayer resolveName(
            KnownPlayersData data,
            String name
    ) {
        UUID uuid = data.getUUID(name);

        if (uuid == null) {
            return null;
        }

        String cachedName = data.getName(uuid);

        if (cachedName == null) {
            return null;
        }

        return new KnownPlayer(uuid, cachedName);
    }

    /**
     * Parses a known player name, a group of known player names,
     * or a Minecraft entity selector.
     */
    @Override
    public Result parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw INVALID_GROUP.createWithContext(reader);
        }

        return switch (reader.peek()) {
            case '@' -> parseSelector(reader);
            case '{' -> parseGroup(reader);
            default -> parseName(reader);
        };
    }

    /**
     * Parses a Minecraft entity selector.
     */
    private Result parseSelector(
            StringReader reader
    ) throws CommandSyntaxException {

        EntitySelectorParser parser =
                new EntitySelectorParser(reader, true);

        EntitySelector selector = parser.parse();

        return Result.selector(selector);
    }

    /**
     * Parses a group such as:
     *
     * <pre>{@code
     * {Kamui,Haehae,Darius}
     * }</pre>
     *
     * <p>Groups may contain only names. Selectors are intentionally
     * rejected inside groups.</p>
     */
    private Result parseGroup(
            StringReader reader
    ) throws CommandSyntaxException {

        reader.skip();

        List<String> names = new ArrayList<>();

        while (reader.canRead()) {

            reader.skipWhitespace();

            /*
             * Closing an empty group is invalid.
             */
            if (reader.peek() == '}') {
                reader.skip();

                if (names.isEmpty()) {
                    throw INVALID_GROUP.createWithContext(reader);
                }

                return Result.group(names);
            }

            int start = reader.getCursor();

            StringBuilder nameBuilder = new StringBuilder();

            while (reader.canRead()) {
                char c = reader.peek();

                if (c == ',' || c == '}') {
                    break;
                }

                nameBuilder.append(c);
                reader.skip();
            }

            String name = nameBuilder.toString().trim();

            if (reader.getCursor() == start || name.isEmpty()) {
                throw INVALID_GROUP.createWithContext(reader);
            }

            /*
             * Groups contain only player names.
             *
             * {Kamui,@a} is intentionally invalid.
             */
            if (name.startsWith("@")) {
                throw INVALID_GROUP.createWithContext(reader);
            }

            names.add(name);

            reader.skipWhitespace();

            /*
             * Another player follows.
             */
            if (reader.canRead() && reader.peek() == ',') {
                reader.skip();
                continue;
            }

            /*
             * The closing brace will be handled by the next
             * iteration.
             */
            if (reader.canRead() && reader.peek() == '}') {
                continue;
            }

            /*
             * Anything else is invalid group syntax.
             */
            throw INVALID_GROUP.createWithContext(reader);
        }

        /*
         * The command ended before the group was closed.
         */
        throw INVALID_GROUP.createWithContext(reader);
    }

    /**
     * Parses a single player name.
     */
    private Result parseName(
            StringReader reader
    ) throws CommandSyntaxException {

        int start = reader.getCursor();

        while (reader.canRead()
                && !Character.isWhitespace(reader.peek())) {

            reader.skip();
        }

        String name = reader.getString()
                .substring(start, reader.getCursor());

        if (name.isBlank()) {
            throw UNKNOWN_PLAYER.createWithContext(reader);
        }

        return Result.name(name);
    }

    /**
     * Provides tab completion for:
     *
     * <ul>
     *     <li>known player names;</li>
     *     <li>known player groups;</li>
     *     <li>Minecraft entity selectors.</li>
     * </ul>
     *
     * <p>Suggestions for names are obtained from {@link KnownPlayersData},
     * so offline players are included. Selector suggestions retain
     * Minecraft's normal selector completion behavior.</p>
     */
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> context,
            SuggestionsBuilder builder
    ) {
        if (!(context.getSource() instanceof CommandSourceStack source)) {
            return Suggestions.empty();
        }

        String input = builder.getRemaining();


        // Selector

        if (input.startsWith("@")) {

            StringReader reader = new StringReader(input);

            EntitySelectorParser parser =
                    new EntitySelectorParser(reader, true);

            /*
             * The input may be incomplete while the user is typing.
             *
             * For example:
             *
             * @
             * @e[
             * @e[type=
             *
             * Therefore, parse errors here are expected and must not
             * prevent suggestions from being generated.
             */
            try {
                parser.parse();
            } catch (CommandSyntaxException ignored) {
            }

            return parser.fillSuggestions(
                    builder,
                    suggestionBuilder -> {

                        Collection<String> onlinePlayers =
                                source.getOnlinePlayerNames();

                        SharedSuggestionProvider.suggest(
                                onlinePlayers,
                                suggestionBuilder
                        );
                    }
            );
        }

        KnownPlayersData data =
                KnownPlayersData.get(source.getServer());

        Collection<String> knownPlayerNames =
                data.getAllUUIDToName().values();


        // Group

        int lastOpen = input.lastIndexOf('{');
        int lastComma = input.lastIndexOf(',');

        int lastDelimiter =
                Math.max(lastOpen, lastComma);

        /*
         * If a '{' or ',' exists, the cursor is currently inside
         * a group.
         */
        if (lastDelimiter >= 0) {

            int startOffset =
                    builder.getStart() + lastDelimiter + 1;

            SuggestionsBuilder subBuilder =
                    builder.createOffset(startOffset);

            String partial = input
                    .substring(lastDelimiter + 1)
                    .trim()
                    .toLowerCase(Locale.ROOT);

            for (String playerName : knownPlayerNames) {

                if (playerName
                        .toLowerCase(Locale.ROOT)
                        .startsWith(partial)) {

                    subBuilder.suggest(playerName);
                }
            }

            return subBuilder.buildFuture();
        }


        // Normal Name

        String partial = input
                .trim()
                .toLowerCase(Locale.ROOT);

        SuggestionsBuilder subBuilder =
                builder.createOffset(builder.getStart());

        for (String playerName : knownPlayerNames) {

            if (playerName
                    .toLowerCase(Locale.ROOT)
                    .startsWith(partial)) {

                subBuilder.suggest(playerName);
            }
        }


        // Selector

        if ("@".startsWith(partial)) {
            subBuilder.suggest("@a");
            subBuilder.suggest("@e");
            subBuilder.suggest("@p");
            subBuilder.suggest("@r");
            subBuilder.suggest("@s");
        }

        return subBuilder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    /**
     * Internal representation of the parsed argument.
     *
     * <p>The parser itself only determines the syntax. Resolution of
     * player names is performed later by {@link #getKnownPlayers} because
     * the command source is not available to {@link #parse}.</p>
     */
    public record Result(
            Type type,
            List<String> values,
            EntitySelector selector
    ) {

        public static Result name(String name) {
            return new Result(
                    Type.NAME,
                    List.of(name),
                    null
            );
        }

        public static Result group(Collection<String> names) {
            return new Result(
                    Type.GROUP,
                    List.copyOf(names),
                    null
            );
        }

        public static Result selector(EntitySelector selector) {
            return new Result(
                    Type.SELECTOR,
                    List.of(),
                    selector
            );
        }
    }

    /**
     * Represents the syntax used by the parsed argument.
     */
    public enum Type {
        NAME,
        GROUP,
        SELECTOR
    }
}