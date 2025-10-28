package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public enum TrailType {
    HEARTS("hearts", null, 2), // Tier-based, particle determined dynamically
    RATVENOM("ratvenom", ModParticles.RATVENOM.get(), 3),
    SPARKLES("sparkles", ParticleTypes.WAX_ON, 3),
    CHERRY_BLOSSOM("cherry_blossom", ParticleTypes.CHERRY_LEAVES, 2),
    TWILIGHT("twilight", ParticleTypes.PORTAL, 2),
    STARS("stars", ParticleTypes.END_ROD, 1),
    ASH("ash", ParticleTypes.ASH, 6),
    WHITE_ASH("white_ash", ParticleTypes.WHITE_ASH, 7),
    BUBBLES("bubbles", ParticleTypes.BUBBLE_POP, 4),
    HONEY("honey", ModParticles.SILENT_HONEY.get(), 2),
    WOLF_PRINTS("wolf_prints", ModParticles.WOLF_PRINT.get(), 1, true);

    private final String id;
    private final ParticleOptions particleType; // Can be null for tier-based trails
    private final int particleCount;
    private final boolean isFootprint; // Whether this is a footprint-style trail (alternating left/right)

    // Cached ID-to-TrailType lookup map for O(1) performance instead of O(n)
    private static final Map<String, TrailType> ID_CACHE = new HashMap<>();

    static {
        for (TrailType type : values()) {
            ID_CACHE.put(type.id, type);
        }
    }

    TrailType(String id, ParticleOptions particleType, int particleCount) {
        this(id, particleType, particleCount, false);
    }

    TrailType(String id, ParticleOptions particleType, int particleCount, boolean isFootprint) {
        this.id = id;
        this.particleType = particleType;
        this.particleCount = particleCount;
        this.isFootprint = isFootprint;
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
     * Check if this trail is a footprint-style trail (alternating left/right placement).
     * @return true if footprint trail, false otherwise
     */
    public boolean isFootprint() {
        return isFootprint;
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