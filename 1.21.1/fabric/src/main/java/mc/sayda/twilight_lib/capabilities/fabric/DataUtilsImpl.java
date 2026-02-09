package mc.sayda.twilight_lib.capabilities.fabric;

import mc.sayda.twilight_lib.capabilities.FabricModAttachments;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ITrails;
import net.minecraft.world.entity.player.Player;

public class DataUtilsImpl {
    public static IMorph getMorphData(Player player) {
        return player.getAttachedOrCreate(FabricModAttachments.MORPH);
    }

    public static IAddons getAddonsData(Player player) {
        return player.getAttachedOrCreate(FabricModAttachments.ADDONS);
    }

    public static ITrails getTrailsData(Player player) {
        return player.getAttachedOrCreate(FabricModAttachments.TRAILS);
    }

    public static IEffects getEffectsData(Player player) {
        return player.getAttachedOrCreate(FabricModAttachments.EFFECTS);
    }

    public static IModelVariant getModelVariantData(Player player) {
        return player.getAttachedOrCreate(FabricModAttachments.MODEL_VARIANT);
    }

    public static net.minecraft.nbt.CompoundTag getPersistentData(Player player) {
        return player.getAttachedOrCreate(FabricModAttachments.PERSISTENT_DATA);
    }
}
