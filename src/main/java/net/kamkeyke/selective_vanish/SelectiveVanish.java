package net.kamkeyke.selective_vanish;

import com.mojang.logging.LogUtils;
import net.kamkeyke.selective_vanish.registry.ModArgumentTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SelectiveVanish.MODID)
public class SelectiveVanish
{
    public static final String MODID = "selective_vanish";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SelectiveVanish(FMLJavaModLoadingContext context)
    {
        // Not so empty anymore. But my bank account on the other hand... qwp
        IEventBus modEventBus = context.getModEventBus();

        ModArgumentTypes.register(modEventBus);
    }
}
