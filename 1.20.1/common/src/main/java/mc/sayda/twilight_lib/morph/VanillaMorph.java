package mc.sayda.twilight_lib.morph;

import mc.sayda.twilight_lib.api.morph.IMorph;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Default implementation of IMorph for standard Minecraft entity types.
 */
public class VanillaMorph implements IMorph {
    private final ResourceLocation id;
    private final EntityType<?> type;

    public VanillaMorph(ResourceLocation id) {
        this.id = id;
        this.type = BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public EntityType<?> getEntityType() {
        return type;
    }
}
