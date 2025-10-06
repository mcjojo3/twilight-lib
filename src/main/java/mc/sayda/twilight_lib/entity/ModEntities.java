package mc.sayda.twilight_lib.entity;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TwilightLib.MODID);

    public static final RegistryObject<EntityType<CustomFoxEntity>> WHITE_FOX = registerFox("white_fox", CustomFoxEntity.FoxColor.WHITE);
    public static final RegistryObject<EntityType<CustomFoxEntity>> BLACK_FOX = registerFox("black_fox", CustomFoxEntity.FoxColor.BLACK);
    public static final RegistryObject<EntityType<CustomFoxEntity>> BLUE_FOX = registerFox("blue_fox", CustomFoxEntity.FoxColor.BLUE);
    public static final RegistryObject<EntityType<CustomFoxEntity>> ORANGE_FOX = registerFox("orange_fox", CustomFoxEntity.FoxColor.ORANGE);
    public static final RegistryObject<EntityType<CustomFoxEntity>> GOLDEN_FOX = registerFox("golden_fox", CustomFoxEntity.FoxColor.GOLDEN);
    public static final RegistryObject<EntityType<CustomFoxEntity>> PURPLE_FOX = registerFox("purple_fox", CustomFoxEntity.FoxColor.PURPLE);
    public static final RegistryObject<EntityType<CustomFoxEntity>> RED_FOX = registerFox("red_fox", CustomFoxEntity.FoxColor.RED);

    private static RegistryObject<EntityType<CustomFoxEntity>> registerFox(String name, CustomFoxEntity.FoxColor color) {
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