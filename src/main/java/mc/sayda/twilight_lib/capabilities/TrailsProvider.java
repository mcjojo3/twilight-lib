package mc.sayda.twilight_lib.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrailsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ITrails> TRAILS_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    private final ITrails trails = new TrailsData();
    private final LazyOptional<ITrails> optionalTrails = LazyOptional.of(() -> trails);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == TRAILS_CAP ? optionalTrails.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return trails.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        trails.deserialize(nbt);
    }

    public void invalidate() {
        optionalTrails.invalidate();
    }
}