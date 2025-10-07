package mc.sayda.twilight_lib.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EffectsProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IEffects> EFFECTS_CAP = CapabilityManager.get(new CapabilityToken<>(){});

    private final EffectsData backend = new EffectsData();
    private final LazyOptional<IEffects> optional = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == EFFECTS_CAP ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        backend.deserialize(tag);
    }
}
