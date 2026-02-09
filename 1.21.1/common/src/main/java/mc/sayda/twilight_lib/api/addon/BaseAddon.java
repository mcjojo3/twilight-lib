package mc.sayda.twilight_lib.api.addon;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Supplier;

import mc.sayda.twilight_lib.addon.BodyPart;

/**
 * A base implementation of {@link IAddon} for convenience.
 */
public abstract class BaseAddon implements IAddon {
    private final ResourceLocation id;
    private final AddonType type;
    private final ModelLayerLocation layerLocation;
    private final Supplier<LayerDefinition> layerDefinition;
    private final ResourceLocation texture;
    private final Set<String> requiredModIds;
    private final Set<BodyPart> hiddenBodyParts;

    protected BaseAddon(ResourceLocation id, AddonType type, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinition, ResourceLocation texture) {
        this(id, type, layerLocation, layerDefinition, texture, Set.of(), Set.of());
    }

    protected BaseAddon(ResourceLocation id, AddonType type, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinition, ResourceLocation texture,
            Set<String> requiredModIds) {
        this(id, type, layerLocation, layerDefinition, texture, requiredModIds, Set.of());
    }

    protected BaseAddon(ResourceLocation id, AddonType type, ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinition, ResourceLocation texture,
            Set<String> requiredModIds, Set<BodyPart> hiddenBodyParts) {
        this.id = id;
        this.type = type;
        this.layerLocation = layerLocation;
        this.layerDefinition = layerDefinition;
        this.texture = texture;
        this.requiredModIds = requiredModIds;
        this.hiddenBodyParts = hiddenBodyParts;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public AddonType getType() {
        return type;
    }

    @Override
    public ModelLayerLocation getLayerLocation() {
        return layerLocation;
    }

    @Override
    public Supplier<LayerDefinition> getLayerDefinition() {
        return layerDefinition;
    }

    @Override
    public ResourceLocation getTexture() {
        return texture;
    }

    @Override
    public Set<String> getRequiredModIds() {
        return requiredModIds;
    }

    @Override
    public Set<BodyPart> getHiddenBodyParts() {
        return hiddenBodyParts;
    }
}
