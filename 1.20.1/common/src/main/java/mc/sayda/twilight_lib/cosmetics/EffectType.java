package mc.sayda.twilight_lib.cosmetics;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of all available cosmetic effects.
 * Each effect has a unique ID, description, and category.
 * Players can only have ONE effect active per category at a time.
 */
public enum EffectType {
    SPAWN_ETHEREAL("spawn_ethereal", "Ethereal particle burst (soul + portal + enchanting particles)",
            EffectCategory.SPAWN, Set.of()),
    SPAWN_RAINBOW("spawn_rainbow", "Rainbow cycling particles (vibrant multi-color)", EffectCategory.SPAWN, Set.of()),
    SPAWN_PORTAL("spawn_portal", "End portal particles with reverse gravity (mysterious void theme)",
            EffectCategory.SPAWN, Set.of()),
    SPAWN_FROST("spawn_frost", "Snowflake particles (icy winter theme)", EffectCategory.SPAWN, Set.of()),
    SPAWN_FLAME("spawn_flame", "Soul fire particles (blazing fire theme)", EffectCategory.SPAWN, Set.of()),
    SPAWN_NATURE("spawn_nature", "Spore blossom particles (natural floral theme)", EffectCategory.SPAWN, Set.of()),

    AMBIENT_FLAME("ambient_flame", "Continuous flame circle around player's feet", EffectCategory.AMBIENT, Set.of()),
    AMBIENT_FROST("ambient_frost", "Continuous frost circle around player's feet", EffectCategory.AMBIENT, Set.of());

    private final String id;
    private final String description;
    private final EffectCategory category;
    private final Set<String> modTags; // Required mod IDs (empty = always load)

    // Cached ID-to-EffectType lookup map for O(1) performance instead of O(n)
    private static final Map<String, EffectType> ID_CACHE = new HashMap<>();

    static {
        for (EffectType type : values()) {
            // Register all effects - availability is checked via isAvailable() when needed
            // This prevents early initialization issues where config isn't loaded yet
            ID_CACHE.put(type.id, type);
        }
    }

    EffectType(String id, String description, EffectCategory category, Set<String> modTags) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.modTags = modTags;
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
     * Get the mod tags for this effect.
     * 
     * @return Set of required mod IDs (empty = always available)
     */
    public Set<String> getModTags() {
        return modTags;
    }

    /**
     * Check if this effect is available based on loaded mods.
     * 
     * @return true if this effect should be accessible
     */
    public boolean isAvailable() {
        return ModRequirement.shouldLoad(modTags);
    }

    /**
     * Look up an effect type by its string ID using cached O(1) lookup.
     * Only returns effects that are available based on loaded mods.
     * 
     * @param id The effect type ID
     * @return The matching EffectType, or null if not found or not available
     */
    @Nullable
    public static EffectType fromId(String id) {
        EffectType type = ID_CACHE.get(id);
        if (type != null)
            return type;

        // Namespace fallback
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && (rl.getNamespace().equals("minecraft") || rl.getNamespace().equals("twilight_lib"))) {
            return ID_CACHE.get(rl.getPath());
        }

        return null;
    }
}
