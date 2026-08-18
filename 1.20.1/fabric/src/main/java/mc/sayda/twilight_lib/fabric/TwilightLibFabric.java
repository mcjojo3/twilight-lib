package mc.sayda.twilight_lib.fabric;

import mc.sayda.twilight_lib.TwilightLib;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;

public class TwilightLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        mc.sayda.twilight_lib.config.fabric.ConfigPlatformImpl.registerConfig();

        TwilightLib.init();

        // Event Listeners for cosmetic syncing
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof ServerPlayer target) {
                TwilightLib.onStartTracking(player, target);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            TwilightLib.onPlayerRespawn(newPlayer);
        });
    }
}
