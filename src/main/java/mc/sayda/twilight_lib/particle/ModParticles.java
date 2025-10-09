package mc.sayda.twilight_lib.particle;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TwilightLib.MODID);

    public static final RegistryObject<SimpleParticleType> BRONZE_HEART =
            PARTICLE_TYPES.register("bronze_heart", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SILVER_HEART =
            PARTICLE_TYPES.register("silver_heart", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> GOLD_HEART =
            PARTICLE_TYPES.register("gold_heart", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> RATVENOM =
            PARTICLE_TYPES.register("ratvenom", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}