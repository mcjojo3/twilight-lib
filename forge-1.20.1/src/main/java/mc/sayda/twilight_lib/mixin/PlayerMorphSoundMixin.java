package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.capabilities.MorphProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * Mixin to play morphed entity's hurt and death sounds.
 * Provides immersion by making morphed players sound like the entity they're morphed as.
 */
@Mixin(Player.class)
public class PlayerMorphSoundMixin {

    /**
     * Inject into getHurtSound to play morphed entity's hurt sound.
     * When a player is morphed, this replaces the player's hurt sound
     * with the morphed entity's hurt sound for immersion.
     * Production injection (SRG name).
     */
    @Inject(
        method = "m_7975_", // SRG name for getHurtSound(DamageSource) in 1.20.1
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0 // Optional - works in production
    )
    private void twilightlib$onGetHurtSound_SRG(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        onGetHurtSound(source, cir);
    }

    /**
     * Development injection (MojMap name).
     */
    @Inject(
        method = "getHurtSound",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0 // Optional - works in dev
    )
    private void twilightlib$onGetHurtSound_MojMap(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        onGetHurtSound(source, cir);
    }

    /**
     * Shared logic for both hurt sound injections.
     */
    private void onGetHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        Player player = (Player) (Object) this;

        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            EntityType<?> morphType = morph.getCachedEntityType();
            if (morphType == null) return;

            try {
                // Create a temporary entity to get its hurt sound
                Level level = player.level();
                LivingEntity morphEntity = (LivingEntity) morphType.create(level);

                if (morphEntity != null) {
                    // Use reflection to call protected getHurtSound method
                    // Try both SRG and MojMap names for compatibility
                    SoundEvent morphSound = null;
                    try {
                        // Try MojMap name first (dev environment)
                        Method method = LivingEntity.class.getDeclaredMethod("getHurtSound", DamageSource.class);
                        method.setAccessible(true);
                        morphSound = (SoundEvent) method.invoke(morphEntity, source);
                    } catch (NoSuchMethodException e) {
                        // Try SRG name (production environment)
                        Method method = LivingEntity.class.getDeclaredMethod("m_7975_", DamageSource.class);
                        method.setAccessible(true);
                        morphSound = (SoundEvent) method.invoke(morphEntity, source);
                    }

                    if (morphSound != null) {
                        cir.setReturnValue(morphSound);
                    }
                    morphEntity.discard(); // Clean up temporary entity
                }
            } catch (Exception e) {
                // If anything fails, just use default player sound
            }
        });
    }

    /**
     * Inject into getDeathSound to play morphed entity's death sound.
     * Production injection (SRG name).
     */
    @Inject(
        method = "m_5592_", // SRG name for getDeathSound() in 1.20.1
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0 // Optional - works in production
    )
    private void twilightlib$onGetDeathSound_SRG(CallbackInfoReturnable<SoundEvent> cir) {
        onGetDeathSound(cir);
    }

    /**
     * Development injection (MojMap name).
     */
    @Inject(
        method = "getDeathSound",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0 // Optional - works in dev
    )
    private void twilightlib$onGetDeathSound_MojMap(CallbackInfoReturnable<SoundEvent> cir) {
        onGetDeathSound(cir);
    }

    /**
     * Shared logic for both death sound injections.
     */
    private void onGetDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        Player player = (Player) (Object) this;

        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            EntityType<?> morphType = morph.getCachedEntityType();
            if (morphType == null) return;

            try {
                // Create a temporary entity to get its death sound
                Level level = player.level();
                LivingEntity morphEntity = (LivingEntity) morphType.create(level);

                if (morphEntity != null) {
                    // Use reflection to call protected getDeathSound method
                    // Try both SRG and MojMap names for compatibility
                    SoundEvent morphSound = null;
                    try {
                        // Try MojMap name first (dev environment)
                        Method method = LivingEntity.class.getDeclaredMethod("getDeathSound");
                        method.setAccessible(true);
                        morphSound = (SoundEvent) method.invoke(morphEntity);
                    } catch (NoSuchMethodException e) {
                        // Try SRG name (production environment)
                        Method method = LivingEntity.class.getDeclaredMethod("m_5592_");
                        method.setAccessible(true);
                        morphSound = (SoundEvent) method.invoke(morphEntity);
                    }

                    if (morphSound != null) {
                        cir.setReturnValue(morphSound);
                    }
                    morphEntity.discard(); // Clean up temporary entity
                }
            } catch (Exception e) {
                // If anything fails, just use default player sound
            }
        });
    }
}
