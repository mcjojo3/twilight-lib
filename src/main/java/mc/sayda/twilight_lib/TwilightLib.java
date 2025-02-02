package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.models.CustomModel;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("twilight_lib")
public class TwilightLib {

    // Create a logger for your mod.
    public static final Logger LOGGER = LogManager.getLogger("twilight_lib");

    public TwilightLib() {
        LOGGER.info("TwilightLib main mod class constructed.");
    }

    @Mod.EventBusSubscriber(modid = "twilight_lib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        static {
            LOGGER.info("TwilightLib ClientEvents loaded.");
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("TwilightLib ClientEvents: onClientSetup triggered.");
            event.enqueueWork(() -> {
                LOGGER.info("TwilightLib ClientEvents: registering custom model.");
                ModelManager.registerModel("custommodel", () -> () -> {
                    LOGGER.info("Baking CustomModel layer.");
                    return new CustomModel(Minecraft.getInstance().getEntityModels().bakeLayer(CustomModel.LAYER_LOCATION));
                });
            });
        }
    }
}
