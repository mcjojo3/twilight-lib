package mc.sayda.twilight_lib.particle;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModParticles {
        public static final dev.architectury.registry.registries.Registrar<ParticleType<?>> PARTICLE_TYPES = dev.architectury.registry.registries.RegistrarManager
                        .get(TwilightLib.MODID).get(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE);

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> BRONZE_HEART = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "bronze_heart"), () -> new SimpleParticleType(false) {
                                        });

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> SILVER_HEART = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "silver_heart"), () -> new SimpleParticleType(false) {
                                        });

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> GOLD_HEART = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "gold_heart"), () -> new SimpleParticleType(false) {
                                        });

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> PLATINUM_HEART = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "platinum_heart"), () -> new SimpleParticleType(false) {
                                        });

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> RATVENOM = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "ratvenom"), () -> new SimpleParticleType(false) {
                                        });

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> SILENT_HONEY = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "silent_honey"), () -> new SimpleParticleType(false) {
                                        });

        public static final dev.architectury.registry.registries.RegistrySupplier<SimpleParticleType> WOLF_PRINT = PARTICLE_TYPES
                        .register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID,
                                        "wolf_print"), () -> new SimpleParticleType(false) {
                                        });

        public static void register() {
                // Accessing the class to force static initializers to run during mod init
                PARTICLE_TYPES.getClass();
        }
}