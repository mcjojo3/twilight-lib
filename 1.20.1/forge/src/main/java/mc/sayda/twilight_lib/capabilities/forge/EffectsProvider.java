package mc.sayda.twilight_lib.capabilities.forge;

import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.EffectsData;

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
    public static final Capability<IEffects> EFFECTS_CAP = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final EffectsData effects = new EffectsData();
    private LazyOptional<IEffects> optionalEffects = LazyOptional.of(() -> effects);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == EFFECTS_CAP) {
            // Recreate LazyOptional if it was invalidated (dimension change bug workaround)
            if (!optionalEffects.isPresent()) {
                optionalEffects = LazyOptional.of(() -> effects);
            }
            return optionalEffects.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        optionalEffects.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return effects.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        effects.deserialize(tag);
    }
}
