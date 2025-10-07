package mc.sayda.twilight_lib.supporter;

import java.util.HashSet;
import java.util.Set;

/**
 * Registry that defines which morphs/addons/trails/effects are supporter-exclusive
 * Items listed here require supporter status to be granted via /cosmetics
 * Admins can still grant these via /twilight_lib regardless
 */
public class SupporterRegistry {

    // ===== TRAILS =====
    public static final Set<String> BRONZE_TRAILS = Set.of(
            "hearts"
    );

    public static final Set<String> SILVER_TRAILS = Set.of(
            "hearts",
            "sparkles",
            "cherry_blossom"
    );

    public static final Set<String> GOLD_TRAILS = Set.of(
            "hearts",
            "sparkles",
            "cherry_blossom",
            "twilight",
            "stars"
    );

    // ===== ADDONS =====
    public static final Set<String> BRONZE_ADDONS = Set.of(
            // None yet
    );

    public static final Set<String> SILVER_ADDONS = Set.of(
            "galaxy_tail"
    );

    public static final Set<String> GOLD_ADDONS = Set.of(
            "galaxy_tail",
            "starlight_ears",
            "halo",
            "twilight_wings"
    );

    // ===== MORPHS =====
    public static final Set<String> BRONZE_MORPHS = Set.of(
            // None yet - example: "minecraft:fox"
    );

    public static final Set<String> SILVER_MORPHS = Set.of(
            // None yet
    );

    public static final Set<String> GOLD_MORPHS = Set.of(
            // None yet
    );

    // ===== EFFECTS =====
    public static final Set<String> BRONZE_EFFECTS = Set.of(
            // None yet
    );

    public static final Set<String> SILVER_EFFECTS = Set.of(
            // None yet
    );

    public static final Set<String> GOLD_EFFECTS = Set.of(
            "respawn_twilight"
    );

    /**
     * Get all supporter trails for a given tier
     */
    public static Set<String> getTrailsForTier(String tier) {
        return switch (tier.toLowerCase()) {
            case "bronze" -> BRONZE_TRAILS;
            case "silver" -> SILVER_TRAILS;
            case "gold" -> GOLD_TRAILS;
            default -> Set.of();
        };
    }

    /**
     * Get all supporter addons for a given tier
     */
    public static Set<String> getAddonsForTier(String tier) {
        return switch (tier.toLowerCase()) {
            case "bronze" -> BRONZE_ADDONS;
            case "silver" -> SILVER_ADDONS;
            case "gold" -> GOLD_ADDONS;
            default -> Set.of();
        };
    }

    /**
     * Get all supporter morphs for a given tier
     */
    public static Set<String> getMorphsForTier(String tier) {
        return switch (tier.toLowerCase()) {
            case "bronze" -> BRONZE_MORPHS;
            case "silver" -> SILVER_MORPHS;
            case "gold" -> GOLD_MORPHS;
            default -> Set.of();
        };
    }

    /**
     * Get all supporter effects for a given tier
     */
    public static Set<String> getEffectsForTier(String tier) {
        return switch (tier.toLowerCase()) {
            case "bronze" -> BRONZE_EFFECTS;
            case "silver" -> SILVER_EFFECTS;
            case "gold" -> GOLD_EFFECTS;
            default -> Set.of();
        };
    }

    /**
     * Get ALL supporter-exclusive trails (across all tiers)
     */
    public static Set<String> getAllSupporterTrails() {
        Set<String> all = new HashSet<>();
        all.addAll(BRONZE_TRAILS);
        all.addAll(SILVER_TRAILS);
        all.addAll(GOLD_TRAILS);
        return all;
    }

    /**
     * Get ALL supporter-exclusive addons (across all tiers)
     */
    public static Set<String> getAllSupporterAddons() {
        Set<String> all = new HashSet<>();
        all.addAll(BRONZE_ADDONS);
        all.addAll(SILVER_ADDONS);
        all.addAll(GOLD_ADDONS);
        return all;
    }

    /**
     * Get ALL supporter-exclusive morphs (across all tiers)
     */
    public static Set<String> getAllSupporterMorphs() {
        Set<String> all = new HashSet<>();
        all.addAll(BRONZE_MORPHS);
        all.addAll(SILVER_MORPHS);
        all.addAll(GOLD_MORPHS);
        return all;
    }

    /**
     * Get ALL supporter-exclusive effects (across all tiers)
     */
    public static Set<String> getAllSupporterEffects() {
        Set<String> all = new HashSet<>();
        all.addAll(BRONZE_EFFECTS);
        all.addAll(SILVER_EFFECTS);
        all.addAll(GOLD_EFFECTS);
        return all;
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
     * Check if a morph is supporter-exclusive
     */
    public static boolean isMorphSupporterExclusive(String morphId) {
        return getAllSupporterMorphs().contains(morphId);
    }

    /**
     * Check if an effect is supporter-exclusive
     */
    public static boolean isEffectSupporterExclusive(String effectId) {
        return getAllSupporterEffects().contains(effectId);
    }
}
