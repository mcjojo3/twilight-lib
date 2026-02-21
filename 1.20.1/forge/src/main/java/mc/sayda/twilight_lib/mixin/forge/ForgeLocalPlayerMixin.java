package mc.sayda.twilight_lib.mixin.forge;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class ForgeLocalPlayerMixin {

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false), require = 0)
    private boolean twilight$redirectCanElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity.getAttribute(ModAttributes.ELYTRA_FLIGHT.get()) != null &&
                entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0 && !entity.isInWater()) {
            return true;
        }
        return stack.canElytraFly(entity);
    }
}
