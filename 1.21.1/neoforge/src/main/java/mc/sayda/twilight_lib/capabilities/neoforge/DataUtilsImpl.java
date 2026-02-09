package mc.sayda.twilight_lib.capabilities.neoforge;

import mc.sayda.twilight_lib.neoforge.capabilities.ModAttachments;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ITrails;
import net.minecraft.world.entity.player.Player;

public class DataUtilsImpl {
    public static IMorph getMorphData(Player player) {
        return player.getData(ModAttachments.MORPH.get());
    }

    public static IAddons getAddonsData(Player player) {
        return player.getData(ModAttachments.ADDONS.get());
    }

    public static ITrails getTrailsData(Player player) {
        return player.getData(ModAttachments.TRAILS.get());
    }

    public static IEffects getEffectsData(Player player) {
        return player.getData(ModAttachments.EFFECTS.get());
    }

    public static IModelVariant getModelVariantData(Player player) {
        return player.getData(ModAttachments.MODEL_VARIANT.get());
    }

    public static net.minecraft.nbt.CompoundTag getPersistentData(Player player) {
        return player.getData(ModAttachments.PERSISTENT_DATA.get());
    }
}
