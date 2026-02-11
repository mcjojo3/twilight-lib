package mc.sayda.twilight_lib.client.renderer;

import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.addon.BodyPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Handles player model visibility for addons that want to hide the base player
 * model or specific body parts.
 *
 * <p>
 * <b>Hiding Strategy:</b>
 * <ul>
 * <li><b>hidePlayerModel</b>: Legacy system - hides ALL parts (backward
 * compatible)</li>
 * <li><b>hiddenBodyParts</b>: New system - hides specific parts (fine-grained
 * control)</li>
 * <li><b>Stacking</b>: Multiple addons can hide the same part - it's simply
 * hidden if ANY addon requests it</li>
 * </ul>
 */
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public class AddonRenderHandler {
    // ThreadLocal prevents race conditions if multiple render threads are used
    // (unlikely but safe)
    // and WeakHashMap handles multiple renderer instances (default/slim)
    private static final ThreadLocal<Map<PlayerRenderer, VisibilityState>> STASH = ThreadLocal
            .withInitial(WeakHashMap::new);

    public static void init() {
        // Registered via AddonRenderMixin in common
    }

    public static void onRenderPlayerPre(Player player, PlayerRenderer renderer) {
        // Don't modify rendering for invisible players
        if (player.isInvisible()) {
            return;
        }

        var playerModel = renderer.getModel();

        // Stash current visibility state to restore it accurately in Post
        STASH.get().put(renderer, new VisibilityState(playerModel));

        // Collect all body parts that should be hidden from ALL active addons
        Set<BodyPart> allHiddenParts = new HashSet<>();
        boolean hideEntireModel = false;

        var addons = mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
        if (addons != null) {
            // Single loop to collect both hidden parts AND check hidePlayerModel flag
            for (String addonIdString : addons.getActiveAddons()) {
                ResourceLocation addonId = ResourceLocation.tryParse(addonIdString);
                if (addonId == null) {
                    addonId = new ResourceLocation("twilight_lib", addonIdString.toLowerCase());
                }

                AddonRegistry.getInstance().get(addonId).ifPresent(addon -> {
                    // Collect specific hidden body parts
                    allHiddenParts.addAll(addon.getHiddenBodyParts());

                    // Check if this addon wants to hide entire model (legacy system)
                    if (addon.hidesPlayerModel()) {
                        // Can't directly set hideEntireModel here due to lambda limitations
                        // Instead, we'll use a marker: add a special sentinel value
                        allHiddenParts.add(null); // Use null as a sentinel for hideEntireModel
                    }
                });
            }

            // Check if hideEntireModel was requested (sentinel value present)
            hideEntireModel = allHiddenParts.remove(null); // Remove and return true if present
        }

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
                    case HEAD -> {
                        playerModel.head.visible = false;
                        playerModel.hat.visible = false;
                    }
                    case HAT -> playerModel.hat.visible = false;
                    case BODY -> {
                        playerModel.body.visible = false;
                        playerModel.jacket.visible = false;
                    }
                    case JACKET -> playerModel.jacket.visible = false;
                    case LEFT_ARM -> {
                        playerModel.leftArm.visible = false;
                        playerModel.leftSleeve.visible = false;
                    }
                    case LEFT_SLEEVE -> playerModel.leftSleeve.visible = false;
                    case RIGHT_ARM -> {
                        playerModel.rightArm.visible = false;
                        playerModel.rightSleeve.visible = false;
                    }
                    case RIGHT_SLEEVE -> playerModel.rightSleeve.visible = false;
                    case LEFT_LEG -> {
                        playerModel.leftLeg.visible = false;
                        playerModel.leftPants.visible = false;
                    }
                    case LEFT_PANTS -> playerModel.leftPants.visible = false;
                    case RIGHT_LEG -> {
                        playerModel.rightLeg.visible = false;
                        playerModel.rightPants.visible = false;
                    }
                    case RIGHT_PANTS -> playerModel.rightPants.visible = false;
                }
            }
        }
    }

    public static void onRenderPlayerPost(PlayerRenderer renderer) {
        // Restore previous visibility state captured in Pre
        VisibilityState stashed = STASH.get().remove(renderer);
        if (stashed != null) {
            stashed.apply(renderer.getModel());
        }
    }

    /**
     * Captures the visibility state of all player model parts.
     */
    private static class VisibilityState {
        final boolean head, hat, body, rightArm, leftArm, rightLeg, leftLeg;
        final boolean rightSleeve, leftSleeve, rightPants, leftPants, jacket;

        VisibilityState(net.minecraft.client.model.PlayerModel<?> model) {
            this.head = model.head.visible;
            this.hat = model.hat.visible;
            this.body = model.body.visible;
            this.rightArm = model.rightArm.visible;
            this.leftArm = model.leftArm.visible;
            this.rightLeg = model.rightLeg.visible;
            this.leftLeg = model.leftLeg.visible;
            this.rightSleeve = model.rightSleeve.visible;
            this.leftSleeve = model.leftSleeve.visible;
            this.rightPants = model.rightPants.visible;
            this.leftPants = model.leftPants.visible;
            this.jacket = model.jacket.visible;
        }

        void apply(net.minecraft.client.model.PlayerModel<?> model) {
            model.head.visible = head;
            model.hat.visible = hat;
            model.body.visible = body;
            model.rightArm.visible = rightArm;
            model.leftArm.visible = leftArm;
            model.rightLeg.visible = rightLeg;
            model.leftLeg.visible = leftLeg;
            model.rightSleeve.visible = rightSleeve;
            model.leftSleeve.visible = leftSleeve;
            model.rightPants.visible = rightPants;
            model.leftPants.visible = leftPants;
            model.jacket.visible = jacket;
        }
    }
}
