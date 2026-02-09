package mc.sayda.twilight_lib.mixin.client;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to GameRenderer to scale Field of View (FOV) based on the FOV_MODIFIER
 * attribute.
 * This provides the cross-platform equivalent of NeoForge's
 * ComputeFovModifierEvent.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Unique
    private float twilightlib$lastFovModifier = 1.0F;
    @Unique
    private static final float FOV_DELTA_EPSILON = 0.001F;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void twilightlib$scaleFov(CallbackInfoReturnable<Double> cir) {
        Player player = this.minecraft.player;
        if (player == null)
            return;

        // Check if player has the attribute
        @SuppressWarnings("null")
        boolean hasAttr = player.getAttributes().hasAttribute(ModAttributes.getHolder(ModAttributes.FOV_MODIFIER));
        if (!hasAttr) {
            return;
        }

        @SuppressWarnings("null")
        double fovModifier = player.getAttributeValue(ModAttributes.getHolder(ModAttributes.FOV_MODIFIER));

        // Case 1: fovModifier >= 1.0 (passthrough mode)
        if (fovModifier >= 1.0) {
            twilightlib$lastFovModifier = cir.getReturnValue().floatValue();
            return;
        }

        float currentModifier = cir.getReturnValue().floatValue();
        float fovDelta = currentModifier - twilightlib$lastFovModifier;
        float distanceFromNormal = Math.abs(currentModifier - 1.0F);
        float lastDistanceFromNormal = Math.abs(twilightlib$lastFovModifier - 1.0F);
        boolean movingTowardsNormal = distanceFromNormal < lastDistanceFromNormal;

        // Case 2: fovModifier <= 0.0 (force normal FOV)
        if (fovModifier <= 0.0) {
            cir.setReturnValue(1.0);
            twilightlib$lastFovModifier = 1.0F;
        }
        // Case 3: 0.0 < fovModifier < 1.0 (partial suppression)
        else {
            if (!movingTowardsNormal && Math.abs(fovDelta) > FOV_DELTA_EPSILON) {
                float scaledDelta = fovDelta * (float) fovModifier;
                float newModifier = twilightlib$lastFovModifier + scaledDelta;
                cir.setReturnValue((double) newModifier);
                twilightlib$lastFovModifier = newModifier;
            } else {
                twilightlib$lastFovModifier = currentModifier;
            }
        }
    }
}
