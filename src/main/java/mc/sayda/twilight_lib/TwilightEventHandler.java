package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.capabilities.MorphProvider;
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

    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float BASE_STEP_HEIGHT = 0.6f;
    private static final float MIN_STEP_SCALE = 0.3f;
    private static final float MAX_STEP_SCALE = 2.0f;
    private static final float EYE_HEIGHT_MULTIPLIER = 0.85f;

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size evt) {
        if (!(evt.getEntity() instanceof Player player)) return;

        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            EntityType<?> type = morph.getCachedEntityType();
            if (type == null) return;

            EntityDimensions morphDims = type.getDimensions();
            Pose pose = evt.getPose();

            if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
                morphDims = morphDims.scale(1.0f, 0.6f);
            } else if (pose == Pose.CROUCHING) {
                morphDims = morphDims.scale(1.0f, 0.75f);
            }

            evt.setNewSize(morphDims, true);
            evt.setNewEyeHeight(morphDims.height * EYE_HEIGHT_MULTIPLIER);
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

            player.setMaxUpStep(BASE_STEP_HEIGHT * Math.max(MIN_STEP_SCALE, Math.min(scale, MAX_STEP_SCALE)));
        });
    }
}