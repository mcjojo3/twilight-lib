package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import org.slf4j.Logger;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import mc.sayda.twilight_lib.interfaces.EntityAccessor;
import mc.sayda.twilight_lib.mixin.EntityMixin;

import dev.architectury.event.events.common.TickEvent;

public class TwilightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Boolean> IS_SCALING = ThreadLocal.withInitial(() -> false);

    private static float getPlayerHeight() {
        return TwilightConfig.PLAYER_DEFAULT_HEIGHT.get().floatValue();
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(TwilightEventHandler::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        if (player.level().isClientSide)
            return;
        if (!player.isAlive())
            return;

        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null)
            return;

        // Ensure dimensions are refreshed if morph changes (server-side safety)
        // This is a backup for when setMorph isn't called directly (e.g. initial login
        // or NBT sync)
        EntityType<?> type = morph.getCachedEntityType();
        if (type != null) {
            // Periodic step height sync (already handled below)
            float morphHeight = type.getDimensions().height;
            float scale = morphHeight / getPlayerHeight();

            // In 1.20.1, step height is handled by Entity.maxUpStep instead of attributes
            float baseStepHeight = 0.6f; // Standard player/entity step height
            float targetStepHeight = baseStepHeight * scale;

            if (player instanceof EntityAccessor accessor) {
                if (Math.abs(accessor.twilight_lib$getMaxUpStep() - targetStepHeight) > 0.01f) {
                    accessor.twilight_lib$setMaxUpStep(Math.max(0.1f, Math.min(2.0f, targetStepHeight)));
                }
            }
        }
    }

    /**
     * Called by Mixin (PlayerMixin or EntityMixin) to handle morph-based size
     * adjustments
     */
    public static EntityDimensions getMorphDimensions(Player player, Pose pose, EntityDimensions original) {
        if (IS_SCALING.get()) {
            return original;
        }

        try {
            IS_SCALING.set(true);
            IMorph morph = DataUtils.getMorphData(player);
            if (morph == null || !morph.getEntityType().isPresent())
                return original;

            EntityType<?> type = morph.getCachedEntityType();
            if (type == null)
                return original;

            EntityDimensions morphDims = type.getDimensions();
            if (morphDims.height <= 0)
                return original;

            // 1. Calculate base morph scale relative to standard player
            float morphScale = morphDims.height / getPlayerHeight();
            float minScale = TwilightConfig.MIN_MORPH_SCALE.get().floatValue();
            float maxScale = TwilightConfig.MAX_MORPH_SCALE.get().floatValue();
            float clampedScale = Math.max(minScale, Math.min(morphScale, maxScale));

            // 2. Adjust morphDims if clamped
            if (Math.abs(clampedScale - morphScale) > 0.001f) {
                float targetHeight = getPlayerHeight() * clampedScale;
                float widthRatio = morphDims.width / morphDims.height;
                morphDims = EntityDimensions.scalable(targetHeight * widthRatio, targetHeight);
            }

            // 3. Pose scaling (Vanilla behavior for morph)
            if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
                morphDims = EntityDimensions.scalable(morphDims.width, morphDims.height * 0.333f);
            } else if (pose == Pose.CROUCHING) {
                morphDims = EntityDimensions.scalable(morphDims.width, morphDims.height * 0.833f);
            }

            // 4. Apply external scaling (Pehkui)
            if (original != null) {
                float vanillaHeight = (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) ? 0.6f
                        : (pose == Pose.CROUCHING ? 1.5f : 1.8f);
                float externalScale = original.height / vanillaHeight;
                // If original height already matches morphDims height (or is very close),
                // it means we've already applied the morph scale in a previous mixin call
                // (e.g. LivingEntityMixin vs PlayerMixin)
                if (Math.abs(externalScale - 1.0f) > 0.001f && Math.abs(original.height - morphDims.height) > 0.001f) {
                    morphDims = morphDims.scale(externalScale);
                }
            }

            return morphDims;
        } finally {
            IS_SCALING.set(false);
        }
    }

    public static boolean onTryToStartFallFlying(Player player) {
        var elytraAttr = ModAttributes.ELYTRA_FLIGHT.get();
        if (elytraAttr != null && player.getAttributes().hasAttribute(elytraAttr)) {
            if (player.getAttributeValue(elytraAttr) > 0) {
                boolean canFly = !player.onGround()
                        && !player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                if (canFly) {
                    player.startFallFlying();
                    return true;
                }
            }
        }
        return false;
    }

    public static Float getMorphEyeHeight(Player player, Pose pose) {
        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null || !morph.getEntityType().isPresent())
            return null;

        EntityDimensions dims = getMorphDimensions(player, pose, player.getDimensions(pose));
        if (dims == null)
            return null; // Should not happen based on getMorphDimensions logic

        float multiplier = TwilightConfig.EYE_HEIGHT_MULTIPLIER.get().floatValue();
        float eyeHeight = dims.height * multiplier;
        return eyeHeight;
    }
}
