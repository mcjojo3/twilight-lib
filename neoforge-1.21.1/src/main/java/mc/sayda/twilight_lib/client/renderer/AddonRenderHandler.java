package mc.sayda.twilight_lib.client.renderer;

import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Handles player model visibility for addons that want to hide the base player model
 */
@EventBusSubscriber(modid = mc.sayda.twilight_lib.TwilightLib.MODID, value = Dist.CLIENT)
public class AddonRenderHandler {

    public static void register() { /* no-op - static subscriber */ }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre evt) {
        Player player = evt.getEntity();

        // Don't modify rendering for invisible players
        if (player.isInvisible()) {
            return;
        }

        // Check if any active addon wants to hide the player model
        var addons = player.getData(ModAttachments.ADDONS);
        if (addons == null) {
            return; // Safe early exit if no addon data
        }
        boolean shouldHidePlayerModel = addons.getActiveAddons().stream()
                .anyMatch(addonId -> AddonRegistry.getAddon(addonId)
                        .map(info -> info.hidePlayerModel())
                        .orElse(false));

        if (shouldHidePlayerModel) {
            // Hide all player model parts by making them invisible
            var playerModel = evt.getRenderer().getModel();
            playerModel.head.visible = false;
            playerModel.hat.visible = false;
            playerModel.body.visible = false;
            playerModel.rightArm.visible = false;
            playerModel.leftArm.visible = false;
            playerModel.rightLeg.visible = false;
            playerModel.leftLeg.visible = false;
            playerModel.rightSleeve.visible = false;
            playerModel.leftSleeve.visible = false;
            playerModel.rightPants.visible = false;
            playerModel.leftPants.visible = false;
            playerModel.jacket.visible = false;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post evt) {
        // Always restore visibility after rendering (in case it was modified)
        var playerModel = evt.getRenderer().getModel();
        playerModel.head.visible = true;
        playerModel.hat.visible = true;
        playerModel.body.visible = true;
        playerModel.rightArm.visible = true;
        playerModel.leftArm.visible = true;
        playerModel.rightLeg.visible = true;
        playerModel.leftLeg.visible = true;
        playerModel.rightSleeve.visible = true;
        playerModel.leftSleeve.visible = true;
        playerModel.rightPants.visible = true;
        playerModel.leftPants.visible = true;
        playerModel.jacket.visible = true;
    }
}
