package mc.sayda.twilight_lib.api.morph;

import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.Optional;

/**
 * Registry for morph definitions.
 */
public interface IMorphRegistry {
    /**
     * Register a new morph.
     */
    void register(IMorph morph);

    /**
     * Get a morph by ID.
     */
    Optional<IMorph> get(ResourceLocation id);

    /**
     * @return All registered morphs.
     */
    Collection<IMorph> getAll();

    /**
     * @return true if the morph is registered.
     */
    boolean contains(ResourceLocation id);

    /**
     * @return The global registry instance.
     */
    static IMorphRegistry getInstance() {
        return MorphRegistryPlaceholder.INSTANCE;
    }

    class MorphRegistryPlaceholder {
        public static IMorphRegistry INSTANCE;
    }
}
