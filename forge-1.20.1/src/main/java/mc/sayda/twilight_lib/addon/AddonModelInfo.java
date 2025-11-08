package mc.sayda.twilight_lib.addon;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Stores information about a registered addon model
 * The modelFactory should create models that extend EntityModel and implement IAddonModel
 */
public record AddonModelInfo(
    String id,
    ModelLayerLocation layerLocation,
    Supplier<LayerDefinition> layerDefinitionSupplier,
    Function<ModelPart, ?> modelFactory,
    ResourceLocation texture,
    boolean usePlayerSkin,
    boolean translucent,
    boolean hidePlayerModel
) {}