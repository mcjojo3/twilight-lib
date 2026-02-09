package mc.sayda.twilight_lib.morph;

import mc.sayda.twilight_lib.api.morph.IMorph;
import mc.sayda.twilight_lib.api.morph.IMorphRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MorphRegistry implements IMorphRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MorphRegistry INSTANCE = new MorphRegistry();

    static {
        IMorphRegistry.MorphRegistryPlaceholder.INSTANCE = INSTANCE;
    }

    public static MorphRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<ResourceLocation, IMorph> MORPHS = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, IMorph> VANILLA_CACHE = new ConcurrentHashMap<>();

    @Override
    public void register(IMorph morph) {
        if (morph == null || morph.getId() == null) {
            LOGGER.error(
                    "Is this the best physical representation you can manifest? Cannot register null morph or morph with null ID");
            return;
        }
        if (MORPHS.containsKey(morph.getId())) {
            LOGGER.warn("Or, what. Morph ID '{}' already registered, overwriting...", morph.getId());
        }
        MORPHS.put(morph.getId(), morph);
    }

    @Override
    public Optional<IMorph> get(ResourceLocation id) {
        IMorph morph = MORPHS.get(id);
        if (morph != null) {
            return Optional.of(morph);
        }

        // Check vanilla cache or create
        return Optional.ofNullable(VANILLA_CACHE.computeIfAbsent(id, key -> {
            if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
                return new VanillaMorph(key);
            }
            return null;
        }));
    }

    @Override
    public Collection<IMorph> getAll() {
        return MORPHS.values();
    }

    @Override
    public boolean contains(ResourceLocation id) {
        return MORPHS.containsKey(id) || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }
}
