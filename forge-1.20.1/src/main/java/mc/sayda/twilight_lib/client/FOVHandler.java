package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.ModAttributes;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles Field of View (FOV) modification based on player attributes.
 *
 * <p>
 * <b>Purpose:</b> Provides granular control over FOV changes from Pehkui and
 * other mods
 * that modify player size/scale. This is essential for gameplay balance when
 * using size-changing
 * cosmetics or morphs.
 *
 * <p>
 * <b>How FOV Scaling Works:</b>
 * <ul>
 * <li><b>fovModifier = 1.0 (default)</b>: Allow all vanilla FOV changes
 * (passthrough mode)</li>
 * <li><b>fovModifier = 0.0</b>: Force FOV to 1.0, completely disabling all
 * external FOV changes</li>
 * <li><b>0.0 &lt; fovModifier &lt; 1.0</b>: Scale FOV changes by this factor
 * (partial suppression)</li>
 * </ul>
 *
 * <p>
 * <b>Scaling Algorithm (partial suppression mode):</b>
 * <ol>
 * <li>Calculate FOV delta: {@code currentFOV - lastFOV}</li>
 * <li>Determine if FOV is moving towards or away from 1.0 (normal FOV)</li>
 * <li>If moving <b>away from normal</b>: scale the delta by fovModifier</li>
 * <li>If moving <b>towards normal</b>: allow full delta (unscaled)</li>
 * </ol>
 *
 * <p>
 * <b>Why this asymmetric behavior?</b>
 * <ul>
 * <li><b>Away from normal</b>: We want to suppress FOV zoom/unzoom caused by
 * scaling</li>
 * <li><b>Towards normal</b>: We want FOV to return to normal quickly when
 * scaling ends</li>
 * <li>This creates smooth FOV transitions that feel natural to players</li>
 * </ul>
 *
 * <p>
 * <b>Example:</b> Player morphs into a small mob (foxling) with Pehkui scaling:
 * <ul>
 * <li>Pehkui tries to zoom out FOV from 1.0 → 0.7 (makes small player look
 * bigger on screen)</li>
 * <li>With fovModifier=0.5, we scale the delta:
 * {@code 1.0 + (0.7-1.0)*0.5 = 0.85}</li>
 * <li>Result: FOV zooms out less (0.85 instead of 0.7), reducing motion
 * sickness</li>
 * <li>When morph ends, FOV returns to 1.0 at full speed (unscaled) for quick
 * recovery</li>
 * </ul>
 *
 * <p>
 * <b>Thread Safety:</b> lastFovModifier is volatile because it's accessed from
 * render thread.
 *
 * @see ModAttributes#FOV_MODIFIER
 */
@Mod.EventBusSubscriber(modid = TwilightLib.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static synchronized void onFOVModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null)
            return;

        // Get the FOV modifier attribute (controls how much we suppress FOV changes)
        // Validate attribute exists to prevent NPE if ModAttributes initialization
        // failed
        var attributeInstance = player.getAttribute(ModAttributes.FOV_MODIFIER.get());
        if (attributeInstance == null) {
            LOGGER.warn("Or, what. FOV_MODIFIER attribute not registered - using default value 1.0");
            return; // Passthrough mode (no FOV modification)
        }
        double fovModifier = attributeInstance.getValue();

        // Case 1: fovModifier >= 1.0 (passthrough mode - allow all vanilla FOV changes)
        if (fovModifier >= 1.0) {
            lastFovModifier = event.getNewFovModifier();
            // if (LOGGER.isDebugEnabled() && Math.abs(lastFovModifier - 1.0F) >
            // FOV_DELTA_EPSILON) {
            // LOGGER.debug("Well, this is a pretty chill reality. FOV modifier enabled
            // ({}), allowing vanilla FOV changes: {}",
            // fovModifier, lastFovModifier);
            // }
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
            if (LOGGER.isDebugEnabled() && Math.abs(currentModifier - 1.0F) > FOV_DELTA_EPSILON) {
                LOGGER.debug("Or, what. FOV modifier disabled, forcing FOV to 1.0 (was {})",
                        currentModifier);
            }
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Time to change! Scaling FOV delta from {} to {} (modifier: {}), new FOV: {}",
                            fovDelta, scaledDelta, fovModifier, newModifier);
                }
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
