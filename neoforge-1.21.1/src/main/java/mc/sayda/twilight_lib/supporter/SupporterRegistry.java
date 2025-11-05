package mc.sayda.twilight_lib.supporter;

import java.util.HashSet;
import java.util.Set;

/**
 * Registry that defines which addons/trails/effects are supporter-exclusive.
 * Items listed here require supporter status to be granted via /cosmetics.
 * Admins can still grant these via /twilightlib regardless.
 *
 * Each tier defines only the NEW cosmetics introduced at that tier.
 * Higher tiers automatically inherit all cosmetics from lower tiers via the getter methods.
 *
 * NOTE: Morphs are NOT part of the supporter system and are admin-only via /twilightlib morph
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

    /**
     * Get all supporter trails for a given tier.
     * Automatically includes all trails from lower tiers (inheritance).
     */
    public static Set<String> getTrailsForTier(String tier) {
        if (tier == null) {
            return Set.of();
        }

        Set<String> trails = new HashSet<>();
        String tierLower = tier.toLowerCase();

        // Add trails from all tiers up to and including the requested tier
        trails.addAll(STONE_TRAILS);
        if (tierLower.equals("stone")) return Set.copyOf(trails);

        trails.addAll(BRONZE_TRAILS);
        if (tierLower.equals("bronze")) return Set.copyOf(trails);

        trails.addAll(SILVER_TRAILS);
        if (tierLower.equals("silver")) return Set.copyOf(trails);

        trails.addAll(GOLD_TRAILS);
        if (tierLower.equals("gold")) return Set.copyOf(trails);

        trails.addAll(PLATINUM_TRAILS);
        if (tierLower.equals("platinum")) return Set.copyOf(trails);

        return Set.of(); // Unknown tier
    }

    /**
     * Get all supporter addons for a given tier.
     * Automatically includes all addons from lower tiers (inheritance).
     */
    public static Set<String> getAddonsForTier(String tier) {
        if (tier == null) {
            return Set.of();
        }

        Set<String> addons = new HashSet<>();
        String tierLower = tier.toLowerCase();

        // Add addons from all tiers up to and including the requested tier
        addons.addAll(STONE_ADDONS);
        if (tierLower.equals("stone")) return Set.copyOf(addons);

        addons.addAll(BRONZE_ADDONS);
        if (tierLower.equals("bronze")) return Set.copyOf(addons);

        addons.addAll(SILVER_ADDONS);
        if (tierLower.equals("silver")) return Set.copyOf(addons);

        addons.addAll(GOLD_ADDONS);
        if (tierLower.equals("gold")) return Set.copyOf(addons);

        addons.addAll(PLATINUM_ADDONS);
        if (tierLower.equals("platinum")) return Set.copyOf(addons);

        return Set.of(); // Unknown tier
    }

    /**
     * Get all supporter effects for a given tier.
     * Automatically includes all effects from lower tiers (inheritance).
     */
    public static Set<String> getEffectsForTier(String tier) {
        if (tier == null) {
            return Set.of();
        }

        Set<String> effects = new HashSet<>();
        String tierLower = tier.toLowerCase();

        // Add effects from all tiers up to and including the requested tier
        effects.addAll(STONE_EFFECTS);
        if (tierLower.equals("stone")) return Set.copyOf(effects);

        effects.addAll(BRONZE_EFFECTS);
        if (tierLower.equals("bronze")) return Set.copyOf(effects);

        effects.addAll(SILVER_EFFECTS);
        if (tierLower.equals("silver")) return Set.copyOf(effects);

        effects.addAll(GOLD_EFFECTS);
        if (tierLower.equals("gold")) return Set.copyOf(effects);

        effects.addAll(PLATINUM_EFFECTS);
        if (tierLower.equals("platinum")) return Set.copyOf(effects);

        return Set.of(); // Unknown tier
    }

    /**
     * Get ALL supporter-exclusive trails (across all tiers)
     */
    public static Set<String> getAllSupporterTrails() {
        Set<String> all = new HashSet<>();
        all.addAll(STONE_TRAILS);
        all.addAll(BRONZE_TRAILS);
        all.addAll(SILVER_TRAILS);
        all.addAll(GOLD_TRAILS);
        all.addAll(PLATINUM_TRAILS);
        return all;
    }

    /**
     * Get ALL supporter-exclusive addons (across all tiers)
     */
    public static Set<String> getAllSupporterAddons() {
        Set<String> all = new HashSet<>();
        all.addAll(STONE_ADDONS);
        all.addAll(BRONZE_ADDONS);
        all.addAll(SILVER_ADDONS);
        all.addAll(GOLD_ADDONS);
        all.addAll(PLATINUM_ADDONS);
        return all;
    }

    /**
     * Get ALL supporter-exclusive effects (across all tiers)
     */
    public static Set<String> getAllSupporterEffects() {
        Set<String> all = new HashSet<>();
        all.addAll(STONE_EFFECTS);
        all.addAll(BRONZE_EFFECTS);
        all.addAll(SILVER_EFFECTS);
        all.addAll(GOLD_EFFECTS);
        all.addAll(PLATINUM_EFFECTS);
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
     * Check if an effect is supporter-exclusive
     */
    public static boolean isEffectSupporterExclusive(String effectId) {
        return getAllSupporterEffects().contains(effectId);
    }
}
