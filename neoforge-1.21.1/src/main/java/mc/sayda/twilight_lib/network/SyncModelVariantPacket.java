package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Network packet to sync player model variant (Steve/Alex) from server to client.
 * Sent on login, respawn, and when variant changes.
 */
public record SyncModelVariantPacket(UUID playerId, String modelVariant, boolean hasCustomVariant) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncModelVariantPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_model_variant"));

    // Sentinel UUID for malformed packets
    private static final UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            try {
                long mostSig = buf.readLong();
                long leastSig = buf.readLong();
                return new UUID(mostSig, leastSig);
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncModelVariantPacket: {}", e.getMessage());
                return SENTINEL_UUID; // Return sentinel on decode error
            }
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncModelVariantPacket> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC,
        SyncModelVariantPacket::playerId,
        ByteBufCodecs.STRING_UTF8,
        SyncModelVariantPacket::modelVariant,
        ByteBufCodecs.BOOL,
        SyncModelVariantPacket::hasCustomVariant,
        SyncModelVariantPacket::new
    );

    public static SyncModelVariantPacket of(UUID id, IModelVariant modelVariant) {
        return new SyncModelVariantPacket(
            id,
            modelVariant.getModelVariant(),
            modelVariant.hasCustomVariant()
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncModelVariantPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                // Detect malformed packets from decode errors
                if (msg.playerId().equals(SENTINEL_UUID)) {
                    LOGGER.warn("This will be fine! Things break all the time. Received malformed model variant packet with null UUID - packet decode failed");
                    return;
                }

                // Update client-side cache for mixin to use (works even if entity not loaded yet)
                if (!msg.hasCustomVariant()) {
                    ClientModelVariantCache.setModelVariant(msg.playerId(), null);
                } else {
                    ClientModelVariantCache.setModelVariant(msg.playerId(), msg.modelVariant());
                }

                var level = Minecraft.getInstance().level;
                if (level == null) {
                    LOGGER.warn("Are we done in this reality yet? Cannot sync model variant - level is null");
                    return;
                }
                var entity = level.getPlayerByUUID(msg.playerId());
                if (entity == null) {
                    LOGGER.debug("I wonder who's around... Player {} not found in level (cached anyway)", msg.playerId());
                    return;
                }

                IModelVariant modelVariant = entity.getData(ModAttachments.MODEL_VARIANT);
                if (modelVariant == null) {
                    LOGGER.error("Failed to get model variant data for player {}", msg.playerId());
                    return;
                }
                // CLIENT-SIDE ATTACHMENT MODIFICATION: This is intentional and safe
                // Server is authoritative and sends sync packets on login/respawn
                // Client attachments are read-only cache for rendering, no gameplay logic depends on them
                if (!msg.hasCustomVariant()) {
                    modelVariant.clearCustomVariant();
                } else {
                    modelVariant.setModelVariant(msg.modelVariant());
                }
                LOGGER.debug("Time to change! Synced model variant {} for {}", msg.modelVariant(), entity.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Failed to sync model variant for player {}", msg.playerId(), e);
            }
        });
    }
}