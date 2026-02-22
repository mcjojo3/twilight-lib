package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncModelVariantPacket {
    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_model_variant");

    private final UUID playerId;
    private final String variant;

    public SyncModelVariantPacket(UUID playerId, String variant) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.variant = variant != null ? variant : "none";
    }

    public SyncModelVariantPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.variant = buf.readUtf(mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeUtf(this.variant != null ? this.variant : "none");
    }

    public static SyncModelVariantPacket of(UUID id, IModelVariant mv) {
        return new SyncModelVariantPacket(id, mv.getModelVariant());
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                net.minecraft.resources.ResourceLocation validatedVariant;
                if (this.variant.equalsIgnoreCase("steve") || this.variant.equalsIgnoreCase("alex")) {
                    validatedVariant = new net.minecraft.resources.ResourceLocation("twilight_lib",
                            this.variant.toLowerCase());
                } else if (this.variant.equalsIgnoreCase("none")) {
                    validatedVariant = null;
                } else {
                    try {
                        validatedVariant = new net.minecraft.resources.ResourceLocation(this.variant);
                    } catch (Exception e) {
                        validatedVariant = null;
                    }
                }

                if (validatedVariant != null
                        && !mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                                .get(validatedVariant)
                                .isPresent()) {
                    validatedVariant = new net.minecraft.resources.ResourceLocation("twilight_lib", "steve");
                }

                // 1. Always update the client-side cache (essential for other players and self
                // fallback)
                if (this.variant.equalsIgnoreCase("none")) {
                    mc.sayda.twilight_lib.client.ClientModelVariantCache.setModelVariant(this.playerId, null);
                } else {
                    mc.sayda.twilight_lib.client.ClientModelVariantCache.setModelVariant(this.playerId,
                            validatedVariant);
                }

                // 2. Try to update the capability on the player entity if it's currently loaded
                var player = context.getPlayer();
                var level = (player != null) ? player.level() : mc.sayda.twilight_lib.client.ClientAccess.getLevel();
                if (level == null)
                    return;

                var entity = level.getPlayerByUUID(this.playerId);
                if (entity == null)
                    return;

                var data = DataUtils.getModelVariantData(entity);
                if (data != null) {
                    if (this.variant.equalsIgnoreCase("none")) {
                        data.clearCustomVariant();
                    } else {
                        data.setModelVariant(this.variant);
                    }
                }
            });
        });
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getVariant() {
        return variant;
    }
}
