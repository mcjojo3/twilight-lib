package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public interface IEffects {
    Set<String> getEffects();
    void addEffect(String effectId);
    void removeEffect(String effectId);
    boolean hasEffect(String effectId);
    void clearEffects();

    CompoundTag serialize();
    void deserialize(CompoundTag tag);
}
