package mc.sayda.twilight_lib.addon;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Enum representing player model body parts that can be hidden by addons.
 *
 * <p>This allows fine-grained control over which parts of the player model are visible
 * when certain addons are equipped. Multiple addons can hide the same parts without conflicts
 * - the parts are simply hidden if ANY active addon requests it.
 *
 * <p>The {@code *_ARMOR} entries hide the vanilla armor overlay for that slot instead of a
 * model part. The armor is still worn (and still protects), it just isn't drawn by the
 * vanilla armor layer - useful when an addon draws that armor itself, like the mermaid tail
 * drawing leggings on its tail and boots on its fin.
 *
 * <p><b>Examples:</b>
 * <ul>
 *   <li>A full-head helmet addon might hide HEAD and HAT</li>
 *   <li>A mask addon might hide only HEAD</li>
 *   <li>Full-body replacement addons might hide all parts except nametag/shadow</li>
 *   <li>A tail that replaces the legs might hide the legs plus LEGGINGS_ARMOR and BOOTS_ARMOR</li>
 * </ul>
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.1
 */
public enum BodyPart {
    /**
     * Main head (first layer) - includes face, hair, etc.
     */
    HEAD,

    /**
     * Hat/overlay layer (second layer) - typically used for custom skins
     */
    HAT,

    /**
     * Main body/torso (first layer)
     */
    BODY,

    /**
     * Left arm (first layer)
     */
    LEFT_ARM,

    /**
     * Right arm (first layer)
     */
    RIGHT_ARM,

    /**
     * Left leg (first layer)
     */
    LEFT_LEG,

    /**
     * Right leg (first layer)
     */
    RIGHT_LEG,

    /**
     * Left arm sleeve/overlay (second layer)
     */
    LEFT_SLEEVE,

    /**
     * Right arm sleeve/overlay (second layer)
     */
    RIGHT_SLEEVE,

    /**
     * Left leg pants/overlay (second layer)
     */
    LEFT_PANTS,

    /**
     * Right leg pants/overlay (second layer)
     */
    RIGHT_PANTS,

    /**
     * Body jacket/overlay (second layer)
     */
    JACKET,

    /**
     * Vanilla helmet overlay
     */
    HELMET_ARMOR(EquipmentSlot.HEAD),

    /**
     * Vanilla chestplate overlay
     */
    CHESTPLATE_ARMOR(EquipmentSlot.CHEST),

    /**
     * Vanilla leggings overlay
     */
    LEGGINGS_ARMOR(EquipmentSlot.LEGS),

    /**
     * Vanilla boots overlay
     */
    BOOTS_ARMOR(EquipmentSlot.FEET);

    private final EquipmentSlot armorSlot;

    BodyPart() {
        this(null);
    }

    BodyPart(EquipmentSlot armorSlot) {
        this.armorSlot = armorSlot;
    }

    /**
     * @return The armor slot whose vanilla overlay this entry hides, or null if this entry
     *         hides a model part instead.
     */
    public EquipmentSlot getArmorSlot() {
        return armorSlot;
    }
}
