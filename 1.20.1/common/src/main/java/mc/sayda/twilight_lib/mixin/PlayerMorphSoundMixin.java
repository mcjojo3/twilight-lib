package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
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
 */
@Mixin(value = Player.class)
public class PlayerMorphSoundMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.Map<EntityType<?>, LivingEntity> SOUND_PROXY_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void twilight_lib$onGetHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        Player player = (Player) (Object) this;

        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null)
            return;

        EntityType<?> morphType = morph.getCachedEntityType();
        if (morphType == null)
            return;

        // Validate that morphType is a LivingEntity
        if (!LivingEntity.class.isAssignableFrom(morphType.create(player.level()).getClass())) {
            // We'll stick to legacy logic but adjusted for compilation correctness.
        }

        // Re-implementing parity logic
        try {
            Level level = player.level();
            LivingEntity morphEntity = SOUND_PROXY_CACHE.computeIfAbsent(morphType, type -> {
                net.minecraft.world.entity.Entity e = type.create(level);
                return (e instanceof LivingEntity le) ? le : null;
            });

            if (morphEntity != null) {
                SoundEvent morphSound = ((LivingEntityAccessor) morphEntity).invokeGetHurtSound(source);
                if (morphSound != null) {
                    cir.setReturnValue(morphSound);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to get morph hurt sound", e);
        }
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void twilight_lib$onGetDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
        Player player = (Player) (Object) this;

        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null)
            return;

        EntityType<?> morphType = morph.getCachedEntityType();
        if (morphType == null)
            return;

        try {
            Level level = player.level();
            LivingEntity morphEntity = SOUND_PROXY_CACHE.computeIfAbsent(morphType, type -> {
                net.minecraft.world.entity.Entity e = type.create(level);
                return (e instanceof LivingEntity le) ? le : null;
            });

            if (morphEntity != null) {
                SoundEvent morphSound = ((LivingEntityAccessor) morphEntity).invokeGetDeathSound();
                if (morphSound != null) {
                    cir.setReturnValue(morphSound);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to get morph death sound", e);
        }
    }
}
