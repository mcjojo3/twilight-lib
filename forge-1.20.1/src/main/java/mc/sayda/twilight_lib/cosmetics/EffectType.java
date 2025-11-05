package mc.sayda.twilight_lib.cosmetics;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of all available cosmetic effects.
 * Each effect has a unique ID, description, and category.
 * Players can only have ONE effect active per category at a time.
 */
public enum EffectType {
    SPAWN_ETHEREAL("spawn_ethereal", "Ethereal particle burst (soul + portal + enchanting particles)", EffectCategory.SPAWN),
    SPAWN_RAINBOW("spawn_rainbow", "Rainbow cycling particles (vibrant multi-color)", EffectCategory.SPAWN),
    SPAWN_PORTAL("spawn_portal", "End portal particles with reverse gravity (mysterious void theme)", EffectCategory.SPAWN),
    SPAWN_FROST("spawn_frost", "Snowflake particles (icy winter theme)", EffectCategory.SPAWN),
    SPAWN_FLAME("spawn_flame", "Soul fire particles (blazing fire theme)", EffectCategory.SPAWN),
    SPAWN_NATURE("spawn_nature", "Spore blossom particles (natural floral theme)", EffectCategory.SPAWN),

    AMBIENT_FLAME("ambient_flame", "Continuous flame circle around player's feet", EffectCategory.AMBIENT),
    AMBIENT_FROST("ambient_frost", "Continuous frost circle around player's feet", EffectCategory.AMBIENT);

    private final String id;
    private final String description;
    private final EffectCategory category;

    // Cached ID-to-EffectType lookup map for O(1) performance instead of O(n)
    private static final Map<String, EffectType> ID_CACHE = new HashMap<>();

    static {
        for (EffectType type : values()) {
            ID_CACHE.put(type.id, type);
        }
    }

    EffectType(String id, String description, EffectCategory category) {
        this.id = id;
        this.description = description;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public EffectCategory getCategory() {
        return category;
    }

    /**
     * Look up an effect type by its string ID using cached O(1) lookup.
     * @param id The effect type ID
     * @return The matching EffectType, or null if not found
     */
    @Nullable
    public static EffectType fromId(String id) {
        return ID_CACHE.get(id);
    }
}