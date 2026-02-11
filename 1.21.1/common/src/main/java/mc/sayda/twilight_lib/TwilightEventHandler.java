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

import dev.architectury.event.events.common.TickEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

        // Auto-scale step height based on morph size
        EntityType<?> type = morph.getCachedEntityType();
        if (type != null) {
            float morphHeight = type.getDimensions().height();
            float scale = morphHeight / getPlayerHeight();

            var stepHeightAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
            if (stepHeightAttr != null) {
                // Clear existing morph modifier if any
                stepHeightAttr.removeModifier(mc.sayda.twilight_lib.TwilightConstants.MORPH_STEP_HEIGHT_MODIFIER_ID);

                // Add new modifier if scale is significant
                if (Math.abs(scale - 1.0f) > 0.05f) {
                    stepHeightAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            mc.sayda.twilight_lib.TwilightConstants.MORPH_STEP_HEIGHT_MODIFIER_ID,
                            scale - 1.0,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
        }
    }

    /**
     * Called by Mixin (PlayerMixin or EntityMixin) to handle morph-based size
     * adjustments
     */
    public static EntityDimensions getMorphDimensions(@Nonnull Player player, Pose pose,
            @Nullable EntityDimensions original) {
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

            // Validate morph dimensions (prevent division by zero)
            if (morphDims.height() <= 0) {
                return original;
            }

            // 1. Calculate base morph scale relative to standard player
            float scale = morphDims.height() / getPlayerHeight();
            float minScale = TwilightConfig.MIN_MORPH_SCALE.get().floatValue();
            float maxScale = TwilightConfig.MAX_MORPH_SCALE.get().floatValue();
            float clampedScale = Math.max(minScale, Math.min(scale, maxScale));

            // 2. Adjust morphDims if clamped
            if (clampedScale != scale) {
                float targetHeight = getPlayerHeight() * clampedScale;
                float widthRatio = morphDims.width() / morphDims.height();
                morphDims = EntityDimensions.scalable(targetHeight * widthRatio, targetHeight);
            }

            // 3. Pose scaling (Vanilla behavior for morph)
            if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
                // Standardize pose scaling: morph height * 0.333f
                morphDims = EntityDimensions.scalable(morphDims.width(), morphDims.height() * 0.333f);
            } else if (pose == Pose.CROUCHING) {
                // Standardize crouch scaling: morph height * 0.833f
                morphDims = EntityDimensions.scalable(morphDims.width(), morphDims.height() * 0.833f);
            }

            // 4. Apply external scaling (Pehkui)
            // Extract external scale factor from 'original' (which is already scaled by
            // Pehkui)
            if (original != null) {
                float vanillaHeight = (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) ? 0.6f
                        : (pose == Pose.CROUCHING ? 1.5f : 1.8f);
                float externalScale = original.height() / vanillaHeight;
                if (Math.abs(externalScale - 1.0f) > 0.001f) {
                    morphDims = morphDims.scale(externalScale);
                }
            }

            float eyeHeightMultiplier = TwilightConfig.EYE_HEIGHT_MULTIPLIER.get().floatValue();
            return morphDims.withEyeHeight(morphDims.height() * eyeHeightMultiplier);
        } finally {
            IS_SCALING.set(false);
        }
    }

    public static boolean onTryToStartFallFlying(Player player) {
        var elytraAttr = ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT);
        if (player.getAttributes().hasAttribute(elytraAttr)) {
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

    @Nullable
    public static Float getMorphEyeHeight(@Nonnull Player player, Pose pose) {
        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null || !morph.getEntityType().isPresent())
            return null;

        EntityDimensions dims = getMorphDimensions(player, pose, player.getDimensions(pose));
        if (dims == null)
            return null;

        float multiplier = TwilightConfig.EYE_HEIGHT_MULTIPLIER.get().floatValue();
        return dims.height() * multiplier;
    }
}
