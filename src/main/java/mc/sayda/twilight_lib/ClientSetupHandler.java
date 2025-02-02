package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.render.CustomModelLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "twilight_lib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupHandler {

    static {
        TwilightLib.LOGGER.info("ClientSetupHandler loaded!");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        TwilightLib.LOGGER.info("ClientSetupHandler: onClientSetup triggered");
        event.enqueueWork(() -> {
            TwilightLib.LOGGER.info("Attaching CustomModelLayer to player renderers");
            Minecraft mc = Minecraft.getInstance();
            mc.getEntityRenderDispatcher().getSkinMap().forEach((skin, renderer) -> {
                if (renderer instanceof PlayerRenderer) {
                    PlayerRenderer pr = (PlayerRenderer) renderer;
                    pr.addLayer(new CustomModelLayer(pr));
                    TwilightLib.LOGGER.info("Added CustomModelLayer to PlayerRenderer for skin: {}", skin);
                }
            });
        });
    }
}
