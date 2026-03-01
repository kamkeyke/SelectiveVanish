package net.kamkeyke.selective_vanish.event;

import com.mojang.brigadier.CommandDispatcher;
import net.kamkeyke.selective_vanish.SelectiveVanish;
import net.kamkeyke.selective_vanish.command.SelectiveVanishCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
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
    }
}
