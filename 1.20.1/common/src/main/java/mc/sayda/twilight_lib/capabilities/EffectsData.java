package mc.sayda.twilight_lib.capabilities;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.cosmetics.EffectCategory;
import mc.sayda.twilight_lib.cosmetics.EffectType;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class EffectsData implements IEffects {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_EFFECTS = "Effects";
    private static final String NBT_EQUIPPED_EFFECTS = "EquippedEffects";
    private static final String NBT_PLAYER_SELECTIONS = "PlayerSelections";
    private static final String NBT_EXTERNAL_GRANTS = "ExternalGrants";
    // TwilightConfig.MAX_NBT_LIST_SIZE.get() moved to TwilightConfig // Same as
    // network packet limit to prevent DoS

    private final Set<String> effects = new HashSet<>(); // Owned effects (from supporter status)
    private final Set<String> equippedEffects = new HashSet<>(); // Currently equipped effects
    private final Set<String> playerSelections = new HashSet<>(); // Player's choices via /tlcosmetics (re-equip if
                                                                  // owned)
    private final Set<String> externalGrants = new HashSet<>(); // Admin/mod grants via /twilightlib (persist regardless
                                                                // of ownership)

    // Owned effects methods
    @Override
    public synchronized Set<String> getEffects() {
        return new HashSet<>(effects);
    }

    @Override
    public synchronized void addEffect(String effectId) {
        effects.add(effectId);
    }

    @Override
    public synchronized void removeEffect(String effectId) {
        effects.remove(effectId);
        equippedEffects.remove(effectId); // Also unequip if removing
    }

    /**
     * Remove effect from owned set WITHOUT affecting active state.
     * Used by supporter sync to revoke ownership while preserving external grants.
     */
    public synchronized void removeEffectOwnership(String effectId) {
        effects.remove(effectId);
        // Don't touch equippedEffects or externalGrants - preserve admin/mod grants
    }

    @Override
    public synchronized boolean hasEffect(String effectId) {
        if (effects.contains(effectId))
            return true;

        // Namespace fallback
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl != null) {
            String path = rl.getPath();
            if (effects.contains(path))
                return true;
            if (effects.contains("twilight_lib:" + path))
                return true;
        }

        return false;
    }

    @Override
    public synchronized void clearEffects() {
        effects.clear();
        equippedEffects.clear(); // Also clear equipped
    }

    // Active effects methods
    @Override
    public synchronized Set<String> getActiveEffects() {
        return new HashSet<>(equippedEffects);
    }

    @Override
    public synchronized void setActiveEffect(String effectId, boolean active) {
        String finalId = effectId;
        if (!effects.contains(effectId)) {
            // Try namespace fallback
            ResourceLocation rl = ResourceLocation.tryParse(effectId);
            if (rl != null) {
                String path = rl.getPath();
                if (effects.contains(path)) {
                    finalId = path;
                } else if (effects.contains("twilight_lib:" + path)) {
                    finalId = "twilight_lib:" + path;
                }
            }
        }

        final String effectiveId = finalId;
        if (active) {
            // Player can only activate owned cosmetics via /tlcosmetics
            if (effects.contains(effectiveId)) {
                // Enforce one-per-category rule: deactivate other effects in same category
                EffectType newEffectType = EffectType.fromId(effectiveId);
                if (newEffectType != null) {
                    EffectCategory category = newEffectType.getCategory();

                    // Remove any other player-selected effects in this category
                    playerSelections.removeIf(existingEffectId -> {
                        EffectType existingType = EffectType.fromId(existingEffectId);
                        if (existingType != null && existingType.getCategory() == category
                                && !existingEffectId.equals(effectiveId)) {
                            equippedEffects.remove(existingEffectId);
                            return true; // Remove from playerSelections
                        }
                        return false;
                    });
                }

                equippedEffects.add(effectiveId);
                playerSelections.add(effectiveId); // Remember player's choice
                externalGrants.remove(effectiveId); // Clear any admin override
            }
            // Silently ignore if they don't own it (external grants are managed separately)
        } else {
            // Player can only deactivate their own selections
            if (playerSelections.contains(effectiveId)) {
                equippedEffects.remove(effectiveId);
                playerSelections.remove(effectiveId);
            }
            // Cannot deactivate external grants via player command
        }
    }

    /**
     * Force-set effect state with persistence control (admin/mod command only).
     * Enforces one-per-category rule: activating an effect will deactivate other
     * effects in the same category.
     * 
     * @param effectId   The effect ID to activate/deactivate
     * @param active     true to activate, false to deactivate
     * @param persistent If true, persists through logout/death (external grant); if
     *                   false, temporary preview
     */
    public synchronized void setActiveEffect(String effectId, boolean active, boolean persistent) {
        if (active) {
            // Enforce one-per-category rule: deactivate other effects in same category
            EffectType newEffectType = EffectType.fromId(effectId);
            if (newEffectType != null) {
                EffectCategory category = newEffectType.getCategory();

                // Remove any other effects in this category (both player selections and
                // external grants)
                equippedEffects.removeIf(existingEffectId -> {
                    EffectType existingType = EffectType.fromId(existingEffectId);
                    if (existingType != null && existingType.getCategory() == category
                            && !existingEffectId.equals(effectId)) {
                        playerSelections.remove(existingEffectId);
                        externalGrants.remove(existingEffectId);
                        return true; // Remove from equipped
                    }
                    return false;
                });
            }

            equippedEffects.add(effectId);
            if (persistent) {
                externalGrants.add(effectId); // Admin grant - persists regardless of ownership
                playerSelections.remove(effectId); // Clear player selection if present
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
     * Removes effect from equipped state and clears all tracking (player selections
     * and external grants).
     * 
     * @param effectId The effect ID to unequip
     */
    public synchronized void forceUnequipEffect(String effectId) {
        equippedEffects.remove(effectId);
        playerSelections.remove(effectId);
        externalGrants.remove(effectId);
    }

    @Override
    public synchronized boolean isEffectActive(String effectId) {
        return equippedEffects.contains(effectId);
    }

    @Override
    public synchronized void clearActiveEffects() {
        equippedEffects.clear();
        playerSelections.clear();
        externalGrants.clear();
    }

    /**
     * Force-sync equipped effects from network packet with registry validation.
     * Used by SyncEffectsPacket to apply server state directly on client.
     * Invalid effect IDs are filtered out to prevent malicious packets.
     */
    public synchronized void syncEquippedFromPacket(Set<String> equipped) {
        this.equippedEffects.clear();
        // Defensive copy to prevent ConcurrentModificationException
        Set<String> equippedCopy = new java.util.HashSet<>(equipped);
        for (String effectId : equippedCopy) {
            if (EffectType.fromId(effectId) != null) {
                this.equippedEffects.add(effectId);
            } else {
                LOGGER.warn("Or, what. Filtered invalid effect ID from sync packet: {}", effectId);
            }
        }
    }

    @Override
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        int maxSize = TwilightConfig.MAX_NBT_LIST_SIZE.get();

        // Serialize owned effects (with size limit check)
        if (effects.size() > maxSize) {
            throw new IllegalStateException("Or, what. Cannot serialize - effects list too large: " + effects.size()
                    + " (max " + maxSize + ")");
        }
        ListTag ownedList = new ListTag();
        for (String effect : effects) {
            ownedList.add(StringTag.valueOf(effect));
        }
        tag.put(NBT_EFFECTS, ownedList);

        // Serialize player selections (with size limit check)
        if (playerSelections.size() > maxSize) {
            throw new IllegalStateException("Or, what. Cannot serialize - playerSelections list too large: "
                    + playerSelections.size() + " (max " + maxSize + ")");
        }
        ListTag selectionsList = new ListTag();
        for (String effect : playerSelections) {
            selectionsList.add(StringTag.valueOf(effect));
        }
        tag.put(NBT_PLAYER_SELECTIONS, selectionsList);

        // Serialize external grants (with size limit check)
        if (externalGrants.size() > maxSize) {
            throw new IllegalStateException("Or, what. Cannot serialize - externalGrants list too large: "
                    + externalGrants.size() + " (max " + maxSize + ")");
        }
        ListTag externalList = new ListTag();
        for (String effect : externalGrants) {
            externalList.add(StringTag.valueOf(effect));
        }
        tag.put(NBT_EXTERNAL_GRANTS, externalList);

        return tag;
    }

    @Override
    public synchronized void deserialize(CompoundTag tag) {
        effects.clear();
        equippedEffects.clear();
        playerSelections.clear();
        externalGrants.clear();

        // Deserialize owned effects (will be synced from GitHub on login)
        if (tag.contains(NBT_EFFECTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_EFFECTS, Tag.TAG_STRING);
            if (list.size() > TwilightConfig.MAX_NBT_LIST_SIZE.get()) {
                throw new IllegalArgumentException("NBT effects list too large: " + list.size() + " (max "
                        + TwilightConfig.MAX_NBT_LIST_SIZE.get() + ")");
            }
            for (int i = 0; i < list.size(); i++) {
                effects.add(list.getString(i));
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

        // Re-equip cosmetics based on type:
        // 1. Player selections: only if still owned
        for (String effect : playerSelections) {
            if (effects.contains(effect)) {
                equippedEffects.add(effect);
            } else {
                LOGGER.warn(
                        "Wait... they aren't coming back? Effect selection '{}' could not be re-equipped (lost ownership - supporter status may have expired)",
                        effect);
            }
        }

        // 2. External grants: always re-equip (regardless of ownership)
        for (String effect : externalGrants) {
            equippedEffects.add(effect);
        }
    }
}
