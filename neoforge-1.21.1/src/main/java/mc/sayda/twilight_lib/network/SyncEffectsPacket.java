package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncEffectsPacket(UUID playerId, Set<String> effects, boolean triggerSpawnEffect) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncEffectsPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_effects"));

    // Sentinel UUID for malformed packets
    private static final UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            try {
                return new UUID(buf.readLong(), buf.readLong());
            } catch (Exception e) {
                LOGGER.warn("Oh. Wow. That's like, totally off the mandala. Failed to decode UUID in SyncEffectsPacket: {}", e.getMessage());
                return SENTINEL_UUID; // Return sentinel on decode error
            }
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncEffectsPacket> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC,
        SyncEffectsPacket::playerId,
        ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 100),
        SyncEffectsPacket::effects,
        ByteBufCodecs.BOOL,
        SyncEffectsPacket::triggerSpawnEffect,
        SyncEffectsPacket::new
    );

    // Convenience constructor without trigger flag (defaults to false)
    public SyncEffectsPacket(UUID playerId, Set<String> effects) {
        this(playerId, effects, false);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncEffectsPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                // Detect malformed packets from decode errors
                if (msg.playerId().equals(SENTINEL_UUID)) {
                    LOGGER.error("This will be fine! Don't worry about it Zoe, things break all the time. Received malformed SyncEffectsPacket with invalid UUID - packet decode failed");
                    return;
                }

                var mc = Minecraft.getInstance();
                var level = mc.level;
                if (level == null) {
                    LOGGER.warn("Daylight is too bright, the night is too dark! Cannot sync effects - level is null");
                    return;
                }
                var entity = level.getPlayerByUUID(msg.playerId());
                if (entity == null) {
                    LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId());
                    return;
                }

                var effects = entity.getData(ModAttachments.EFFECTS);
                if (effects == null) {
                    LOGGER.error("Failed to get effects data for player {}", msg.playerId());
                    return;
                }

                // Type-safe cast with validation to prevent crashes in heavily modded environments
                if (!(effects instanceof EffectsData)) {
                    LOGGER.error("Incompatible effects attachment implementation for player {}. Expected EffectsData but got {}. " +
                                 "This may be caused by another mod replacing the attachment.",
                                 entity.getName().getString(), effects.getClass().getName());
                    return; // Gracefully skip instead of crashing
                }

                // Directly sync equipped effects from server (bypasses ownership validation)
                ((EffectsData) effects).syncEquippedFromPacket(msg.effects());
                LOGGER.debug("Time to change! Synced {} active effects for {}", msg.effects().size(), entity.getName().getString());

                // Only trigger spawn effect on actual spawn events (login/respawn), not on every sync
                // Check for any spawn effect (spawn_ethereal, spawn_rainbow, etc.)
                if (msg.triggerSpawnEffect() && !msg.effects().isEmpty()) {
                    LOGGER.debug("C'mon! Try and catch me! Scheduling spawn effect for player {}", entity.getName().getString());
                    SpawnEffectHandler.scheduleSpawnEffect(msg.playerId());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to sync effects for player {}", msg.playerId(), e);
            }
        });
    }
}
