package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class TrailsData implements ITrails {
    private final Set<String> trails = new HashSet<>();
    private String activeTrail = null;
    private boolean trailEnabled = true;

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
     * Used by /twilightlib trail set to temporarily activate trails.
     */
    public synchronized void forceSetActiveTrail(String trail) {
        this.activeTrail = trail;
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
        tag.put("Trails", trailsList);

        if (activeTrail != null) {
            tag.putString("ActiveTrail", activeTrail);
        }
        tag.putBoolean("TrailEnabled", trailEnabled);

        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        trails.clear();
        activeTrail = null;  // Reset to null before loading

        if (tag.contains("Trails", Tag.TAG_LIST)) {
            ListTag trailsList = tag.getList("Trails", Tag.TAG_STRING);
            for (int i = 0; i < trailsList.size(); i++) {
                trails.add(trailsList.getString(i));
            }
        }

        if (tag.contains("ActiveTrail")) {
            String loadedTrail = tag.getString("ActiveTrail");
            // Validate that the loaded trail exists in the trails set
            if (trails.contains(loadedTrail)) {
                activeTrail = loadedTrail;
            }
        }

        trailEnabled = !tag.contains("TrailEnabled") || tag.getBoolean("TrailEnabled");
    }
}
