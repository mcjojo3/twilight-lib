package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true, remap = false)
    private void twilight$onTryToStartFallFlying(CallbackInfoReturnable<Boolean> cir) {
        if (this.getAttributeValue(ModAttributes.ELYTRA_FLIGHT) > 0) {
            boolean flag = !this.onGround() && !this.hasEffect(MobEffects.LEVITATION);
            if (flag) {
                System.out.println("TwilightLib 1.21.1: Starting elytra flight via attribute!");
                // Manually start fall flying by setting shared flag 7 to true
                this.setSharedFlag(7, true);
                cir.setReturnValue(true);
            } else {
                System.out.println("TwilightLib 1.21.1: Attribute present but conditions failed");
            }
        }
    }
}
