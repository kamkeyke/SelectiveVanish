package net.kamkeyke.selective_vanish.event;

import com.mojang.brigadier.CommandDispatcher;
import net.kamkeyke.selective_vanish.SelectiveVanish;
import net.kamkeyke.selective_vanish.command.SelectiveVanishCommand;
import net.kamkeyke.selective_vanish.saveddata.KnownPlayersData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class CommonEvents {

    @Mod.EventBusSubscriber(modid = SelectiveVanish.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents{

        @SubscribeEvent
        public static void registerCommandsEvent(RegisterCommandsEvent event){
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

            SelectiveVanishCommand.register(dispatcher);
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
            if(!(event.getEntity() instanceof ServerPlayer player)) return;

            KnownPlayersData.get(player.server).cache(player);
        }
    }
}
