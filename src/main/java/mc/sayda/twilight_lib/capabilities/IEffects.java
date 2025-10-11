package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * Capability interface for player cosmetic effects.
 * Effects are special event-triggered cosmetics (e.g., respawn particle bursts).
 *
 * <p>Unlike trails, effects do NOT have an active/equipped state.
 * All owned effects are automatically active and trigger when their event occurs.
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
    /**
     * Get all owned effects.
     * All owned effects are automatically active.
     * @return Set of effect IDs the player has unlocked
     */
    Set<String> getEffects();

    /**
     * Grant an effect to the player.
     * The effect becomes immediately active.
     * @param effectId The effect ID to grant
     */
    void addEffect(String effectId);

    /**
     * Remove an owned effect from the player.
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
     */
    void clearEffects();

    /**
     * Serialize effect data to NBT for persistence.
     * @return CompoundTag containing effect data
     */
    CompoundTag serialize();

    /**
     * Deserialize effect data from NBT.
     * @param tag CompoundTag containing effect data
     */
    void deserialize(CompoundTag tag);
}
