package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * Capability interface for player particle trails.
 * Trails are cosmetic particle effects that follow players as they move.
 *
 * <p>Trails have two states:
 * <ul>
 *   <li><b>Owned</b>: Trails the player has unlocked (via supporter tier or admin grant)</li>
 *   <li><b>Active</b>: The single trail currently being rendered (only one at a time)</li>
 * </ul>
 *
 * <p>IMPORTANT: Unlike addons and effects, trails are CLEARED on login to match supporter tier.
 * This prevents tier drift where players retain trails after their subscription expires.
 * Admin grants still persist, but supporter tier trails are re-granted on each login.
 *
 * <p>Players manage trails via /cosmetics trails commands.
 * Trails can be toggled on/off without losing the selection.
 */
public interface ITrails {
    // Owned trails (what the player has access to)
    /**
     * Get all owned trails.
     * @return Set of trail IDs the player has unlocked
     */
    Set<String> getTrails();

    /**
     * Grant a trail to the player.
     * @param trailId The trail ID to grant
     */
    void addTrail(String trailId);

    /**
     * Remove an owned trail from the player.
     * Also clears active trail if it was the removed trail.
     * @param trailId The trail ID to remove
     */
    void removeTrail(String trailId);

    /**
     * Check if the player owns a trail.
     * @param trailId The trail ID to check
     * @return true if the player owns this trail
     */
    boolean hasTrail(String trailId);

    /**
     * Remove all owned trails.
     * Also clears the active trail.
     */
    void clearTrails();

    // Active trail (what's currently visible) - singular because only one can be active
    /**
     * Get the currently active trail.
     * @return The trail ID currently being rendered, or null if none
     */
    String getActiveTrail();

    /**
     * Set the active trail.
     * Player must own the trail to activate it.
     * Pass null to clear the active trail.
     * @param trail The trail ID to activate, or null to clear
     */
    void setActiveTrail(String trail);

    /**
     * Check if a trail is currently active and enabled.
     * @param trailId The trail ID to check
     * @return true if this trail is active AND the trail toggle is enabled
     */
    boolean isTrailActive(String trailId);

    // Enable/disable toggle for trail rendering
    /**
     * Check if trail rendering is enabled.
     * When disabled, no particles are rendered even if a trail is active.
     * @return true if trail rendering is enabled
     */
    boolean isTrailEnabled();

    /**
     * Enable or disable trail rendering.
     * Does not affect the active trail selection.
     * @param enabled true to enable trail rendering
     */
    void setTrailEnabled(boolean enabled);

    /**
     * Serialize trail data to NBT for persistence.
     * @return CompoundTag containing owned trails, active trail, and enabled state
     */
    CompoundTag serialize();

    /**
     * Deserialize trail data from NBT.
     * @param tag CompoundTag containing trail data
     */
    void deserialize(CompoundTag tag);
}
