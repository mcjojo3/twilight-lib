package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwilightEventHandler {

    private static final float PLAYER_HEIGHT = TwilightConstants.PLAYER_DEFAULT_HEIGHT;

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size evt) {
        if (!(evt.getEntity() instanceof Player player)) return;

        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            EntityType<?> type = morph.getCachedEntityType();
            if (type == null) return;

            EntityDimensions morphDims = type.getDimensions();

            // Apply scale limits to prevent exploits and rendering issues
            float scale = morphDims.height / PLAYER_HEIGHT;
            float minScale = TwilightConfig.MIN_MORPH_SCALE.get().floatValue();
            float maxScale = TwilightConfig.MAX_MORPH_SCALE.get().floatValue();
            float clampedScale = Math.max(minScale, Math.min(scale, maxScale));

            // If scale was clamped, recalculate morph dimensions
            if (clampedScale != scale) {
                float targetHeight = PLAYER_HEIGHT * clampedScale;
                float widthRatio = morphDims.width / morphDims.height;
                morphDims = EntityDimensions.scalable(targetHeight * widthRatio, targetHeight);
            }

            Pose pose = evt.getPose();

            if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
                morphDims = morphDims.scale(1.0f, 0.6f);
            } else if (pose == Pose.CROUCHING) {
                morphDims = morphDims.scale(1.0f, 0.75f);
            }

            evt.setNewSize(morphDims, true);
            evt.setNewEyeHeight(morphDims.height * TwilightConfig.EYE_HEIGHT_MULTIPLIER.get().floatValue());
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent evt) {
        if (!(evt.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!player.isAlive()) return;

        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            EntityType<?> type = morph.getCachedEntityType();
            if (type == null) return;

            EntityDimensions dims = type.getDimensions();
            float scale = dims.height / PLAYER_HEIGHT;

            // TODO: Step height adjustment will be handled manually via attributes (e.g., Pehkui integration)
            // Disabled for consistency across both Forge 1.20.1 and NeoForge 1.21.1 versions
            // float baseStepHeight = TwilightConfig.BASE_STEP_HEIGHT.get().floatValue();
            // float minScale = TwilightConfig.MIN_STEP_SCALE.get().floatValue();
            // float maxScale = TwilightConfig.MAX_STEP_SCALE.get().floatValue();
            // player.setMaxUpStep(baseStepHeight * Math.max(minScale, Math.min(scale, maxScale)));
        });
    }
}