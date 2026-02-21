package mc.sayda.twilight_lib.mixin.forge;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ForgeLivingEntityMixin {

    @Inject(method = "travel", at = @At("HEAD"))
    private void twilight_lib$onTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        var attr = self.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        double flightValue = attr != null ? attr.getValue() : 0;
        if (self.isFallFlying() && flightValue > 0 && !self.isInWater()) {
            // input.z > 0 means forward input (W), input.z < 0 means backward input (S)
            if (input.z > 0) {
                Vec3 look = self.getLookAngle();
                Vec3 move = self.getDeltaMovement();
                double boost = flightValue * 0.001;
                self.setDeltaMovement(move.add(look.x * boost, look.y * boost, look.z * boost));
            } else if (input.z < 0) {
                // Brake effect: scale velocity down smoothly
                Vec3 move = self.getDeltaMovement();
                self.setDeltaMovement(move.scale(0.95));
            }
        }
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false), require = 0)
    private boolean twilight$redirectCanElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity.getAttribute(ModAttributes.ELYTRA_FLIGHT.get()) != null &&
                entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0 && !entity.isInWater()) {
            return true;
        }
        return stack.canElytraFly(entity);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z", remap = false), require = 0)
    private boolean twilight$redirectElytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (entity.getAttribute(ModAttributes.ELYTRA_FLIGHT.get()) != null &&
                entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0 && !entity.isInWater()) {
            return true;
        }
        return stack.elytraFlightTick(entity, flightTicks);
    }
}
