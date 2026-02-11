package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    // Lazy-initialized reflection handles for Forge-only ItemStack methods.
    // Cannot use a static{} block because Mixin merges it into
    // LivingEntity.<clinit>
    // where the try/catch doesn't survive properly, crashing Fabric.
    @Unique
    private static volatile boolean twilight$reflectionInitialized = false;
    @Unique
    private static Method twilight$canElytraFlyMethod = null;
    @Unique
    private static Method twilight$elytraFlightTickMethod = null;

    @Unique
    private static void twilight$ensureReflection() {
        if (!twilight$reflectionInitialized) {
            synchronized (LivingEntityMixin.class) {
                if (!twilight$reflectionInitialized) {
                    try {
                        twilight$canElytraFlyMethod = ItemStack.class.getMethod("canElytraFly", LivingEntity.class);
                        twilight$elytraFlightTickMethod = ItemStack.class.getMethod("elytraFlightTick",
                                LivingEntity.class, int.class);
                    } catch (NoSuchMethodException e) {
                        // Fabric — these methods don't exist
                    }
                    twilight$reflectionInitialized = true;
                }
            }
        }
    }

    protected LivingEntityMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean twilight_lib$redirectIsElytra(ItemStack stack, Item item) {
        if (item == Items.ELYTRA && ((LivingEntity) (Object) this)
                .getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        return stack.is(item);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"), require = 0)
    private boolean twilight_lib$redirectIsFlyEnabled(ItemStack stack) {
        if (((LivingEntity) (Object) this)
                .getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        return ElytraItem.isFlyEnabled(stack);
    }

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"), require = 0)
    private void twilight_lib$redirectHurtAndBreak(ItemStack stack, int amount, LivingEntity entity,
            java.util.function.Consumer<LivingEntity> onBroken) {
        if (entity.getAttributeValue(mc.sayda.twilight_lib.ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return; // Don't damage "phantom" elytra
        }
        stack.hurtAndBreak(amount, entity, onBroken);
    }

    // These @Redirect targets only exist on Forge (canElytraFly/elytraFlightTick
    // are
    // Forge-added methods on ItemStack). require=0 means the mixin silently skips
    // these on Fabric where the target methods don't exist.
    @Redirect(method = { "updateFallFlying",
            "m_21323_" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"), require = 0, remap = false)
    private boolean twilight$redirectCanElytraFly(ItemStack stack, LivingEntity entity) {
        if (entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        twilight$ensureReflection();
        if (twilight$canElytraFlyMethod != null) {
            try {
                return (boolean) twilight$canElytraFlyMethod.invoke(stack, entity);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Redirect(method = { "updateFallFlying",
            "m_21323_" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"), require = 0, remap = false)
    private boolean twilight$redirectElytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (entity.getAttributeValue(ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            return true;
        }
        twilight$ensureReflection();
        if (twilight$elytraFlightTickMethod != null) {
            try {
                return (boolean) twilight$elytraFlightTickMethod.invoke(stack, entity, flightTicks);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void twilight_lib$onTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isFallFlying()
                && self.getAttributeValue(mc.sayda.twilight_lib.ModAttributes.ELYTRA_FLIGHT.get()) > 0) {
            // input.z > 0 means forward input
            if (input.z > 0) {
                Vec3 look = this.getLookAngle();
                Vec3 move = this.getDeltaMovement();
                double attributeValue = self.getAttributeValue(mc.sayda.twilight_lib.ModAttributes.ELYTRA_FLIGHT.get());
                double boost = attributeValue * 0.001;
                this.setDeltaMovement(move.add(look.x * boost, look.y * boost, look.z * boost));
            }
        }
    }

    @Inject(method = "getEyeHeight", at = @At("RETURN"), cancellable = true)
    private void twilight_lib$getEyeHeight(net.minecraft.world.entity.Pose pose,
            net.minecraft.world.entity.EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof net.minecraft.world.entity.player.Player player) {
            Float eyeHeight = mc.sayda.twilight_lib.TwilightEventHandler.getMorphEyeHeight(player, pose);
            if (eyeHeight != null) {
                cir.setReturnValue(eyeHeight);
            }
        }
    }

}
