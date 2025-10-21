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

public class AddonsProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IAddons> ADDONS_CAP = CapabilityManager.get(new CapabilityToken<>(){});

    private final AddonsData addons = new AddonsData();
    private final LazyOptional<IAddons> optionalAddons = LazyOptional.of(() -> addons);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == ADDONS_CAP ? optionalAddons.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optionalAddons.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return addons.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        addons.deserialize(tag);
    }
}