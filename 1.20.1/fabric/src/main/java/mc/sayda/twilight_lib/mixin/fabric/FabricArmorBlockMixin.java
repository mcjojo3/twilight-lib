package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric-side armor equip blocking.
 * Intercepts Player.setItemSlot before armor is placed into an equipment slot.
 * Covers drag-to-slot, hotbar-key-swap, and any other equip path on Fabric.
 */
@Mixin(Player.class)
public class FabricArmorBlockMixin {

    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void twilight$onSetItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (stack.isEmpty())
            return;
        if (!(stack.getItem() instanceof ArmorItem armorItem))
            return;

        AttributeInstance attributeInstance = switch (armorItem.getType()) {
            case HELMET -> self.getAttribute(ModAttributes.ALLOW_HELMET.get());
            case CHESTPLATE -> self.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
            case LEGGINGS -> self.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
            case BOOTS -> self.getAttribute(ModAttributes.ALLOW_BOOTS.get());
            default -> null;
        };

        if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
            ci.cancel();
        }
    }
}
