package mc.sayda.twilight_lib.mixin.forge;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ArmorItem.class, remap = false)
public class ForgeArmorItemMixin {

    /**
     * Forge-specific hook to block equipping.
     * This is used by many Forge-based inventory interactions.
     */
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        if (entity instanceof Player player && stack.getItem() instanceof ArmorItem armorItem) {
            AttributeInstance attributeInstance = switch (armorItem.getType()) {
                case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
                case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
                case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
                case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
                default -> null;
            };

            if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
                return false;
            }
        }

        // Fallback to default Forge-added behavior if needed,
        // but usually ArmorItem just returns Mob.getEquipmentSlotForItem(stack) ==
        // armorType;
        // However, we can't easily call super or the original logic if it's a default
        // method on IForgeItem.
        // Actually, for ArmorItem, Forge patches it to override canEquip.

        return net.minecraft.world.entity.Mob.getEquipmentSlotForItem(stack) == armorType;
    }
}
