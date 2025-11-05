package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Packet to sync trails data between server and clients
 */
public record SyncTrailsPacket(UUID playerUuid, CompoundTag trailsData) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncTrailsPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_trails"));

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

    public static final StreamCodec<ByteBuf, SyncTrailsPacket> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC,
        SyncTrailsPacket::playerUuid,
        ByteBufCodecs.COMPOUND_TAG,
        SyncTrailsPacket::trailsData,
        SyncTrailsPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTrailsPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Validate packet data
            if (msg.trailsData() == null || msg.trailsData().isEmpty()) {
                LOGGER.warn("What's with all the negative waves? Received invalid trail data for player {}", msg.playerUuid());
                return;
            }

            // Client-side handling
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player player = mc.level.getPlayerByUUID(msg.playerUuid());
                if (player != null) {
                    var trails = player.getData(ModAttachments.TRAILS);
                    trails.deserialize(msg.trailsData());
                }
            }
        });
    }
}
