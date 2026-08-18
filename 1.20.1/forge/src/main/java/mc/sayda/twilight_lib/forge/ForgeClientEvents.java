package mc.sayda.twilight_lib.forge;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.addon.AddonInit;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.client.model.PlayerRigModel;
import mc.sayda.twilight_lib.client.model.addon.ChestArmorModel;
import mc.sayda.twilight_lib.client.renderer.CustomFoxRenderer;
import mc.sayda.twilight_lib.client.renderer.PlayerAddonLayer;
import mc.sayda.twilight_lib.entity.ModEntities;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = TwilightLib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientEvents {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Explicitly add PlayerAddonLayer to both "default" (wide) and "slim" player
        // skins
        addLayerToSkin(event, "default");
        addLayerToSkin(event, "slim");
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        TwilightLib.LOGGER.info("Twilight Lib: Manually registering model layer definitions for Forge...");

        // Ensure addons are registered before their layers are needed
        AddonInit.registerAddons();

        event.registerLayerDefinition(PlayerRigModel.LAYER_LOCATION, PlayerRigModel::createBodyLayer);
        event.registerLayerDefinition(ChestArmorModel.LAYER_LOCATION, ChestArmorModel::createBodyLayer);

        // Register all addon layer definitions
        AddonRegistry.getAllRegisteredAddons().forEach(addon -> {
            try {
                event.registerLayerDefinition(addon.layerLocation(), addon.layerDefinitionSupplier());
            } catch (Exception e) {
                mc.sayda.twilight_lib.TwilightLib.LOGGER
                        .error("Failed to register layer definition for addon: " + addon.id(), e);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        mc.sayda.twilight_lib.TwilightLib.LOGGER.info("Twilight Lib: Manually registering renderers for Forge...");
        List.of(
                ModEntities.WHITE_FOX,
                ModEntities.BLACK_FOX,
                ModEntities.BLUE_FOX,
                ModEntities.YELLOW_FOX,
                ModEntities.ORANGE_FOX,
                ModEntities.PURPLE_FOX,
                ModEntities.RED_FOX,
                ModEntities.GRAY_FOX).forEach(fox -> event.registerEntityRenderer(fox.get(), CustomFoxRenderer::new));
    }

    private static void addLayerToSkin(EntityRenderersEvent.AddLayers event, String skinName) {
        try {
            PlayerRenderer renderer = event.getSkin(skinName);
            if (renderer != null) {
                renderer.addLayer(new PlayerAddonLayer(renderer));
                TwilightLib.LOGGER.info("Twilight Lib: Successfully added PlayerAddonLayer to {} skin", skinName);
            } else {
                TwilightLib.LOGGER.error("Twilight Lib: Failed to retrieve renderer for skin: {}", skinName);
            }
        } catch (Exception e) {
            TwilightLib.LOGGER.error("Twilight Lib: Exception adding layer to skin " + skinName, e);
        }
    }
}
