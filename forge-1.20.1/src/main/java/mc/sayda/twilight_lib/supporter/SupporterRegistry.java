package mc.sayda.twilight_lib.supporter;

import java.util.HashSet;
import java.util.Set;

/**
 * Registry that defines which cosmetics are supporter-exclusive per Patreon tier.
 *
 * <p><b>Supporter Tier System</b>: Cosmetics are unlocked based on Patreon support tier:
 * <ul>
 *   <li><b>Stone</b>: Reserved for future basic tier (currently no cosmetics)</li>
 *   <li><b>Bronze</b>: First paid tier (hearts trail)</li>
 *   <li><b>Silver</b>: Mid tier (ash, sparkles, cherry_blossom trails)</li>
 *   <li><b>Gold</b>: High tier (white_ash, twilight, stars trails + ethereal/ambient effects)</li>
 *   <li><b>Platinum</b>: Top tier (tiara addon + all spawn effects)</li>
 * </ul>
 *
 * <p><b>Tier Inheritance</b>: Higher tiers automatically include all lower tier cosmetics.
 * Each tier constant (e.g., {@code GOLD_TRAILS}) defines ONLY the NEW cosmetics for that tier.
 * The getter methods (e.g., {@link #getTrailsForTier(String)}) return cumulative sets.
 *
 * <p><b>Example</b>:
 * <pre>
 * BRONZE_TRAILS = ["hearts"]
 * SILVER_TRAILS = ["ash", "sparkles", "cherry_blossom"]
 *
 * getTrailsForTier("bronze") returns ["hearts"]
 * getTrailsForTier("silver") returns ["hearts", "ash", "sparkles", "cherry_blossom"]
 * </pre>
 *
 * <p><b>Performance Optimization</b>: Tier results are pre-computed at class load time
 * and cached in immutable maps. This avoids repeated set construction during player login.
 *
 * <p><b>Access Control</b>:
 * <ul>
 *   <li>Players with supporter tier: Cosmetics auto-granted on login via {@link mc.sayda.twilight_lib.TwilightLib#onPlayerLogin}</li>
 *   <li>Admins: Can grant any cosmetic via {@code /twilightlib} commands (bypasses supporter check)</li>
 *   <li>Manual grants: Persist independently of tier (for gifts/special events)</li>
 * </ul>
 *
 * <p><b>Important</b>: Morphs are NOT part of the supporter system. Morphs are admin-only
 * via {@code /twilightlib morph} and cannot be unlocked through Patreon support.
 *
 * @see mc.sayda.twilight_lib.supporter.SupporterData for supporter tier data structure
 * @see mc.sayda.twilight_lib.supporter.SupporterService for supporter data fetching
 * @author Sayda (MrJojo)
 * @version 1.0
 */
public class SupporterRegistry {

    // ===== TRAILS =====
    // Each tier defines only NEW trails introduced at that level
    public static final Set<String> STONE_TRAILS = Set.of(
            // None yet
    );

    public static final Set<String> BRONZE_TRAILS = Set.of(
            "hearts"
    );

    public static final Set<String> SILVER_TRAILS = Set.of(
            "ash",
            "sparkles",
            "cherry_blossom"
    );

    public static final Set<String> GOLD_TRAILS = Set.of(
            "white_ash",
            "twilight",
            "stars"
    );

    public static final Set<String> PLATINUM_TRAILS = Set.of(
            // Platinum gets all previous trails, no new exclusives yet
    );

    // ===== ADDONS =====
    // Each tier defines only NEW addons introduced at that level
    public static final Set<String> STONE_ADDONS = Set.of(
            // None yet
    );

    public static final Set<String> BRONZE_ADDONS = Set.of(
            // None yet
    );

    public static final Set<String> SILVER_ADDONS = Set.of(
            // None yet
    );

    public static final Set<String> GOLD_ADDONS = Set.of(
            // None yet
    );

    public static final Set<String> PLATINUM_ADDONS = Set.of(
            "tiara"
    );

    // ===== EFFECTS =====
    // Each tier defines only NEW effects introduced at that level
    public static final Set<String> STONE_EFFECTS = Set.of(
            // None yet
    );

    public static final Set<String> BRONZE_EFFECTS = Set.of(
            // None yet
    );

    public static final Set<String> SILVER_EFFECTS = Set.of(
            // None yet
    );

    public static final Set<String> GOLD_EFFECTS = Set.of(
            "spawn_ethereal",
            "ambient_flame",
            "ambient_frost"
    );

    public static final Set<String> PLATINUM_EFFECTS = Set.of(
            "spawn_rainbow",
            "spawn_portal",
            "spawn_frost",
            "spawn_flame",
            "spawn_nature"
    );

    // ===== CACHED TIER RESULTS =====
    // Pre-computed to avoid repeated HashSet creation and addAll operations
    private static final java.util.Map<String, Set<String>> TRAILS_CACHE = computeTrailsCache();
    private static final java.util.Map<String, Set<String>> ADDONS_CACHE = computeAddonsCache();
    private static final java.util.Map<String, Set<String>> EFFECTS_CACHE = computeEffectsCache();

    private static java.util.Map<String, Set<String>> computeTrailsCache() {
        Set<String> stone = Set.copyOf(STONE_TRAILS);

        Set<String> bronze = new HashSet<>(STONE_TRAILS);
        bronze.addAll(BRONZE_TRAILS);

        Set<String> silver = new HashSet<>(bronze);
        silver.addAll(SILVER_TRAILS);

        Set<String> gold = new HashSet<>(silver);
        gold.addAll(GOLD_TRAILS);

        Set<String> platinum = new HashSet<>(gold);
        platinum.addAll(PLATINUM_TRAILS);

        return java.util.Map.of(
            "stone", Set.copyOf(stone),
            "bronze", Set.copyOf(bronze),
            "silver", Set.copyOf(silver),
            "gold", Set.copyOf(gold),
            "platinum", Set.copyOf(platinum)
        );
    }

    private static java.util.Map<String, Set<String>> computeAddonsCache() {
        Set<String> stone = Set.copyOf(STONE_ADDONS);

        Set<String> bronze = new HashSet<>(STONE_ADDONS);
        bronze.addAll(BRONZE_ADDONS);

        Set<String> silver = new HashSet<>(bronze);
        silver.addAll(SILVER_ADDONS);

        Set<String> gold = new HashSet<>(silver);
        gold.addAll(GOLD_ADDONS);

        Set<String> platinum = new HashSet<>(gold);
        platinum.addAll(PLATINUM_ADDONS);

        return java.util.Map.of(
            "stone", Set.copyOf(stone),
            "bronze", Set.copyOf(bronze),
            "silver", Set.copyOf(silver),
            "gold", Set.copyOf(gold),
            "platinum", Set.copyOf(platinum)
        );
    }

    private static java.util.Map<String, Set<String>> computeEffectsCache() {
        Set<String> stone = Set.copyOf(STONE_EFFECTS);

        Set<String> bronze = new HashSet<>(STONE_EFFECTS);
        bronze.addAll(BRONZE_EFFECTS);

        Set<String> silver = new HashSet<>(bronze);
        silver.addAll(SILVER_EFFECTS);

        Set<String> gold = new HashSet<>(silver);
        gold.addAll(GOLD_EFFECTS);

        Set<String> platinum = new HashSet<>(gold);
        platinum.addAll(PLATINUM_EFFECTS);

        return java.util.Map.of(
            "stone", Set.copyOf(stone),
            "bronze", Set.copyOf(bronze),
            "silver", Set.copyOf(silver),
            "gold", Set.copyOf(gold),
            "platinum", Set.copyOf(platinum)
        );
    }

    /**
     * Get all supporter trails for a given tier.
     * Automatically includes all trails from lower tiers (inheritance).
     */
    public static Set<String> getTrailsForTier(String tier) {
        if (tier == null) {
            return Set.of();
        }
        return TRAILS_CACHE.getOrDefault(tier.toLowerCase(), Set.of());
    }

    /**
     * Get all supporter addons for a given tier.
     * Automatically includes all addons from lower tiers (inheritance).
     */
    public static Set<String> getAddonsForTier(String tier) {
        if (tier == null) {
            return Set.of();
        }
        return ADDONS_CACHE.getOrDefault(tier.toLowerCase(), Set.of());
    }

    /**
     * Get all supporter effects for a given tier.
     * Automatically includes all effects from lower tiers (inheritance).
     */
    public static Set<String> getEffectsForTier(String tier) {
        if (tier == null) {
            return Set.of();
        }
        return EFFECTS_CACHE.getOrDefault(tier.toLowerCase(), Set.of());
    }

    // Cached "ALL" results for performance
    private static final Set<String> ALL_TRAILS = TRAILS_CACHE.get("platinum");
    private static final Set<String> ALL_ADDONS = ADDONS_CACHE.get("platinum");
    private static final Set<String> ALL_EFFECTS = EFFECTS_CACHE.get("platinum");

    /**
     * Get ALL supporter-exclusive trails (across all tiers)
     */
    public static Set<String> getAllSupporterTrails() {
        return ALL_TRAILS;
    }

    /**
     * Get ALL supporter-exclusive addons (across all tiers)
     */
    public static Set<String> getAllSupporterAddons() {
        return ALL_ADDONS;
    }

    /**
     * Get ALL supporter-exclusive effects (across all tiers)
     */
    public static Set<String> getAllSupporterEffects() {
        return ALL_EFFECTS;
    }

    /**
     * Check if a trail is supporter-exclusive
     */
    public static boolean isTrailSupporterExclusive(String trailId) {
        return getAllSupporterTrails().contains(trailId);
    }

    /**
     * Check if an addon is supporter-exclusive
     */
    public static boolean isAddonSupporterExclusive(String addonId) {
        return getAllSupporterAddons().contains(addonId);
    }

    /**
     * Check if an effect is supporter-exclusive
     */
    public static boolean isEffectSupporterExclusive(String effectId) {
        return getAllSupporterEffects().contains(effectId);
    }
}
