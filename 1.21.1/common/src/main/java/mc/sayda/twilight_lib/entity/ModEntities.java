package mc.sayda.twilight_lib.entity;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

public class ModEntities {
        public static final dev.architectury.registry.registries.Registrar<EntityType<?>> ENTITIES = dev.architectury.registry.registries.RegistrarManager
                        .get(TwilightLib.MODID).get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE);

        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> WHITE_FOX = registerFox(
                        "white_fox", CustomFoxEntity.FoxColor.WHITE);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> BLACK_FOX = registerFox(
                        "black_fox", CustomFoxEntity.FoxColor.BLACK);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> BLUE_FOX = registerFox(
                        "blue_fox", CustomFoxEntity.FoxColor.BLUE);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> ORANGE_FOX = registerFox(
                        "orange_fox", CustomFoxEntity.FoxColor.ORANGE);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> YELLOW_FOX = registerFox(
                        "yellow_fox", CustomFoxEntity.FoxColor.YELLOW);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> PURPLE_FOX = registerFox(
                        "purple_fox", CustomFoxEntity.FoxColor.PURPLE);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> RED_FOX = registerFox(
                        "red_fox", CustomFoxEntity.FoxColor.RED);
        public static final dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> GRAY_FOX = registerFox(
                        "gray_fox", CustomFoxEntity.FoxColor.GRAY);

        private static dev.architectury.registry.registries.RegistrySupplier<EntityType<CustomFoxEntity>> registerFox(
                        String name, CustomFoxEntity.FoxColor color) {
                return ENTITIES.register(
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, name),
                                () -> EntityType.Builder.of(
                                                (EntityType<CustomFoxEntity> type,
                                                                Level level) -> new CustomFoxEntity(type, level, color),
                                                MobCategory.CREATURE)
                                                .sized(0.6F, 0.7F)
                                                .clientTrackingRange(8)
                                                .build(""));
        }

        public static void register() {
                // Accessing the class to force static initializers to run during mod init
                ENTITIES.getClass();
        }
}