package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilight$onUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        AttributeInstance attributeInstance = switch (armorItem.getType()) {
            case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET);
            case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE);
            case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS);
            case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS);
            default -> null;
        };

        if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        }
    }

    @Redirect(method = "dispenseArmor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"), remap = false)
    private static void twilight$redirectSetItemSlot(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        if (entity instanceof Player player && stack.getItem() instanceof ArmorItem armorItem) {
            AttributeInstance attributeInstance = switch (armorItem.getType()) {
                case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET);
                case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE);
                case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS);
                case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS);
                default -> null;
            };

            if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
                return; // Block equipping by not calling setItemSlot
            }
        }
        entity.setItemSlot(slot, stack);
    }
}