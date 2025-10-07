package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class EffectsData implements IEffects {
    private final Set<String> effects = new HashSet<>();

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
    }

    @Override
    public boolean hasEffect(String effectId) {
        return effects.contains(effectId);
    }

    @Override
    public void clearEffects() {
        effects.clear();
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String effect : effects) {
            list.add(StringTag.valueOf(effect));
        }
        tag.put("Effects", list);
        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        effects.clear();
        if (tag.contains("Effects", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Effects", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                effects.add(list.getString(i));
            }
        }
    }
}
