package mc.sayda.twilight_lib.client.renderer;

import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.addon.BodyPart;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles player model visibility for addons that want to hide the base player model or specific body parts.
 *
 * <p><b>Hiding Strategy:</b>
 * <ul>
 *   <li><b>hidePlayerModel</b>: Legacy system - hides ALL parts (backward compatible)</li>
 *   <li><b>hiddenBodyParts</b>: New system - hides specific parts (fine-grained control)</li>
 *   <li><b>Stacking</b>: Multiple addons can hide the same part - it's simply hidden if ANY addon requests it</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = mc.sayda.twilight_lib.TwilightLib.MODID, value = Dist.CLIENT)
public class AddonRenderHandler {

    public static void register() { /* no-op - static subscriber */ }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre evt) {
        Player player = evt.getEntity();

        // Don't modify rendering for invisible players
        if (player.isInvisible()) {
            return;
        }

        var playerModel = evt.getRenderer().getModel();

        // Collect all body parts that should be hidden from ALL active addons
        Set<BodyPart> allHiddenParts = new HashSet<>();
        boolean hideEntireModel = false;

        // Single capability access to collect both hidden parts AND check hidePlayerModel flag
        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            for (String addonId : addons.getActiveAddons()) {
                AddonRegistry.getAddon(addonId).ifPresent(info -> {
                    // Collect specific hidden body parts
                    allHiddenParts.addAll(info.hiddenBodyParts());

                    // Check if this addon wants to hide entire model (legacy system)
                    if (info.hidePlayerModel()) {
                        // Can't directly set hideEntireModel here due to lambda limitations
                        // Instead, we'll use a marker: add a special sentinel value
                        allHiddenParts.add(null);  // Use null as a sentinel for hideEntireModel
                    }
                });
            }
        });

        // Check if hideEntireModel was requested (sentinel value present)
        hideEntireModel = allHiddenParts.remove(null);  // Remove and return true if present

        // Hide the entire model if requested (legacy system)
        if (hideEntireModel) {
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
        } else {
            // Hide specific parts based on hiddenBodyParts (new system)
            for (BodyPart part : allHiddenParts) {
                switch (part) {
                    case HEAD -> playerModel.head.visible = false;
                    case HAT -> playerModel.hat.visible = false;
                    case BODY -> playerModel.body.visible = false;
                    case LEFT_ARM -> playerModel.leftArm.visible = false;
                    case RIGHT_ARM -> playerModel.rightArm.visible = false;
                    case LEFT_LEG -> playerModel.leftLeg.visible = false;
                    case RIGHT_LEG -> playerModel.rightLeg.visible = false;
                    case LEFT_SLEEVE -> playerModel.leftSleeve.visible = false;
                    case RIGHT_SLEEVE -> playerModel.rightSleeve.visible = false;
                    case LEFT_PANTS -> playerModel.leftPants.visible = false;
                    case RIGHT_PANTS -> playerModel.rightPants.visible = false;
                    case JACKET -> playerModel.jacket.visible = false;
                }
            }
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
