package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncEffectsPacket(UUID playerId, Set<String> effects, boolean triggerSpawnEffect)
        implements CustomPacketPayload {
    public SyncEffectsPacket {
        java.util.Objects.requireNonNull(playerId, "playerId");
        effects = effects != null ? effects : java.util.Collections.emptySet();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncEffectsPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_effects"));

    // Sentinel UUID for malformed packets
    private static final UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            try {
                return new UUID(buf.readLong(), buf.readLong());
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncEffectsPacket: {}", e.getMessage());
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
            // Hardcoded max size (128) to avoid NeoForge crash from config not being loaded
            // at static init
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 128),
            SyncEffectsPacket::effects,
            ByteBufCodecs.BOOL,
            SyncEffectsPacket::triggerSpawnEffect,
            SyncEffectsPacket::new);

    // Convenience constructor without trigger flag (defaults to false)
    public SyncEffectsPacket(UUID playerId, Set<String> effects) {
        this(playerId, effects, false);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncEffectsPacket msg, dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                try {
                    if (msg.playerId().equals(SENTINEL_UUID)) {
                        LOGGER.error("How did I?! Uuuughh! Received malformed SyncEffectsPacket with invalid UUID");
                        return;
                    }

                    var level = context.getPlayer().level();
                    if (level == null)
                        return;

                    var entity = level.getPlayerByUUID(msg.playerId());
                    if (entity == null)
                        return;

                    var effects = DataUtils.getEffectsData(entity);
                    if (effects == null)
                        return;

                    effects.syncEquippedFromPacket(msg.effects());
                    LOGGER.debug("Time to change! Synced {} active effects for {}", msg.effects().size(),
                            entity.getName().getString());

                    if (msg.triggerSpawnEffect() && !msg.effects().isEmpty()) {
                        LOGGER.debug("More sparkles, now! Scheduling spawn effect for player {}",
                                entity.getName().getString());
                        SpawnEffectHandler.scheduleSpawnEffect(msg.playerId());
                    }
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync effects for player {}", msg.playerId(), e);
                }
            });
        });
    }
}
