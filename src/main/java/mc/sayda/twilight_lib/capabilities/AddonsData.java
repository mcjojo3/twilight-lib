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
        } else {
            equippedAddons.remove(addonId);
        }
    }

    @Override
    public boolean isAddonActive(String addonId) {
        return equippedAddons.contains(addonId);
    }

    @Override
    public void clearActiveAddons() {
        equippedAddons.clear();
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

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        addons.clear();
        equippedAddons.clear();

        // Deserialize owned addons
        if (tag.contains("Addons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Addons", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                addons.add(list.getString(i));
            }
        }

        // Deserialize equipped addons
        if (tag.contains("EquippedAddons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("EquippedAddons", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String addonId = list.getString(i);
                // Load all equipped addons (including admin force-equipped ones)
                equippedAddons.add(addonId);
            }
        }
    }
}