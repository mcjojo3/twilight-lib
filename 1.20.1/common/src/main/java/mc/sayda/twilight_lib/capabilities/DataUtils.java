package mc.sayda.twilight_lib.capabilities;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public class DataUtils {
    @ExpectPlatform
    public static IMorph getMorphData(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static IAddons getAddonsData(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ITrails getTrailsData(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static IEffects getEffectsData(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static IModelVariant getModelVariantData(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static net.minecraft.nbt.CompoundTag getPersistentData(Player player) {
        throw new AssertionError();
    }
}
