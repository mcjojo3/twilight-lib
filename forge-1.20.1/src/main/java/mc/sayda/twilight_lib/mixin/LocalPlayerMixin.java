package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends LivingEntity {

    protected LocalPlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    // Redirect the check for canElytraFly in aiStep to allow attribute-based flight
    @Redirect(method = { "aiStep",
            "m_8107_" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"), remap = false)
    private boolean twilight$redirectCanElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        return stack.canElytraFly(entity);
    }
}
