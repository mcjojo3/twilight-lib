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
    private static final String NBT_HIDE_NAMETAG = "HideNametag";

    private Optional<ResourceLocation> entityType = Optional.empty();
    private EntityType<?> cachedEntityType = null;
    private boolean hideNametag = false; // Default: show nametag

    @Override
    public synchronized Optional<ResourceLocation> getEntityType() {
        return entityType;
    }

    @Override
    public synchronized void setEntityType(Optional<ResourceLocation> type) {
        // Only refresh cache if entity type actually changed (avoid unnecessary registry lookups)
        if (!this.entityType.equals(type)) {
            this.entityType = type;
            this.cachedEntityType = type.map(BuiltInRegistries.ENTITY_TYPE::get).orElse(null);
        }
    }

    @Nullable
    public synchronized EntityType<?> getCachedEntityType() {
        return cachedEntityType;
    }

    @Override
    public synchronized boolean isNametagHidden() {
        return hideNametag;
    }

    @Override
    public synchronized void setNametagHidden(boolean hidden) {
        this.hideNametag = hidden;
    }

    @Override
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        entityType.ifPresent(rl -> tag.putString(NBT_ENTITY, rl.toString()));
        tag.putBoolean(NBT_HIDE_NAMETAG, hideNametag);
        return tag;
    }

    @Override
    public synchronized void deserialize(CompoundTag tag) {
        if (tag.contains(NBT_ENTITY, Tag.TAG_STRING)) {
            try {
                // Use tryParse() to avoid deprecated constructor
                ResourceLocation rl = ResourceLocation.tryParse(tag.getString(NBT_ENTITY));
                if (rl != null) {
                    setEntityType(Optional.of(rl));
                    LOGGER.debug("Ahh... I need a nap. Deserialized morph: {}", rl);
                } else {
                    LOGGER.warn("This will be fine! Things break all the time. Invalid resource location format: {}", tag.getString(NBT_ENTITY));
                    setEntityType(Optional.empty());
                }
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to deserialize morph from NBT", e);
                setEntityType(Optional.empty());
            }
        } else {
            setEntityType(Optional.empty());
        }

        // Deserialize nametag visibility (default: false = show nametag)
        if (tag.contains(NBT_HIDE_NAMETAG, Tag.TAG_BYTE)) {
            this.hideNametag = tag.getBoolean(NBT_HIDE_NAMETAG);
        } else {
            this.hideNametag = false;
        }
    }
}
