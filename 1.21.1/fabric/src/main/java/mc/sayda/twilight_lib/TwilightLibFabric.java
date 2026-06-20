package mc.sayda.twilight_lib;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.network.SyncModelVariantPacket;

public class TwilightLibFabric implements ModInitializer {
        @Override
        public void onInitialize() {
                mc.sayda.twilight_lib.capabilities.FabricModAttachments.init();

                TwilightLib.init();

                // Register S2C payload types using native Fabric networking.
                // Architectury 13.0.8 has bugs in both registerReceiver and sendToPlayer on Fabric,
                // so we bypass it entirely for packet registration and sending.
                PayloadTypeRegistry.playS2C().register(SyncMorphPacket.TYPE, SyncMorphPacket.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(SyncAddonsPacket.TYPE, SyncAddonsPacket.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(SyncTrailsPacket.TYPE, SyncTrailsPacket.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(SyncEffectsPacket.TYPE, SyncEffectsPacket.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(SyncModelVariantPacket.TYPE, SyncModelVariantPacket.STREAM_CODEC);

                NetworkHandler.PLATFORM_SEND_TO_PLAYER = (player, pkt) -> ServerPlayNetworking.send(player, pkt);

                // Register attribute modifications

                // Attribute modification is handled via Mixin (FabricPlayerAttributeMixin)

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
