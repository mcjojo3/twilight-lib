package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {

    @Inject(method = { "use", "m_7203_" }, at = @At("HEAD"), cancellable = true, remap = false)
    private void twilight$onUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        AttributeInstance attributeInstance = switch (armorItem.getType()) {
            case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
            case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
            case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
            case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
            default -> null;
        };

        if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }

    // Dev environment redirect
    @Redirect(method = "dispenseArmor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"), remap = false, require = 0)
    private static void twilight$redirectSetItemSlotDev(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        twilight$commonSetItemSlot(entity, slot, stack);
    }

    // Production environment redirect (SRG)
    @Redirect(method = "m_40398_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"), remap = false, require = 0)
    private static void twilight$redirectSetItemSlotProd(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        twilight$commonSetItemSlot(entity, slot, stack);
    }

    @Unique
    private static void twilight$commonSetItemSlot(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        if (entity instanceof Player player && stack.getItem() instanceof ArmorItem armorItem) {
            AttributeInstance attributeInstance = switch (armorItem.getType()) {
                case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
                case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
                case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
                case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
                default -> null;
            };

            if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
                return; // Block equipping
            }
        }
        entity.setItemSlot(slot, stack);
    }

    /**
     * Prevents equipping via inventory slots (dragging/shift-clicking).
     * This overrides the IForgeItem default implementation.
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
        return Mob.getEquipmentSlotForItem(stack) == armorType;
    }
}
