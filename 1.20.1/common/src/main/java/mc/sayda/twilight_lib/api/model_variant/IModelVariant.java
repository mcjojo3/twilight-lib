package mc.sayda.twilight_lib.api.model_variant;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a player model variant (Steve, Alex, or custom).
 */
public interface IModelVariant {
    /**
     * @return The unique identifier for this model variant.
     */
    ResourceLocation getId();

    /**
     * @return The base player model type ("default" for Steve, "slim" for Alex).
     */
    String getPlayerModelType();

    /**
     * @return true if this is a slim (Alex) model.
     */
    default boolean isSlim() {
        return "slim".equals(getPlayerModelType());
    }

    /**
     * @return true if this variant should be selectable by players.
     */
    default boolean isSelectable() {
        return true;
    }
}
