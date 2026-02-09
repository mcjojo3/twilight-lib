package mc.sayda.twilight_lib.neoforge;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import mc.sayda.twilight_lib.client.renderer.CustomFoxRenderer;
import mc.sayda.twilight_lib.client.model.PlayerRigModel;
import mc.sayda.twilight_lib.client.model.addon.ChestArmorModel;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.entity.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.model.geom.ModelLayerLocation;

@EventBusSubscriber(modid = TwilightLib.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoForgeClientEvents {

    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        // TwilightLibClient.init() moved to mod constructor for earlier registration
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        TwilightLib.LOGGER.info("Twilight Lib NeoForge: Registering entity renderers natively...");
        event.registerEntityRenderer(ModEntities.WHITE_FOX.get(), CustomFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_FOX.get(), CustomFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.BLUE_FOX.get(), CustomFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.YELLOW_FOX.get(), CustomFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.ORANGE_FOX.get(), CustomFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.PURPLE_FOX.get(), CustomFoxRenderer::new);
        event.registerEntityRenderer(ModEntities.RED_FOX.get(), CustomFoxRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        TwilightLib.LOGGER.info("Twilight Lib NeoForge: Registering model layer definitions natively...");
        event.registerLayerDefinition(PlayerRigModel.LAYER_LOCATION, PlayerRigModel::createBodyLayer);
        event.registerLayerDefinition(ChestArmorModel.LAYER_LOCATION, ChestArmorModel::createBodyLayer);

        Set<net.minecraft.client.model.geom.ModelLayerLocation> registeredLayers = new HashSet<>();
        AddonRegistry.getAllRegisteredAddons().forEach(addon -> {
            if (registeredLayers.add(addon.layerLocation())) {
                event.registerLayerDefinition(addon.layerLocation(), addon.layerDefinitionSupplier());
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        TwilightLib.LOGGER.info("Twilight Lib NeoForge: Registering particle providers natively...");

        event.registerSpriteSet(ModParticles.BRONZE_HEART.get(), BronzeHeartParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SILVER_HEART.get(), SilverHeartParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GOLD_HEART.get(), GoldHeartParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PLATINUM_HEART.get(), PlatinumHeartParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RATVENOM.get(), RatvenomParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SILENT_HONEY.get(), SilentHoneyParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WOLF_PRINT.get(), WolfPrintParticle.Provider::new);

        TwilightLib.LOGGER.info("Twilight Lib NeoForge: Native particle provider registration complete.");
    }
}
