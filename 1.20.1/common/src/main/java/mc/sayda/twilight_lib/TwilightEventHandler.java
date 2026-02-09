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

public class TwilightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

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
            return; // Null safety check
        // TODO: Logic for step height or other tick-based morph adjustments can go here
        // in
        // the future
    }

    /**
     * Called by Mixin (PlayerMixin or EntityMixin) to handle morph-based size
     * adjustments
     */
    public static EntityDimensions getMorphDimensions(Player player, Pose pose, EntityDimensions original) {
        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null)
            return original;

        EntityType<?> type = morph.getCachedEntityType();
        if (type == null)
            return original;

        EntityDimensions morphDims = type.getDimensions();

        // Validate morph dimensions (prevent division by zero)
        if (morphDims.height <= 0) {
            return original;
        }

        // Apply scale limits to prevent exploits and rendering issues
        float scale = morphDims.height / getPlayerHeight();
        float minScale = TwilightConfig.MIN_MORPH_SCALE.get().floatValue();
        float maxScale = TwilightConfig.MAX_MORPH_SCALE.get().floatValue();
        float clampedScale = Math.max(minScale, Math.min(scale, maxScale));

        // In 1.20.1, scalable() might not exist or use different params
        if (clampedScale != scale) {
            float targetHeight = getPlayerHeight() * clampedScale;
            float widthRatio = morphDims.width / morphDims.height;
            morphDims = new EntityDimensions(targetHeight * widthRatio, targetHeight, false);
        }

        if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
            morphDims = morphDims.scale(1.0f, 0.6f);
        } else if (pose == Pose.CROUCHING) {
            morphDims = morphDims.scale(1.0f, 0.75f);
        }

        return new EntityDimensions(morphDims.width, morphDims.height, false);
    }

    public static float getMorphEyeHeight(Player player, Pose pose, float original) {
        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null || !morph.getEntityType().isPresent())
            return original;

        EntityDimensions dims = getMorphDimensions(player, pose, player.getDimensions(pose));
        float multiplier = TwilightConfig.EYE_HEIGHT_MULTIPLIER.get().floatValue();
        return dims.height * multiplier;
    }
}
