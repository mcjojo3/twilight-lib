package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class EffectsData implements IEffects {
    private static final String NBT_EFFECTS = "Effects";
    private static final String NBT_EQUIPPED_EFFECTS = "EquippedEffects";
    private static final String NBT_PLAYER_SELECTIONS = "PlayerSelections";
    private static final String NBT_EXTERNAL_GRANTS = "ExternalGrants";

    private final Set<String> effects = new HashSet<>();  // Owned effects (from supporter status)
    private final Set<String> equippedEffects = new HashSet<>();  // Currently equipped effects
    private final Set<String> playerSelections = new HashSet<>();  // Player's choices via /tlcosmetics (re-equip if owned)
    private final Set<String> externalGrants = new HashSet<>();  // Admin/mod grants via /twilightlib (persist regardless of ownership)

    // Owned effects methods
    @Override
    public Set<String> getEffects() {
        return new HashSet<>(effects);
    }

    @Override
    public void addEffect(String effectId) {
        effects.add(effectId);
    }

    @Override
    public void removeEffect(String effectId) {
        effects.remove(effectId);
        equippedEffects.remove(effectId);  // Also unequip if removing
    }

    /**
     * Remove effect from owned set WITHOUT affecting active state.
     * Used by supporter sync to revoke ownership while preserving external grants.
     */
    public void removeEffectOwnership(String effectId) {
        effects.remove(effectId);
        // Don't touch equippedEffects or externalGrants - preserve admin/mod grants
    }

    @Override
    public boolean hasEffect(String effectId) {
        return effects.contains(effectId);
    }

    @Override
    public void clearEffects() {
        effects.clear();
        equippedEffects.clear();  // Also clear equipped
    }

    // Active effects methods
    @Override
    public Set<String> getActiveEffects() {
        return new HashSet<>(equippedEffects);
    }

    @Override
    public void setActiveEffect(String effectId, boolean active) {
        if (active) {
            // Player can only activate owned cosmetics via /tlcosmetics
            if (effects.contains(effectId)) {
                equippedEffects.add(effectId);
                playerSelections.add(effectId);  // Remember player's choice
                externalGrants.remove(effectId);  // Clear any admin override
            }
            // Silently ignore if they don't own it (external grants are managed separately)
        } else {
            // Player can only deactivate their own selections
            if (playerSelections.contains(effectId)) {
                equippedEffects.remove(effectId);
                playerSelections.remove(effectId);
            }
            // Cannot deactivate external grants via player command
        }
    }

    /**
     * Force-set effect state with persistence control (admin/mod command only).
     * @param effectId The effect ID to activate/deactivate
     * @param active true to activate, false to deactivate
     * @param persistent If true, persists through logout/death (external grant); if false, temporary preview
     */
    public void setActiveEffect(String effectId, boolean active, boolean persistent) {
        if (active) {
            equippedEffects.add(effectId);
            if (persistent) {
                externalGrants.add(effectId);  // Admin grant - persists regardless of ownership
                playerSelections.remove(effectId);  // Clear player selection if present
            } else {
                // Temporary admin preview - not persistent
                externalGrants.remove(effectId);
                playerSelections.remove(effectId);
            }
        } else {
            equippedEffects.remove(effectId);
            externalGrants.remove(effectId);
            playerSelections.remove(effectId);
        }
    }

    /**
     * Force unequip effect regardless of source (admin command only).
     * Removes effect from equipped state and clears all tracking (player selections and external grants).
     * @param effectId The effect ID to unequip
     */
    public void forceUnequipEffect(String effectId) {
        equippedEffects.remove(effectId);
        playerSelections.remove(effectId);
        externalGrants.remove(effectId);
    }

    @Override
    public boolean isEffectActive(String effectId) {
        return equippedEffects.contains(effectId);
    }

    @Override
    public void clearActiveEffects() {
        equippedEffects.clear();
        playerSelections.clear();
        externalGrants.clear();
    }

    /**
     * Force-sync equipped effects from network packet (bypasses all validation).
     * Used by SyncEffectsPacket to apply server state directly on client.
     */
    public void syncEquippedFromPacket(Set<String> equipped) {
        this.equippedEffects.clear();
        this.equippedEffects.addAll(equipped);
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        // Serialize owned effects
        ListTag ownedList = new ListTag();
        for (String effect : effects) {
            ownedList.add(StringTag.valueOf(effect));
        }
        tag.put(NBT_EFFECTS, ownedList);

        // Serialize player selections (for re-equipping if still owned)
        ListTag selectionsList = new ListTag();
        for (String effect : playerSelections) {
            selectionsList.add(StringTag.valueOf(effect));
        }
        tag.put(NBT_PLAYER_SELECTIONS, selectionsList);

        // Serialize external grants (admin/mod forced, always persist)
        ListTag externalList = new ListTag();
        for (String effect : externalGrants) {
            externalList.add(StringTag.valueOf(effect));
        }
        tag.put(NBT_EXTERNAL_GRANTS, externalList);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        effects.clear();
        equippedEffects.clear();
        playerSelections.clear();
        externalGrants.clear();

        // Deserialize owned effects (will be synced from GitHub on login)
        if (tag.contains(NBT_EFFECTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_EFFECTS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                effects.add(list.getString(i));
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
        for (String effect : playerSelections) {
            if (effects.contains(effect)) {
                equippedEffects.add(effect);
            }
        }

        // 2. External grants: always re-equip (regardless of ownership)
        for (String effect : externalGrants) {
            equippedEffects.add(effect);
        }
    }
}
