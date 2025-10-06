package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public interface IPlayerAddons {
    Set<String> getAddons();
    void addAddon(String addonId);
    void removeAddon(String addonId);
    boolean hasAddon(String addonId);
    void clearAddons();

    CompoundTag serialize();
    void deserialize(CompoundTag tag);
}