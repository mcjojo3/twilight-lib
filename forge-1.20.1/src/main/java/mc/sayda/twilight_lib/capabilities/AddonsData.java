package mc.sayda.twilight_lib.capabilities;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class AddonsData implements IAddons {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_ADDONS = "Addons";
    private static final String NBT_EQUIPPED_ADDONS = "EquippedAddons";
    private static final String NBT_PLAYER_SELECTIONS = "PlayerSelections";
    private static final String NBT_EXTERNAL_GRANTS = "ExternalGrants";

    private final Set<String> addons = new HashSet<>();  // Owned addons (from supporter status)
    private final Set<String> equippedAddons = new HashSet<>();  // Currently equipped addons
    private final Set<String> playerSelections = new HashSet<>();  // Player's choices via /tlcosmetics (re-equip if owned)
    private final Set<String> externalGrants = new HashSet<>();  // Admin/mod grants via /twilightlib (persist regardless of ownership)

    // Owned addons methods
    @Override
    public Set<String> getAddons() {
        return new HashSet<>(addons);
    }

    @Override
    public void addAddon(String addonId) {
        addons.add(addonId);
    }

    @Override
    public void removeAddon(String addonId) {
        addons.remove(addonId);
        equippedAddons.remove(addonId);  // Also unequip if removing
    }

    /**
     * Remove addon from owned set WITHOUT affecting active state.
     * Used by supporter sync to revoke ownership while preserving external grants.
     */
    public void removeAddonOwnership(String addonId) {
        addons.remove(addonId);
        // Don't touch equippedAddons or externalGrants - preserve admin/mod grants
    }

    @Override
    public boolean hasAddon(String addonId) {
        return addons.contains(addonId);
    }

    @Override
    public void clearAddons() {
        addons.clear();
        equippedAddons.clear();  // Also clear equipped
    }

    // Active addons methods
    @Override
    public Set<String> getActiveAddons() {
        return new HashSet<>(equippedAddons);
    }

    @Override
    public void setActiveAddon(String addonId, boolean active) {
        if (active) {
            // Player can only activate owned cosmetics via /tlcosmetics
            if (addons.contains(addonId)) {
                equippedAddons.add(addonId);
                playerSelections.add(addonId);  // Remember player's choice
                externalGrants.remove(addonId);  // Clear any admin override
            }
            // Silently ignore if they don't own it (external grants are managed separately)
        } else {
            // Player can only deactivate their own selections
            if (playerSelections.contains(addonId)) {
                equippedAddons.remove(addonId);
                playerSelections.remove(addonId);
            }
            // Cannot deactivate external grants via player command
        }
    }

    /**
     * Force-set addon state with persistence control (admin/mod command only).
     * @param addonId The addon ID to activate/deactivate
     * @param active true to activate, false to deactivate
     * @param persistent If true, persists through logout/death (external grant); if false, temporary preview
     */
    public void setActiveAddon(String addonId, boolean active, boolean persistent) {
        if (active) {
            equippedAddons.add(addonId);
            if (persistent) {
                externalGrants.add(addonId);  // Admin grant - persists regardless of ownership
                playerSelections.remove(addonId);  // Clear player selection if present
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
     * Removes addon from equipped state and clears all tracking (player selections and external grants).
     * @param addonId The addon ID to unequip
     */
    public void forceUnequipAddon(String addonId) {
        equippedAddons.remove(addonId);
        playerSelections.remove(addonId);
        externalGrants.remove(addonId);
    }

    @Override
    public boolean isAddonActive(String addonId) {
        return equippedAddons.contains(addonId);
    }

    @Override
    public void clearActiveAddons() {
        equippedAddons.clear();
        playerSelections.clear();
        externalGrants.clear();
    }

    /**
     * Force-sync equipped addons from network packet (bypasses all validation).
     * Used by SyncAddonsPacket to apply server state directly on client.
     */
    public void syncEquippedFromPacket(Set<String> equipped) {
        this.equippedAddons.clear();
        this.equippedAddons.addAll(equipped);
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        // Serialize owned addons
        ListTag ownedList = new ListTag();
        for (String addon : addons) {
            ownedList.add(StringTag.valueOf(addon));
        }
        tag.put(NBT_ADDONS, ownedList);

        // Serialize player selections (for re-equipping if still owned)
        ListTag selectionsLi = new ListTag();
        for (String addon : playerSelections) {
            selectionsLi.add(StringTag.valueOf(addon));
        }
        tag.put(NBT_PLAYER_SELECTIONS, selectionsLi);

        // Serialize external grants (admin/mod forced, always persist)
        ListTag externalList = new ListTag();
        for (String addon : externalGrants) {
            externalList.add(StringTag.valueOf(addon));
        }
        tag.put(NBT_EXTERNAL_GRANTS, externalList);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        addons.clear();
        equippedAddons.clear();
        playerSelections.clear();
        externalGrants.clear();

        // Deserialize owned addons (will be synced from GitHub on login)
        if (tag.contains(NBT_ADDONS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_ADDONS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                addons.add(list.getString(i));
            }
        }

        // Deserialize player selections
        if (tag.contains(NBT_PLAYER_SELECTIONS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_PLAYER_SELECTIONS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                playerSelections.add(list.getString(i));
            }
        }

        // Deserialize external grants (always persist)
        if (tag.contains(NBT_EXTERNAL_GRANTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_EXTERNAL_GRANTS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                externalGrants.add(list.getString(i));
            }
        }

        // Re-equip cosmetics based on type:
        // 1. Player selections: only if still owned
        for (String addon : playerSelections) {
            if (addons.contains(addon)) {
                equippedAddons.add(addon);
            } else {
                LOGGER.warn("Oh no! Player selection '{}' could not be re-equipped (lost ownership - supporter status may have expired)", addon);
            }
        }

        // 2. External grants: always re-equip (regardless of ownership)
        for (String addon : externalGrants) {
            equippedAddons.add(addon);
        }
    }
}