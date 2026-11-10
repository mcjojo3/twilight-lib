package mc.sayda.twilight_lib.cosmetics;

/**
 * Defines how trail particles should spawn for different trail types.
 * This allows fine-grained control over particle behavior.
 */
public enum TrailSpawnMode {
    /** Spawns around the player while moving, in the air or on the ground. */
    MOVEMENT,

    /** Alternating left and right footprints while walking on the ground. */
    FOOTPRINT,

    /** Spawns all the time, even when standing still. */
    CONTINUOUS,

    /** Spawns around the player while moving on the ground. */
    GROUNDED
}
