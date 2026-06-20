package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncModelVariantPacket {
    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_model_variant");

    private static final @javax.annotation.Nonnull UUID SENTINEL_UUID = new UUID(0, 0);
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private final UUID playerId;
    private final String variant;

    public SyncModelVariantPacket(UUID playerId, String variant) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.variant = variant != null ? variant : "none";
    }

    public SyncModelVariantPacket(FriendlyByteBuf buf) {
        UUID id;
        try {
            id = buf.readUUID();
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncModelVariantPacket: {}", e.getMessage());
            id = SENTINEL_UUID;
        }
        this.playerId = id;
        this.variant = buf.readUtf(mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(java.util.Objects.requireNonNull(this.playerId, "playerId"));
        buf.writeUtf(this.variant != null ? this.variant : "none");
    }

    public static SyncModelVariantPacket of(UUID id, IModelVariant mv) {
        return new SyncModelVariantPacket(id, mv.getModelVariant());
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> ClientHandler.apply(this)));
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        static void apply(SyncModelVariantPacket pkt) {
            try {
                if (pkt.playerId.equals(SENTINEL_UUID)) {
                    LOGGER.warn("How did I?! Uuuughh! Received malformed sync_model_variant packet with invalid UUID");
                    return;
                }

                ResourceLocation validatedVariant;
                if (pkt.variant.equalsIgnoreCase("steve") || pkt.variant.equalsIgnoreCase("alex")) {
                    validatedVariant = new ResourceLocation("twilight_lib", pkt.variant.toLowerCase());
                } else if (pkt.variant.equalsIgnoreCase("none")) {
                    validatedVariant = null;
                } else {
                    try {
                        validatedVariant = new ResourceLocation(pkt.variant);
                    } catch (Exception e) {
                        validatedVariant = null;
                    }
                }

                if (validatedVariant != null
                        && !mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                                .get(validatedVariant).isPresent()) {
                    LOGGER.warn("Or, what. Rejected invalid model variant from network: '{}'", validatedVariant);
                    validatedVariant = new ResourceLocation("twilight_lib", "steve");
                }

                if (pkt.variant.equalsIgnoreCase("none")) {
                    mc.sayda.twilight_lib.client.ClientModelVariantCache.setModelVariant(pkt.playerId, null);
                } else {
                    mc.sayda.twilight_lib.client.ClientModelVariantCache.setModelVariant(pkt.playerId, validatedVariant);
                }

                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.world.entity.player.Player player = null;

                if (minecraft.player != null && minecraft.player.getUUID().equals(pkt.playerId)) {
                    player = minecraft.player;
                } else if (minecraft.level != null) {
                    player = minecraft.level.getPlayerByUUID(pkt.playerId);
                }

                if (player == null) {
                    LOGGER.warn("Or, what. Player {} not found in level (cached anyway)", pkt.playerId);
                    return;
                }

                var data = DataUtils.getModelVariantData(player);
                if (data == null) {
                    LOGGER.error("How did I?! Uuuughh! Failed to get model variant data for player {}", pkt.playerId);
                    return;
                }

                if (pkt.variant.equalsIgnoreCase("none") || validatedVariant == null) {
                    data.clearCustomVariant();
                } else {
                    data.setVariant(mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry
                            .getInstance().get(validatedVariant));
                }

                LOGGER.debug("Want to see something neat? Synced model variant {} for {}", validatedVariant,
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to sync model variant for player {}", pkt.playerId, e);
            }
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getVariant() {
        return variant;
    }
}
