package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * Capability for managing player particle trails
 * Works identically to Addons and Effects systems
 */
public interface ITrails {
    Set<String> getTrails();
    void addTrail(String trailId);
    void removeTrail(String trailId);
    boolean hasTrail(String trailId);
    void clearTrails();

    /**
     * Get the currently active trail (null if disabled)
     */
    String getActiveTrail();

    /**
     * Set the active trail
     */
    void setActiveTrail(String trail);

    /**
     * Check if trails are enabled
     */
    boolean isTrailEnabled();

    /**
     * Toggle trails on/off
     */
    void setTrailEnabled(boolean enabled);

    CompoundTag serialize();
    void deserialize(CompoundTag tag);
}
