package mc.sayda.twilight_lib.api.morph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;

/**
 * Defines a morph transformation.
 */
public interface IMorph {
    /**
     * @return The unique identifier for this morph.
     */
    ResourceLocation getId();

    /**
     * @return The entity type to morph into.
     */
    default EntityType<?> getEntityType() {
        return BuiltInRegistries.ENTITY_TYPE.get(getId());
    }

    /**
     * @return true if the player's nametag should be hidden.
     */
    default boolean shouldHideNametag() {
        return false;
    }

    /**
     * @return Custom width for the morph, or empty to use entity default.
     */
    default Optional<Float> getWidthOverride(Player player) {
        return Optional.empty();
    }

    /**
     * @return Custom height for the morph, or empty to use entity default.
     */
    default Optional<Float> getHeightOverride(Player player) {
        return Optional.empty();
    }
}
