package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class FabricLivingEntityMixin {

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

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean twilight_lib$redirectIsElytra(ItemStack stack, net.minecraft.world.item.Item item) {
        if (item == Items.ELYTRA) {
            LivingEntity self = (LivingEntity) (Object) this;
            var attr = self.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
            if (attr != null && attr.getValue() > 0
                    && !self.isInWater()) {
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
        return ElytraItem.isFlyEnabled(stack);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"), require = 0)
    private void twilight_lib$redirectHurtAndBreak(ItemStack stack, int amount, LivingEntity entity,
            Consumer<LivingEntity> onBroken) {
        var attr = entity.getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        if (attr != null && attr.getValue() > 0) {
            return; // Don't damage "phantom" elytra
        }
        stack.hurtAndBreak(amount, entity, onBroken);
    }
}
