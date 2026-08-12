package net.demolutio.sereneregions;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SereneRegions.MOD_ID)
public class SereneRegions
{
    public static final String MOD_ID = "sereneregions";

    private static final Logger LOGGER = LogUtils.getLogger();

    public SereneRegions(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onModLoaded);
    }

    @SubscribeEvent
    public void onModLoaded(final FMLLoadCompleteEvent event) {
        LOGGER.info("Serene Regions is loaded!");
    }
}
