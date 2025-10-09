package mc.sayda.twilight_lib.client;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.client.model.PlayerRigModel;
import mc.sayda.twilight_lib.client.renderer.CustomFoxRenderer;
import mc.sayda.twilight_lib.client.renderer.PlayerAddonLayer;
import mc.sayda.twilight_lib.cosmetics.TrailRenderer;
import mc.sayda.twilight_lib.entity.ModEntities;
import mc.sayda.twilight_lib.particle.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = TwilightLib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        List.of(
            ModEntities.WHITE_FOX,
            ModEntities.BLACK_FOX,
            ModEntities.BLUE_FOX,
            ModEntities.YELLOW_FOX,
            ModEntities.ORANGE_FOX,
            ModEntities.PURPLE_FOX,
            ModEntities.RED_FOX
        ).forEach(fox -> event.registerEntityRenderer(fox.get(), CustomFoxRenderer::new));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlayerRigModel.LAYER_LOCATION, PlayerRigModel::createBodyLayer);

        // Register built-in addons before registering their layers
        mc.sayda.twilight_lib.addon.AddonInit.registerAddons();

        // Register all addon layer definitions
        mc.sayda.twilight_lib.addon.AddonRegistry.getAllAddons().forEach(addon ->
            event.registerLayerDefinition(addon.layerLocation(), addon.layerDefinitionSupplier())
        );
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Add PlayerAddonLayer to all player renderer skins
        for (String skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new PlayerAddonLayer(playerRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register trail renderer to tick event
        MinecraftForge.EVENT_BUS.register(TrailRenderer.class);

        // Register respawn effect handler
        MinecraftForge.EVENT_BUS.register(mc.sayda.twilight_lib.cosmetics.RespawnEffectHandler.class);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BRONZE_HEART.get(), BronzeHeartParticle::provider);
        event.registerSpriteSet(ModParticles.SILVER_HEART.get(), SilverHeartParticle::provider);
        event.registerSpriteSet(ModParticles.GOLD_HEART.get(), GoldHeartParticle::provider);
        event.registerSpriteSet(ModParticles.RATVENOM.get(), RatvenomParticle::provider);
    }
}