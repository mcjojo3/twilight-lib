package mc.sayda.twilight_lib.capabilities;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.cosmetics.TrailType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class TrailsData implements ITrails {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_TRAILS = "Trails";
    private static final String NBT_EQUIPPED_TRAILS = "EquippedTrails";
    private static final String NBT_PLAYER_SELECTIONS = "PlayerSelections";
    private static final String NBT_EXTERNAL_GRANTS = "ExternalGrants";
    private static final int MAX_NBT_LIST_SIZE = 1000;  // Same as network packet limit to prevent DoS

    private final Set<String> trails = new HashSet<>();  // Owned trails (from supporter status)
    private final Set<String> equippedTrails = new HashSet<>();  // Currently equipped trails
    private final Set<String> playerSelections = new HashSet<>();  // Player's choices via /tlcosmetics (re-equip if owned)
    private final Set<String> externalGrants = new HashSet<>();  // Admin/mod grants via /twilightlib (persist regardless of ownership)

    // Owned trails methods
    @Override
    public synchronized Set<String> getTrails() {
        return new HashSet<>(trails);
    }

    @Override
    public synchronized void addTrail(String trailId) {
        trails.add(trailId);
    }

    @Override
    public synchronized void removeTrail(String trailId) {
        trails.remove(trailId);
        equippedTrails.remove(trailId);  // Also unequip if removing
    }

    /**
     * Remove trail from owned set WITHOUT affecting active state.
     * Used by supporter sync to revoke ownership while preserving external grants.
     */
    public synchronized void removeTrailOwnership(String trailId) {
        trails.remove(trailId);
        // Don't touch equippedTrails or externalGrants - preserve admin/mod grants
    }

    @Override
    public synchronized boolean hasTrail(String trailId) {
        return trails.contains(trailId);
    }

    @Override
    public synchronized void clearTrails() {
        trails.clear();
        equippedTrails.clear();  // Also clear equipped
    }

    // Active trails methods
    @Override
    public synchronized Set<String> getActiveTrails() {
        return new HashSet<>(equippedTrails);
    }

    @Override
    public synchronized void setActiveTrail(String trailId, boolean active) {
        if (active) {
            // Player can only activate owned cosmetics via /tlcosmetics
            if (trails.contains(trailId)) {
                equippedTrails.add(trailId);
                playerSelections.add(trailId);  // Remember player's choice
                externalGrants.remove(trailId);  // Clear any admin override
            }
            // Silently ignore if they don't own it (external grants are managed separately)
        } else {
            // Player can only deactivate their own selections
            if (playerSelections.contains(trailId)) {
                equippedTrails.remove(trailId);
                playerSelections.remove(trailId);
            }
            // Cannot deactivate external grants via player command
        }
    }

    /**
     * Force-set trail state with persistence control (admin/mod command only).
     * @param trailId The trail ID to activate/deactivate
     * @param active true to activate, false to deactivate
     * @param persistent If true, persists through logout/death (external grant); if false, temporary preview
     */
    public synchronized void setActiveTrail(String trailId, boolean active, boolean persistent) {
        if (active) {
            equippedTrails.add(trailId);
            if (persistent) {
                externalGrants.add(trailId);  // Admin grant - persists regardless of ownership
                playerSelections.remove(trailId);  // Clear player selection if present
            } else {
                // Temporary admin preview - not persistent
                externalGrants.remove(trailId);
                playerSelections.remove(trailId);
            }
        } else {
            equippedTrails.remove(trailId);
            externalGrants.remove(trailId);
            playerSelections.remove(trailId);
        }
    }

    /**
     * Force unequip trail regardless of source (admin command only).
     * Removes trail from equipped state and clears all tracking (player selections and external grants).
     * @param trailId The trail ID to unequip
     */
    public synchronized void forceUnequipTrail(String trailId) {
        equippedTrails.remove(trailId);
        playerSelections.remove(trailId);
        externalGrants.remove(trailId);
    }

    @Override
    public synchronized boolean isTrailActive(String trailId) {
        return equippedTrails.contains(trailId);
    }

    @Override
    public synchronized void clearActiveTrails() {
        equippedTrails.clear();
        playerSelections.clear();
        externalGrants.clear();
    }

    /**
     * Force-sync equipped trails from network packet with registry validation.
     * Used by SyncTrailsPacket to apply server state directly on client.
     * Invalid trail IDs are filtered out to prevent malicious packets.
     */
    public synchronized void syncEquippedFromPacket(Set<String> equipped) {
        this.equippedTrails.clear();
        for (String trailId : equipped) {
            if (TrailType.fromId(trailId) != null) {
                this.equippedTrails.add(trailId);
            } else {
                LOGGER.warn("Filtered invalid trail ID from sync packet: {}", trailId);
            }
        }
    }

    @Override
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        // Serialize owned trails
        ListTag ownedList = new ListTag();
        for (String trail : trails) {
            ownedList.add(StringTag.valueOf(trail));
        }
        tag.put(NBT_TRAILS, ownedList);

        // Serialize player selections (for re-equipping if still owned)
        ListTag selectionsLi = new ListTag();
        for (String trail : playerSelections) {
            selectionsLi.add(StringTag.valueOf(trail));
        }
        tag.put(NBT_PLAYER_SELECTIONS, selectionsLi);

        // Serialize external grants (admin/mod forced, always persist)
        ListTag externalList = new ListTag();
        for (String trail : externalGrants) {
            externalList.add(StringTag.valueOf(trail));
        }
        tag.put(NBT_EXTERNAL_GRANTS, externalList);

        return tag;
    }

    @Override
    public synchronized void deserialize(CompoundTag tag) {
        trails.clear();
        equippedTrails.clear();
        playerSelections.clear();
        externalGrants.clear();

        // Deserialize owned trails (will be synced from GitHub on login)
        if (tag.contains(NBT_TRAILS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_TRAILS, Tag.TAG_STRING);
            if (list.size() > MAX_NBT_LIST_SIZE) {
                throw new IllegalArgumentException("NBT trails list too large: " + list.size() + " (max " + MAX_NBT_LIST_SIZE + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                trails.add(list.getString(i));
            }
        }

        // Deserialize player selections
        if (tag.contains(NBT_PLAYER_SELECTIONS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_PLAYER_SELECTIONS, Tag.TAG_STRING);
            if (list.size() > MAX_NBT_LIST_SIZE) {
                throw new IllegalArgumentException("NBT player selections list too large: " + list.size() + " (max " + MAX_NBT_LIST_SIZE + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                playerSelections.add(list.getString(i));
            }
        }

        // Deserialize external grants (always persist)
        if (tag.contains(NBT_EXTERNAL_GRANTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_EXTERNAL_GRANTS, Tag.TAG_STRING);
            if (list.size() > MAX_NBT_LIST_SIZE) {
                throw new IllegalArgumentException("NBT external grants list too large: " + list.size() + " (max " + MAX_NBT_LIST_SIZE + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                externalGrants.add(list.getString(i));
            }
        }

        // Re-equip cosmetics based on type:
        // 1. Player selections: only if still owned
        for (String trail : playerSelections) {
            if (trails.contains(trail)) {
                equippedTrails.add(trail);
            } else {
                LOGGER.warn("Really?! Player selection '{}' could not be re-equipped (lost ownership - supporter status may have expired)", trail);
            }
        }

        // 2. External grants: always re-equip (regardless of ownership)
        for (String trail : externalGrants) {
            equippedTrails.add(trail);
        }
    }
}
