package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
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
    private static final Logger LOGGER = LogUtils.getLogger();

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

            // Validate that morphType is a LivingEntity before attempting cast
            if (!LivingEntity.class.isAssignableFrom(morphType.getBaseClass())) {
                LOGGER.debug("Or, what. Morph type {} is not a LivingEntity, skipping hurt sound override", morphType);
                return;
            }

            LivingEntity morphEntity = null;
            try {
                // Create a temporary entity to get its hurt sound
                Level level = player.level();
                morphEntity = (LivingEntity) morphType.create(level);

                if (morphEntity == null) {
                    LOGGER.warn("Yeah? Well... Failed to create morph entity for hurt sound: {}", morphType);
                    return;
                }

                // Use reflection to call protected getHurtSound method
                // Try both SRG and MojMap names for compatibility
                SoundEvent morphSound = null;
                try {
                    // Try MojMap name first (dev environment)
                    Method method = LivingEntity.class.getDeclaredMethod("getHurtSound", DamageSource.class);
                    method.setAccessible(true);
                    morphSound = (SoundEvent) method.invoke(morphEntity, source);
                    LOGGER.debug("Time to change! Retrieved hurt sound via MojMap for morph: {}", morphType);
                } catch (NoSuchMethodException e) {
                    // Try SRG name (production environment)
                    try {
                        Method method = LivingEntity.class.getDeclaredMethod("m_7975_", DamageSource.class);
                        method.setAccessible(true);
                        morphSound = (SoundEvent) method.invoke(morphEntity, source);
                        LOGGER.debug("Time to change! Retrieved hurt sound via SRG for morph: {}", morphType);
                    } catch (NoSuchMethodException e2) {
                        LOGGER.error("How did I?! Uuuughh! Failed to find getHurtSound method (tried both MojMap and SRG)", e2);
                    }
                }

                if (morphSound != null) {
                    cir.setReturnValue(morphSound);
                    LOGGER.debug("Well, this is a pretty chill reality. Applied hurt sound for morph: {}", morphType);
                }
            } catch (ClassCastException e) {
                LOGGER.error("Shoot! Morph type {} cannot be cast to LivingEntity", morphType, e);
            } catch (Exception e) {
                LOGGER.error("Shoot! Unexpected error getting hurt sound for morph {}: {}", morphType, e.getMessage(), e);
            } finally {
                // CRITICAL: Always clean up temporary entity to prevent memory leak
                if (morphEntity != null) {
                    morphEntity.discard();
                }
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

            // Validate that morphType is a LivingEntity before attempting cast
            if (!LivingEntity.class.isAssignableFrom(morphType.getBaseClass())) {
                LOGGER.debug("Or, what. Morph type {} is not a LivingEntity, skipping death sound override", morphType);
                return;
            }

            LivingEntity morphEntity = null;
            try {
                // Create a temporary entity to get its death sound
                Level level = player.level();
                morphEntity = (LivingEntity) morphType.create(level);

                if (morphEntity == null) {
                    LOGGER.warn("Yeah? Well... Failed to create morph entity for death sound: {}", morphType);
                    return;
                }

                // Use reflection to call protected getDeathSound method
                // Try both SRG and MojMap names for compatibility
                SoundEvent morphSound = null;
                try {
                    // Try MojMap name first (dev environment)
                    Method method = LivingEntity.class.getDeclaredMethod("getDeathSound");
                    method.setAccessible(true);
                    morphSound = (SoundEvent) method.invoke(morphEntity);
                    LOGGER.debug("Time to change! Retrieved death sound via MojMap for morph: {}", morphType);
                } catch (NoSuchMethodException e) {
                    // Try SRG name (production environment)
                    try {
                        Method method = LivingEntity.class.getDeclaredMethod("m_5592_");
                        method.setAccessible(true);
                        morphSound = (SoundEvent) method.invoke(morphEntity);
                        LOGGER.debug("Time to change! Retrieved death sound via SRG for morph: {}", morphType);
                    } catch (NoSuchMethodException e2) {
                        LOGGER.error("How did I?! Uuuughh! Failed to find getDeathSound method (tried both MojMap and SRG)", e2);
                    }
                }

                if (morphSound != null) {
                    cir.setReturnValue(morphSound);
                    LOGGER.debug("Well, this is a pretty chill reality. Applied death sound for morph: {}", morphType);
                }
            } catch (ClassCastException e) {
                LOGGER.error("Shoot! Morph type {} cannot be cast to LivingEntity", morphType, e);
            } catch (Exception e) {
                LOGGER.error("Shoot! Unexpected error getting death sound for morph {}: {}", morphType, e.getMessage(), e);
            } finally {
                // CRITICAL: Always clean up temporary entity to prevent memory leak
                if (morphEntity != null) {
                    morphEntity.discard();
                }
            }
        });
    }
}
