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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Central registry for all player addon models - 3D attachments rendered on
 * players.
 *
 * <p>
 * <b>What are addons?</b> Addons are cosmetic 3D models attached to players:
 * <ul>
 * <li>Tails that sway with player movement</li>
 * <li>Wings that animate during flight</li>
 * <li>Horns, ears, and other decorative attachments</li>
 * </ul>
 *
 * <p>
 * <b>How to register an addon</b>:
 * 
 * <pre>{@code
 * // During client setup
 * AddonRegistry.registerAddon(
 *         "my_tail", // Unique ID
 *         new ModelLayerLocation( // Model layer
 *                 new ResourceLocation("mymod", "tail"),
 *                 "main"),
 *         MyTailModel::createBodyLayer, // Layer definition supplier
 *         MyTailModel::new, // Model factory
 *         new ResourceLocation("mymod", "textures/tail.png"), // Texture
 *         false // Don't use player skin
 * );
 * }</pre>
 *
 * <p>
 * <b>Advanced Options</b>:
 * <ul>
 * <li><b>usePlayerSkin</b>: Use player's skin texture instead of custom
 * texture</li>
 * <li><b>translucent</b>: Render addon with 50% transparency</li>
 * <li><b>hidePlayerModel</b>: Hide base player model (for full-body
 * replacements)</li>
 * <li><b>forceAllTranslucent</b>: Make ALL active addons translucent (for ghost
 * effects)</li>
 * </ul>
 *
 * <p>
 * <b>Thread Safety</b>: Uses {@link ConcurrentHashMap} for safe concurrent
 * registration
 * during mod loading. Multiple mods can register addons simultaneously.
 *
 * <p>
 * <b>Performance</b>: Registry is built once during startup. Lookups during
 * rendering
 * are O(1) via hash map.
 *
 * @see AddonModelInfo for detailed addon configuration options
 * @see IAddonModel for addon model interface requirements
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
 */
public class AddonRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Thread-safe map of addon ID to model info.
     *
     * <p>
     * <b>Thread Safety</b>: ConcurrentHashMap allows safe concurrent registration
     * from multiple mod loading threads.
     *
     * <p>
     * <b>Immutability</b>: AddonModelInfo objects are immutable after creation
     * to prevent race conditions during rendering.
     */
    private static final Map<String, AddonModelInfo> ADDONS = new ConcurrentHashMap<>();

    /**
     * Register a new addon model
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon (ignored if
     *                                usePlayerSkin is true)
     * @param usePlayerSkin           If true, the addon will use the player's skin
     *                                texture instead of the provided texture
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, false, false,
                false, Set.of());
    }

    /**
     * Register a new addon model with a custom texture (backwards compatible)
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, false, false, false, false,
                Set.of());
    }

    /**
     * Register a new addon model with translucency
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon (ignored if
     *                                usePlayerSkin is true)
     * @param usePlayerSkin           If true, the addon will use the player's skin
     *                                texture instead of the provided texture
     * @param translucent             If true, the addon will render with 50%
     *                                transparency
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin,
            boolean translucent) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, translucent,
                false, false, Set.of());
    }

    /**
     * Register a new addon model with standard options
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon (ignored if
     *                                usePlayerSkin is true)
     * @param usePlayerSkin           If true, the addon will use the player's skin
     *                                texture instead of the provided texture
     * @param translucent             If true, the addon will render with 50%
     *                                transparency
     * @param hidePlayerModel         If true, the base player model will be hidden
     *                                (nametag and shadow remain visible)
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin,
            boolean translucent,
            boolean hidePlayerModel) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, translucent,
                hidePlayerModel, false, Set.of());
    }

    /**
     * Register a new addon model with all options (without mod tags - backwards
     * compatible)
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon (ignored if
     *                                usePlayerSkin is true)
     * @param usePlayerSkin           If true, the addon will use the player's skin
     *                                texture instead of the provided texture
     * @param translucent             If true, THIS addon will render with 50%
     *                                transparency
     * @param hidePlayerModel         If true, the base player model will be hidden
     *                                (nametag and shadow remain visible)
     * @param forceAllTranslucent     If true, ALL active addons will render with
     *                                50% transparency
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin,
            boolean translucent,
            boolean hidePlayerModel,
            boolean forceAllTranslucent) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, translucent,
                hidePlayerModel, forceAllTranslucent, Set.of());
    }

    /**
     * Register a new addon model with all options including mod tags (with mod
     * tags, no hidden parts - backward compat)
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon (ignored if
     *                                usePlayerSkin is true)
     * @param usePlayerSkin           If true, the addon will use the player's skin
     *                                texture instead of the provided texture
     * @param translucent             If true, THIS addon will render with 50%
     *                                transparency
     * @param hidePlayerModel         If true, the base player model will be hidden
     *                                (nametag and shadow remain visible)
     * @param forceAllTranslucent     If true, ALL active addons will render with
     *                                50% transparency
     * @param modTags                 Set of mod IDs required for this addon (empty
     *                                = always load, OR logic for multiple)
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin,
            boolean translucent,
            boolean hidePlayerModel,
            boolean forceAllTranslucent,
            Set<String> modTags) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, translucent,
                hidePlayerModel, forceAllTranslucent, modTags, Set.of());
    }

    /**
     * Register a new addon model with all options including mod tags and hidden
     * body parts (MAIN METHOD)
     * 
     * @param id                      Unique identifier for the addon (e.g.,
     *                                "horns", "wings")
     * @param layerLocation           Model layer location
     * @param layerDefinitionSupplier Supplier for the layer definition
     * @param modelFactory            Function to create model instance from
     *                                ModelPart (should return EntityModel &
     *                                IAddonModel)
     * @param texture                 Texture location for the addon (ignored if
     *                                usePlayerSkin is true)
     * @param usePlayerSkin           If true, the addon will use the player's skin
     *                                texture instead of the provided texture
     * @param translucent             If true, THIS addon will render with 50%
     *                                transparency
     * @param hidePlayerModel         If true, the base player model will be hidden
     *                                (nametag and shadow remain visible)
     * @param forceAllTranslucent     If true, ALL active addons will render with
     *                                50% transparency
     * @param modTags                 Set of mod IDs required for this addon (empty
     *                                = always load, OR logic for multiple)
     * @param hiddenBodyParts         Set of specific body parts to hide when this
     *                                addon is equipped (e.g.,
     *                                Set.of(BodyPart.HEAD))
     */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin,
            boolean translucent,
            boolean hidePlayerModel,
            boolean forceAllTranslucent,
            Set<String> modTags,
            Set<BodyPart> hiddenBodyParts) {
        // Validate addon ID parameter (Issue #29: prevent null/empty/oversized IDs)
        if (id == null || id.trim().isEmpty()) {
            LOGGER.error("Or, what. Cannot register addon with null or empty ID");
            return;
        }
        if (id.length() > 128) {
            LOGGER.error("Or, what. Addon ID too long: {} chars (max 128)", id.length());
            return;
        }

        // Check registry size limit (Issue #30: prevent memory exhaustion)
        if (ADDONS.size() >= 1000 && !ADDONS.containsKey(id)) {
            LOGGER.error("Really?! Addon registry full ({} addons). Cannot register '{}'", ADDONS.size(), id);
            return;
        }

        // All addons are registered - filtering happens during rendering and in
        // commands
        // This ensures the config option FORCE_LOAD_ALL_ADDONS is respected correctly

        if (ADDONS.containsKey(id)) {
            LOGGER.warn(
                    "If you won't pay attention to me, I'll blow up your world! Probably not... but I might! Addon '{}' already registered, overwriting...",
                    id);
        }

        ADDONS.put(id, new AddonModelInfo(id, layerLocation, layerDefinitionSupplier, modelFactory, texture,
                usePlayerSkin, translucent, hidePlayerModel, forceAllTranslucent, modTags, hiddenBodyParts));

        String modInfo = modTags.isEmpty() ? "all mods" : String.join(", ", modTags);
        String partsInfo = hiddenBodyParts.isEmpty() ? "none"
                : hiddenBodyParts.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("none");
        LOGGER.debug("What's your name? Registered addon: {} (mods: {}, hidden parts: {})", id, modInfo, partsInfo);
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