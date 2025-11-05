package mc.sayda.twilight_lib.cosmetics;

/**
 * Defines how trail particles should spawn for different trail types.
 * This allows fine-grained control over particle behavior.
 */
public enum TrailSpawnMode {
    /**
     * Standard trail that spawns particles when the player is moving.
     * - Requires horizontal movement above MIN_HORIZONTAL_SPEED
     * - Spawns particles in a random spread around the player
     * - Works in air and on ground
     * - Example: HEARTS, SPARKLES, CHERRY_BLOSSOM
     */
    MOVEMENT,

    /**
     * Footprint trail that spawns alternating left/right when walking on the ground.
     * - Requires horizontal movement above MIN_HORIZONTAL_SPEED
     * - ONLY spawns when player is on the ground (isOnGround())
     * - Alternates between left and right foot placement
     * - Uses movement direction for particle rotation/orientation
     * - Example: WOLF_PRINTS, player footsteps
     */
    FOOTPRINT,

    /**
     * Continuous trail that spawns particles constantly, regardless of movement.
     * - Does NOT require movement
     * - Spawns even when standing still
     * - Spawns both in air and on ground
     * - Useful for ambient effects like auras, flies, or magical effects
     * - Example: Aura effects, ambient particles
     */
    CONTINUOUS,

    /**
     * Grounded trail that only spawns when moving AND on the ground.
     * - Requires horizontal movement above MIN_HORIZONTAL_SPEED
     * - ONLY spawns when player is on the ground (isOnGround())
     * - Does NOT alternate feet (unlike FOOTPRINT)
     * - Spawns particles in a random spread
     * - Useful for ground-drag effects, dust clouds, etc.
     * - Example: Dust trails, ground effects
     */
    GROUNDED
}
