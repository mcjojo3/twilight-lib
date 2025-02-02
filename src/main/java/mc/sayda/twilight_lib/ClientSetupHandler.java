package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.render.CustomModelLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "twilight_lib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupHandler {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Iterate over the player skin map
            for (EntityRenderer<? extends Player> renderer : Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values()) {
                if (renderer instanceof PlayerRenderer playerRenderer) {
                    // Using pattern matching for instanceof (available in newer Java versions)
                    playerRenderer.addLayer(new CustomModelLayer(playerRenderer));
                }
            }
        });
    }
}
