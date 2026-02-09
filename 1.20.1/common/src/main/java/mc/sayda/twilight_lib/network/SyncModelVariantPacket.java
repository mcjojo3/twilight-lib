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
                var player = context.getPlayer();
                var level = (player != null) ? player.level() : net.minecraft.client.Minecraft.getInstance().level;
                if (level == null)
                    return;
                var entity = level.getPlayerByUUID(this.playerId);
                if (entity == null)
                    return;
                String validatedVariant = this.variant;
                if (!mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                        .get(new net.minecraft.resources.ResourceLocation(validatedVariant))
                        .isPresent()) {
                    validatedVariant = "none";
                }

                var data = DataUtils.getModelVariantData(entity);
                if (data != null) {
                    data.setModelVariant(validatedVariant);
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
