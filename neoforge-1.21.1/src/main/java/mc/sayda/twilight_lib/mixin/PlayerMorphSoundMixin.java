package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
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

/**
 * Mixin to play morphed entity's hurt and death sounds.
 * Provides immersion by making morphed players sound like the entity they're morphed as.
 */
@Mixin(value = Player.class, remap = false)
public class PlayerMorphSoundMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Inject into getHurtSound to play morphed entity's hurt sound.
     * When a player is morphed, this replaces the player's hurt sound
     * with the morphed entity's hurt sound for immersion.
     */
    @Inject(
        method = "getHurtSound",
        at = @At("HEAD"),
        cancellable = true
    )
    private void twilightlib$onGetHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        Player player = (Player) (Object) this;

        IMorph morph = player.getData(ModAttachments.MORPH);
        if (morph == null) return;

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

            // Use accessor to call protected getHurtSound method
            SoundEvent morphSound = ((LivingEntityAccessor) morphEntity).invokeGetHurtSound(source);
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
    }

    /**
     * Inject into getDeathSound to play morphed entity's death sound.
     * When a morphed player dies, this replaces the player's death sound
     * with the morphed entity's death sound for immersion.
     */
    @Inject(
        method = "getDeathSound",
        at = @At("HEAD"),
        cancellable = true
    )
    private void twilightlib$onGetDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        Player player = (Player) (Object) this;

        IMorph morph = player.getData(ModAttachments.MORPH);
        if (morph == null) return;

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

            // Use accessor to call protected getDeathSound method
            SoundEvent morphSound = ((LivingEntityAccessor) morphEntity).invokeGetDeathSound();
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
    }
}
