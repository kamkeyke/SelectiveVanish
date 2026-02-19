package net.kamkeyke.selective_vanish;

import com.mojang.logging.LogUtils;
import net.kamkeyke.selective_vanish.registry.ArgumentTypes;
import net.minecraftforge.common.MinecraftForge;
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
        raccoonGreetings();
        IEventBus modEventBus = context.getModEventBus();

        ArgumentTypes.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void raccoonGreetings(){
        if(System.getProperty("raccoon.greeted") == null){
            LOGGER.info("Greetings from the raccoon!");
            System.setProperty("raccoon.greeted", "true");
        }
    }
}
