package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.ModelVariantProvider;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Mixin to override player model type (Steve vs Alex) based on custom model variant.
 *
 * <p><b>Why this mixin?</b> Minecraft determines player model type (classic 4px arms vs slim 3px arms)
 * in the {@link PlayerInfo#getModelName()} method. This method is called by the renderer to select
 * which model to use. By intercepting this method, we can force a specific model type regardless
 * of the player's actual skin type.
 *
 * <p><b>Client-side only</b>: This mixin is client-side because model rendering happens on the client.
 * The model variant data is synced from server via network packets and cached in {@link ClientModelVariantCache}.
 *
 * <p><b>Injection Point</b>: Injects at RETURN to override the return value after vanilla logic runs.
 * This ensures we don't break any vanilla behavior that depends on the original model name.
 *
 * @author Sayda (MrJojo)
 * @version 1.0
 */
@Mixin(value = PlayerInfo.class, remap = false)
public class PlayerInfoMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Overrides player model type based on custom model variant.
     *
     * <p><b>How it works</b>:
     * <ol>
     *   <li>Vanilla determines model type ("default" or "slim") based on UUID/skin</li>
     *   <li>We check if this player has a custom model variant set</li>
     *   <li>If yes, override the return value with "default" (Steve) or "slim" (Alex)</li>
     *   <li>If no custom variant, return vanilla's original value</li>
     * </ol>
     *
     * <p><b>Model Name Mapping</b>:
     * <ul>
     *   <li>"steve" → "default" (classic 4px arms)</li>
     *   <li>"alex" → "slim" (slim 3px arms)</li>
     * </ul>
     *
     * <p><b>Why check client player?</b> On the client, we need to get the actual Player entity
     * to access capabilities. We first check the client player (for first-person rendering),
     * then fall back to cached data synced from server (for third-person and other players).
     *
     * @param cir Mixin callback info containing the return value
     */
    @Inject(
        method = "getModelName()Ljava/lang/String;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void twilightlib$overrideModelName(CallbackInfoReturnable<String> cir) {
        try {
            PlayerInfo playerInfo = (PlayerInfo) (Object) this;
            UUID playerUUID = playerInfo.getProfile().getId();

            // Try to get model variant from client player capability (for local player)
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(playerUUID)) {
                mc.player.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
                    if (modelVariant.hasCustomVariant()) {
                        String variant = modelVariant.getModelVariant();
                        String modelName = variant.equals("alex") ? "slim" : "default";
                        cir.setReturnValue(modelName);
                        LOGGER.debug("Overriding local player model to: {}", modelName);
                    }
                });
                return;
            }

            // For other players, check client-side cache (synced from server)
            String cachedVariant = ClientModelVariantCache.getModelVariant(playerUUID);
            if (cachedVariant != null) {
                String modelName = cachedVariant.equals("alex") ? "slim" : "default";
                cir.setReturnValue(modelName);
                LOGGER.debug("Overriding player {} model to: {}", playerUUID, modelName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to override player model type", e);
        }
    }
}
