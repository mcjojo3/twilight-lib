package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class EffectsData implements IEffects {
    private final Set<String> effects = new HashSet<>();  // Owned effects
    private final Set<String> equippedEffects = new HashSet<>();  // Currently equipped effects

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
            // Allow activating without ownership check (admin commands can force-activate)
            equippedEffects.add(effectId);
        } else {
            equippedEffects.remove(effectId);
        }
    }

    @Override
    public boolean isEffectActive(String effectId) {
        return equippedEffects.contains(effectId);
    }

    @Override
    public void clearActiveEffects() {
        equippedEffects.clear();
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        // Serialize owned effects
        ListTag ownedList = new ListTag();
        for (String effect : effects) {
            ownedList.add(StringTag.valueOf(effect));
        }
        tag.put("Effects", ownedList);

        // Serialize equipped effects
        ListTag equippedList = new ListTag();
        for (String effect : equippedEffects) {
            equippedList.add(StringTag.valueOf(effect));
        }
        tag.put("EquippedEffects", equippedList);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        effects.clear();
        equippedEffects.clear();

        // Deserialize owned effects first
        if (tag.contains("Effects", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Effects", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                effects.add(list.getString(i));
            }
        }

        // Deserialize equipped effects, validating ownership
        if (tag.contains("EquippedEffects", Tag.TAG_LIST)) {
            ListTag list = tag.getList("EquippedEffects", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String effectId = list.getString(i);
                // Only restore equipped effects that are owned
                // This removes admin force-equipped effects on logout/death
                if (effects.contains(effectId)) {
                    equippedEffects.add(effectId);
                }
            }
        }
    }
}
