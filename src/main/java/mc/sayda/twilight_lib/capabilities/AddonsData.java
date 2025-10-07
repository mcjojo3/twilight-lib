package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class AddonsData implements IAddons {
    private final Set<String> addons = new HashSet<>();

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
    }

    @Override
    public boolean hasAddon(String addonId) {
        return addons.contains(addonId);
    }

    @Override
    public void clearAddons() {
        addons.clear();
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String addon : addons) {
            list.add(StringTag.valueOf(addon));
        }
        tag.put("Addons", list);
        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        addons.clear();
        if (tag.contains("Addons", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Addons", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                addons.add(list.getString(i));
            }
        }
    }
}