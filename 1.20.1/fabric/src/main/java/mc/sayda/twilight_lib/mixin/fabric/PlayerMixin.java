package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.*;
import mc.sayda.twilight_lib.fabric.FabricPlayerExtensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;

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

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void twilightlib$addAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        CompoundTag twilightData = new CompoundTag();
        twilightData.put(TwilightConstants.NBT_MORPH, twilight_lib$morph.serialize());
        twilightData.put(TwilightConstants.NBT_ADDONS, twilight_lib$addons.serialize());
        twilightData.put(TwilightConstants.NBT_TRAILS, twilight_lib$trails.serialize());
        twilightData.put(TwilightConstants.NBT_EFFECTS, twilight_lib$effects.serialize());
        twilightData.put(TwilightConstants.NBT_MODEL_VARIANT, twilight_lib$modelVariant.serialize());
        twilightData.put("persistent_data", twilight_lib$persistentData.copy());

        nbt.put("TwilightLibPersistence", twilightData);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void twilightlib$readAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("TwilightLibPersistence", 10)) {
            CompoundTag twilightData = nbt.getCompound("TwilightLibPersistence");
            if (twilightData.contains(TwilightConstants.NBT_MORPH, 10)) {
                twilight_lib$morph.deserialize(twilightData.getCompound(TwilightConstants.NBT_MORPH));
            }
            if (twilightData.contains(TwilightConstants.NBT_ADDONS, 10)) {
                twilight_lib$addons.deserialize(twilightData.getCompound(TwilightConstants.NBT_ADDONS));
            }
            if (twilightData.contains(TwilightConstants.NBT_TRAILS, 10)) {
                twilight_lib$trails.deserialize(twilightData.getCompound(TwilightConstants.NBT_TRAILS));
            }
            if (twilightData.contains(TwilightConstants.NBT_EFFECTS, 10)) {
                twilight_lib$effects.deserialize(twilightData.getCompound(TwilightConstants.NBT_EFFECTS));
            }
            if (twilightData.contains(TwilightConstants.NBT_MODEL_VARIANT, 10)) {
                twilight_lib$modelVariant.deserialize(twilightData.getCompound(TwilightConstants.NBT_MODEL_VARIANT));
            }
            if (twilightData.contains("persistent_data", 10)) {
                twilight_lib$persistentData.merge(twilightData.getCompound("persistent_data"));
            }
        }
    }

    @Unique
    public void twilight_lib$copyFrom(net.minecraft.world.entity.player.Player other) {
        FabricPlayerExtensions extensions = (FabricPlayerExtensions) other;
        this.twilight_lib$morph.deserialize(extensions.twilight_lib$getMorph().serialize());
        this.twilight_lib$addons.deserialize(extensions.twilight_lib$getAddons().serialize());
        this.twilight_lib$trails.deserialize(extensions.twilight_lib$getTrails().serialize());
        this.twilight_lib$effects.deserialize(extensions.twilight_lib$getEffects().serialize());
        this.twilight_lib$modelVariant.deserialize(extensions.twilight_lib$getModelVariant().serialize());

        // Merge persistent data
        CompoundTag otherPersistentData = extensions.twilight_lib$getPersistentData();
        if (otherPersistentData != null && !otherPersistentData.isEmpty()) {
            this.twilight_lib$persistentData.merge(otherPersistentData);
        }
    }
}
