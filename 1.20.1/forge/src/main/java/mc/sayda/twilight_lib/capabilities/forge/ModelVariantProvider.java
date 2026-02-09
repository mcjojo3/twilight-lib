package mc.sayda.twilight_lib.capabilities.forge;

import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.ModelVariantData;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Capability provider for player model variant (Steve/Alex).
 */
public class ModelVariantProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<IModelVariant> MODEL_VARIANT_CAP = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final IModelVariant modelVariant = new ModelVariantData();
    private LazyOptional<IModelVariant> optional = LazyOptional.of(() -> modelVariant);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MODEL_VARIANT_CAP) {
            // Recreate LazyOptional if it was invalidated (dimension change bug workaround)
            if (!optional.isPresent()) {
                optional = LazyOptional.of(() -> modelVariant);
            }
        }
        return MODEL_VARIANT_CAP.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return modelVariant.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        modelVariant.deserialize(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
