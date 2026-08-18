package mc.sayda.twilight_lib.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import mc.sayda.twilight_lib.client.model.PlayerRigModel;
import mc.sayda.twilight_lib.client.model.addon.ChestArmorModel;
import mc.sayda.twilight_lib.client.renderer.CustomFoxRenderer;
import mc.sayda.twilight_lib.cosmetics.AmbientEffectHandler;
import mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler;
import mc.sayda.twilight_lib.cosmetics.TrailRenderer;
import mc.sayda.twilight_lib.entity.ModEntities;
import mc.sayda.twilight_lib.particle.BronzeHeartParticle;
import mc.sayda.twilight_lib.particle.GoldHeartParticle;
import mc.sayda.twilight_lib.particle.ModParticles;
import mc.sayda.twilight_lib.particle.PlatinumHeartParticle;
import mc.sayda.twilight_lib.particle.RatvenomParticle;
import mc.sayda.twilight_lib.particle.SilentHoneyParticle;
import mc.sayda.twilight_lib.particle.SilverHeartParticle;
import mc.sayda.twilight_lib.particle.WolfPrintParticle;

public class TwilightLibClient {

    public static void init() {
        mc.sayda.twilight_lib.TwilightLib.LOGGER.info("Yes! This'll be fun! Right? Client Initializing...");

        boolean isNeoForge = dev.architectury.platform.Platform.isModLoaded("neoforge");

        if (!isNeoForge) {
            // Register Renderers
            EntityRendererRegistry.register(ModEntities.WHITE_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.BLACK_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.BLUE_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.YELLOW_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.ORANGE_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.PURPLE_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.RED_FOX, CustomFoxRenderer::new);
            EntityRendererRegistry.register(ModEntities.GRAY_FOX, CustomFoxRenderer::new);

            // Register Layer Definitions
            EntityModelLayerRegistry.register(PlayerRigModel.LAYER_LOCATION, PlayerRigModel::createBodyLayer);
            EntityModelLayerRegistry.register(ChestArmorModel.LAYER_LOCATION, ChestArmorModel::createBodyLayer);
        }

        // Addons
        mc.sayda.twilight_lib.addon.AddonInit.registerAddons();

        if (!isNeoForge) {
            java.util.Set<net.minecraft.client.model.geom.ModelLayerLocation> registeredLayers = new java.util.HashSet<>();
            mc.sayda.twilight_lib.addon.AddonRegistry.getAllRegisteredAddons().forEach(addon -> {
                if (registeredLayers.add(addon.layerLocation())) {
                    EntityModelLayerRegistry.register(addon.layerLocation(), addon.layerDefinitionSupplier());
                }
            });

            // Particles
            ParticleProviderRegistry.register(ModParticles.BRONZE_HEART, BronzeHeartParticle.Provider::new);
            ParticleProviderRegistry.register(ModParticles.SILVER_HEART, SilverHeartParticle.Provider::new);
            ParticleProviderRegistry.register(ModParticles.GOLD_HEART, GoldHeartParticle.Provider::new);
            ParticleProviderRegistry.register(ModParticles.PLATINUM_HEART, PlatinumHeartParticle.Provider::new);
            ParticleProviderRegistry.register(ModParticles.RATVENOM, RatvenomParticle.Provider::new);
            ParticleProviderRegistry.register(ModParticles.SILENT_HONEY, SilentHoneyParticle.Provider::new);
            ParticleProviderRegistry.register(ModParticles.WOLF_PRINT, WolfPrintParticle.Provider::new);
        }

        // Event Handlers
        TrailRenderer.init();
        SpawnEffectHandler.init();
        AmbientEffectHandler.init();
        mc.sayda.twilight_lib.client.renderer.MorphRenderHandler.init();
        ClientModelVariantCache.init();

        // Client Tick (Animations & Cleanup)
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST
                .register(mc.sayda.twilight_lib.client.renderer.MorphRenderHandler::onClientTick);

        // Cleanup on Logout/Unload
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            mc.sayda.twilight_lib.client.renderer.MorphRenderHandler.onClientDisconnect();
            mc.sayda.twilight_lib.client.ClientModelVariantCache.clear();
            mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler.onClientDisconnect();
            mc.sayda.twilight_lib.cosmetics.TrailRenderer.onClientDisconnect();
        });
    }
}
