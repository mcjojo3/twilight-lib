package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class TrailsData implements ITrails {
    private static final String NBT_TRAILS = "Trails";
    private static final String NBT_ACTIVE_TRAIL = "ActiveTrail";
    private static final String NBT_PERSISTENT_TRAIL = "PersistentTrail";
    private static final String NBT_TRAIL_ENABLED = "TrailEnabled";

    private final Set<String> trails = new HashSet<>();
    private String activeTrail = null;
    private boolean trailEnabled = true;
    private boolean isPersistentTrail = false;  // Track if active trail is persistent (CreRaces)

    @Override
    public Set<String> getTrails() {
        return new HashSet<>(trails);
    }

    @Override
    public void addTrail(String trailId) {
        trails.add(trailId);
    }

    @Override
    public void removeTrail(String trailId) {
        trails.remove(trailId);
        // Clear active trail if the removed trail was active
        if (trailId != null && trailId.equals(activeTrail)) {
            activeTrail = null;
        }
    }

    @Override
    public boolean hasTrail(String trailId) {
        return trails.contains(trailId);
    }

    @Override
    public void clearTrails() {
        trails.clear();
        // Clear active trail when clearing all trails
        activeTrail = null;
    }

    @Override
    public String getActiveTrail() {
        return activeTrail;
    }

    @Override
    public synchronized void setActiveTrail(String trail) {
        if (trail == null || trails.contains(trail)) {
            this.activeTrail = trail;
        }
    }

    /**
     * Force set active trail without ownership check (for admin commands).
     * Used by /twilightlib trail set to activate trails with persistence control.
     * @param trail The trail ID to activate
     * @param persistent If true, trail persists through logout/death; if false, cleared on logout
     */
    public synchronized void forceSetActiveTrail(String trail, boolean persistent) {
        this.activeTrail = trail;
        this.isPersistentTrail = persistent;
    }

    @Override
    public boolean isTrailActive(String trailId) {
        return trailId != null && trailId.equals(activeTrail) && trailEnabled;
    }

    @Override
    public boolean isTrailEnabled() {
        return trailEnabled;
    }

    @Override
    public void setTrailEnabled(boolean enabled) {
        this.trailEnabled = enabled;
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();

        ListTag trailsList = new ListTag();
        for (String trail : trails) {
            trailsList.add(StringTag.valueOf(trail));
        }
        tag.put(NBT_TRAILS, trailsList);

        if (activeTrail != null) {
            tag.putString(NBT_ACTIVE_TRAIL, activeTrail);
            tag.putBoolean(NBT_PERSISTENT_TRAIL, isPersistentTrail);
        }
        tag.putBoolean(NBT_TRAIL_ENABLED, trailEnabled);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        trails.clear();
        activeTrail = null;  // Reset to null before loading
        isPersistentTrail = false;

        if (tag.contains(NBT_TRAILS, Tag.TAG_LIST)) {
            ListTag trailsList = tag.getList(NBT_TRAILS, Tag.TAG_STRING);
            for (int i = 0; i < trailsList.size(); i++) {
                trails.add(trailsList.getString(i));
            }
        }

        if (tag.contains(NBT_ACTIVE_TRAIL)) {
            String loadedTrail = tag.getString(NBT_ACTIVE_TRAIL);
            isPersistentTrail = tag.getBoolean(NBT_PERSISTENT_TRAIL);  // Default false if not present

            // Validate ownership for non-persistent trails
            if (isPersistentTrail || trails.contains(loadedTrail)) {
                activeTrail = loadedTrail;
            }
            // Non-persistent trails without ownership are cleared (temporary admin previews)
        }

        trailEnabled = !tag.contains(NBT_TRAIL_ENABLED) || tag.getBoolean(NBT_TRAIL_ENABLED);
    }
}
