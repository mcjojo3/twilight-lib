package mc.sayda.twilight_lib.model_variant;

import mc.sayda.twilight_lib.api.model_variant.IModelVariant;
import net.minecraft.resources.ResourceLocation;

/**
 * Default implementation for built-in player model variants.
 */
public class DefaultModelVariant implements IModelVariant {
    private final ResourceLocation id;
    private final String modelType;

    public DefaultModelVariant(ResourceLocation id, String modelType) {
        this.id = id;
        this.modelType = modelType;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getPlayerModelType() {
        return modelType;
    }
}
