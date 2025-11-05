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

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            return new UUID(buf.readLong(), buf.readLong());
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
            // Directly sync equipped effects from server (bypasses ownership validation)
            ((EffectsData) effects).syncEquippedFromPacket(msg.effects());
            LOGGER.debug("Time to change! Synced {} active effects for {}", msg.effects().size(), entity.getName().getString());

            // Only trigger spawn effect on actual spawn events (login/respawn), not on every sync
            // Check for any spawn effect (spawn_ethereal, spawn_rainbow, etc.)
            if (msg.triggerSpawnEffect() && !msg.effects().isEmpty()) {
                LOGGER.debug("C'mon! Try and catch me! Scheduling spawn effect for player {}", entity.getName().getString());
                SpawnEffectHandler.scheduleSpawnEffect(msg.playerId());
            }
        });
    }
}
