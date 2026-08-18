package mc.sayda.twilight_lib.addon;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import mc.sayda.twilight_lib.api.addon.AddonType;
import mc.sayda.twilight_lib.api.addon.IAddon;
import java.util.Optional;

/**
 * Stores information about a registered addon model
 * The modelFactory should create models that extend EntityModel and implement
 * IAddonModel
 *
 * @param modTags         Set of mod IDs required for this addon to load (empty
 *                        = always load)
 * @param hiddenBodyParts Set of body parts this addon should hide (e.g., HEAD,
 *                        BODY)
 */
public record AddonModelInfo(
        String id,
        ModelLayerLocation layerLocation,
        Supplier<LayerDefinition> layerDefinitionSupplier,
        Function<ModelPart, ?> modelFactory,
        ResourceLocation texture,
        boolean usePlayerSkin,
        boolean translucent,
        boolean hidePlayerModel,
        boolean forceAllTranslucent,
        Set<String> modTags,
        Set<BodyPart> hiddenBodyParts,
        Optional<ResourceLocation> maskTexture) implements IAddon {

    @Override
    public ResourceLocation getId() {
        // Safe conversion for legacy string IDs
        return Optional.ofNullable(ResourceLocation.tryParse(id))
                .orElse(new ResourceLocation("twilight_lib", id.toLowerCase()));
    }

    @Override
    public AddonType getType() {
        return AddonType.OTHER;
    }

    @Override
    public ModelLayerLocation getLayerLocation() {
        return layerLocation;
    }

    @Override
    public Supplier<LayerDefinition> getLayerDefinition() {
        return layerDefinitionSupplier;
    }

    @Override
    public ResourceLocation getTexture() {
        return texture;
    }

    @Override
    public Optional<ResourceLocation> getMaskTexture() {
        if (maskTexture.isPresent()) {
            return maskTexture;
        }
        if (usePlayerSkin) {
            // texture is just the shared stub placeholder here, not a real
            // per-addon texture to derive a mask from - auto-discovery would
            // wrongly apply whatever mask/stub.png happens to be present (if
            // any) to every player-skin addon at once.
            return Optional.empty();
        }
        return mc.sayda.twilight_lib.client.tint.TintTextureCompositor.resolveMask(texture);
    }

    @Override
    public boolean usePlayerSkin() {
        return usePlayerSkin;
    }

    @Override
    public boolean isTranslucent() {
        return translucent;
    }

    @Override
    public boolean hidesPlayerModel() {
        return hidePlayerModel;
    }

    @Override
    public Set<String> getRequiredModIds() {
        return modTags;
    }

    @Override
    public Set<BodyPart> getHiddenBodyParts() {
        return hiddenBodyParts;
    }

    @Override
    public boolean isVisible(net.minecraft.world.entity.Entity entity) {
        return true;
    }

    @Override
    public mc.sayda.twilight_lib.client.model.IAddonModel createModel(ModelPart root) {
        return (mc.sayda.twilight_lib.client.model.IAddonModel) modelFactory.apply(root);
    }
}
