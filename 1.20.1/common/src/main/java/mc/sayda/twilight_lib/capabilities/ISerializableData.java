package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * Shared interface for all data types that can be serialized to/from NBT.
 * This allows the DataSerializer in ModAttachments to use type bounds
 * instead of instanceof chains, improving type safety and maintainability.
 */
public interface ISerializableData {
    /**
     * Serialize this data to NBT for persistence.
     * @return CompoundTag containing this data
     */
    CompoundTag serialize();

    /**
     * Deserialize this data from NBT.
     * @param tag CompoundTag containing data to deserialize
     */
    void deserialize(CompoundTag tag);
}
