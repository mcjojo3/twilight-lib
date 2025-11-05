package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public record SyncMorphPacket(UUID playerId, Optional<ResourceLocation> entity) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncMorphPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_morph"));

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            return new UUID(mostSig, leastSig);
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncMorphPacket> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC,
        SyncMorphPacket::playerId,
        ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
        SyncMorphPacket::entity,
        SyncMorphPacket::new
    );

    public static SyncMorphPacket of(UUID id, ResourceLocation rlOrNull) {
        return new SyncMorphPacket(id, Optional.ofNullable(rlOrNull));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncMorphPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Cannot sync morph - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId());
            if (entity == null) {
                LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId());
                return;
            }

            IMorph morph = entity.getData(ModAttachments.MORPH);
            // CLIENT-SIDE ATTACHMENT MODIFICATION: This is intentional and safe
            // Server is authoritative and sends sync packets on login/respawn
            // Client attachments are read-only cache for rendering, no gameplay logic depends on them
            morph.setEntityType(msg.entity());
            // CRITICAL: Refresh dimensions on the client side after attachment update
            entity.refreshDimensions();
            LOGGER.debug("Trickster never loses. Because Zoe changes the rules. Synced morph {} for {}", msg.entity(), entity.getName().getString());
        });
    }
}
