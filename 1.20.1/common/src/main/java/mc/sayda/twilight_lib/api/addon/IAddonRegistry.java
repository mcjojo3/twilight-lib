package mc.sayda.twilight_lib.api.addon;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for cosmetic addons.
 * Other mods should use this to register their custom tails, wings, and other
 * attachments.
 */
public interface IAddonRegistry {
    /**
     * Registers a new addon.
     */
    void register(IAddon addon);

    /**
     * Retrieves an addon by its unique identifier.
     */
    Optional<IAddon> get(ResourceLocation id);

    /**
     * Retrieves all registered addons.
     */
    Collection<IAddon> getAll();

    /**
     * Checks if an addon is registered with the given ID.
     */
    boolean contains(ResourceLocation id);

    /**
     * Internal singleton accessor.
     * Implementation should be provided by Twilight Lib core.
     */
    static IAddonRegistry getInstance() {
        return AddonRegistryPlaceholder.INSTANCE;
    }

    /**
     * Placeholder class to avoid circular dependencies until implementation is
     * ready.
     */
    class AddonRegistryPlaceholder {
        public static IAddonRegistry INSTANCE;
    }
}
