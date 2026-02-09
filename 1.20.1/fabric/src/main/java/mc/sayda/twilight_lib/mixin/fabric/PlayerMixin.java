package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.capabilities.*;
import mc.sayda.twilight_lib.fabric.FabricPlayerExtensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerMixin implements FabricPlayerExtensions {
    @Unique
    private final IMorph twilight_lib$morph = new MorphData();
    @Unique
    private final IAddons twilight_lib$addons = new AddonsData();
    @Unique
    private final ITrails twilight_lib$trails = new TrailsData();
    @Unique
    private final IEffects twilight_lib$effects = new EffectsData();
    @Unique
    private final IModelVariant twilight_lib$modelVariant = new ModelVariantData();
    @Unique
    private final CompoundTag twilight_lib$persistentData = new CompoundTag();

    @Override
    public IMorph twilight_lib$getMorph() {
        return twilight_lib$morph;
    }

    @Override
    public IAddons twilight_lib$getAddons() {
        return twilight_lib$addons;
    }

    @Override
    public ITrails twilight_lib$getTrails() {
        return twilight_lib$trails;
    }

    @Override
    public IEffects twilight_lib$getEffects() {
        return twilight_lib$effects;
    }

    @Override
    public IModelVariant twilight_lib$getModelVariant() {
        return twilight_lib$modelVariant;
    }

    @Override
    public CompoundTag twilight_lib$getPersistentData() {
        return twilight_lib$persistentData;
    }
}
