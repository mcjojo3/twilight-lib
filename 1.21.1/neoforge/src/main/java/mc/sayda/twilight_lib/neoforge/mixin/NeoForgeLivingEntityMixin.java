package mc.sayda.twilight_lib.neoforge.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class NeoForgeLivingEntityMixin {

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"), remap = false)
    private boolean twilight_lib$redirectCanElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity.getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT)) > 0) {
            return true;
        }
        return stack.canElytraFly(entity);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"), remap = false)
    private boolean twilight_lib$redirectElytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (entity.getAttributeValue(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT)) > 0) {
            return true;
        }
        return stack.elytraFlightTick(entity, flightTicks);
    }
}
