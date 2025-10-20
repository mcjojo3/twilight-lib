package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class AddonsData implements IAddons {
    private final Set<String> addons = new HashSet<>();  // Owned addons
    private final Set<String> equippedAddons = new HashSet<>();  // Currently equipped addons
    private final Set<String> persistentAddons = new HashSet<>();  // Addons that bypass ownership (CreRaces)

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
            // Allow activating without ownership check (admin commands can force-activate)
            equippedAddons.add(addonId);
            // Default to non-persistent for backwards compatibility
            persistentAddons.remove(addonId);
        } else {
            equippedAddons.remove(addonId);
            persistentAddons.remove(addonId);
        }
    }

    /**
     * Set whether an addon is active with persistence control.
     * @param addonId The addon ID to activate/deactivate
     * @param active true to activate, false to deactivate
     * @param persistent If true, addon persists through logout/death; if false, cleared on logout
     */
    public void setActiveAddon(String addonId, boolean active, boolean persistent) {
        if (active) {
            equippedAddons.add(addonId);
            if (persistent) {
                persistentAddons.add(addonId);
            } else {
                persistentAddons.remove(addonId);
            }
        } else {
            equippedAddons.remove(addonId);
            persistentAddons.remove(addonId);
        }
    }

    @Override
    public boolean isAddonActive(String addonId) {
        return equippedAddons.contains(addonId);
    }

    @Override
    public void clearActiveAddons() {
        equippedAddons.clear();
        persistentAddons.clear();
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        // Serialize owned addons
        ListTag ownedList = new ListTag();
        for (String addon : addons) {
            ownedList.add(StringTag.valueOf(addon));
        }
        tag.put("Addons", ownedList);

        // Serialize equipped addons
        ListTag equippedList = new ListTag();
        for (String addon : equippedAddons) {
            equippedList.add(StringTag.valueOf(addon));
        }
        tag.put("EquippedAddons", equippedList);

        // Serialize persistent addons
        ListTag persistentList = new ListTag();
        for (String addon : persistentAddons) {
            persistentList.add(StringTag.valueOf(addon));
        }
        tag.put("PersistentAddons", persistentList);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        addons.clear();
        equippedAddons.clear();
        persistentAddons.clear();

        // Deserialize owned addons
        if (tag.contains("Addons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Addons", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                addons.add(list.getString(i));
            }
        }

        // Deserialize persistent addons (these bypass ownership validation)
        if (tag.contains("PersistentAddons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("PersistentAddons", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                persistentAddons.add(list.getString(i));
            }
        }

        // Deserialize equipped addons with ownership validation
        if (tag.contains("EquippedAddons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("EquippedAddons", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String addonId = list.getString(i);
                // Load persistent addons or owned addons only
                if (persistentAddons.contains(addonId) || addons.contains(addonId)) {
                    equippedAddons.add(addonId);
                }
                // Non-persistent addons without ownership are cleared (temporary admin previews)
            }
        }
    }
}