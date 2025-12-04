package mc.sayda.twilight_lib.addon;

/**
 * Enum representing player model body parts that can be hidden by addons.
 *
 * <p>This allows fine-grained control over which parts of the player model are visible
 * when certain addons are equipped. Multiple addons can hide the same parts without conflicts
 * - the parts are simply hidden if ANY active addon requests it.
 *
 * <p><b>Examples:</b>
 * <ul>
 *   <li>A full-head helmet addon might hide HEAD and HAT</li>
 *   <li>A mask addon might hide only HEAD</li>
 *   <li>Full-body replacement addons might hide all parts except nametag/shadow</li>
 * </ul>
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
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
    JACKET
}
