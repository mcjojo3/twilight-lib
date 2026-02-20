package mc.sayda.twilight_lib.fabric;

import net.fabricmc.api.ClientModInitializer;
import mc.sayda.twilight_lib.client.TwilightLibClient;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import mc.sayda.twilight_lib.particle.ModParticles;
import mc.sayda.twilight_lib.particle.BronzeHeartParticle;
import mc.sayda.twilight_lib.particle.SilverHeartParticle;
import mc.sayda.twilight_lib.particle.GoldHeartParticle;
import mc.sayda.twilight_lib.particle.PlatinumHeartParticle;
import mc.sayda.twilight_lib.particle.RatvenomParticle;
import mc.sayda.twilight_lib.particle.SilentHoneyParticle;
import mc.sayda.twilight_lib.particle.WolfPrintParticle;

public class TwilightLibFabricClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
                TwilightLibClient.init();

                // Native Fabric particle registration (fallback for Architectury)
                ParticleFactoryRegistry.getInstance().register(ModParticles.BRONZE_HEART.get(),
                                BronzeHeartParticle.Provider::new);
                ParticleFactoryRegistry.getInstance().register(ModParticles.SILVER_HEART.get(),
                                SilverHeartParticle.Provider::new);
                ParticleFactoryRegistry.getInstance().register(ModParticles.GOLD_HEART.get(),
                                GoldHeartParticle.Provider::new);
                ParticleFactoryRegistry.getInstance().register(ModParticles.PLATINUM_HEART.get(),
                                PlatinumHeartParticle.Provider::new);
                ParticleFactoryRegistry.getInstance().register(ModParticles.RATVENOM.get(),
                                RatvenomParticle.Provider::new);
                ParticleFactoryRegistry.getInstance().register(ModParticles.SILENT_HONEY.get(),
                                SilentHoneyParticle.Provider::new);
                ParticleFactoryRegistry.getInstance().register(ModParticles.WOLF_PRINT.get(),
                                WolfPrintParticle.Provider::new);
        }
}
