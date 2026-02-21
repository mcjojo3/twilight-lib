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
 * TODO: Improve Fabric-side armor slot write protection for 1.20.1.
 * 
 * WHY THIS EXISTS AND DIFFERS FROM OTHER VERSIONS/PLATFORMS:
 * - In 1.21.1, we safely replace the slots entirely using NeoForge/Fabric API
 * events.
 * - In 1.20.1 Forge, we have `IForgeItem.canEquip()` to cleanly block
 * placement.
 * - In 1.20.1 Fabric, neither exist. Replacing `InventoryMenu` slots via Mixin
 * can likely break mods,
 * and Fabric lacks a `canEquip` hook. Furthermore, `AbstractContainerMenuMixin`
 * `Slot.set` redirects
 * don't catch drag-and-drop on Fabric due to how `InventoryMenu$1` overrides
 * it.
 * 
 * Therefore, we intercept the absolute lowest level: `Inventory.setItem` at
 * slots 36–39.
 * This guarantees we catch drag-and-drop, hotbar swaps, and shift-clicks.
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
