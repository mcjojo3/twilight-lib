package mc.sayda.twilight_lib.model_variant;

import mc.sayda.twilight_lib.api.model_variant.IModelVariant;
import mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ModelVariantRegistry implements IModelVariantRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModelVariantRegistry INSTANCE = new ModelVariantRegistry();

    static {
        IModelVariantRegistry.ModelVariantRegistryPlaceholder.INSTANCE = INSTANCE;
    }

    public static ModelVariantRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<ResourceLocation, IModelVariant> VARIANTS = new ConcurrentHashMap<>();

    public ModelVariantRegistry() {
        register(new DefaultModelVariant(new ResourceLocation("twilight_lib", "steve"), "default"));
        register(new DefaultModelVariant(new ResourceLocation("twilight_lib", "alex"), "slim"));
    }

    @Override
    public void register(IModelVariant variant) {
        if (variant == null || variant.getId() == null) {
            LOGGER.error(
                    "Is this the best physical representation you can manifest? Cannot register null variant or variant with null ID");
            return;
        }
        if (VARIANTS.containsKey(variant.getId())) {
            LOGGER.warn("Model variant ID '{}' already registered, overwriting...", variant.getId());
        }
        VARIANTS.put(variant.getId(), variant);
    }

    @Override
    public Optional<IModelVariant> get(ResourceLocation id) {
        return Optional.ofNullable(VARIANTS.get(id));
    }

    @Override
    public Collection<IModelVariant> getAll() {
        return VARIANTS.values();
    }

    @Override
    public boolean contains(ResourceLocation id) {
        return VARIANTS.containsKey(id);
    }
}
