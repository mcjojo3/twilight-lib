package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/** Player trails: the ones they own and the ones they have equipped. */
public interface ITrails extends ISerializableData {
    // Owned trails (what the player has access to)
    /**
     * Get all owned trails.
     * 
     * @return Set of trail IDs the player has unlocked
     */
    Set<String> getTrails();

    /**
     * Grant a trail to the player.
     * 
     * @param trailId The trail ID to grant
     */
    void addTrail(String trailId);

    /** Removes an owned trail and unequips it. */
    void removeTrail(String trailId);

    /** Whether the player owns this trail. */
    boolean hasTrail(String trailId);

    /**
     * Remove all owned trails.
     * Also clears all active trails.
     */
    void clearTrails();

    // Active trails (what's currently visible) - plural because multiple can be
    // active
    /**
     * Get all currently active (equipped) trails.
     * 
     * @return Set of trail IDs currently being rendered
     */
    Set<String> getActiveTrails();

    /** Equips or unequips a trail. The player must own it. */
    void setActiveTrail(String trailId, boolean active);

    /** Whether the trail is owned and equipped. */
    boolean isTrailActive(String trailId);

    /**
     * Deactivate all trails without removing ownership.
     */
    void clearActiveTrails();

    /**
     * Serialize trail data to NBT for persistence.
     * 
     * @return CompoundTag containing owned and active trail data
     */
    CompoundTag serialize();

    /**
     * Deserialize trail data from NBT.
     * 
     * @param tag CompoundTag containing trail data
     */
    void deserialize(CompoundTag tag);

    /**
     * Force-sync equipped trails from network packet.
     * 
     * @param activeTrails Set of trail IDs to activate
     */
    void syncEquippedFromPacket(Set<String> activeTrails);
}
