package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;

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
    HONEY("honey", ModParticles.SILENT_HONEY.get(), 2);

    private final String id;
    private final ParticleOptions particleType; // Can be null for tier-based trails
    private final int particleCount;

    TrailType(String id, ParticleOptions particleType, int particleCount) {
        this.id = id;
        this.particleType = particleType;
        this.particleCount = particleCount;
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
     * Look up a trail type by its string ID.
     * @param id The trail type ID
     * @return The matching TrailType, or null if not found
     */
    @Nullable
    public static TrailType fromId(String id) {
        for (TrailType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}