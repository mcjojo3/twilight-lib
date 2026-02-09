package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * Implementation of IModelVariant for storing player model variant.
 *
 * <p>
 * Thread-safe implementation using synchronized methods.
 */
public class ModelVariantData implements IModelVariant {
    private java.util.Optional<mc.sayda.twilight_lib.api.model_variant.IModelVariant> variant = java.util.Optional
            .empty();

    @Override
    public synchronized java.util.Optional<mc.sayda.twilight_lib.api.model_variant.IModelVariant> getVariant() {
        return variant;
    }

    @Override
    public synchronized void setVariant(
            java.util.Optional<mc.sayda.twilight_lib.api.model_variant.IModelVariant> variant) {
        this.variant = variant;
    }

    @Override
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        getVariant().ifPresent(v -> tag.putString("modelVariant", v.getId().toString()));
        return tag;
    }

    @Override
    public synchronized void deserialize(CompoundTag tag) {
        if (tag.contains("modelVariant")) {
            try {
                String val = tag.getString("modelVariant");
                // Support both old "steve"/"alex" and new ResourceLocation format
                net.minecraft.resources.ResourceLocation rl;
                if (val.contains(":")) {
                    rl = new net.minecraft.resources.ResourceLocation(val);
                } else {
                    rl = new net.minecraft.resources.ResourceLocation("twilight_lib",
                            val.toLowerCase());
                }
                setVariant(mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance().get(rl));
            } catch (Exception e) {
                setVariant(java.util.Optional.empty());
            }
        } else {
            setVariant(java.util.Optional.empty());
        }
    }
}
