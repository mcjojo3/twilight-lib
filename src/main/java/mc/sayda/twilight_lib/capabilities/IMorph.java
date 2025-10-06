package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IMorph {
    Optional<ResourceLocation> getEntityType();
    void setEntityType(Optional<ResourceLocation> type);

    @Nullable
    default EntityType<?> getCachedEntityType() {
        return null;
    }

    CompoundTag serialize();
    void deserialize(CompoundTag tag);
}
