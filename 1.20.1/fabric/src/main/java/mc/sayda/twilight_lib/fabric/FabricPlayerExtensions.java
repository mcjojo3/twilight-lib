package mc.sayda.twilight_lib.fabric;

import mc.sayda.twilight_lib.capabilities.*;
import net.minecraft.nbt.CompoundTag;

public interface FabricPlayerExtensions {
    IMorph twilight_lib$getMorph();

    IAddons twilight_lib$getAddons();

    ITrails twilight_lib$getTrails();

    IEffects twilight_lib$getEffects();

    IModelVariant twilight_lib$getModelVariant();

    CompoundTag twilight_lib$getPersistentData();
}
