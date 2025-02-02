package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.models.CustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("twilight_lib")
public class TwilightLib {

    public TwilightLib() {
        // Other initialization, event bus subscriptions, etc.
    }

    @Mod.EventBusSubscriber(modid = "twilight_lib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Register your model during client setup. Using enqueueWork ensures that it happens on the main thread.
            event.enqueueWork(() -> {
                // Register the model under a key (use lowercase to match the check in ModelManager)
                ModelManager.registerModel("custommodel", () -> () -> {
                    // Bake the model layer using the client instance.
                    // Ensure your CustomModel.LAYER_LOCATION is correct.
                    return new CustomModel(Minecraft.getInstance().getEntityModels().bakeLayer(CustomModel.LAYER_LOCATION));
                });
            });
        }
    }
}
