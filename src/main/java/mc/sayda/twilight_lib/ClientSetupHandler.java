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
        System.out.println("ClientSetupHandler loaded!");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        System.out.println("ClientSetupHandler: onClientSetup triggered");
        event.enqueueWork(() -> {
            System.out.println("Attaching CustomModelLayer to player renderers");
            Minecraft mc = Minecraft.getInstance();
            mc.getEntityRenderDispatcher().getSkinMap().forEach((skin, renderer) -> {
                if (renderer instanceof PlayerRenderer) {
                    PlayerRenderer pr = (PlayerRenderer) renderer;
                    pr.addLayer(new CustomModelLayer(pr));
                    System.out.println("Added CustomModelLayer to PlayerRenderer for skin: " + skin);
                }
            });
        });
    }
}
