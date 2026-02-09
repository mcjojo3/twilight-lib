package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncTrailsPacket(UUID playerId, Set<String> trails) implements CustomPacketPayload {
    public SyncTrailsPacket {
        java.util.Objects.requireNonNull(playerId, "playerId");
        trails = trails != null ? trails : java.util.Collections.emptySet();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncTrailsPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_trails"));

    // Sentinel UUID for malformed packets
    private static final UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            try {
                return new UUID(buf.readLong(), buf.readLong());
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncTrailsPacket: {}", e.getMessage());
                return SENTINEL_UUID; // Return sentinel on decode error
            }
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncTrailsPacket> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            SyncTrailsPacket::playerId,
            // Hardcoded max size (128) to avoid NeoForge crash from config not being loaded
            // at static init
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 128),
            SyncTrailsPacket::trails,
            SyncTrailsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTrailsPacket msg, dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                try {
                    if (msg.playerId().equals(SENTINEL_UUID)) {
                        LOGGER.error("How did I?! Uuuughh! Received malformed SyncTrailsPacket with invalid UUID");
                        return;
                    }

                    var level = context.getPlayer().level();
                    if (level == null) {
                        LOGGER.warn("How did I?! Uuuughh! Cannot sync trails - level is null");
                        return;
                    }

                    var entity = level.getPlayerByUUID(msg.playerId());
                    if (entity == null) {
                        LOGGER.warn("Or, what. Player {} not found in level (might be out of range)", msg.playerId());
                        return;
                    }

                    var trails = DataUtils.getTrailsData(entity);
                    if (trails == null) {
                        LOGGER.error("How did I?! Uuuughh! Failed to get trails data for player {}", msg.playerId());
                        return;
                    }

                    trails.syncEquippedFromPacket(msg.trails());
                    LOGGER.debug("Synced {} active trails for {}", msg.trails().size(), entity.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync trails for player {}", msg.playerId(), e);
                }
            });
        });
    }
}
