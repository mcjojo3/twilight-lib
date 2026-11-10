package mc.sayda.twilight_lib.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/** Player addons: the ones they own and the ones they have equipped. */
public interface IAddons extends ISerializableData {
    // Owned addons (what the player has access to)
    /**
     * Get all owned addons.
     * 
     * @return Set of addon IDs the player has unlocked
     */
    Set<String> getAddons();

    /**
     * Grant an addon to the player.
     * 
     * @param addonId The addon ID to grant
     */
    void addAddon(String addonId);

    /** Removes an owned addon and unequips it. */
    void removeAddon(String addonId);

    /** Whether the player owns this addon. */
    boolean hasAddon(String addonId);

    /**
     * Remove all owned addons.
     * Also clears all active addons.
     */
    void clearAddons();

    // Active addons (what's currently visible) - plural because multiple can be
    // active
    /**
     * Get all currently active (equipped) addons.
     * 
     * @return Set of addon IDs currently being rendered
     */
    Set<String> getActiveAddons();

    /** Equips or unequips an addon. The player must own it. */
    void setActiveAddon(String addonId, boolean active);

    /** Whether the addon is owned and equipped. */
    boolean isAddonActive(String addonId);

    /**
     * Deactivate all addons without removing ownership.
     */
    void clearActiveAddons();

    // Tint color methods
    /** Tint color as 0xRRGGBB, 0xFFFFFF (no tint) by default. */
    int getAddonTint(String addonId);

    /** Sets the tint color (0xRRGGBB, 0xFFFFFF = no tint). Saved per player. */
    void setAddonTint(String addonId, int color);

    /**
     * Get all addon tint colors.
     * 
     * @return Map of addon ID to RGB color (only non-default colors)
     */
    java.util.Map<String, Integer> getAllAddonTints();

    /**
     * Serialize addon data to NBT for persistence.
     * 
     * @return CompoundTag containing owned and active addon data
     */
    @Nonnull
    CompoundTag serialize();

    /**
     * Deserialize addon data from NBT.
     * 
     * @param tag CompoundTag containing addon data
     */
    void deserialize(@Nonnull CompoundTag tag);

    /**
     * Force-sync equipped addons from network packet.
     * 
     * @param activeAddons Set of addon IDs to activate
     */
    void syncEquippedFromPacket(Set<String> activeAddons);

    /**
     * Get a snapshot of all external grants (race/mod-applied cosmetics).
     * 
     * @return Set of addon IDs granted externally
     */
    Set<String> getExternalGrants();

    /**
     * Force-sync external grants from network packet.
     * 
     * @param externalGrants Set of addon IDs granted externally
     */
    void syncExternalGrantsFromPacket(@Nonnull Set<String> externalGrants);

    /**
     * Force-sync tint colors from network packet.
     * 
     * @param tints Map of addon ID to RGB color
     */
    void syncTintsFromPacket(java.util.Map<String, Integer> tints);
}