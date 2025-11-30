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
 * Mixin to override player model variant (Steve vs Alex / default vs slim arms).
 * Allows custom model variant selection independent of UUID-based default.
 * Updated for Forge 1.20.1 - handles both dev (MojMap) and production (SRG) environments.
 */
@Mixin(value = PlayerInfo.class, remap = false)
public class PlayerModelVariantMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Production injection (SRG name)
    @Inject(
            method = "m_105336_", // SRG name for getModelName() in 1.20.1
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0 // Optional - works in production
    )
    private void twilightlib$overrideModelName_SRG(CallbackInfoReturnable<String> cir) {
        overrideModelName(cir);
    }

    // Development injection (MojMap name)
    @Inject(
            method = "getModelName", // MojMap name for dev environment
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0 // Optional - works in dev
    )
    private void twilightlib$overrideModelName_MojMap(CallbackInfoReturnable<String> cir) {
        overrideModelName(cir);
    }

    // Shared logic for both injections
    private void overrideModelName(CallbackInfoReturnable<String> cir) {
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
