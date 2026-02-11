package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.interfaces.EntityAccessor;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.Entity.class)
public abstract class EntityMixin implements EntityAccessor {

    @Invoker("setSharedFlag")
    public abstract void twilight_lib$setSharedFlag(int flag, boolean value);

    @Accessor("maxUpStep")
    public abstract float twilight_lib$getMaxUpStep();

    @Accessor("maxUpStep")
    public abstract void twilight_lib$setMaxUpStep(float value);

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void twilight_lib$getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ((Object) this instanceof Player player) {
            cir.setReturnValue(
                    mc.sayda.twilight_lib.TwilightEventHandler.getMorphDimensions(player, pose,
                            cir.getReturnValue()));
        }
    }

    @Inject(method = "getEyeHeight", at = @At("RETURN"), cancellable = true)
    private void twilight_lib$getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player) {
            Float eyeHeight = mc.sayda.twilight_lib.TwilightEventHandler.getMorphEyeHeight(player, pose);
            if (eyeHeight != null) {
                cir.setReturnValue(eyeHeight);
            }
        }
    }

}
