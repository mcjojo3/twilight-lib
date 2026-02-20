package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    protected LivingEntityMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean twilight_lib$redirectIsElytra(ItemStack stack, net.minecraft.world.item.Item item) {
        if (item == net.minecraft.world.item.Items.ELYTRA) {
            LivingEntity self = (LivingEntity) (Object) this;
            var attr = self.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
            if (attr != null && attr.getValue() > 0 && !self.isInWater()) {
                return true;
            }
        }
        return stack.is(item);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"), require = 0)
    private boolean twilight_lib$redirectIsFlyEnabled(ItemStack stack) {
        LivingEntity self = (LivingEntity) (Object) this;
        var attr = self.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        if (attr != null && attr.getValue() > 0 && !self.isInWater()) {
            return true;
        }
        return net.minecraft.world.item.ElytraItem.isFlyEnabled(stack);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"), require = 0)
    private void twilight_lib$redirectHurtAndBreak(ItemStack stack, int amount, LivingEntity entity,
            java.util.function.Consumer<LivingEntity> onBroken) {
        var attr = entity.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        if (attr != null && attr.getValue() > 0) {
            return; // Don't damage "phantom" elytra
        }
        stack.hurtAndBreak(amount, entity, onBroken);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void twilight_lib$onTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        var attr = self.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        double flightValue = attr != null ? attr.getValue() : 0;
        if (self.isFallFlying() && flightValue > 0) {
            // input.z > 0 means forward input (W), input.z < 0 means backward input (S)
            if (input.z > 0) {
                Vec3 look = this.getLookAngle();
                Vec3 move = this.getDeltaMovement();
                double boost = flightValue * 0.001;
                this.setDeltaMovement(move.add(look.x * boost, look.y * boost, look.z * boost));
            } else if (input.z < 0) {
                // Brake effect: scale velocity down smoothly
                Vec3 move = this.getDeltaMovement();
                this.setDeltaMovement(move.scale(0.95));
            }
        }
    }

}
