package mc.sayda.twilight_lib.capabilities.forge;

import mc.sayda.twilight_lib.capabilities.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

public class DataUtilsImpl {
    public static IMorph getMorphData(Player player) {
        return player.getCapability(MorphProvider.MORPH_CAP).orElse(null);
    }

    public static IAddons getAddonsData(Player player) {
        return player.getCapability(AddonsProvider.ADDONS_CAP).orElse(null);
    }

    public static ITrails getTrailsData(Player player) {
        return player.getCapability(TrailsProvider.TRAILS_CAP).orElse(null);
    }

    public static IEffects getEffectsData(Player player) {
        return player.getCapability(EffectsProvider.EFFECTS_CAP).orElse(null);
    }

    public static IModelVariant getModelVariantData(Player player) {
        return player.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).orElse(null);
    }

    public static CompoundTag getPersistentData(Player player) {
        CompoundTag forgeData = player.getPersistentData();
        if (!forgeData.contains("TwilightPersistentData", 10)) {
            forgeData.put("TwilightPersistentData", new CompoundTag());
        }
        return forgeData.getCompound("TwilightPersistentData");
    }
}
