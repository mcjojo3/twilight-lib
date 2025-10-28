package mc.sayda.twilight_lib.cosmetics;

/**
 * Categories for cosmetic effects.
 * Players can only have ONE effect active per category at a time.
 * This prevents visual clutter (e.g., all 6 spawn effects triggering simultaneously).
 */
public enum EffectCategory {
    /**
     * Spawn effects - triggered on login, respawn, dimension change
     * (Only one spawn effect can be active at a time)
     */
    SPAWN("Spawn Effects"),

    /**
     * Crafting effects - triggered when crafting items
     * (Only one crafting effect can be active at a time)
     */
    CRAFTING("Crafting Effects"),

    /**
     * Combat effects - triggered during combat
     * (Only one combat effect can be active at a time)
     */
    COMBAT("Combat Effects"),

    /**
     * Ambient effects - passive aura effects around the player
     * (Only one ambient effect can be active at a time)
     */
    AMBIENT("Ambient Effects");

    private final String displayName;

    EffectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}