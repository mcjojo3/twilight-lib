package mc.sayda.twilight_lib.addon;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.client.model.IAddonModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Central registry for all player addon models.
 * Addons can be registered programmatically via registerAddon()
 * Thread-safe for concurrent mod loading.
 */
public class AddonRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, AddonModelInfo> ADDONS = new ConcurrentHashMap<>();

    /**
     * Register a new addon model
     * @param id Unique identifier for the addon (e.g., "horns", "wings")
     * @param layerLocation Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory Function to create model instance from ModelPart (should return EntityModel & IAddonModel)
     * @param texture Texture location for the addon
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
                                    Supplier<LayerDefinition> layerDefinitionSupplier,
                                    Function<ModelPart, ?> modelFactory,
                                    ResourceLocation texture) {
        if (ADDONS.containsKey(id)) {
            LOGGER.warn("If you won't pay attention to me, I'll blow up your world! Probably not... but I might! Addon '{}' already registered, overwriting...", id);
        }

        ADDONS.put(id, new AddonModelInfo(id, layerLocation, layerDefinitionSupplier, modelFactory, texture));
        LOGGER.debug("What's your name? Registered addon: {}", id);
    }

    /**
     * Get addon info by ID
     */
    public static Optional<AddonModelInfo> getAddon(String id) {
        return Optional.ofNullable(ADDONS.get(id));
    }

    /**
     * Get all registered addon IDs
     */
    public static Set<String> getAllAddonIds() {
        return new HashSet<>(ADDONS.keySet());
    }

    /**
     * Get all registered addon infos
     */
    public static Collection<AddonModelInfo> getAllAddons() {
        return new ArrayList<>(ADDONS.values());
    }

    /**
     * Check if an addon exists
     */
    public static boolean hasAddon(String id) {
        return ADDONS.containsKey(id);
    }

    /**
     * Clear all registered addons (for reloading)
     */
    public static void clear() {
        ADDONS.clear();
        LOGGER.debug("Well, this is a pretty chill reality. Cleared all addon registrations");
    }
}