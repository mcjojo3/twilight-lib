package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for detecting loaded mods and checking cosmetic requirements.
 *
 * <p>
 * This system enables mod-specific cosmetics that only load when their required
 * mods are present.
 * Cosmetics can be tagged with one or more mod IDs, and will only register if
 * at least one
 * of those mods is loaded (OR logic).
 *
 * <p>
 * <b>Tagging Examples:</b>
 * <ul>
 * <li><b>Empty tags</b>: Cosmetic always loads (default Twilight Lib
 * cosmetics)</li>
 * <li><b>Single tag</b>: {@code Set.of("creraces")} - Loads only if CreRaces is
 * present</li>
 * <li><b>Multiple tags</b>: {@code Set.of("creraces", "other_mod")} - Loads if
 * EITHER mod is present</li>
 * </ul>
 *
 * <p>
 * <b>Complete Exclusion:</b> If a cosmetic's required mods aren't loaded, it's
 * completely
 * invisible to the system - not registered, doesn't appear in commands, lists,
 * or supporter tiers.
 *
 * <p>
 * <b>Thread Safety:</b> Initialization happens once during mod loading
 * (single-threaded).
 * After initialization, the loaded mods set is immutable (read-only access).
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
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

    /**
     * Initialize mod detection on startup.
     * Called from TwilightLib constructor before cosmetic registration.
     *
     * <p>
     * <b>Execution Order (Critical!):</b>
     * <ol>
     * <li>Call this method FIRST (before AddonInit, TrailType, EffectType
     * initialization)</li>
     * <li>Then register cosmetics (they'll be filtered based on detected mods)</li>
     * </ol>
     */
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
            LOGGER.info("I like all these things around me! Detected {} compatible mod(s): {}",
                    detected, String.join(", ", LOADED_MODS));
        } else {
            LOGGER.info(
                    "I like all these things around me! No integration mods detected, using default cosmetics only.");
        }

        // Make the set immutable after initialization for thread safety
        LOADED_MODS = Set.copyOf(LOADED_MODS);
        initialized = true;
    }

    /**
     * Check if a specific mod is loaded and add it to the loaded set.
     * 
     * @param modId The mod ID to check (e.g., "creraces")
     * @return true if mod is loaded
     */
    private static boolean checkMod(String modId) {
        if (ModList.get().isLoaded(modId)) {
            LOADED_MODS.add(modId);
            LOGGER.info("Hi! My name is Zoe. Found compatible mod: {}", modId);
            return true;
        }
        return false;
    }

    /**
     * Check if a cosmetic should load based on its mod tags.
     *
     * <p>
     * <b>Logic:</b>
     * <ul>
     * <li>Config override → if FORCE_LOAD_ALL_ADDONS is enabled, always load</li>
     * <li>Empty tags → always load (default behavior)</li>
     * <li>Non-empty tags → load if ANY tag matches a loaded mod (OR logic)</li>
     * </ul>
     *
     * @param modTags Set of mod IDs this cosmetic requires (empty = always load)
     * @return true if cosmetic should load
     */
    public static boolean shouldLoad(Set<String> modTags) {
        // Config override: force load all addons regardless of mod requirements
        try {
            if (TwilightConfig.FORCE_LOAD_ALL_ADDONS != null && TwilightConfig.FORCE_LOAD_ALL_ADDONS.get()) {
                LOGGER.info(
                        "I should come here every millennium! FORCE_LOAD_ALL_ADDONS is enabled, allowing cosmetic registration for tags: {}",
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

        return false;
    }

    /**
     * Check if a specific mod is loaded.
     * 
     * @param modId The mod ID to check
     * @return true if mod is loaded
     */
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