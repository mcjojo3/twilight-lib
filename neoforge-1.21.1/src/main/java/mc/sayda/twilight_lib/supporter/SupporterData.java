package mc.sayda.twilight_lib.supporter;

import java.util.HashSet;
import java.util.Set;

/**
 * Data class representing a supporter's cosmetic permissions
 *
 * Tier unlocks are automatic based on SupporterRegistry.
 * Manual cosmetics are overrides that persist even if tier expires.
 */
public class SupporterData {
    private final String uuid;
    private final String name;
    private final String tier; // "stone", "bronze", "silver", "gold", "platinum", "none", or null/none = not a supporter
    private final Set<String> manualTrails; // Manual overrides (persist forever)
    private final Set<String> manualAddons;
    private final Set<String> manualEffects;

    public SupporterData(String uuid, String name, String tier, Set<String> manualTrails, Set<String> manualAddons, Set<String> manualEffects) {
        this.uuid = uuid;
        this.name = name;
        this.tier = tier;
        this.manualTrails = new HashSet<>(manualTrails);
        this.manualAddons = new HashSet<>(manualAddons);
        this.manualEffects = new HashSet<>(manualEffects);
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getTier() {
        return tier;
    }

    /**
     * Check if this user is an active supporter (has a valid tier)
     */
    public boolean isActiveSupporter() {
        return tier != null && !tier.equalsIgnoreCase("none");
    }

    /**
     * Get ALL trails this user should have (tier unlocks + manual overrides)
     */
    public Set<String> getAllTrails() {
        Set<String> all = new HashSet<>(manualTrails);
        if (isActiveSupporter()) {
            all.addAll(SupporterRegistry.getTrailsForTier(tier));
        }
        return all;
    }

    /**
     * Get ALL addons this user should have (tier unlocks + manual overrides)
     */
    public Set<String> getAllAddons() {
        Set<String> all = new HashSet<>(manualAddons);
        if (isActiveSupporter()) {
            all.addAll(SupporterRegistry.getAddonsForTier(tier));
        }
        return all;
    }

    /**
     * Get ALL effects this user should have (tier unlocks + manual overrides)
     */
    public Set<String> getAllEffects() {
        Set<String> all = new HashSet<>(manualEffects);
        if (isActiveSupporter()) {
            all.addAll(SupporterRegistry.getEffectsForTier(tier));
        }
        return all;
    }

    /**
     * Get only manual trail overrides (not tier-based)
     */
    public Set<String> getManualTrails() {
        return new HashSet<>(manualTrails);
    }

    /**
     * Get only manual addon overrides (not tier-based)
     */
    public Set<String> getManualAddons() {
        return new HashSet<>(manualAddons);
    }

    /**
     * Get only manual effect overrides (not tier-based)
     */
    public Set<String> getManualEffects() {
        return new HashSet<>(manualEffects);
    }
}