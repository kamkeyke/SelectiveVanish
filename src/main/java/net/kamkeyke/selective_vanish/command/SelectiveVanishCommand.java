package net.kamkeyke.selective_vanish.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kamkeyke.raccooncore.util.TextUtils;
import net.kamkeyke.selective_vanish.command.argumenttype.KnownPlayerListArgument;
import net.kamkeyke.selective_vanish.data.KnownPlayer;
import net.kamkeyke.selective_vanish.saveddata.KnownPlayersData;
import net.kamkeyke.selective_vanish.saveddata.VanishVisibilityData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import redstonedubstep.mods.vanishmod.VanishUtil;
import redstonedubstep.mods.vanishmod.VanishingHandler;

import java.util.*;

public class SelectiveVanishCommand {
    SelectiveVanishCommand(){}

    private static final SuggestionProvider<CommandSourceStack> KNOWN_PLAYERS_SUGGESTIONS =
            (context, builder) -> KnownPlayerListArgument.knownPlayers().listSuggestions(context, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        alias("svanish", dispatcher);
        alias("sv", dispatcher);
    }

    private static void alias(String prefix, CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(prefix)
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        // Allow and Deny
        for(String action : new String[]{"allow", "deny"}) {
            var actionNode = Commands.literal(action);

            actionNode.then(Commands.argument("viewers", KnownPlayerListArgument.knownPlayers())
                    .suggests(KNOWN_PLAYERS_SUGGESTIONS)
                    .then(Commands.literal("see")
                            .then(Commands.argument("vanishedPlayers", KnownPlayerListArgument.knownPlayers())
                                    .suggests(KNOWN_PLAYERS_SUGGESTIONS)
                                    .executes(context ->
                                            action.equals("allow") ? allow(context) : deny(context)
                                    )
                            )
                    )
            );
            command.then(actionNode);
        }

        // Clear
        command.then(Commands.literal("clear")
                .then(Commands.argument("vanishedPlayers", KnownPlayerListArgument.knownPlayers())
                        .suggests(KNOWN_PLAYERS_SUGGESTIONS)
                        .executes(SelectiveVanishCommand::clear)
                )
        );

        // Get
        command.then(Commands.literal("get")
                .then(Commands.argument("vanishedPlayers", KnownPlayerListArgument.knownPlayers())
                        .suggests(KNOWN_PLAYERS_SUGGESTIONS)
                        .executes(SelectiveVanishCommand::get)));

        command.then(Commands.literal("cached")
                .then(Commands.literal("knownPlayers")
                        .executes(SelectiveVanishCommand::cache)
                )
        );

        dispatcher.register(command);
    }

    private static int cache(CommandContext<CommandSourceStack> context) {
        KnownPlayersData knownPlayersData = KnownPlayersData.get(context.getSource().getServer());

        MutableComponent result = Component.literal("KnownPlayersData:");

        if(!knownPlayersData.getAllUUIDToName().isEmpty()){
            knownPlayersData.getAllNameToUUID().forEach((name, uuid) -> {
                KnownPlayer knownPlayer = new KnownPlayer(uuid, name);
                result.append("\n- ").append(knownPlayer.toNameCopyNameComponent().append(Component.literal(" -> ").withStyle(ChatFormatting.WHITE)).append(knownPlayer.toUUIDCopyUUIDComponent()));
            });
            context.getSource().sendSuccess(() -> result, false);
        } else {
            result.append("\nCache is empty!");
        }

        return 1;
    }

    private static int allow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<KnownPlayer> viewers = KnownPlayerListArgument.getKnownPlayers(context, "viewers");
        Collection<KnownPlayer> targets = KnownPlayerListArgument.getKnownPlayers(context, "vanishedPlayers");
        if (viewers.isEmpty()){
            context.getSource().sendFailure(Component.translatable("command.selective_vanish.svanish.notfound.viewers"));
            return 0;
        }
        if (targets.isEmpty()){
            context.getSource().sendFailure(Component.translatable("command.selective_vanish.svanish.notfound.target"));
            return 0;
        }

        VanishVisibilityData visibilityData = VanishVisibilityData.get(context.getSource().getServer());

        Set<UUID> affectedViewerUuids = new HashSet<>();
        Set<UUID> notAffectedViewerUuids = new HashSet<>();
        List<MutableComponent> affectedViewers = new ArrayList<>();
        List<MutableComponent> notAffectedViewers = new ArrayList<>();

        for(KnownPlayer target : targets){
            for(KnownPlayer viewer : viewers){
                if (viewer.uuid().equals(target.uuid())) continue;

                if(!visibilityData.canSee(viewer.uuid(), target.uuid())){
                    visibilityData.allow(viewer.uuid(), target.uuid());
                    if (affectedViewerUuids.add(viewer.uuid())) {
                        affectedViewers.add(viewer.toNameCopyNameComponent());
                    }
                } else {
                    if (notAffectedViewerUuids.add(viewer.uuid())) {
                        notAffectedViewers.add(viewer.toNameCopyNameComponent());
                    }
                }
            }

            if(!affectedViewers.isEmpty()){
                ServerPlayer onlineTarget = context.getSource().getServer().getPlayerList().getPlayer(target.uuid());
                if(onlineTarget != null) updateVanish(onlineTarget);
            }
        }

        // Feedback
        if (!affectedViewers.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.selective_vanish.svanish.allow",
                    TextUtils.buildComponentList(affectedViewers),
                    TextUtils.buildComponentList(targets.stream().map(KnownPlayer::toNameCopyNameComponent).toList())
            ), true);
        }

        if (!notAffectedViewers.isEmpty()) {
            context.getSource().sendSystemMessage(Component.translatable(
                    "command.selective_vanish.svanish.allow.allowed_already",
                    TextUtils.buildComponentList(notAffectedViewers),
                    TextUtils.buildComponentList(targets.stream().map(KnownPlayer::toNameCopyNameComponent).toList())
            ));
        }

        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<KnownPlayer> viewers = KnownPlayerListArgument.getKnownPlayers(context, "viewers");
        Collection<KnownPlayer> targets = KnownPlayerListArgument.getKnownPlayers(context, "vanishedPlayers");
        if (viewers.isEmpty()){
            context.getSource().sendFailure(Component.translatable("command.selective_vanish.svanish.notfound.viewers"));
            return 0;
        }
        if (targets.isEmpty()){
            context.getSource().sendFailure(Component.translatable("command.selective_vanish.svanish.notfound.target"));
            return 0;
        }

        VanishVisibilityData visibilityData = VanishVisibilityData.get(context.getSource().getServer());

        Set<UUID> affectedViewerUuids = new HashSet<>();
        Set<UUID> notAffectedViewerUuids = new HashSet<>();
        List<MutableComponent> affectedViewers = new ArrayList<>();
        List<MutableComponent> notAffectedViewers = new ArrayList<>();

        for(KnownPlayer target : targets){
            for(KnownPlayer viewer : viewers){
                if (viewer.uuid().equals(target.uuid())) continue;

                if(visibilityData.canSee(viewer.uuid(), target.uuid())){
                    visibilityData.deny(viewer.uuid(), target.uuid());
                    if (affectedViewerUuids.add(viewer.uuid())) {
                        affectedViewers.add(viewer.toNameCopyNameComponent());
                    }
                } else {
                    if (notAffectedViewerUuids.add(viewer.uuid())) {
                        notAffectedViewers.add(viewer.toNameCopyNameComponent());
                    }
                }
            }

            if(!affectedViewers.isEmpty()){
                ServerPlayer onlineTarget = context.getSource().getServer().getPlayerList().getPlayer(target.uuid());
                if(onlineTarget != null) updateVanish(onlineTarget);
            }
        }

        // Feedback
        if (!affectedViewers.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.selective_vanish.svanish.deny",
                    TextUtils.buildComponentList(affectedViewers),
                    TextUtils.buildComponentList(targets.stream().map(KnownPlayer::toNameCopyNameComponent).toList())
            ), true);
        }

        if (!notAffectedViewers.isEmpty()) {
            context.getSource().sendSystemMessage(Component.translatable(
                    "command.selective_vanish.svanish.deny.denied_already",
                    TextUtils.buildComponentList(notAffectedViewers),
                    TextUtils.buildComponentList(targets.stream().map(KnownPlayer::toNameCopyNameComponent).toList())
            ));
        }

        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<KnownPlayer> vanishedPlayers = KnownPlayerListArgument.getKnownPlayers(context, "vanishedPlayers");
        if (vanishedPlayers.isEmpty()){
            context.getSource().sendFailure(Component.translatable("argument.entity.notfound.player"));
            return 0;
        }

        VanishVisibilityData visibilityData = VanishVisibilityData.get(context.getSource().getServer());

        List<MutableComponent> affectedPlayers = new ArrayList<>();
        List<MutableComponent> notAffectedPlayers = new ArrayList<>();

        for (KnownPlayer player : vanishedPlayers){
            if(!visibilityData.getPlayersWhoCanSee(player.uuid()).isEmpty()){
                visibilityData.clear(player.uuid());
                affectedPlayers.add(player.toNameCopyNameComponent());
            } else {
                notAffectedPlayers.add(player.toNameCopyNameComponent());
            }

            if(!affectedPlayers.isEmpty()){
                ServerPlayer onlineTarget = context.getSource().getServer().getPlayerList().getPlayer(player.uuid());
                if(onlineTarget != null) updateVanish(onlineTarget);
            }
        }

        // Feedback
        if (!affectedPlayers.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.selective_vanish.svanish.clear",
                    TextUtils.buildComponentList(affectedPlayers)
            ), true);
        }

        if (!notAffectedPlayers.isEmpty()) {
            context.getSource().sendSystemMessage(Component.translatable(
                    "command.selective_vanish.svanish.clear.clear_already",
                    TextUtils.buildComponentList(notAffectedPlayers)
            ));
        }

        return 1;
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<KnownPlayer> vanishedPlayers = KnownPlayerListArgument.getKnownPlayers(context, "vanishedPlayers");
        if (vanishedPlayers.isEmpty()){
            context.getSource().sendFailure(Component.translatable("command.selective_vanish.svanish.notfound.target"));
            return 0;
        }

        VanishVisibilityData visibilityData = VanishVisibilityData.get(context.getSource().getServer());
        KnownPlayersData knownPlayersData = KnownPlayersData.get(context.getSource().getServer());
        List<MutableComponent> emptyPlayers = new ArrayList<>();

        for (KnownPlayer player : vanishedPlayers){
            Set<UUID> viewers = visibilityData.getPlayersWhoCanSee(player.uuid());

            if(!viewers.isEmpty()){
                MutableComponent result = Component.translatable("command.selective_vanish.svanish.get", player.toNameCopyNameComponent()).append("\n");

                List<MutableComponent> knownViewers = new ArrayList<>();

                for(UUID viewerUUID : viewers){
                    if(!knownPlayersData.contains(viewerUUID)) continue;
                    KnownPlayer knownViewer = new KnownPlayer(viewerUUID, knownPlayersData.getName(viewerUUID));
                    knownViewers.add(knownViewer.toNameCopyNameComponent());
                }

                result.append(TextUtils.buildComponentUnorderedList(knownViewers));
                context.getSource().sendSuccess(() -> result, false);
            } else {
                emptyPlayers.add(player.toNameCopyNameComponent());
            }
        }

        if(!emptyPlayers.isEmpty()){
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.selective_vanish.svanish.empty",
                    TextUtils.buildComponentList(emptyPlayers)
            ), false);
        }

        return 1;
    }

    private static void updateVanish(ServerPlayer serverPlayer){
        boolean vanishes = VanishUtil.isVanished(serverPlayer);
        VanishingHandler.sendPacketsOnVanish(serverPlayer, serverPlayer.serverLevel(), vanishes);
    }
}