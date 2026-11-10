package mc.sayda.twilight_lib.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;

/** Player model variant override (classic or slim arms). */
public interface IModelVariant extends ISerializableData {
    /**
     * Get the active model variant.
     * 
     * @return Optional containing the active variant, or empty if using default
     */
    java.util.Optional<mc.sayda.twilight_lib.api.model_variant.IModelVariant> getVariant();

    /** Sets the model variant, or clears it with an empty Optional. */
    void setVariant(java.util.Optional<mc.sayda.twilight_lib.api.model_variant.IModelVariant> variant);

    /**
     * Get the current model variant name.
     * 
     * @return "steve" or "alex" (lowercase)
     */
    default String getModelVariant() {
        return getVariant().map(v -> v.getId().getPath()).orElse("none");
    }

    /**
     * Set the model variant by name.
     * 
     * @param variant "steve" or "alex" (case-insensitive)
     */
    default void setModelVariant(String variant) {
        if (variant == null || variant.equalsIgnoreCase("none")) {
            clearCustomVariant();
            return;
        }
        setVariant(mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance().get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("twilight_lib", variant.toLowerCase())));
    }

    /**
     * Check if a custom model variant is set (overriding the default).
     * 
     * @return true if model variant is explicitly set, false if using default
     */
    default boolean hasCustomVariant() {
        return getVariant().isPresent();
    }

    /**
     * Clear custom model variant, reverting to default (based on skin).
     */
    default void clearCustomVariant() {
        setVariant(java.util.Optional.empty());
    }

    /**
     * Serialize to NBT for persistent storage.
     */
    @Nonnull
    CompoundTag serialize();

    /**
     * Deserialize from NBT.
     */
    void deserialize(@Nonnull CompoundTag tag);
}