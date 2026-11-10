package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks restricted armor at Inventory.setItem (slots 36-39). 1.20.1 Fabric has no
 * canEquip hook, and slot redirects miss drag-and-drop.
 *
 * TODO: find a cleaner hook.
 */
@Mixin(Inventory.class)
public class FabricInventoryArmorMixin {

    @Shadow
    public Player player;

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void twilight$preventForbiddenArmor(int slot, ItemStack stack, CallbackInfo ci) {
        // Armor slots in Inventory: 36 = boots, 37 = leggings, 38 = chestplate, 39 =
        // helmet
        if (slot < 36 || slot > 39)
            return;
        if (stack == null || stack.isEmpty())
            return;
        if (player == null)
            return;
        if (!(stack.getItem() instanceof ArmorItem armorItem))
            return;

        AttributeInstance attributeInstance = switch (armorItem.getType()) {
            case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
            case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
            case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
            case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
            default -> null;
        };

        if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
            if (!player.level().isClientSide) {
                // TODO: A better UX would be to place the item back onto the cursor stack
                player.getInventory().placeItemBackInInventory(stack);
            }
            ci.cancel();
        }
    }
}
