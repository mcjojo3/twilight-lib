package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Redirect(method = { "updateFallFlying",
            "m_21323_" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"), remap = false)
    private boolean twilight$redirectCanElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        return stack.canElytraFly(entity);
    }

    @Redirect(method = { "updateFallFlying",
            "m_21323_" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"), remap = false)
    private boolean twilight$redirectElytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        return stack.elytraFlightTick(entity, flightTicks);
    }

    @Inject(method = { "travel", "m_7023_" }, at = @At("HEAD"), remap = false)
    private void twilight$onTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isFallFlying() && self.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            // input.z > 0 means forward input
            if (input.z > 0) {
                Vec3 look = self.getLookAngle();
                Vec3 move = self.getDeltaMovement();
                double attributeValue = self.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get());
                double boost = attributeValue * 0.001;
                self.setDeltaMovement(move.add(look.x * boost, look.y * boost, look.z * boost));
            }
        }
    }
}
