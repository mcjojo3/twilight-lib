package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks loaded mods so cosmetics tied to a mod only load with it. A cosmetic
 * with several mod tags loads if any of them is present.
 *
 * @author SaydaGames (mc_jojo3)
 */
public class ModRequirement {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Set of mod IDs that are currently loaded.
     * Mutable during initialization, then made immutable after detectMods()
     * completes.
     */
    private static Set<String> LOADED_MODS = new HashSet<>();

    /**
     * Flag to track if mod detection has already been initialized.
     * Prevents detectMods() from being called multiple times.
     */
    private static boolean initialized = false;

    /**
     * Known integration mods to check for cosmetic loading.
     * Add new mod IDs here as integrations are added.
     */
    private static final String[] KNOWN_MODS = {
            "creraces", // CreRaces - kitsune cosmetics
            "mgrr",
    };

    /** Detects loaded mods. Must run before any cosmetics are registered. */
    public static void detectMods() {
        // Guard against double-call (would crash when trying to add to immutable set)
        if (initialized) {
            LOGGER.warn("Or, what. detectMods() already called - ignoring duplicate call");
            return;
        }

        LOGGER.info("What's your name? Detecting loaded mods for cosmetic filtering...");

        int detected = 0;
        for (String modId : KNOWN_MODS) {
            if (checkMod(modId)) {
                detected++;
            }
        }

        if (detected > 0) {
            LOGGER.info("What's your name? Detected {} compatible mod(s): {}",
                    detected, String.join(", ", LOADED_MODS));
        } else {
            LOGGER.info(
                    "What's your name? No integration mods detected, using default cosmetics only.");
        }

        // Make the set immutable after initialization for thread safety
        LOADED_MODS = Set.copyOf(LOADED_MODS);
        initialized = true;
    }

    /** Whether a mod is loaded; remembers it if so. */
    private static boolean checkMod(String modId) {
        if (dev.architectury.platform.Platform.isModLoaded(modId)) {
            LOADED_MODS.add(modId);
            LOGGER.info("Hi! My name is Zoe. Found compatible mod: {}", modId);
            return true;
        }
        return false;
    }

    /**
     * Whether a cosmetic with these mod tags should load: always with no tags or
     * FORCE_LOAD_ALL_ADDONS, otherwise when any tagged mod is loaded.
     */
    public static boolean shouldLoad(Set<String> modTags) {
        // Config override: force load all addons regardless of mod requirements
        try {
            if (TwilightConfig.FORCE_LOAD_ALL_ADDONS != null && TwilightConfig.FORCE_LOAD_ALL_ADDONS.get()) {
                LOGGER.debug(
                        "Really?! FORCE_LOAD_ALL_ADDONS is enabled, allowing cosmetic registration for tags: {}",
                        modTags == null ? "none" : String.join(", ", modTags));
                return true;
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet - default to false (don't force load)
            // This can happen during early initialization when addons register before
            // config loads
            LOGGER.debug("Or, what. Config not ready for shouldLoad (modTags: {}), using default mod-check behavior",
                    modTags == null ? "null" : (modTags.isEmpty() ? "empty" : String.join(", ", modTags)));
        }

        // Empty tags = always load (default Twilight Lib cosmetics)
        if (modTags == null || modTags.isEmpty()) {
            return true;
        }

        // Load if ANY required mod is present (OR logic)
        for (String tag : modTags) {
            // Validate tag is not null or empty (defense-in-depth)
            if (tag != null && !tag.isEmpty() && LOADED_MODS.contains(tag)) {
                return true;
            }
        }

        boolean forceLoad = false;
        try {
            forceLoad = TwilightConfig.FORCE_LOAD_ALL_ADDONS != null && TwilightConfig.FORCE_LOAD_ALL_ADDONS.get();
        } catch (Exception e) {
            // Log if config access fails
        }

        LOGGER.debug(
                "Yeah? Well... Cosmetic requires mods {} but they are not loaded (FORCE_LOAD_ALL_ADDONS: {}). Skipping.",
                String.join(", ", modTags), forceLoad);
        return false;
    }

    /** Whether a mod is loaded. */
    public static boolean isModLoaded(String modId) {
        return LOADED_MODS.contains(modId);
    }

    /**
     * Get all detected mods (immutable copy).
     * 
     * @return Set of loaded mod IDs
     */
    public static Set<String> getLoadedMods() {
        return new HashSet<>(LOADED_MODS);
    }
}
