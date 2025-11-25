package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.ModelVariantProvider;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Mixin to override player model type (Steve vs Alex) based on custom model variant.
 * Updated for Forge 1.20.1 using SRG names.
 */
@Mixin(value = PlayerInfo.class, remap = false)
public class PlayerInfoMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(
            method = "m_105336_", // SRG name for getModelName() in 1.20.1
            at = @At("RETURN"),
            cancellable = true
    )
    private void twilightlib$overrideModelName(CallbackInfoReturnable<String> cir) {
        try {
            PlayerInfo playerInfo = (PlayerInfo) (Object) this;
            UUID playerUUID = playerInfo.getProfile().getId();

            // Check client player first (for first-person rendering)
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(playerUUID)) {
                mc.player.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
                    if (modelVariant.hasCustomVariant()) {
                        String variant = modelVariant.getModelVariant();
                        String modelName = variant.equals("alex") ? "slim" : "default";
                        cir.setReturnValue(modelName);
                        LOGGER.debug("Time to change! Overriding player model to: {}", modelName);
                    }
                });
                return;
            }

            // For other players, use client-side cache
            String cachedVariant = ClientModelVariantCache.getModelVariant(playerUUID);
            if (cachedVariant != null) {
                String modelName = cachedVariant.equals("alex") ? "slim" : "default";
                cir.setReturnValue(modelName);
                LOGGER.debug("Time to change! Overriding player model to: {}", modelName);
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to override player model type", e);
        }
    }
}
