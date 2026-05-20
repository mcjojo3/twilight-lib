package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nonnull;

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
    @Nonnull
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        getVariant().ifPresent(v -> tag.putString("modelVariant", v.getId().toString()));
        return tag;
    }

    @Override
    public synchronized void deserialize(@Nonnull CompoundTag tag) {
        if (tag.contains("modelVariant", Tag.TAG_STRING)) {
            try {
                String val = tag.getString("modelVariant");
                // Support both old "steve"/"alex" and new ResourceLocation format
                net.minecraft.resources.ResourceLocation rl;
                if (val.contains(":")) {
                    rl = net.minecraft.resources.ResourceLocation.parse(val);
                } else {
                    rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("twilight_lib",
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