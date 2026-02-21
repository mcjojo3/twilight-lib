package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean twilight$redirectMayPlace(Slot slot, ItemStack stack) {
        if (!slot.mayPlace(stack)) {
            return false;
        }
        return !twilight$isForbidden(slot, stack);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;set(Lnet/minecraft/world/item/ItemStack;)V"))
    private void twilight$redirectSet(Slot slot, ItemStack stack) {
        if (twilight$isForbidden(slot, stack)) {
            return;
        }
        slot.set(stack);
    }

    @Unique
    private boolean twilight$isForbidden(Slot slot, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
            return false;
        }

        if (slot.container instanceof Inventory inventory && slot.getContainerSlot() >= 36
                && slot.getContainerSlot() <= 39) {
            Player player = inventory.player;
            if (player == null) {
                return false;
            }

            EquipmentSlot equipmentSlot = switch (slot.getContainerSlot()) {
                case 39 -> EquipmentSlot.HEAD;
                case 38 -> EquipmentSlot.CHEST;
                case 37 -> EquipmentSlot.LEGS;
                case 36 -> EquipmentSlot.FEET;
                default -> null;
            };

            if (equipmentSlot == null) {
                return false;
            }

            AttributeInstance attributeInstance = switch (armorItem.getType()) {
                case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
                case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
                case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
                case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
                default -> null;
            };

            if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
                return true;
            }
        }
        return false;
    }
}
