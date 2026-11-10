package mc.sayda.twilight_lib.addon;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.api.addon.IAddon;
import mc.sayda.twilight_lib.api.addon.IAddonRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registry of player addon models (tails, wings, ears and so on).
 *
 * @author SaydaGames (mc_jojo3)
 */
public class AddonRegistry implements IAddonRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AddonRegistry INSTANCE = new AddonRegistry();

    static {
        IAddonRegistry.AddonRegistryPlaceholder.INSTANCE = INSTANCE;
    }

    public static AddonRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * @deprecated Use {@link #register(IAddon)} or {@link #get(ResourceLocation)}
     */
    @Deprecated
    private static final Map<String, AddonModelInfo> ADDONS = new ConcurrentHashMap<>();
    private static final Map<String, LogicalAddon> LOGICAL_ADDONS = new ConcurrentHashMap<>();

    private final Map<ResourceLocation, IAddon> NEW_ADDONS = new ConcurrentHashMap<>();

    // Registration-time limits, hardcoded to avoid config-timing issues
    // (NeoForge config values aren't available during mod construction)
    private static final int MAX_ID_LENGTH = 128;
    private static final int MAX_REGISTRY_SIZE = 1000;

    public static void registerLogical(String id, Set<String> modTags, Set<BodyPart> hiddenBodyParts) {
        if (id == null || id.trim().isEmpty())
            return;
        if (id.length() > MAX_ID_LENGTH)
            return;
        LOGICAL_ADDONS.put(id, new LogicalAddon(id, modTags, hiddenBodyParts));
    }

    /** Registers an addon, optionally textured with the player's skin. */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, false, false,
                false, Set.of());
    }

    /** Registers an addon with its own texture. */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, false, false, false, false,
                Set.of());
    }

    /** Registers an addon, optionally skin-textured and translucent. */
    public static void registerAddon(String id, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinitionSupplier,
            Function<ModelPart, ?> modelFactory,
            ResourceLocation texture,
            boolean usePlayerSkin,
            boolean translucent) {
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, translucent,
                false, false, Set.of());
    }

    /** Registers an addon with the common render options. */
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

    /** Registers an addon with every render option but no required mods. */
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

    /** Registers an addon with render options and required mods. */
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

    /** Registers an addon without a tint mask. */
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
        registerAddon(id, layerLocation, layerDefinitionSupplier, modelFactory, texture, usePlayerSkin, translucent,
                hidePlayerModel, forceAllTranslucent, modTags, hiddenBodyParts, null);
    }

    /**
     * Registers an addon with every option.
     *
     * @param texture             addon texture, ignored when usePlayerSkin is set
     * @param usePlayerSkin       draw it with the player's skin instead
     * @param translucent         draw it at 50% opacity
     * @param hidePlayerModel     hide the player model (name tag and shadow stay)
     * @param forceAllTranslucent draw every other active addon at 50% opacity
     * @param modTags             mods it needs, any one is enough (empty = always loads)
     * @param hiddenBodyParts     player parts hidden while it's worn
     * @param maskTexture         tint mask (alpha = tint strength), or null for a flat tint
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
            Set<BodyPart> hiddenBodyParts,
            ResourceLocation maskTexture) {
        // Validate addon ID parameter (Issue #29: prevent null/empty/oversized IDs)
        if (id == null || id.trim().isEmpty()) {
            LOGGER.error("Or, what. Cannot register addon with null or empty ID");
            return;
        }

        if (id.length() > MAX_ID_LENGTH) {
            LOGGER.error("Or, what. Addon ID too long: {} chars (max {})", id.length(), MAX_ID_LENGTH);
            return;
        }

        // Check registry size limit (Issue #30: prevent memory exhaustion)
        if (ADDONS.size() >= MAX_REGISTRY_SIZE && !ADDONS.containsKey(id)) {
            LOGGER.error("Really?! Addon registry full ({} addons). Cannot register '{}'", ADDONS.size(), id);
            return;
        }

        // Merge-on-Register: Pull metadata from LOGICAL_ADDONS if available
        LogicalAddon logical = LOGICAL_ADDONS.get(id);
        Set<String> effectiveModTags = modTags;
        Set<BodyPart> effectiveHiddenParts = hiddenBodyParts;

        if (logical != null) {
            if (effectiveModTags.isEmpty())
                effectiveModTags = logical.modTags();
            if (effectiveHiddenParts.isEmpty())
                effectiveHiddenParts = logical.hiddenBodyParts();
        } else {
            registerLogical(id, modTags, hiddenBodyParts);
        }

        if (ADDONS.containsKey(id)) {
            LOGGER.warn("Really?! Addon '{}' already registered, overwriting...", id);
        }

        ADDONS.put(id, new AddonModelInfo(id, layerLocation, layerDefinitionSupplier, modelFactory, texture,
                usePlayerSkin, translucent, hidePlayerModel, forceAllTranslucent, effectiveModTags,
                effectiveHiddenParts, Optional.ofNullable(maskTexture)));

        String modInfo = effectiveModTags.isEmpty() ? "all mods" : String.join(", ", effectiveModTags);
        String partsInfo = effectiveHiddenParts.isEmpty() ? "none"
                : effectiveHiddenParts.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("none");
        LOGGER.debug("What's your name? Registered addon: {} (mods: {}, hidden parts: {})", id, modInfo, partsInfo);
    }

    @Override
    public void register(IAddon addon) {
        if (addon == null || addon.getId() == null) {
            LOGGER.error("Cannot register null addon or addon with null ID");
            return;
        }

        if (NEW_ADDONS.containsKey(addon.getId())) {
            LOGGER.warn("Addon '{}' already registered, overwriting...", addon.getId());
        }

        NEW_ADDONS.put(addon.getId(), addon);

        // Maintain logic registry
        registerLogical(addon.getId().toString(), addon.getRequiredModIds(), addon.getHiddenBodyParts());
        // Fallback for short paths
        if (addon.getId().getNamespace().equals("twilight_lib")) {
            registerLogical(addon.getId().getPath(), addon.getRequiredModIds(), addon.getHiddenBodyParts());
        }

        // Maintain legacy compatibility
        // We wrap IAddon into AddonModelInfo if possible, but some IAddon
        // implementations
        // might not fit the old record perfectly. We'll do our best.
        // For now, we mainly want new code to use the new API.
    }

    @Override
    public Optional<IAddon> get(ResourceLocation id) {
        // Check new system first
        if (NEW_ADDONS.containsKey(id)) {
            return Optional.of(NEW_ADDONS.get(id));
        }

        // Check legacy system (filtered)
        // 1. Try exact string match (e.g. "mod:addon")
        if (ADDONS.containsKey(id.toString())) {
            AddonModelInfo info = ADDONS.get(id.toString());
            if (mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags())) {
                return Optional.of(info);
            }
        }

        // 3. Try fallback to twilight_lib if namespace is minecraft, null, or
        // twilight_lib
        if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("twilight_lib")) {
            ResourceLocation twilightId = ResourceLocation.fromNamespaceAndPath("twilight_lib", id.getPath());
            if (NEW_ADDONS.containsKey(twilightId)) {
                return Optional.of(NEW_ADDONS.get(twilightId));
            }
            if (ADDONS.containsKey(twilightId.toString())) {
                AddonModelInfo info = ADDONS.get(twilightId.toString());
                if (mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags())) {
                    return Optional.of(info);
                }
            }
            if (ADDONS.containsKey(id.getPath())) {
                AddonModelInfo info = ADDONS.get(id.getPath());
                if (mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags())) {
                    return Optional.of(info);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<IAddon> getAll() {
        Set<IAddon> allAddons = new HashSet<>(NEW_ADDONS.values());
        // AddonModelInfo implements IAddon now, so we can add them directly (filtered)
        ADDONS.values().stream()
                .filter(info -> mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags()))
                .forEach(allAddons::add);
        return new ArrayList<>(allAddons);
    }

    @Override
    public boolean contains(ResourceLocation id) {
        if (NEW_ADDONS.containsKey(id))
            return true;
        if (ADDONS.containsKey(id.toString()))
            return true;
        if (LOGICAL_ADDONS.containsKey(id.toString()))
            return true;
        if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("twilight_lib")) {
            String path = id.getPath();
            if (ADDONS.containsKey(path) || LOGICAL_ADDONS.containsKey(path))
                return true;
            ResourceLocation twilightId = ResourceLocation.fromNamespaceAndPath("twilight_lib", path);
            if (NEW_ADDONS.containsKey(twilightId))
                return true;
        }
        return false;
    }

    /**
     * Get addon info by ID (Legacy String-based) [Filtered]
     */
    public static Optional<AddonModelInfo> getAddon(String id) {
        // 1. Try exact match
        AddonModelInfo info = ADDONS.get(id);
        if (info != null && mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags())) {
            return Optional.of(info);
        }

        // 2. Try ResourceLocation based fallback
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            // Try path-only fallback for minecraft/twilight_lib namespace
            if (rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("twilight_lib")) {
                info = ADDONS.get(rl.getPath());
                if (info != null && mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags())) {
                    return Optional.of(info);
                }
            }
        }

        return Optional.empty();
    }

    /** Addon ids whose required mods are loaded, for autocomplete and player-facing lists. */
    public static List<String> getAllAddonIds() {
        Set<String> ids = new HashSet<>();

        // Add from legacy system (filtered)
        ADDONS.entrySet().stream()
                .filter(entry -> mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(entry.getValue().modTags()))
                .map(java.util.Map.Entry::getKey)
                .forEach(ids::add);

        // Add from logical system (filtered)
        LOGICAL_ADDONS.entrySet().stream()
                .filter(entry -> mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(entry.getValue().modTags()))
                .map(java.util.Map.Entry::getKey)
                .forEach(ids::add);

        // Add from new system (filtered)
        INSTANCE.NEW_ADDONS.entrySet().stream()
                .filter(entry -> mc.sayda.twilight_lib.cosmetics.ModRequirement
                        .shouldLoad(entry.getValue().getRequiredModIds()))
                .map(entry -> entry.getKey().toString())
                .forEach(ids::add);

        // Also add short paths for twilight_lib addons in new system
        INSTANCE.NEW_ADDONS.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals("twilight_lib"))
                .filter(entry -> mc.sayda.twilight_lib.cosmetics.ModRequirement
                        .shouldLoad(entry.getValue().getRequiredModIds()))
                .map(entry -> entry.getKey().getPath())
                .forEach(ids::add);

        return ids.stream().sorted().toList();
    }

    /** Every registered addon id, even ones whose required mods aren't loaded. */
    public static Set<String> getAllAddonIdsUnfiltered() {
        Set<String> ids = new HashSet<>(ADDONS.keySet());
        ids.addAll(LOGICAL_ADDONS.keySet());
        INSTANCE.NEW_ADDONS.keySet().forEach(rl -> {
            ids.add(rl.toString());
            if (rl.getNamespace().equals("twilight_lib")) {
                ids.add(rl.getPath());
            }
        });
        return ids;
    }

    /**
     * Get all registered addon infos (Legacy) [Filtered]
     */
    public static Collection<AddonModelInfo> getAllAddons() {
        return ADDONS.values().stream()
                .filter(info -> mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get ALL registered addon infos (Unfiltered - Use for initial Registration)
     */
    public static Collection<AddonModelInfo> getAllRegisteredAddons() {
        return new ArrayList<>(ADDONS.values());
    }

    /**
     * Check if an addon is registered AND its mod requirements are met.
     */
    public static boolean hasAddon(String id) {
        // 1. Try exact match in all registries
        LogicalAddon logical = LOGICAL_ADDONS.get(id);
        if (logical != null) {
            return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(logical.modTags());
        }

        AddonModelInfo info = ADDONS.get(id);
        if (info != null) {
            return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(info.modTags());
        }

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            IAddon addon = INSTANCE.NEW_ADDONS.get(rl);
            if (addon != null) {
                return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(addon.getRequiredModIds());
            }

            // 2. Try namespace fallbacks
            if (rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("twilight_lib")) {
                String path = rl.getPath();

                // Try legacy/logical path match
                LogicalAddon logicalPath = LOGICAL_ADDONS.get(path);
                if (logicalPath != null) {
                    return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(logicalPath.modTags());
                }
                AddonModelInfo infoPath = ADDONS.get(path);
                if (infoPath != null) {
                    return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(infoPath.modTags());
                }

                // Try new system twilight_lib override
                IAddon twilightAddon = INSTANCE.NEW_ADDONS
                        .get(ResourceLocation.fromNamespaceAndPath("twilight_lib", path));
                if (twilightAddon != null) {
                    return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(twilightAddon.getRequiredModIds());
                }
            }
        }

        // 3. Final fallback: treat as path-only for twilight_lib
        LogicalAddon logicalPath = LOGICAL_ADDONS.get(id);
        if (logicalPath != null) {
            return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(logicalPath.modTags());
        }
        AddonModelInfo infoPath = ADDONS.get(id);
        if (infoPath != null) {
            return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(infoPath.modTags());
        }
        IAddon pathOnly = INSTANCE.NEW_ADDONS.get(ResourceLocation.fromNamespaceAndPath("twilight_lib", id));
        if (pathOnly != null) {
            return mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(pathOnly.getRequiredModIds());
        }

        return false;
    }

    /**
     * Check if an addon is registered regardless of mod requirements.
     */
    public static boolean exists(String id) {
        // 1. Exact matches
        if (LOGICAL_ADDONS.containsKey(id) || ADDONS.containsKey(id)) {
            return true;
        }

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            if (INSTANCE.NEW_ADDONS.containsKey(rl))
                return true;

            // 2. Namespace fallbacks
            if (rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("twilight_lib")) {
                String path = rl.getPath();
                if (LOGICAL_ADDONS.containsKey(path) || ADDONS.containsKey(path))
                    return true;
                if (INSTANCE.NEW_ADDONS.containsKey(ResourceLocation.fromNamespaceAndPath("twilight_lib", path)))
                    return true;
            }
        }

        // 3. Final fallback
        if (LOGICAL_ADDONS.containsKey(id) || ADDONS.containsKey(id))
            return true;
        return INSTANCE.NEW_ADDONS.containsKey(ResourceLocation.fromNamespaceAndPath("twilight_lib", id));
    }

    /**
     * Clear all registered addons (for reloading)
     */
    public static void clear() {
        ADDONS.clear();
        LOGICAL_ADDONS.clear();
        INSTANCE.NEW_ADDONS.clear();
        LOGGER.debug("Time to change! Cleared all addon registrations");
    }
}