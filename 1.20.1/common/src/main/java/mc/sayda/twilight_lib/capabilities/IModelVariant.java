package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * Capability interface for storing player model variant (Steve/Alex).
 *
 * <p>
 * Minecraft has two built-in player model types:
 * <ul>
 * <li><b>Steve (classic)</b>: 4px wide arms</li>
 * <li><b>Alex (slim)</b>: 3px wide arms</li>
 * </ul>
 *
 * <p>
 * By default, the model type is determined by the player's UUID/skin,
 * but this capability allows cosmetic overriding for supporters.
 */
public interface IModelVariant extends ISerializableData {
    /**
     * Get the active model variant.
     * 
     * @return Optional containing the active variant, or empty if using default
     */
    java.util.Optional<mc.sayda.twilight_lib.api.model_variant.IModelVariant> getVariant();

    /**
     * Set the active model variant.
     * 
     * @param variant Optional containing the new variant, or empty to revert to
     *                default
     */
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
                new net.minecraft.resources.ResourceLocation("twilight_lib", variant.toLowerCase())));
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
    CompoundTag serialize();

    /**
     * Deserialize from NBT.
     */
    void deserialize(CompoundTag tag);
}
