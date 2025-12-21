package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Mixin to override player model variant (Steve vs Alex / default vs slim arms).
 * Allows custom model variant selection independent of UUID-based default.
 *
 * <p><b>Why this mixin?</b> In Minecraft 1.21.1, player model type (classic 4px arms vs slim 3px arms)
 * is determined by the {@link PlayerInfo#getSkin()} method, which returns a {@link PlayerSkin} object
 * containing the model type. By intercepting this method, we can replace the PlayerSkin with a modified
 * version that has our desired model type.
 *
 * <p><b>Client-side only</b>: This mixin is client-side because model rendering happens on the client.
 * The model variant data is synced from server via network packets and cached in {@link ClientModelVariantCache}.
 *
 * <p><b>Injection Point</b>: Injects at RETURN to override the return value after vanilla logic runs.
 * This ensures we don't break any vanilla behavior that depends on the original skin data.
 *
 * @author SaydaGames (mc_jojo3)
 * @version 2.0
 */
@Mixin(value = PlayerInfo.class, remap = false)
public class PlayerModelVariantMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Overrides player model type based on custom model variant.
     *
     * <p><b>How it works</b>:
     * <ol>
     *   <li>Vanilla determines model type based on UUID/skin and returns a PlayerSkin object</li>
     *   <li>We check if this player has a custom model variant set</li>
     *   <li>If yes, create a new PlayerSkin with the desired model (WIDE for Steve, SLIM for Alex)</li>
     *   <li>If no custom variant, return vanilla's original PlayerSkin</li>
     * </ol>
     *
     * <p><b>Model Type Mapping</b>:
     * <ul>
     *   <li>"steve" → PlayerSkin.Model.WIDE (classic 4px arms)</li>
     *   <li>"alex" → PlayerSkin.Model.SLIM (slim 3px arms)</li>
     * </ul>
     *
     * <p><b>Why check client player?</b> On the client, we need to get the actual Player entity
     * to access attachments. We first check the client player (for first-person rendering),
     * then fall back to cached data synced from server (for third-person and other players).
     *
     * @param cir Mixin callback info containing the return value (PlayerSkin)
     */
    @Inject(
        method = "getSkin",
        at = @At("RETURN"),
        cancellable = true
    )
    private void twilightlib$overridePlayerSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        try {
            PlayerInfo playerInfo = (PlayerInfo) (Object) this;
            UUID playerUUID = playerInfo.getProfile().getId();
            PlayerSkin originalSkin = cir.getReturnValue();

            // Try to get model variant from client player attachment (for local player)
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(playerUUID)) {
                IModelVariant modelVariant = mc.player.getData(ModAttachments.MODEL_VARIANT);
                if (modelVariant != null && modelVariant.hasCustomVariant()) {
                    String variant = modelVariant.getModelVariant();
                    PlayerSkin.Model newModel = variant.equals("alex") ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;

                    // Create new PlayerSkin with modified model
                    PlayerSkin newSkin = new PlayerSkin(
                        originalSkin.texture(),
                        originalSkin.textureUrl(),
                        originalSkin.capeTexture(),
                        originalSkin.elytraTexture(),
                        newModel,
                        originalSkin.secure()
                    );
                    cir.setReturnValue(newSkin);
                    LOGGER.debug("Time to change! Overriding player model to: {}", newModel);
                }
                return;
            }

            // For other players, check client-side cache (synced from server)
            String cachedVariant = ClientModelVariantCache.getModelVariant(playerUUID);
            if (cachedVariant != null) {
                PlayerSkin.Model newModel = cachedVariant.equals("alex") ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;

                // Create new PlayerSkin with modified model
                PlayerSkin newSkin = new PlayerSkin(
                    originalSkin.texture(),
                    originalSkin.textureUrl(),
                    originalSkin.capeTexture(),
                    originalSkin.elytraTexture(),
                    newModel,
                    originalSkin.secure()
                );
                cir.setReturnValue(newSkin);
                LOGGER.debug("Time to change! Overriding player model to: {}", newModel);
            }
        } catch (NullPointerException | IllegalStateException e) {
            LOGGER.error("How did I?! Uuuughh! Failed to override player skin model", e);
        }
    }
}