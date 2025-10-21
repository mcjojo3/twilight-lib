package mc.sayda.twilight_lib.capabilities;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;

public class MorphData implements IMorph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_ENTITY = "Entity";

    private Optional<ResourceLocation> entityType = Optional.empty();
    private EntityType<?> cachedEntityType = null;

    @Override
    public Optional<ResourceLocation> getEntityType() {
        return entityType;
    }

    @Override
    public void setEntityType(Optional<ResourceLocation> type) {
        this.entityType = type;
        this.cachedEntityType = type.map(BuiltInRegistries.ENTITY_TYPE::get).orElse(null);
    }

    @Nullable
    public EntityType<?> getCachedEntityType() {
        return cachedEntityType;
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        entityType.ifPresent(rl -> tag.putString(NBT_ENTITY, rl.toString()));
        return tag;
    }

    @Override
    public void deserialize(CompoundTag tag) {
        if (tag.contains(NBT_ENTITY, Tag.TAG_STRING)) {
            try {
                ResourceLocation rl = new ResourceLocation(tag.getString(NBT_ENTITY));
                setEntityType(Optional.of(rl));
                LOGGER.debug("Ahh... I need a nap. Deserialized morph: {}", rl);
            } catch (Exception e) {
                LOGGER.warn("Oh, farn it! Failed to deserialize morph: {}", e.getMessage());
                setEntityType(Optional.empty());
            }
        } else {
            setEntityType(Optional.empty());
        }
    }
}
