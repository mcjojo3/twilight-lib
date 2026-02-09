package mc.sayda.twilight_lib.api.addon;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.function.Supplier;

import mc.sayda.twilight_lib.addon.BodyPart;

/**
 * Interface for all cosmetic addons in Twilight Lib.
 * Addons are visual attachments rendered on player models.
 */
public interface IAddon {
    /**
     * Unique identifier for this addon.
     */
    ResourceLocation getId();

    /**
     * @return The type/category of this addon.
     */
    default AddonType getType() {
        return AddonType.OTHER;
    }

    /**
     * The model layer location for this addon's model.
     */
    ModelLayerLocation getLayerLocation();

    /**
     * Supplier for the model's layer definition (cubes, bones, etc.).
     */
    Supplier<LayerDefinition> getLayerDefinition();

    /**
     * The texture to use for this addon.
     */
    ResourceLocation getTexture();

    /**
     * Whether this addon should use the player's skin texture instead of a custom
     * one.
     */
    default boolean usePlayerSkin() {
        return false;
    }

    /**
     * Whether this addon should be rendered with transparency.
     */
    default boolean isTranslucent() {
        return false;
    }

    /**
     * Whether the base player model should be hidden when this addon is active.
     */
    default boolean hidesPlayerModel() {
        return false;
    }

    /**
     * Whether this addon forces ALL other active addons to differ rendered with
     * transparency.
     * Useful for full-body "ghost" effects.
     */
    default boolean forceAllTranslucent() {
        return false;
    }

    /**
     * Set of specific body parts to hide when this addon is active.
     */
    default Set<BodyPart> getHiddenBodyParts() {
        return Set.of();
    }

    /**
     * Set of mod IDs required for this addon to be available.
     * Empty means no requirements.
     */
    default Set<String> getRequiredModIds() {
        return Set.of();
    }

    /**
     * Whether this addon is currently visible on the given entity.
     */
    default boolean isVisible(Entity entity) {
        return true;
    }

    /**
     * Creates the model instance for this addon.
     * The returned model must implement
     * {@link mc.sayda.twilight_lib.client.model.IAddonModel}
     * and extend {@link net.minecraft.client.model.EntityModel}.
     */
    mc.sayda.twilight_lib.client.model.IAddonModel createModel(net.minecraft.client.model.geom.ModelPart root);
}
