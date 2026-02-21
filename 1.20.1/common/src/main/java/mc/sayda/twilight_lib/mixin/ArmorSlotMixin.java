package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class ArmorSlotMixin {

    @Shadow
    @Final
    public Container container;

    @Shadow
    public abstract int getContainerSlot();

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void twilight$onMayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem))
            return;

        if (this.container instanceof Inventory inventory && this.getContainerSlot() >= 36
                && this.getContainerSlot() <= 39) {
            Player player = inventory.player;
            if (player == null)
                return;

            EquipmentSlot equipmentSlot = switch (this.getContainerSlot()) {
                case 39 -> EquipmentSlot.HEAD;
                case 38 -> EquipmentSlot.CHEST;
                case 37 -> EquipmentSlot.LEGS;
                case 36 -> EquipmentSlot.FEET;
                default -> null;
            };

            if (equipmentSlot == null)
                return;

            AttributeInstance attributeInstance = switch (armorItem.getType()) {
                case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
                case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
                case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
                case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
                default -> null;
            };

            if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
                cir.setReturnValue(false);
            }
        }
    }
}
