package mc.sayda.twilight_lib.addon;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Stores information about a registered addon model
 * The modelFactory should create models that extend EntityModel and implement IAddonModel
 *
 * @param modTags Set of mod IDs required for this addon to load (empty = always load)
 * @param hiddenBodyParts Set of body parts this addon should hide (e.g., HEAD, BODY)
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
    Set<BodyPart> hiddenBodyParts
) {}