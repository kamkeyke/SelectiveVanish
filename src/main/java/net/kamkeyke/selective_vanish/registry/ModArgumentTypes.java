package net.kamkeyke.selective_vanish.registry;

import net.kamkeyke.selective_vanish.SelectiveVanish;
import net.kamkeyke.selective_vanish.command.argumenttype.KnownPlayerListArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, SelectiveVanish.MODID);

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> KNOWNPLAYER_LIST_ARGUMENT_TYPE =
            ARGUMENT_TYPES.register("knownplayer_list",
                    () -> ArgumentTypeInfos.registerByClass(KnownPlayerListArgument.class, SingletonArgumentInfo.contextFree(KnownPlayerListArgument::new)));

    public static void register(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }
}
