package net.kamkeyke.selective_vanish.registry;

import net.kamkeyke.selective_vanish.SelectiveVanish;
import net.kamkeyke.selective_vanish.command.argumenttype.PlayerListArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, SelectiveVanish.MODID);

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> PLAYERLIST_ARGUMENT_TYPE =
            ARGUMENT_TYPES.register("playerlist",
                    () -> ArgumentTypeInfos.registerByClass(PlayerListArgument.class, new PlayerListArgument.Info()));

    public static void register(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }
}
