package mc.sayda.twilight_lib.addon;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Player model parts an addon can hide. The *_ARMOR entries hide that slot's
 * vanilla armor instead, so the addon can draw it itself.
 *
 * @author SaydaGames (mc_jojo3)
 */
public enum BodyPart {
    /** Head, including the hat layer */
    HEAD,

    /** Hat layer only */
    HAT,

    /** Torso, including the jacket layer */
    BODY,

    /** Left arm, including the sleeve */
    LEFT_ARM,

    /** Right arm, including the sleeve */
    RIGHT_ARM,

    /** Left leg, including the pants layer */
    LEFT_LEG,

    /** Right leg, including the pants layer */
    RIGHT_LEG,

    /** Left sleeve layer only */
    LEFT_SLEEVE,

    /** Right sleeve layer only */
    RIGHT_SLEEVE,

    /** Left pants layer only */
    LEFT_PANTS,

    /** Right pants layer only */
    RIGHT_PANTS,

    /** Jacket layer only */
    JACKET,

    HELMET_ARMOR(EquipmentSlot.HEAD),
    CHESTPLATE_ARMOR(EquipmentSlot.CHEST),
    LEGGINGS_ARMOR(EquipmentSlot.LEGS),
    BOOTS_ARMOR(EquipmentSlot.FEET);

    private final EquipmentSlot armorSlot;

    BodyPart() {
        this(null);
    }

    BodyPart(EquipmentSlot armorSlot) {
        this.armorSlot = armorSlot;
    }

    /** The armor slot this entry hides, or null for model parts. */
    public EquipmentSlot getArmorSlot() {
        return armorSlot;
    }
}
