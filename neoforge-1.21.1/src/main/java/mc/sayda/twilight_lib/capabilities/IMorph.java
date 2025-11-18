package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Capability interface for player morphing functionality.
 * Allows players to transform into different LivingEntity types.
 *
 * <p>Morphing changes:
 * <ul>
 *   <li>Visual appearance (rendered as the morphed entity)</li>
 *   <li>Hitbox size (matches the morphed entity's dimensions)</li>
 *   <li>Eye height (configurable via TwilightConfig)</li>
 *   <li>Step height (scaled based on entity size)</li>
 * </ul>
 *
 * <p>Morphs are admin-only and granted via /twilightlib morph command.
 * Data persists through death and dimension changes via NBT serialization.
 */
public interface IMorph extends ISerializableData {
    /**
     * Get the currently morphed entity type.
     * @return Optional containing the entity type ResourceLocation, or empty if not morphed
     */
    Optional<ResourceLocation> getEntityType();

    /**
     * Set the morphed entity type.
     * @param type Optional containing the entity type ResourceLocation, or empty to clear morph
     */
    void setEntityType(Optional<ResourceLocation> type);

    /**
     * Get the cached EntityType instance for performance.
     * Avoids repeated registry lookups during rendering.
     * @return The cached EntityType, or null if not morphed or not yet cached
     */
    @Nullable
    default EntityType<?> getCachedEntityType() {
        return null;
    }

    /**
     * Serialize morph data to NBT for persistence.
     * @return CompoundTag containing morph data
     */
    CompoundTag serialize();

    /**
     * Deserialize morph data from NBT.
     * @param tag CompoundTag containing morph data
     */
    void deserialize(CompoundTag tag);
}
