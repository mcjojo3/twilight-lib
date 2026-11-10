package mc.sayda.twilight_lib.mixin.client;

import mc.sayda.twilight_lib.client.renderer.AddonRenderHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips vanilla armor overlays that an active addon hides with a {@code *_ARMOR}
 * BodyPart (e.g. the mermaid tail hides leggings and boots and draws them itself).
 *
 * <p>
 * Hooks setPartVisibility rather than renderArmorPiece because its signature is the
 * same on vanilla, Forge and NeoForge (NeoForge adds a renderArmorPiece overload).
 * Hiding every part of the armor model also skips the trim and glint passes, since
 * they reuse the same model.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(method = "setPartVisibility", at = @At("RETURN"))
    private void twilight_lib$hideAddonArmor(HumanoidModel<?> model, EquipmentSlot slot, CallbackInfo ci) {
        RenderLayer<?, ?> self = (RenderLayer<?, ?>) (Object) this;
        if (AddonRenderHandler.isArmorSlotHidden(self.getParentModel(), slot)) {
            model.setAllVisible(false);
        }
    }
}
