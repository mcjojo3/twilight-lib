package mc.sayda.twilight_lib.capabilities;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import net.minecraft.nbt.CompoundTag;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class AddonsData implements IAddons, ISerializableData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_ADDONS = "Addons";
    private static final String NBT_EQUIPPED_ADDONS = "EquippedAddons";
    private static final String NBT_PLAYER_SELECTIONS = "PlayerSelections";
    private static final String NBT_EXTERNAL_GRANTS = "ExternalGrants";
    private static final String NBT_ADDON_TINTS = "AddonTints";
    // TwilightConfig.MAX_NBT_LIST_SIZE.get() moved to TwilightConfig // Same as
    // network packet limit to prevent DoS

    private final Set<String> addons = new HashSet<>(); // Owned addons (from supporter status)
    private final Set<String> equippedAddons = new HashSet<>(); // Currently equipped addons
    private final Set<String> playerSelections = new HashSet<>(); // Player's choices via /tlcosmetics (re-equip if
                                                                  // owned)
    private final Set<String> externalGrants = new HashSet<>(); // Admin/mod grants via /twilightlib (persist regardless
                                                                // of ownership)
    private final java.util.Map<String, Integer> addonTints = new java.util.HashMap<>(); // Per-addon RGB tint colors
                                                                                         // (0xRRGGBB, default 0xFFFFFF)

    // Owned addons methods
    @Override
    public synchronized Set<String> getAddons() {
        return new HashSet<>(addons);
    }

    @Override
    public synchronized void addAddon(String addonId) {
        addons.add(addonId);
    }

    @Override
    public synchronized void removeAddon(String addonId) {
        addons.remove(addonId);
        equippedAddons.remove(addonId); // Also unequip if removing
    }

    /**
     * Remove addon from owned set WITHOUT affecting active state.
     * Used by supporter sync to revoke ownership while preserving external grants.
     */
    public synchronized void removeAddonOwnership(String addonId) {
        addons.remove(addonId);
        // Don't touch equippedAddons or externalGrants - preserve admin/mod grants
    }

    @Override
    public synchronized boolean hasAddon(String addonId) {
        if (addons.contains(addonId))
            return true;

        // Namespace fallback
        ResourceLocation rl = ResourceLocation.tryParse(addonId);
        if (rl != null) {
            String path = rl.getPath();
            if (addons.contains(path))
                return true;
            if (addons.contains("twilight_lib:" + path))
                return true;
        }

        return false;
    }

    @Override
    public synchronized void clearAddons() {
        addons.clear();
        equippedAddons.clear(); // Also clear equipped
    }

    // Active addons methods
    @Override
    public synchronized Set<String> getActiveAddons() {
        return new HashSet<>(equippedAddons);
    }

    @Override
    public synchronized void setActiveAddon(String addonId, boolean active) {
        String finalId = addonId;
        if (!addons.contains(addonId)) {
            // Try namespace fallback
            ResourceLocation rl = ResourceLocation.tryParse(addonId);
            if (rl != null) {
                String path = rl.getPath();
                if (addons.contains(path)) {
                    finalId = path;
                } else if (addons.contains("twilight_lib:" + path)) {
                    finalId = "twilight_lib:" + path;
                }
            }
        }

        if (active) {
            // Player can only activate owned cosmetics via /tlcosmetics
            if (addons.contains(finalId)) {
                equippedAddons.add(finalId);
                playerSelections.add(finalId); // Remember player's choice
                externalGrants.remove(finalId); // Clear any admin override
            }
            // Silently ignore if they don't own it (external grants are managed separately)
        } else {
            // Player can only deactivate their own selections
            if (playerSelections.contains(finalId)) {
                equippedAddons.remove(finalId);
                playerSelections.remove(finalId);
            }
            // Cannot deactivate external grants via player command
        }
    }

    /**
     * Force-set addon state with persistence control (admin/mod command only).
     * 
     * @param addonId    The addon ID to activate/deactivate
     * @param active     true to activate, false to deactivate
     * @param persistent If true, persists through logout/death (external grant); if
     *                   false, temporary preview
     */
    public synchronized void setActiveAddon(String addonId, boolean active, boolean persistent) {
        if (active) {
            equippedAddons.add(addonId);
            if (persistent) {
                externalGrants.add(addonId); // Admin grant - persists regardless of ownership
                playerSelections.remove(addonId); // Clear player selection if present
            } else {
                // Temporary admin preview - not persistent
                externalGrants.remove(addonId);
                playerSelections.remove(addonId);
            }
        } else {
            equippedAddons.remove(addonId);
            externalGrants.remove(addonId);
            playerSelections.remove(addonId);
        }
    }

    /**
     * Force unequip addon regardless of source (admin command only).
     * Removes addon from equipped state and clears all tracking (player selections
     * and external grants).
     * 
     * @param addonId The addon ID to unequip
     */
    public synchronized void forceUnequipAddon(String addonId) {
        equippedAddons.remove(addonId);
        playerSelections.remove(addonId);
        externalGrants.remove(addonId);
    }

    @Override
    public synchronized boolean isAddonActive(String addonId) {
        return equippedAddons.contains(addonId);
    }

    @Override
    public synchronized void clearActiveAddons() {
        equippedAddons.clear();
        playerSelections.clear();
        externalGrants.clear();
    }

    /**
     * Force-sync equipped addons from network packet with registry validation.
     * Used by SyncAddonsPacket to apply server state directly on client.
     * Invalid addon IDs are filtered out to prevent malicious packets.
     */
    public synchronized void syncEquippedFromPacket(Set<String> equipped) {
        this.equippedAddons.clear();
        // Defensive copy to prevent ConcurrentModificationException
        Set<String> equippedCopy = new java.util.HashSet<>(equipped);
        for (String addonId : equippedCopy) {
            if (AddonRegistry.exists(addonId)) {
                this.equippedAddons.add(addonId);
            } else {
                LOGGER.warn("Or, what. Filtered invalid addon ID from sync packet: {}", addonId);
            }
        }
    }

    // Tint color methods
    @Override
    public synchronized int getAddonTint(String addonId) {
        return addonTints.getOrDefault(addonId, 0xFFFFFF); // Default to white (no tint)
    }

    @Override
    public synchronized void setAddonTint(String addonId, int color) {
        if (color == 0xFFFFFF) {
            // Remove white tints to save memory (white is default)
            addonTints.remove(addonId);
        } else {
            addonTints.put(addonId, color);
        }
    }

    @Override
    public synchronized java.util.Map<String, Integer> getAllAddonTints() {
        return new java.util.HashMap<>(addonTints);
    }

    /**
     * Force-sync tint colors from network packet (bypasses all validation).
     * Used by sync packet to apply server state directly on client.
     */
    public synchronized void syncTintsFromPacket(java.util.Map<String, Integer> tints) {
        this.addonTints.clear();
        this.addonTints.putAll(tints);
    }

    @Override
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        int maxSize = TwilightConfig.MAX_NBT_LIST_SIZE.get();

        // Serialize owned addons (with size limit check)
        if (addons.size() > maxSize) {
            throw new IllegalStateException(
                    "Or, what. Cannot serialize - addons list too large: " + addons.size() + " (max " + maxSize + ")");
        }
        ListTag ownedList = new ListTag();
        for (String addon : addons) {
            ownedList.add(StringTag.valueOf(addon));
        }
        tag.put(NBT_ADDONS, ownedList);

        // Serialize player selections (with size limit check)
        if (playerSelections.size() > maxSize) {
            throw new IllegalStateException("Or, what. Cannot serialize - playerSelections list too large: "
                    + playerSelections.size() + " (max " + maxSize + ")");
        }
        ListTag selectionsLi = new ListTag();
        for (String addon : playerSelections) {
            selectionsLi.add(StringTag.valueOf(addon));
        }
        tag.put(NBT_PLAYER_SELECTIONS, selectionsLi);

        // Serialize external grants (with size limit check)
        if (externalGrants.size() > maxSize) {
            throw new IllegalStateException("Or, what. Cannot serialize - externalGrants list too large: "
                    + externalGrants.size() + " (max " + maxSize + ")");
        }
        ListTag externalList = new ListTag();
        for (String addon : externalGrants) {
            externalList.add(StringTag.valueOf(addon));
        }
        tag.put(NBT_EXTERNAL_GRANTS, externalList);

        // Serialize addon tint colors (only non-white colors to save space)
        CompoundTag tintsTag = new CompoundTag();
        for (java.util.Map.Entry<String, Integer> entry : addonTints.entrySet()) {
            tintsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put(NBT_ADDON_TINTS, tintsTag);

        return tag;
    }

    @Override
    public synchronized void deserialize(CompoundTag tag) {
        addons.clear();
        equippedAddons.clear();
        playerSelections.clear();
        externalGrants.clear();
        addonTints.clear();

        // Deserialize owned addons (will be synced from GitHub on login)
        if (tag.contains(NBT_ADDONS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_ADDONS, Tag.TAG_STRING);
            if (list.size() > TwilightConfig.MAX_NBT_LIST_SIZE.get()) {
                throw new IllegalArgumentException("NBT addons list too large: " + list.size() + " (max "
                        + TwilightConfig.MAX_NBT_LIST_SIZE.get() + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                addons.add(list.getString(i));
            }
        }

        // Deserialize player selections
        if (tag.contains(NBT_PLAYER_SELECTIONS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_PLAYER_SELECTIONS, Tag.TAG_STRING);
            if (list.size() > TwilightConfig.MAX_NBT_LIST_SIZE.get()) {
                throw new IllegalArgumentException("NBT player selections list too large: " + list.size() + " (max "
                        + TwilightConfig.MAX_NBT_LIST_SIZE.get() + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                playerSelections.add(list.getString(i));
            }
        }

        // Deserialize external grants (always persist)
        if (tag.contains(NBT_EXTERNAL_GRANTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_EXTERNAL_GRANTS, Tag.TAG_STRING);
            if (list.size() > TwilightConfig.MAX_NBT_LIST_SIZE.get()) {
                throw new IllegalArgumentException("NBT external grants list too large: " + list.size() + " (max "
                        + TwilightConfig.MAX_NBT_LIST_SIZE.get() + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                externalGrants.add(list.getString(i));
            }
        }

        // Deserialize addon tint colors
        if (tag.contains(NBT_ADDON_TINTS, Tag.TAG_COMPOUND)) {
            CompoundTag tintsTag = tag.getCompound(NBT_ADDON_TINTS);
            Set<String> keys = tintsTag.getAllKeys();
            if (keys.size() > TwilightConfig.MAX_NBT_LIST_SIZE.get()) {
                throw new IllegalArgumentException("NBT addon tints map too large: " + keys.size() + " (max "
                        + TwilightConfig.MAX_NBT_LIST_SIZE.get() + ")");
            }
            for (String key : keys) {
                addonTints.put(key, tintsTag.getInt(key));
            }
        }

        // Re-equip cosmetics based on type:
        // 1. Player selections: only if still owned
        for (String addon : playerSelections) {
            if (addons.contains(addon)) {
                equippedAddons.add(addon);
            } else {
                LOGGER.warn(
                        "Wait... they aren't coming back? Player selection '{}' could not be re-equipped (lost ownership - supporter status may have expired)",
                        addon);
            }
        }

        // 2. External grants: always re-equip (regardless of ownership)
        for (String addon : externalGrants) {
            equippedAddons.add(addon);
        }
    }
}
