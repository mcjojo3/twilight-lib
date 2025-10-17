package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * Capability interface for player cosmetic effects.
 * Effects are special event-triggered cosmetics (e.g., respawn particle bursts).
 *
 * <p>Effects have two states:
 * <ul>
 *   <li><b>Owned</b>: Effects the player has unlocked (via supporter tier or admin grant)</li>
 *   <li><b>Active</b>: Effects currently equipped and triggering (multiple can be active simultaneously)</li>
 * </ul>
 *
 * <p>Effects are granted additively via supporter tier (never removed on login).
 * Admin grants via /twilightlib persist independently of supporter status.
 *
 * <p>Available effects:
 * <ul>
 *   <li><b>respawn_twilight</b>: Beautiful twilight particle burst on respawn (Gold+ tier)</li>
 * </ul>
 *
 * <p>Players manage effects via /cosmetics effects commands.
 */
public interface IEffects {
    // Owned effects (what the player has access to)
    /**
     * Get all owned effects.
     * @return Set of effect IDs the player has unlocked
     */
    Set<String> getEffects();

    /**
     * Grant an effect to the player.
     * @param effectId The effect ID to grant
     */
    void addEffect(String effectId);

    /**
     * Remove an owned effect from the player.
     * Also deactivates the effect if currently active.
     * @param effectId The effect ID to remove
     */
    void removeEffect(String effectId);

    /**
     * Check if the player owns an effect.
     * @param effectId The effect ID to check
     * @return true if the player owns this effect
     */
    boolean hasEffect(String effectId);

    /**
     * Remove all owned effects.
     * Also clears all active effects.
     */
    void clearEffects();

    // Active effects (what's currently equipped) - plural because multiple can be active
    /**
     * Get all currently active (equipped) effects.
     * @return Set of effect IDs currently being triggered
     */
    Set<String> getActiveEffects();

    /**
     * Set whether an effect is active (equipped).
     * Player must own the effect to activate it.
     * @param effectId The effect ID to activate/deactivate
     * @param active true to activate, false to deactivate
     */
    void setActiveEffect(String effectId, boolean active);

    /**
     * Check if an effect is currently active.
     * @param effectId The effect ID to check
     * @return true if the effect is both owned and active
     */
    boolean isEffectActive(String effectId);

    /**
     * Deactivate all effects without removing ownership.
     */
    void clearActiveEffects();

    /**
     * Serialize effect data to NBT for persistence.
     * @return CompoundTag containing owned and active effect data
     */
    CompoundTag serialize();

    /**
     * Deserialize effect data from NBT.
     * @param tag CompoundTag containing effect data
     */
    void deserialize(CompoundTag tag);
}
