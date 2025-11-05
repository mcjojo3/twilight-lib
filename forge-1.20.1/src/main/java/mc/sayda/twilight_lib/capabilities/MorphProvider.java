package mc.sayda.twilight_lib.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class MorphProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IMorph> MORPH_CAP = CapabilityManager.get(new CapabilityToken<>(){});

    private final MorphData morph = new MorphData();
    private final LazyOptional<IMorph> optionalMorph = LazyOptional.of(() -> morph);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == MORPH_CAP ? optionalMorph.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optionalMorph.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return morph.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        morph.deserialize(tag);
    }
}