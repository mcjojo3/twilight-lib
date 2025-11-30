package mc.sayda.twilight_lib.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose protected methods from LivingEntity.
 * Allows calling getHurtSound and getDeathSound from outside the class hierarchy.
 */
@Mixin(value = LivingEntity.class, remap = false)
public interface LivingEntityAccessor {

    /**
     * Invoke the protected getHurtSound method.
     * NeoForge 1.21.1 uses MojMap mappings exclusively.
     */
    @Invoker("getHurtSound")
    SoundEvent invokeGetHurtSound(DamageSource source);

    /**
     * Invoke the protected getDeathSound method.
     * NeoForge 1.21.1 uses MojMap mappings exclusively.
     */
    @Invoker("getDeathSound")
    SoundEvent invokeGetDeathSound();
}
