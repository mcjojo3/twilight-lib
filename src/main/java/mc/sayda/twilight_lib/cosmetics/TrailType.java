package mc.sayda.twilight_lib.cosmetics;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Defines available trail types for supporters
 */
public enum TrailType {
    HEARTS("hearts", null, 2), // Tier-based, particle determined dynamically
    SPARKLES("sparkles", ParticleTypes.WAX_ON, 3),
    CHERRY_BLOSSOM("cherry_blossom", ParticleTypes.CHERRY_LEAVES, 2),
    TWILIGHT("twilight", ParticleTypes.PORTAL, 2),
    STARS("stars", ParticleTypes.END_ROD, 1);

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

    public ParticleOptions getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public boolean isTierBased() {
        return particleType == null;
    }

    public static TrailType fromId(String id) {
        for (TrailType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}