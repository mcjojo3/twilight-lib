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

    /** Removes an owned effect and unequips it. */
    void removeEffect(String effectId);

    /** Whether the player owns this effect. */
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
     * Equips or unequips an effect. The player must own it. Equipping turns off
     * other effects in the same category.
     */
    void setActiveEffect(String effectId, boolean active);

    /** Whether the effect is owned and equipped. */
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
