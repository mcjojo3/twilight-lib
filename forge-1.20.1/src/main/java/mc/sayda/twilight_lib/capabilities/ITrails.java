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
 *   <li><b>Active</b>: Trails currently equipped and visible (multiple can be active simultaneously)</li>
 * </ul>
 *
 * <p>Players manage trails via /cosmetics trails commands.
 * Supporter tiers grant trails additively (never removed on login).
 * Admin grants via /twilightlib persist independently of supporter status.
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
     * Also deactivates the trail if currently active.
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
     * Also clears all active trails.
     */
    void clearTrails();

    // Active trails (what's currently visible) - plural because multiple can be active
    /**
     * Get all currently active (equipped) trails.
     * @return Set of trail IDs currently being rendered
     */
    Set<String> getActiveTrails();

    /**
     * Set whether a trail is active (equipped).
     * Player must own the trail to activate it.
     * @param trailId The trail ID to activate/deactivate
     * @param active true to activate, false to deactivate
     */
    void setActiveTrail(String trailId, boolean active);

    /**
     * Check if a trail is currently active.
     * @param trailId The trail ID to check
     * @return true if the trail is both owned and active
     */
    boolean isTrailActive(String trailId);

    /**
     * Deactivate all trails without removing ownership.
     */
    void clearActiveTrails();

    /**
     * Serialize trail data to NBT for persistence.
     * @return CompoundTag containing owned and active trail data
     */
    CompoundTag serialize();

    /**
     * Deserialize trail data from NBT.
     * @param tag CompoundTag containing trail data
     */
    void deserialize(CompoundTag tag);
}
