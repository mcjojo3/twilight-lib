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

public class TrailsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ITrails> TRAILS_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    private final ITrails trails = new TrailsData();
    private LazyOptional<ITrails> optionalTrails = LazyOptional.of(() -> trails);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == TRAILS_CAP) {
            // Recreate LazyOptional if it was invalidated (dimension change bug workaround)
            if (!optionalTrails.isPresent()) {
                optionalTrails = LazyOptional.of(() -> trails);
            }
            return optionalTrails.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        optionalTrails.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return trails.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        trails.deserialize(nbt);
    }
}