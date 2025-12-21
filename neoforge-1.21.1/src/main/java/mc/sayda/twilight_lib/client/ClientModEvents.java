package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import org.slf4j.Logger;
import mc.sayda.twilight_lib.client.model.PlayerRigModel;
import mc.sayda.twilight_lib.client.model.addon.ChestArmorModel;
import mc.sayda.twilight_lib.client.renderer.CustomFoxRenderer;
import mc.sayda.twilight_lib.client.renderer.PlayerAddonLayer;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

// In 1.21.1, bus parameter is deprecated - MOD bus is implied for mod events
@EventBusSubscriber(modid = TwilightLib.MODID, value = Dist.CLIENT)
public class ClientModEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LOGGER.info("What's your name? Registering custom fox renderers...");
        List.of(
            ModEntities.WHITE_FOX,
            ModEntities.BLACK_FOX,
            ModEntities.BLUE_FOX,
            ModEntities.YELLOW_FOX,
            ModEntities.ORANGE_FOX,
            ModEntities.PURPLE_FOX,
            ModEntities.RED_FOX
        ).forEach(fox -> event.registerEntityRenderer(fox.get(), CustomFoxRenderer::new));
        LOGGER.info("Well, this is a pretty chill reality. Registered {} custom fox renderers.", 7);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LOGGER.info("What's your name? Registering model layer definitions...");
        event.registerLayerDefinition(PlayerRigModel.LAYER_LOCATION, PlayerRigModel::createBodyLayer);
        event.registerLayerDefinition(ChestArmorModel.LAYER_LOCATION, ChestArmorModel::createBodyLayer);

        // Register built-in addons before registering their layers
        try {
            mc.sayda.twilight_lib.addon.AddonInit.registerAddons();
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to register built-in addons", e);
        }

        // Register all addon layer definitions
        int addonCount = mc.sayda.twilight_lib.addon.AddonRegistry.getAllAddons().size();
        mc.sayda.twilight_lib.addon.AddonRegistry.getAllAddons().forEach(addon ->
            event.registerLayerDefinition(addon.layerLocation(), addon.layerDefinitionSupplier())
        );
        LOGGER.info("I like all these things around me! Registered {} addon layer definitions.", addonCount);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        LOGGER.info("What's your name? Adding PlayerAddonLayer to player renderers...");
        // Add PlayerAddonLayer to all player renderer skins
        int layersAdded = 0;
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new PlayerAddonLayer(playerRenderer));
                layersAdded++;
            }
        }
        LOGGER.info("Well, this is a pretty chill reality. Added PlayerAddonLayer to {} player skins.", layersAdded);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Come on, this is gonna be fun! Registering client event handlers...");
        // Register trail renderer to tick event
        NeoForge.EVENT_BUS.register(TrailRenderer.class);

        // Register spawn effect handler
        NeoForge.EVENT_BUS.register(mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler.class);

        // Register ambient effect handler
        NeoForge.EVENT_BUS.register(mc.sayda.twilight_lib.cosmetics.AmbientEffectHandler.class);
        LOGGER.info("Well, this is a pretty chill reality. Registered 3 cosmetic event handlers (trails, spawn effects, ambient effects).");
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        LOGGER.info("More sparkles, now! Registering particle providers...");
        event.registerSpriteSet(ModParticles.BRONZE_HEART.get(), BronzeHeartParticle::provider);
        event.registerSpriteSet(ModParticles.SILVER_HEART.get(), SilverHeartParticle::provider);
        event.registerSpriteSet(ModParticles.GOLD_HEART.get(), GoldHeartParticle::provider);
        event.registerSpriteSet(ModParticles.PLATINUM_HEART.get(), PlatinumHeartParticle::provider);
        event.registerSpriteSet(ModParticles.RATVENOM.get(), RatvenomParticle::provider);
        event.registerSpriteSet(ModParticles.SILENT_HONEY.get(), SilentHoneyParticle::provider);
        event.registerSpriteSet(ModParticles.WOLF_PRINT.get(), WolfPrintParticle::provider);
        LOGGER.info("Well, this is a pretty chill reality. Registered {} particle providers.", 7);
    }
}