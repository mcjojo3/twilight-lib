package mc.sayda.twilight_lib.forge;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.client.renderer.MorphRenderHandler;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightLib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientRuntimeEvents {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        PlayerRenderer playerRenderer = event.getRenderer();
        if (playerRenderer != null) {
            // Morph rendering cancellation
            boolean cancelled = MorphRenderHandler.onRenderPlayerPre(
                    event.getEntity(),
                    playerRenderer,
                    event.getPartialTick(),
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight());

            if (cancelled) {
                event.setCanceled(true);
            }
        }
    }
}
