package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    protected LivingEntityMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean twilight_lib$redirectIsElytra(ItemStack stack, Item item) {
        if (item == Items.ELYTRA && ((LivingEntity) (Object) this)
                .getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT)) > 0) {
            return true;
        }
        return stack.is(item);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"), require = 0)
    private boolean twilight_lib$redirectIsFlyEnabled(ItemStack stack) {
        if (((LivingEntity) (Object) this)
                .getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT)) > 0) {
            return true;
        }
        return ElytraItem.isFlyEnabled(stack);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"), require = 0)
    private void twilight_lib$redirectHurtAndBreak(ItemStack stack, int amount, LivingEntity entity,
            EquipmentSlot slot) {
        if (entity.getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT)) > 0) {
            return; // Don't damage "phantom" elytra
        }
        stack.hurtAndBreak(amount, entity, slot);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void twilight_lib$onTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isFallFlying() && self.getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT)) > 0) {
            // input.z > 0 means forward input
            if (input.z > 0) {
                Vec3 look = this.getLookAngle();
                Vec3 move = this.getDeltaMovement();
                double attributeValue = self.getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT));
                double boost = attributeValue * 0.001;
                this.setDeltaMovement(move.add(look.x * boost, look.y * boost, look.z * boost));
            }
        }
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void twilight_lib$modifyDimensions(net.minecraft.world.entity.Pose pose,
            CallbackInfoReturnable<net.minecraft.world.entity.EntityDimensions> cir) {
        if ((Object) this instanceof Player player) {
            cir.setReturnValue(
                    mc.sayda.twilight_lib.TwilightEventHandler.getMorphDimensions(player, pose, cir.getReturnValue()));
        }
    }
}
