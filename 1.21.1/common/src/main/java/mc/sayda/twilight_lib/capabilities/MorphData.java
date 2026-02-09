package mc.sayda.twilight_lib.capabilities;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nonnull;
import org.slf4j.Logger;

import java.util.Optional;

public class MorphData implements IMorph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_ENTITY = "Entity";
    private static final String NBT_HIDE_NAMETAG = "HideNametag";

    private Optional<mc.sayda.twilight_lib.api.morph.IMorph> morph = Optional.empty();
    private boolean hideNametag = false; // Default: show nametag

    @Override
    public synchronized Optional<mc.sayda.twilight_lib.api.morph.IMorph> getMorph() {
        return morph;
    }

    @Override
    public synchronized void setMorph(Optional<mc.sayda.twilight_lib.api.morph.IMorph> morph) {
        this.morph = morph;
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
    @Nonnull
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        getMorph().ifPresent(m -> tag.putString(NBT_ENTITY, m.getId().toString()));
        tag.putBoolean(NBT_HIDE_NAMETAG, hideNametag);
        return tag;
    }

    @Override
    public synchronized void deserialize(@Nonnull CompoundTag tag) {
        if (tag.contains(NBT_ENTITY, Tag.TAG_STRING)) {
            try {
                ResourceLocation rl = ResourceLocation.parse(tag.getString(NBT_ENTITY));
                setMorph(mc.sayda.twilight_lib.api.morph.IMorphRegistry.getInstance().get(rl));
                LOGGER.debug("Ahh... I need a nap. Deserialized morph: {}", rl);
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to deserialize morph from NBT", e);
                setMorph(Optional.empty());
            }
        } else {
            setMorph(Optional.empty());
        }

        // Deserialize nametag visibility (default: false = show nametag)
        if (tag.contains(NBT_HIDE_NAMETAG, Tag.TAG_BYTE)) {
            this.hideNametag = tag.getBoolean(NBT_HIDE_NAMETAG);
        } else {
            this.hideNametag = false;
        }
    }
}
