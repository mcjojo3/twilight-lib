package mc.sayda.twilight_lib.capabilities.fabric;

import mc.sayda.twilight_lib.capabilities.*;
import mc.sayda.twilight_lib.fabric.FabricPlayerExtensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

public class DataUtilsImpl {
    public static IMorph getMorphData(Player player) {
        return ((FabricPlayerExtensions) player).twilight_lib$getMorph();
    }

    public static IAddons getAddonsData(Player player) {
        return ((FabricPlayerExtensions) player).twilight_lib$getAddons();
    }

    public static ITrails getTrailsData(Player player) {
        return ((FabricPlayerExtensions) player).twilight_lib$getTrails();
    }

    public static IEffects getEffectsData(Player player) {
        return ((FabricPlayerExtensions) player).twilight_lib$getEffects();
    }

    public static IModelVariant getModelVariantData(Player player) {
        return ((FabricPlayerExtensions) player).twilight_lib$getModelVariant();
    }

    public static CompoundTag getPersistentData(Player player) {
        return ((FabricPlayerExtensions) player).twilight_lib$getPersistentData();
    }
}
