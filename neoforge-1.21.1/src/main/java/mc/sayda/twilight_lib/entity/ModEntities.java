package mc.sayda.twilight_lib.entity;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, TwilightLib.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> WHITE_FOX = registerFox("white_fox", CustomFoxEntity.FoxColor.WHITE);
    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> BLACK_FOX = registerFox("black_fox", CustomFoxEntity.FoxColor.BLACK);
    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> BLUE_FOX = registerFox("blue_fox", CustomFoxEntity.FoxColor.BLUE);
    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> ORANGE_FOX = registerFox("orange_fox", CustomFoxEntity.FoxColor.ORANGE);
    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> YELLOW_FOX = registerFox("yellow_fox", CustomFoxEntity.FoxColor.YELLOW);
    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> PURPLE_FOX = registerFox("purple_fox", CustomFoxEntity.FoxColor.PURPLE);
    public static final DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> RED_FOX = registerFox("red_fox", CustomFoxEntity.FoxColor.RED);

    private static DeferredHolder<EntityType<?>, EntityType<CustomFoxEntity>> registerFox(String name, CustomFoxEntity.FoxColor color) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(
                (EntityType<CustomFoxEntity> type, Level level) -> new CustomFoxEntity(type, level, color),
                MobCategory.CREATURE)
                .sized(0.6F, 0.7F)
                .clientTrackingRange(8)
                .build(name));
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}