package mc.sayda.twilight_lib.capabilities;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Capability interface for player morphing functionality.
 * Allows players to transform into different LivingEntity types.
 *
 * <p>
 * Morphing changes:
 * <ul>
 * <li>Visual appearance (rendered as the morphed entity)</li>
 * <li>Hitbox size (matches the morphed entity's dimensions)</li>
 * <li>Eye height (configurable via TwilightConfig)</li>
 * <li>Step height (scaled based on entity size)</li>
 * </ul>
 *
 * <p>
 * Morphs are admin-only and granted via /twilightlib morph command.
 * Data persists through death and dimension changes via NBT serialization.
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

    /**
     * Get the currently morphed entity type.
     * 
     * @return Optional containing the entity type ResourceLocation, or empty if not
     *         morphed
     */
    default Optional<ResourceLocation> getEntityType() {
        return getMorph().map(mc.sayda.twilight_lib.api.morph.IMorph::getId);
    }

    /**
     * Set the morphed entity type.
     * 
     * @param type Optional containing the entity type ResourceLocation, or empty to
     *             clear morph
     */
    default void setEntityType(Optional<ResourceLocation> type) {
        setMorph(type.flatMap(rl -> mc.sayda.twilight_lib.api.morph.IMorphRegistry.getInstance().get(rl)));
    }

    /**
     * Get the cached EntityType instance for performance.
     * Avoids repeated registry lookups during rendering.
     * 
     * @return The cached EntityType, or null if not morphed or not yet cached
     */
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

    /**
     * Set the tint color applied to the morph. Does not change whether the
     * current tint is persistent - use this from network/render sync paths where
     * persistence isn't a relevant concern.
     *
     * @param tint packed 0xRRGGBB color; 0xFFFFFF clears the tint
     */
    void setTint(int tint);

    /**
     * Set the tint color applied to the morph, with persistence control (mirrors
     * {@link IAddons}'s equip-with-persistence pattern).
     *
     * @param tint       packed 0xRRGGBB color; 0xFFFFFF clears the tint
     * @param persistent if true, the tint survives {@link #serialize()} (written
     *                   to NBT, restored on next login); if false, it applies for
     *                   the current session only and is dropped the next time
     *                   this data is saved
     */
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
    @Nonnull
    CompoundTag serialize();

    /**
     * Deserialize morph data from NBT.
     * 
     * @param tag CompoundTag containing morph data
     */
    void deserialize(@Nonnull CompoundTag tag);
}
