package mc.sayda.twilight_lib.mixin.client;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Unique
    private float lastFovModifier = 1.0F;
    @Unique
    private static final float FOV_DELTA_EPSILON = 0.001F;

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void twilight_lib$modifyFov(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        float currentModifier = cir.getReturnValue();

        // Get the attribute holder
        var attributeHolder = ModAttributes.getHolder(ModAttributes.FOV_MODIFIER);

        if (!player.getAttributes().hasAttribute(attributeHolder)) {
            return;
        }

        double fovModifierAttr = player.getAttributeValue(attributeHolder);

        // Case 1: fovModifier >= 1.0 (passthrough)
        if (fovModifierAttr >= 1.0) {
            lastFovModifier = currentModifier;
            return;
        }

        // Case 2: fovModifier <= 0.0 (force normal FOV)
        if (fovModifierAttr <= 0.0) {
            cir.setReturnValue(1.0F);
            lastFovModifier = 1.0F;
            return;
        }

        // Case 3: partial suppression
        float fovDelta = currentModifier - lastFovModifier;
        float distanceFromNormal = Math.abs(currentModifier - 1.0F);
        float lastDistanceFromNormal = Math.abs(lastFovModifier - 1.0F);
        boolean movingTowardsNormal = distanceFromNormal < lastDistanceFromNormal;

        if (!movingTowardsNormal && Math.abs(fovDelta) > FOV_DELTA_EPSILON) {
            float scaledDelta = fovDelta * (float) fovModifierAttr;
            float newModifier = lastFovModifier + scaledDelta;
            cir.setReturnValue(newModifier);
            lastFovModifier = newModifier;
        } else {
            lastFovModifier = currentModifier;
        }
    }
}
