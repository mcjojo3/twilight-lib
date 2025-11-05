package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public enum TrailType {
    // Standard movement trails (spawn when moving, work in air and on ground)
    HEARTS("hearts", null, 2, TrailSpawnMode.MOVEMENT),
    RATVENOM("ratvenom", ModParticles.RATVENOM.get(), 3, TrailSpawnMode.MOVEMENT),
    SPARKLES("sparkles", ParticleTypes.WAX_ON, 3, TrailSpawnMode.MOVEMENT),
    CHERRY_BLOSSOM("cherry_blossom", ParticleTypes.CHERRY_LEAVES, 2, TrailSpawnMode.MOVEMENT),
    TWILIGHT("twilight", ParticleTypes.PORTAL, 2, TrailSpawnMode.MOVEMENT),
    STARS("stars", ParticleTypes.END_ROD, 1, TrailSpawnMode.MOVEMENT),
    ASH("ash", ParticleTypes.ASH, 6, TrailSpawnMode.MOVEMENT),
    WHITE_ASH("white_ash", ParticleTypes.WHITE_ASH, 7, TrailSpawnMode.MOVEMENT),
    BUBBLES("bubbles", ParticleTypes.BUBBLE_POP, 4, TrailSpawnMode.MOVEMENT),
    HONEY("honey", ModParticles.SILENT_HONEY.get(), 2, TrailSpawnMode.MOVEMENT),

    // Footprint trails (spawn on ground only, alternating left/right)
    WOLF_PRINTS("wolf_prints", ModParticles.WOLF_PRINT.get(), 1, TrailSpawnMode.FOOTPRINT);

    private final String id;
    private final ParticleOptions particleType; // Can be null for tier-based trails
    private final int particleCount;
    private final TrailSpawnMode spawnMode; // How this trail spawns (movement, footprint, continuous, grounded)

    // Cached ID-to-TrailType lookup map for O(1) performance instead of O(n)
    private static final Map<String, TrailType> ID_CACHE = new HashMap<>();

    static {
        for (TrailType type : values()) {
            ID_CACHE.put(type.id, type);
        }
    }

    TrailType(String id, ParticleOptions particleType, int particleCount, TrailSpawnMode spawnMode) {
        this.id = id;
        this.particleType = particleType;
        this.particleCount = particleCount;
        this.spawnMode = spawnMode;
    }

    public String getId() {
        return id;
    }

    /**
     * Get the particle type for this trail. Can be null for tier-based trails (e.g., hearts).
     * @return The particle type, or null if tier-based
     */
    @Nullable
    public ParticleOptions getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public boolean isTierBased() {
        return particleType == null;
    }

    /**
     * Get the spawn mode for this trail, which determines when and how particles spawn.
     * @return The spawn mode (MOVEMENT, FOOTPRINT, CONTINUOUS, or GROUNDED)
     */
    public TrailSpawnMode getSpawnMode() {
        return spawnMode;
    }

    /**
     * Check if this trail is a footprint-style trail (alternating left/right placement).
     * @return true if spawn mode is FOOTPRINT
     * @deprecated Use getSpawnMode() instead for more flexible spawn behavior checking
     */
    @Deprecated
    public boolean isFootprint() {
        return spawnMode == TrailSpawnMode.FOOTPRINT;
    }

    /**
     * Look up a trail type by its string ID using cached O(1) lookup.
     * @param id The trail type ID
     * @return The matching TrailType, or null if not found
     */
    @Nullable
    public static TrailType fromId(String id) {
        return ID_CACHE.get(id);
    }
}