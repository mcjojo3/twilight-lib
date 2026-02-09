package mc.sayda.twilight_lib.api.model_variant;

import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.Optional;

/**
 * Registry for player model variant definitions.
 */
public interface IModelVariantRegistry {
    /**
     * Register a new model variant.
     */
    void register(IModelVariant variant);

    /**
     * Get a model variant by ID.
     */
    Optional<IModelVariant> get(ResourceLocation id);

    /**
     * @return All registered model variants.
     */
    Collection<IModelVariant> getAll();

    /**
     * @return true if the variant is registered.
     */
    boolean contains(ResourceLocation id);

    /**
     * @return The global registry instance.
     */
    static IModelVariantRegistry getInstance() {
        return ModelVariantRegistryPlaceholder.INSTANCE;
    }

    class ModelVariantRegistryPlaceholder {
        public static IModelVariantRegistry INSTANCE;
    }
}
