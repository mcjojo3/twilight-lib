package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.capabilities.TrailsData;
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

public record SyncTrailsPacket(UUID playerId, Set<String> trails) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncTrailsPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_trails"));

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
        ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 1000),
        SyncTrailsPacket::trails,
        SyncTrailsPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTrailsPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                // Detect malformed packets from decode errors
                if (msg.playerId().equals(SENTINEL_UUID)) {
                    LOGGER.error("This will be fine! Things break all the time. Received malformed SyncTrailsPacket with invalid UUID - packet decode failed");
                    return;
                }

                var level = Minecraft.getInstance().level;
                if (level == null) {
                    LOGGER.warn("Are we done in this reality yet? Cannot sync trails - level is null");
                    return;
                }
                var entity = level.getPlayerByUUID(msg.playerId());
                if (entity == null) {
                    LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId());
                    return;
                }

                var trails = entity.getData(ModAttachments.TRAILS);
                if (trails == null) {
                    LOGGER.error("How did I?! Uuuughh! Failed to get trails data for player {}", msg.playerId());
                    return;
                }

                // Type-safe cast with validation to prevent crashes in heavily modded environments
                if (!(trails instanceof TrailsData)) {
                    LOGGER.error("Incompatible trails attachment implementation for player {}. Expected TrailsData but got {}. " +
                                 "This may be caused by another mod replacing the attachment.",
                                 entity.getName().getString(), trails.getClass().getName());
                    return; // Gracefully skip instead of crashing
                }

                // Directly sync equipped trails from server (bypasses ownership validation)
                ((TrailsData) trails).syncEquippedFromPacket(msg.trails());
                LOGGER.debug("Time to change! Synced {} active trails for {}",
                    msg.trails().size(), entity.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to sync trails for player {}", msg.playerId(), e);
            }
        });
    }
}
