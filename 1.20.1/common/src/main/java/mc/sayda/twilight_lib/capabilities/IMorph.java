package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Player morph into another living entity. Admin-only, and kept through death
 * and dimension changes.
 */
public interface IMorph extends ISerializableData {
    /**
     * Get the active morph.
     * 
     * @return Optional containing the active morph, or empty if not morphed
     */
    Optional<mc.sayda.twilight_lib.api.morph.IMorph> getMorph();

    /**
     * Set the active morph.
     * 
     * @param morph Optional containing the new morph, or empty to clear
     */
    void setMorph(Optional<mc.sayda.twilight_lib.api.morph.IMorph> morph);

    /** The morphed entity type, or empty if not morphed. */
    default Optional<ResourceLocation> getEntityType() {
        return getMorph().map(mc.sayda.twilight_lib.api.morph.IMorph::getId);
    }

    /** Sets the morphed entity type, or clears the morph with an empty Optional. */
    default void setEntityType(Optional<ResourceLocation> type) {
        setMorph(type.flatMap(rl -> mc.sayda.twilight_lib.api.morph.IMorphRegistry.getInstance().get(rl)));
    }

    /** Cached EntityType so rendering skips the registry lookup. Null if not morphed. */
    @Nullable
    default EntityType<?> getCachedEntityType() {
        return getMorph().map(mc.sayda.twilight_lib.api.morph.IMorph::getEntityType).orElse(null);
    }

    /**
     * Check if the player's nametag should be hidden when morphed.
     *
     * @return true if nametag should be hidden, false if visible (default)
     */
    boolean isNametagHidden();

    /**
     * Set whether the player's nametag should be hidden when morphed.
     *
     * @param hidden true to hide nametag, false to show it
     */
    void setNametagHidden(boolean hidden);

    /**
     * Get the tint color applied to the morph, packed as 0xRRGGBB.
     *
     * @return the packed tint color, or 0xFFFFFF (white) if no tint is set
     */
    int getTint();

    /** Sets the morph tint (0xRRGGBB, 0xFFFFFF clears it) without changing whether it's saved. */
    void setTint(int tint);

    /** Sets the morph tint (0xRRGGBB, 0xFFFFFF clears it). Non-persistent tints aren't saved. */
    void setTint(int tint, boolean persistent);

    /**
     * Whether the current tint (as last set via {@link #setTint(int, boolean)})
     * is persistent. Used to decide whether a tint should be cleared when the
     * player unmorphs, or carried forward to the next morph.
     */
    boolean isTintPersistent();

    /**
     * Serialize morph data to NBT for persistence.
     * 
     * @return CompoundTag containing morph data
     */
    CompoundTag serialize();

    /**
     * Deserialize morph data from NBT.
     * 
     * @param tag CompoundTag containing morph data
     */
    void deserialize(CompoundTag tag);
}
