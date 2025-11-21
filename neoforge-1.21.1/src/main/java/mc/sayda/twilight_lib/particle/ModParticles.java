package mc.sayda.twilight_lib.particle;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, TwilightLib.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BRONZE_HEART =
            PARTICLE_TYPES.register("bronze_heart", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SILVER_HEART =
            PARTICLE_TYPES.register("silver_heart", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GOLD_HEART =
            PARTICLE_TYPES.register("gold_heart", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLATINUM_HEART =
            PARTICLE_TYPES.register("platinum_heart", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RATVENOM =
            PARTICLE_TYPES.register("ratvenom", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SILENT_HONEY =
            PARTICLE_TYPES.register("silent_honey", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WOLF_PRINT =
            PARTICLE_TYPES.register("wolf_print", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}