package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Network packet to sync player model variant (Steve/Alex) from server to
 * client.
 * Sent on login, respawn, and when variant changes.
 */
public record SyncModelVariantPacket(UUID playerId, net.minecraft.resources.ResourceLocation modelVariant,
        boolean hasCustomVariant) implements CustomPacketPayload {
    public SyncModelVariantPacket {
        java.util.Objects.requireNonNull(playerId, "playerId");
        if (modelVariant == null) {
            modelVariant = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("twilight_lib", "steve");
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncModelVariantPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_model_variant"));

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
            ResourceLocation.STREAM_CODEC,
            SyncModelVariantPacket::modelVariant,
            ByteBufCodecs.BOOL,
            SyncModelVariantPacket::hasCustomVariant,
            SyncModelVariantPacket::new);

    public static SyncModelVariantPacket of(UUID id, IModelVariant modelVariant) {
        return new SyncModelVariantPacket(
                id,
                modelVariant.getVariant().map(mc.sayda.twilight_lib.api.model_variant.IModelVariant::getId)
                        .orElse(ResourceLocation.fromNamespaceAndPath("twilight_lib", "steve")),
                modelVariant.hasCustomVariant());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncModelVariantPacket msg,
            dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                try {
                    if (msg.playerId().equals(SENTINEL_UUID)) {
                        LOGGER.warn("How did I?! Uuuughh! Received malformed model variant packet with null UUID");
                        return;
                    }

                    net.minecraft.resources.ResourceLocation validatedVariant = msg.modelVariant();
                    if (!mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                            .get(validatedVariant)
                            .isPresent()) {
                        LOGGER.warn("Or, what. Rejected invalid model variant from network: '{}'", validatedVariant);
                        validatedVariant = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("twilight_lib",
                                "steve");
                    }

                    if (!msg.hasCustomVariant()) {
                        mc.sayda.twilight_lib.client.ClientModelVariantCache.setModelVariant(msg.playerId(), null);
                    } else {
                        mc.sayda.twilight_lib.client.ClientModelVariantCache.setModelVariant(msg.playerId(),
                                validatedVariant);
                    }

                    var level = context.getPlayer().level();
                    if (level == null)
                        return;

                    var entity = level.getPlayerByUUID(msg.playerId());
                    if (entity == null) {
                        LOGGER.debug("This will be fine! Player {} not found in level (cached anyway)", msg.playerId());
                        return;
                    }

                    IModelVariant modelVariant = DataUtils.getModelVariantData(entity);
                    if (modelVariant == null)
                        return;

                    if (!msg.hasCustomVariant()) {
                        modelVariant.clearCustomVariant();
                    } else {
                        modelVariant
                                .setVariant(mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                                        .get(validatedVariant));
                    }
                    LOGGER.debug("Time to change! Synced model variant {} for {}", validatedVariant,
                            entity.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync model variant for player {}", msg.playerId(), e);
                }
            });
        });
    }
}