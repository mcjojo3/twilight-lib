package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {
    @Shadow
    @Final
    private Player owner;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void twilight$onQuickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = player.containerMenu.getSlot(index).getItem();
        if (stack.getItem() instanceof ArmorItem armorItem) {
            AttributeInstance attributeInstance = switch (armorItem.getType()) {
                case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
                case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
                case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
                case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
                default -> null;
            };

            if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
                // If it's an armor item and we're disallowed, block shift-clicking if it would
                // go into an armor slot
                // In InventoryMenu, slots 36-39 are armor. quickMoveStack for inventory slots
                // (9-35, 0-8)
                // would try to move them to armor slots if they fit.
                if (index < 36 || index > 39) {
                    cir.setReturnValue(ItemStack.EMPTY);
                }
            }
        }
    }
}
