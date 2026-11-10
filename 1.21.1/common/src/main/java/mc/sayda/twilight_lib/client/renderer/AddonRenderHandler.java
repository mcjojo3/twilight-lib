package mc.sayda.twilight_lib.client.renderer;

import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.addon.BodyPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/** Hides the player model parts and armor that active addons ask to hide. */
public class AddonRenderHandler {
    /** Armor slots hidden for the player being rendered, read by HumanoidArmorLayerMixin. */
    private static final ThreadLocal<HiddenArmor> HIDDEN_ARMOR = new ThreadLocal<>();

    private record HiddenArmor(Object parentModel, Set<EquipmentSlot> slots) {
    }

    public static void init() {
        // Registered via AddonRenderMixin in common
    }

    /** Whether the armor layer for parentModel should skip this slot. */
    public static boolean isArmorSlotHidden(Object parentModel, EquipmentSlot slot) {
        HiddenArmor hidden = HIDDEN_ARMOR.get();
        return hidden != null && hidden.parentModel() == parentModel && hidden.slots().contains(slot);
    }

    public static void onRenderPlayerPre(Player player, PlayerRenderer renderer) {
        // Clear leftovers from a render that never reached Post
        HIDDEN_ARMOR.remove();

        // Don't modify rendering for invisible players
        if (player.isInvisible()) {
            return;
        }

        var playerModel = renderer.getModel();

        // Collect all body parts that should be hidden from ALL active addons
        Set<BodyPart> allHiddenParts = new HashSet<>();
        boolean hideEntireModel = false;

        var addons = mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
        if (addons != null) {
            // Single loop to collect both hidden parts AND check hidePlayerModel flag
            for (String addonIdString : addons.getActiveAddons()) {
                ResourceLocation addonId = ResourceLocation.tryParse(addonIdString);
                if (addonId == null) {
                    addonId = ResourceLocation.fromNamespaceAndPath("twilight_lib", addonIdString.toLowerCase());
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

        // Armor to skip, even when the whole model is hidden
        Set<EquipmentSlot> hiddenArmorSlots = EnumSet.noneOf(EquipmentSlot.class);
        for (BodyPart part : allHiddenParts) {
            if (part.getArmorSlot() != null) {
                hiddenArmorSlots.add(part.getArmorSlot());
            }
        }
        if (!hiddenArmorSlots.isEmpty()) {
            HIDDEN_ARMOR.set(new HiddenArmor(playerModel, hiddenArmorSlots));
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
                    case HELMET_ARMOR, CHESTPLATE_ARMOR, LEGGINGS_ARMOR, BOOTS_ARMOR -> {
                        // Armor overlays are skipped by HumanoidArmorLayerMixin, not here
                    }
                }
            }
        }
    }

    public static void onRenderPlayerPost(PlayerRenderer renderer) {
        HIDDEN_ARMOR.remove();

        // Always restore visibility after rendering (in case it was modified)
        var playerModel = renderer.getModel();
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
