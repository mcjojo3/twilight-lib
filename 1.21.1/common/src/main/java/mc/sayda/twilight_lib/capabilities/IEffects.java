package mc.sayda.twilight_lib.capabilities;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * <p>
 * Players manage effects via /cosmetics effects commands.
 */
public interface IEffects extends ISerializableData {
    // Owned effects (what the player has access to)
    /**
     * Get all owned effects.
     * 
     * @return Set of effect IDs the player has unlocked
     */
    Set<String> getEffects();

    /**
     * Grant an effect to the player.
     * 
     * @param effectId The effect ID to grant
     */
    void addEffect(String effectId);

    /**
     * Remove an owned effect from the player.
     * Also deactivates the effect if currently active.
     * 
     * @param effectId The effect ID to remove
     */
    void removeEffect(String effectId);

    /**
     * Check if the player owns an effect.
     * 
     * @param effectId The effect ID to check
     * @return true if the player owns this effect
     */
    boolean hasEffect(String effectId);

    /**
     * Remove all owned effects.
     * Also clears all active effects.
     */
    void clearEffects();

    // Active effects (what's currently equipped) - plural because multiple can be
    // active
    /**
     * Get all currently active (equipped) effects.
     * 
     * @return Set of effect IDs currently being triggered
     */
    Set<String> getActiveEffects();

    /**
     * Set whether an effect is active (equipped).
     * Player must own the effect to activate it.
     *
     * <p>
     * <b>Category Enforcement</b>: Activating an effect will automatically
     * deactivate
     * any other effects in the same category (e.g., only one spawn effect can be
     * active at a time).
     *
     * @param effectId The effect ID to activate/deactivate
     * @param active   true to activate, false to deactivate
     */
    void setActiveEffect(String effectId, boolean active);

    /**
     * Check if an effect is currently active.
     * 
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
     * 
     * @return CompoundTag containing owned and active effect data
     */
    @Nonnull
    CompoundTag serialize();

    /**
     * Deserialize effect data from NBT.
     * 
     * @param tag CompoundTag containing effect data
     */
    void deserialize(@Nonnull CompoundTag tag);

    /**
     * Force-sync equipped effects from network packet.
     * 
     * @param activeEffects Set of effect IDs to activate
     */
    void syncEquippedFromPacket(java.util.Set<String> activeEffects);
}
