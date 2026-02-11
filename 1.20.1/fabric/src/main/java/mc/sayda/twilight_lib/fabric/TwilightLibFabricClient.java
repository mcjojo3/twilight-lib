package mc.sayda.twilight_lib.fabric;

import mc.sayda.twilight_lib.client.TwilightLibClient;
import net.fabricmc.api.ClientModInitializer;

public class TwilightLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TwilightLibClient.init();

        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT
                .register((entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityRenderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                        registrationHelper
                                .register(new mc.sayda.twilight_lib.client.renderer.PlayerAddonLayer(playerRenderer));
                    }
                });
    }
}
