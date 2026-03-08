package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * Capability interface for player cosmetic addons (visual attachments).
 * Addons are 3D model attachments that render on top of the player (e.g.,
 * tails, wings).
 *
 * <p>
 * Addons have two states:
 * <ul>
 * <li><b>Owned</b>: Addons the player has unlocked (via supporter tier or admin
 * grant)</li>
 * <li><b>Active</b>: Addons currently equipped and visible (multiple can be
 * active simultaneously)</li>
 * </ul>
 *
 * <p>
 * Players manage addons via /cosmetics addons commands.
 * Supporter tiers grant addons additively (never removed on login).
 * Admin grants via /twilightlib persist independently of supporter status.
 */
public interface IAddons {
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

    /**
     * Remove an owned addon from the player.
     * Also deactivates the addon if currently active.
     * 
     * @param addonId The addon ID to remove
     */
    void removeAddon(String addonId);

    /**
     * Check if the player owns an addon.
     * 
     * @param addonId The addon ID to check
     * @return true if the player owns this addon
     */
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

    /**
     * Set whether an addon is active (equipped).
     * Player must own the addon to activate it.
     * 
     * @param addonId The addon ID to activate/deactivate
     * @param active  true to activate, false to deactivate
     */
    void setActiveAddon(String addonId, boolean active);

    /**
     * Check if an addon is currently active.
     * 
     * @param addonId The addon ID to check
     * @return true if the addon is both owned and active
     */
    boolean isAddonActive(String addonId);

    /**
     * Deactivate all addons without removing ownership.
     */
    void clearActiveAddons();

    /**
     * Get all external grants (addons activated by mod/race logic, not by the
     * player).
     * These are the addons set via setActiveAddon(id, true, true).
     *
     * @return Set of addon IDs that are externally granted (race cosmetics etc.)
     */
    Set<String> getExternalGrants();

    // Tint color methods
    /**
     * Get the RGB tint color for an addon.
     * 
     * @param addonId The addon ID
     * @return RGB color as packed int (0xRRGGBB), default 0xFFFFFF (white/no tint)
     */
    int getAddonTint(String addonId);

    /**
     * Set the RGB tint color for an addon.
     * Color is stored per-player and persists across sessions.
     * 
     * @param addonId The addon ID
     * @param color   RGB color as packed int (0xRRGGBB), 0xFFFFFF = white (no tint)
     */
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
    CompoundTag serialize();

    /**
     * Deserialize addon data from NBT.
     * 
     * @param tag CompoundTag containing addon data
     */
    void deserialize(CompoundTag tag);

    /**
     * Force-sync equipped addons from network packet.
     * 
     * @param activeAddons Set of addon IDs to activate
     */
    void syncEquippedFromPacket(Set<String> activeAddons);

    /**
     * Force-sync tint colors from network packet.
     * 
     * @param tints Map of addon ID to RGB color
     */
    void syncTintsFromPacket(java.util.Map<String, Integer> tints);
}
