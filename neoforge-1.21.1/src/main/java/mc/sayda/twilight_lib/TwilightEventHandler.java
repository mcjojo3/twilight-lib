package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

// In 1.21.1, bus parameter is deprecated - events default to game bus
@EventBusSubscriber(modid = TwilightLib.MODID)
public class TwilightEventHandler {

    private static final float PLAYER_HEIGHT = TwilightConstants.PLAYER_DEFAULT_HEIGHT;

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size evt) {
        if (!(evt.getEntity() instanceof Player player)) return;

        IMorph morph = player.getData(ModAttachments.MORPH);
        EntityType<?> type = morph.getCachedEntityType();
        if (type == null) return;

        EntityDimensions morphDims = type.getDimensions();
        Pose pose = evt.getPose();

        if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
            morphDims = morphDims.scale(1.0f, 0.6f);
        } else if (pose == Pose.CROUCHING) {
            morphDims = morphDims.scale(1.0f, 0.75f);
        }

        // In 1.21.1, setNewSize only takes the dimensions (no second parameter)
        // Eye height is calculated automatically from dimensions
        evt.setNewSize(morphDims);
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post evt) {
        if (!(evt.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!player.isAlive()) return;

        IMorph morph = player.getData(ModAttachments.MORPH);
        EntityType<?> type = morph.getCachedEntityType();
        if (type == null) return;

        EntityDimensions dims = type.getDimensions();
        // In 1.21.1, height is now a method instead of a field
        float scale = dims.height() / PLAYER_HEIGHT;

        float baseStepHeight = TwilightConfig.BASE_STEP_HEIGHT.get().floatValue();
        float minScale = TwilightConfig.MIN_STEP_SCALE.get().floatValue();
        float maxScale = TwilightConfig.MAX_STEP_SCALE.get().floatValue();

        // TODO: In 1.21.1, step height system was changed - needs to be reimplemented
        // The maxUpStep field no longer exists and needs to be set via a different mechanism
        // (possibly using entity attributes or a Mixin to access the private field)
        // float newStepHeight = baseStepHeight * Math.max(minScale, Math.min(scale, maxScale));
        // player.maxUpStep = newStepHeight;
    }
}