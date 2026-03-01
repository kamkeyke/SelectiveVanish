package net.kamkeyke.selective_vanish.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kamkeyke.raccooncore.command.argumenttype.PlayerListArgument;
import net.kamkeyke.raccooncore.util.TextUtils;
import net.kamkeyke.selective_vanish.saveddata.VanishVisibilityData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import redstonedubstep.mods.vanishmod.VanishUtil;
import redstonedubstep.mods.vanishmod.VanishingHandler;

import java.util.Collection;
import java.util.List;

public class SelectiveVanishCommand {
    SelectiveVanishCommand(){}

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

            actionNode.then(Commands.argument("viewers", EntityArgument.players())
                    .then(Commands.literal("see")
                            .then(Commands.argument("vanishedPlayer", EntityArgument.player())
                                    .executes(context ->
                                            action.equals("allow") ? allow(context, false) : deny(context, false)
                                    )
                            )
                    )
            );

            actionNode.then(Commands.literal("group")
                    .then(Commands.argument("viewersGroup", PlayerListArgument.players())
                            .then(Commands.literal("see")
                                    .then(Commands.argument("vanishedPlayer", EntityArgument.player())
                                            .executes(context ->
                                                    action.equals("allow") ? allow(context, true) : deny(context, true)
                                            )
                                    )
                            )
                    )
            );

            command.then(actionNode);
        }

        // Clear
        command.then(Commands.literal("clear")
                .then(Commands.argument("vanishedPlayers", EntityArgument.players())
                        .executes(context -> clear(context, false))
                )
                .then(Commands.literal("group")
                        .then(Commands.argument("vanishedGroup", PlayerListArgument.players())
                                .executes(context -> clear(context, true))
                        )
                )
        );

        // Get
        command.then(Commands.literal("get")
                .then(Commands.argument("vanishedPlayer", EntityArgument.player())
                        .executes(SelectiveVanishCommand::get)));

        dispatcher.register(command);
    }

    private static int allow(CommandContext<CommandSourceStack> context, boolean isGroup) throws CommandSyntaxException {
        String argName = isGroup ? "viewersGroup" : "viewers";
        Collection<ServerPlayer> viewers = isGroup ? PlayerListArgument.getPlayerList(context, argName) : EntityArgument.getPlayers(context, argName);

        ServerPlayer vanishedPlayer = EntityArgument.getPlayer(context, "vanishedPlayer");

        VanishVisibilityData visibilityData = VanishVisibilityData.get(vanishedPlayer.server);
        viewers.forEach(viewer -> visibilityData.allow(viewer, vanishedPlayer));

        updateVanish(vanishedPlayer);

        context.getSource().sendSuccess(() -> Component.translatable("command.selective_vanish.svanish.allow", TextUtils.buildPlayerList(viewers), vanishedPlayer.getDisplayName()), true);
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> context, boolean isGroup) throws CommandSyntaxException {
        String argName = isGroup ? "viewersGroup" : "viewers";
        Collection<ServerPlayer> viewers = isGroup ? PlayerListArgument.getPlayerList(context, argName) : EntityArgument.getPlayers(context, argName);
        ServerPlayer vanishedPlayer = EntityArgument.getPlayer(context, "vanishedPlayer");

        VanishVisibilityData visibilityData = VanishVisibilityData.get(vanishedPlayer.server);
        viewers.forEach(viewer -> visibilityData.deny(viewer, vanishedPlayer));

        updateVanish(vanishedPlayer);

        context.getSource().sendSuccess(() -> Component.translatable("command.selective_vanish.svanish.revoke", TextUtils.buildPlayerList(viewers), vanishedPlayer.getDisplayName()), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context, boolean isGroup) throws CommandSyntaxException {
        String argName = isGroup ? "vanishedGroup" : "vanishedPlayers";
        Collection<ServerPlayer> vanishedPlayers = isGroup ? PlayerListArgument.getPlayerList(context, argName) : EntityArgument.getPlayers(context, argName);
        if (vanishedPlayers.isEmpty()) return 0;

        VanishVisibilityData visibilityData = VanishVisibilityData.get(context.getSource().getServer());

        for (ServerPlayer serverPlayer : vanishedPlayers){
            visibilityData.clear(serverPlayer);
            updateVanish(serverPlayer);
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.selective_vanish.svanish.clear", TextUtils.buildPlayerList(vanishedPlayers)), true);
        return 1;
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer vanishedPlayer = EntityArgument.getPlayer(context, "vanishedPlayer");
        VanishVisibilityData visibilityData = VanishVisibilityData.get(vanishedPlayer.server);
        List<ServerPlayer> playersWhoCanSee = visibilityData.getPlayersWhoCanSee(vanishedPlayer);

        if(playersWhoCanSee.isEmpty()){
            context.getSource().sendSuccess(() -> Component.translatable("command.selective_vanish.svanish.get.empty", vanishedPlayer.getDisplayName()), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("command.selective_vanish.svanish.get", vanishedPlayer.getDisplayName()), false);
            context.getSource().sendSuccess(() -> TextUtils.buildPlayerList(playersWhoCanSee), false);
        }

        return 1;
    }

    private static void updateVanish(ServerPlayer serverPlayer){
        boolean vanishes = VanishUtil.isVanished(serverPlayer);
        VanishingHandler.sendPacketsOnVanish(serverPlayer, serverPlayer.serverLevel(), vanishes);
    }
}
