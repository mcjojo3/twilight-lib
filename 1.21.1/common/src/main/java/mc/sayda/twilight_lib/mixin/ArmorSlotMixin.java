package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.inventory.ArmorSlot.class)
public class ArmorSlotMixin {

    @Shadow
    @Final
    private LivingEntity owner;
    @Shadow
    @Final
    private EquipmentSlot slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void twilight$onMayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem))
            return;
        if (!(this.owner instanceof Player player))
            return;

        AttributeInstance attributeInstance = switch (armorItem.getType()) {
            case HELMET -> player.getAttribute(ModAttributes.getHolder(ModAttributes.ALLOW_HELMET));
            case CHESTPLATE -> player.getAttribute(ModAttributes.getHolder(ModAttributes.ALLOW_CHESTPLATE));
            case LEGGINGS -> player.getAttribute(ModAttributes.getHolder(ModAttributes.ALLOW_LEGGINGS));
            case BOOTS -> player.getAttribute(ModAttributes.getHolder(ModAttributes.ALLOW_BOOTS));
            default -> null;
        };

        if (attributeInstance != null && attributeInstance.getValue() == 0.0) {
            cir.setReturnValue(false);
        }
    }
}
