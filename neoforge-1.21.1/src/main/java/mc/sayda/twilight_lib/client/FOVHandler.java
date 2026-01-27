package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.ModAttributes;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles Field of View (FOV) modification based on player attributes.
 * Ported to NeoForge 1.21.1
 */
@EventBusSubscriber(modid = TwilightLib.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class FOVHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Last known FOV modifier value. Used to calculate deltas between frames.
     * Access synchronized via onFOVModifier method to prevent race conditions.
     */
    private static float lastFovModifier = 1.0F;

    /**
     * Minimum FOV change to process (prevents jitter from floating-point rounding).
     */
    private static final float FOV_DELTA_EPSILON = 0.001F;

    @SubscribeEvent
    public static synchronized void onFOVModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null)
            return;

        // Get the attribute holder
        var attributeHolder = ModAttributes.FOV_MODIFIER;

        // In 1.21, attributes are usually retrieved via holder
        // Check if player has the attribute
        if (!player.getAttributes().hasAttribute(attributeHolder)) {
            // LOGGER.warn("Or, what. FOV_MODIFIER attribute not registered - using default
            // value 1.0");
            return; // Passthrough mode
        }

        // Get value using holder
        double fovModifier = player.getAttributeValue(attributeHolder);

        // Case 1: fovModifier >= 1.0 (passthrough mode - allow all vanilla FOV changes)
        if (fovModifier >= 1.0) {
            lastFovModifier = event.getNewFovModifier();
            // Debug logs disabled
            return;
        }

        // Calculate FOV change metrics
        float currentModifier = event.getNewFovModifier(); // What Pehkui/vanilla wants FOV to be
        float fovDelta = currentModifier - lastFovModifier; // How much FOV is changing this frame
        float distanceFromNormal = Math.abs(currentModifier - 1.0F); // How far from normal (1.0) we'd be
        float lastDistanceFromNormal = Math.abs(lastFovModifier - 1.0F); // How far we were last frame

        // Determine if FOV is returning to normal (1.0) or moving away from it
        // This is key for asymmetric scaling: suppress changes away, allow changes
        // towards normal
        boolean movingTowardsNormal = distanceFromNormal < lastDistanceFromNormal;

        // Case 2: fovModifier <= 0.0 (force normal FOV - completely disable all FOV
        // changes)
        if (fovModifier <= 0.0) {
            // Debug logs disabled
            event.setNewFovModifier(1.0F);
            lastFovModifier = 1.0F;
        }
        // Case 3: 0.0 < fovModifier < 1.0 (partial suppression - scale FOV changes)
        else {
            // Only scale when moving AWAY from normal (asymmetric scaling for smooth
            // experience)
            if (!movingTowardsNormal && Math.abs(fovDelta) > FOV_DELTA_EPSILON) {
                // Scale the delta: larger fovModifier = more FOV change allowed
                // Example: fovModifier=0.5 means FOV changes at 50% speed
                float scaledDelta = fovDelta * (float) fovModifier;
                float newModifier = lastFovModifier + scaledDelta;

                // Debug logs disabled

                event.setNewFovModifier(newModifier);
                lastFovModifier = newModifier;
            } else {
                // Moving towards normal or negligible change - allow full delta (quick
                // recovery)
                lastFovModifier = currentModifier;
            }
        }
    }
}
