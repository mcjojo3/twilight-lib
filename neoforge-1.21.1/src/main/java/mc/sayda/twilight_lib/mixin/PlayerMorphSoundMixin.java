package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
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

/**
 * Mixin to play morphed entity's hurt and death sounds.
 * Provides immersion by making morphed players sound like the entity they're morphed as.
 */
@Mixin(value = Player.class, remap = false)
public class PlayerMorphSoundMixin {

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

        try {
            // Create a temporary entity to get its hurt sound
            Level level = player.level();
            LivingEntity morphEntity = (LivingEntity) morphType.create(level);

            if (morphEntity != null) {
                // Use accessor to call protected getHurtSound method
                SoundEvent morphSound = ((LivingEntityAccessor) morphEntity).invokeGetHurtSound(source);
                if (morphSound != null) {
                    cir.setReturnValue(morphSound);
                }
                morphEntity.discard(); // Clean up temporary entity
            }
        } catch (Exception e) {
            // If anything fails, just use default player sound
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

        try {
            // Create a temporary entity to get its death sound
            Level level = player.level();
            LivingEntity morphEntity = (LivingEntity) morphType.create(level);

            if (morphEntity != null) {
                // Use accessor to call protected getDeathSound method
                SoundEvent morphSound = ((LivingEntityAccessor) morphEntity).invokeGetDeathSound();
                if (morphSound != null) {
                    cir.setReturnValue(morphSound);
                }
                morphEntity.discard(); // Clean up temporary entity
            }
        } catch (Exception e) {
            // If anything fails, just use default player sound
        }
    }
}
