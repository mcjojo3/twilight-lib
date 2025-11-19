package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * Capability interface for storing player model variant (Steve/Alex).
 *
 * <p>Minecraft has two built-in player model types:
 * <ul>
 *   <li><b>Steve (classic)</b>: 4px wide arms</li>
 *   <li><b>Alex (slim)</b>: 3px wide arms</li>
 * </ul>
 *
 * <p>By default, the model type is determined by the player's UUID/skin,
 * but this capability allows cosmetic overriding for supporters.
 */
public interface IModelVariant extends ISerializableData {
    /**
     * Get the current model variant.
     * @return "steve" or "alex" (lowercase)
     */
    String getModelVariant();

    /**
     * Set the model variant.
     * @param variant "steve" or "alex" (case-insensitive)
     */
    void setModelVariant(String variant);

    /**
     * Check if a custom model variant is set (overriding the default).
     * @return true if model variant is explicitly set, false if using default
     */
    boolean hasCustomVariant();

    /**
     * Clear custom model variant, reverting to default (based on skin).
     */
    void clearCustomVariant();

    /**
     * Serialize to NBT for persistent storage.
     */
    CompoundTag serialize();

    /**
     * Deserialize from NBT.
     */
    void deserialize(CompoundTag tag);
}