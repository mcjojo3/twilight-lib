package mc.sayda.twilight_lib;

/**
 * Centralized constants for Twilight Lib.
 *
 * <p>
 * This class contains all magic numbers, configuration values, and string
 * constants
 * used throughout the mod. Centralizing constants:
 * <ul>
 * <li>Prevents typos in string literals (compiler-checked)</li>
 * <li>Makes refactoring easier (single point of change)</li>
 * <li>Documents the meaning and units of magic numbers</li>
 * <li>Provides type safety via nested classes for related constants</li>
 * </ul>
 *
 * <p>
 * <b>Design Pattern</b>: Uses nested static classes to group related constants
 * (e.g., {@link Animation}, {@link SpawnEffect}, {@link Trail}).
 * This provides namespace organization without creating separate files.
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
 */
public class TwilightConstants {

    /**
     * NBT tag names for persistent cosmetic data.
     *
     * <p>
     * <b>Why prefixed with "TwilightLib"?</b> NBT tags are stored in the player's
     * persistent data compound, which is shared by all mods. The prefix prevents
     * collisions with other mods that might use generic names like "morph" or
     * "trails".
     *
     * <p>
     * <b>Persistence Scope</b>: These tags are saved to player data files and
     * persist through logout, death, and dimension changes.
     */
    public static final String NBT_MORPH = "TwilightLibMorph";
    public static final String NBT_ADDONS = "TwilightLibAddons";
    public static final String NBT_TRAILS = "TwilightLibTrails";
    public static final String NBT_EFFECTS = "TwilightLibEffects";
    public static final String NBT_MODEL_VARIANT = "TwilightLibModelVariant";

    /**
     * Animation constants for cosmetic addons.
     *
     * <p>
     * These values control the procedural animation of tails and other moving
     * addons.
     * Extracted from magic numbers to make animation tuning easier.
     *
     * <p>
     * <b>Performance Note</b>: Animation calculations run every render frame (60+
     * FPS).
     * All constants are pre-calculated to avoid runtime divisions/multiplications.
     */
    public static final class Animation {
        /**
         * Base swing speed for tail animation (radians per tick).
         * Lower values = slower tail swaying.
         */
        public static final float TAIL_SWING_BASE = 0.053F;

        /**
         * Maximum swing angle amplitude (radians).
         * Controls how far the tail swings from center.
         * Higher values = more dramatic tail movement.
         */
        public static final float TAIL_SWING_AMPLITUDE = 0.3F;

        /**
         * Wave propagation modifier for multi-segment tails.
         * Creates a flowing wave effect along the tail length.
         */
        public static final float TAIL_WAVE_MODIFIER = 0.04F;
    }

    /**
     * Default player entity height in blocks.
     *
     * <p>
     * Used as reference for morph scaling calculations.
     * When morphing, the morph's height is compared to this value
     * to determine how much to scale movement and step height.
     */
    public static final float PLAYER_DEFAULT_HEIGHT = 1.8f;

    /**
     * Spawn effect constants - particle bursts triggered on login/respawn.
     *
     * <p>
     * <b>Performance Consideration</b>: Spawn effects trigger once per event (not
     * every frame).
     * High particle counts are acceptable because they're infrequent.
     *
     * <p>
     * All spawn effects use these shared parameters for consistent visual scale.
     * Individual effects may override specific values (e.g., portal effect uses
     * reverse gravity).
     */
    public static final class SpawnEffect {
        /**
         * Number of main particles in spawn burst (e.g., portal, enchant particles).
         */
        public static final int PARTICLE_COUNT = 50;

        /** Number of soul particles in ethereal spawn effect. Fewer for subtlety. */
        public static final int SOUL_PARTICLE_COUNT = 20;

        /** Maximum horizontal radius of spawn effect in blocks. */
        public static final double MAX_RADIUS = 2.0;

        /** Maximum vertical height of spawn effect in blocks. */
        public static final double MAX_HEIGHT = 3.0;

        /** Initial horizontal velocity for outward particle burst (blocks per tick). */
        public static final double PARTICLE_VELOCITY_HORIZONTAL = 0.2;

        /** Initial vertical velocity for upward particle burst (blocks per tick). */
        public static final double PARTICLE_VELOCITY_VERTICAL = 0.1;

        /** Radius around player center for ground-based spawn particles (blocks). */
        public static final double CENTER_SPAWN_RADIUS = 0.5;
    }

    /**
     * Trail rendering constants - particle effects following player movement.
     *
     * <p>
     * <b>Performance Note</b>: Trails are rendered every tick while player is
     * moving.
     * Particle spawn is throttled based on movement speed to avoid overwhelming the
     * client.
     *
     * <p>
     * <b>Movement Detection</b>: Trails only spawn when horizontal speed exceeds
     * {@link #MIN_HORIZONTAL_SPEED} to prevent stationary particles when idle.
     */
    public static final class Trail {
        /**
         * Minimum horizontal movement speed to trigger trail particles (blocks per
         * tick).
         *
         * <p>
         * This prevents trails from spawning when the player is stationary or
         * moving very slowly (e.g., sneaking). Without this threshold, particles
         * would pile up at the player's feet when idle.
         */
        public static final double MIN_HORIZONTAL_SPEED = 0.01;

        /**
         * Vertical offset for trail particles (spawns slightly above ground for better
         * visibility).
         */
        public static final double FEET_OFFSET_Y = 0.1;

        /** Random horizontal spread for trail particles (prevents straight line). */
        public static final double PARTICLE_SPREAD_HORIZONTAL = 0.4;

        /** Random vertical spread for trail particles (adds natural variation). */
        public static final double PARTICLE_SPREAD_VERTICAL = 0.3;
    }

    /**
     * Supporter service configuration - limits for external data fetching.
     *
     * <p>
     * <b>Security</b>: These limits prevent abuse/DoS from malicious or oversized
     * supporter data.
     * <ul>
     * <li><b>Size limit</b>: Prevents memory exhaustion from huge JSON files</li>
     * <li><b>Timeout limits</b>: Prevents indefinite blocking on network
     * operations</li>
     * <li><b>Cache limits</b>: Prevents memory leaks from unbounded supporter
     * growth</li>
     * </ul>
     *
     * <p>
     * All values can be overridden via
     * {@link mc.sayda.twilight_lib.config.TwilightConfig}.
     */
    public static final class Supporter {
        /** Default backup URL for supporter data. Used if primary URL fails. */
        public static final String DEFAULT_BACKUP_URL = "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json";

        /**
         * Default maximum supporter JSON file size in megabytes. Prevents memory
         * exhaustion.
         */
        public static final int DEFAULT_MAX_JSON_SIZE_MB = 10;

        /** Default cache duration in minutes. Reduces GitHub API calls. */
        public static final int DEFAULT_CACHE_DURATION_MINUTES = 60;

        /**
         * Default HTTP connection timeout in milliseconds. Prevents indefinite
         * blocking.
         */
        public static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

        /** Default HTTP read timeout in milliseconds. Prevents slow read attacks. */
        public static final int DEFAULT_READ_TIMEOUT_MS = 5000;

        /** Default maximum supporters in cache. Prevents unbounded memory growth. */
        public static final int DEFAULT_MAX_SUPPORTERS = 100000;

        /** Default maximum retry attempts for supporter data fetch. */
        public static final int DEFAULT_MAX_RETRIES = 3;

        /** Default delay between retry attempts in milliseconds. */
        public static final int DEFAULT_RETRY_DELAY_MS = 2000;
    }

    /**
     * Ambient effect constants - continuous particle effects around player.
     *
     * <p>
     * <b>Performance Critical</b>: Ambient effects spawn particles EVERY TICK while
     * active.
     * Use low spawn probabilities and small particle counts to avoid lag.
     *
     * <p>
     * <b>Random Variation</b>: Probability constants add natural variation to
     * prevent
     * mechanical/repetitive particle patterns.
     */
    public static final class AmbientEffect {
        /**
         * Height above ground to spawn ambient particles (blocks). Prevents ground
         * clipping.
         */
        public static final double PARTICLE_HEIGHT_ABOVE_GROUND = 0.05;

        /** Upward velocity for regular flame particles (blocks per tick). */
        public static final double FLAME_UPWARD_VELOCITY = 0.02;

        /**
         * Upward velocity for small flame particles (blocks per tick). Slower for
         * variety.
         */
        public static final double SMALL_FLAME_UPWARD_VELOCITY = 0.01;

        /**
         * Probability (0.0 to 1.0) of spawning small flame variant.
         *
         * <p>
         * 30% chance creates a 70/30 mix of regular and small flames for visual
         * variety.
         */
        public static final double SMALL_FLAME_SPAWN_PROBABILITY = 0.3;

        /** Upward velocity for snowflake particles (blocks per tick). */
        public static final double SNOWFLAKE_UPWARD_VELOCITY = 0.01;

        /** Upward velocity for white ash particles (blocks per tick). */
        public static final double WHITE_ASH_UPWARD_VELOCITY = 0.02;

        /**
         * Probability (0.0 to 1.0) of spawning white ash variant.
         *
         * <p>
         * 40% chance creates a 60/40 mix of snowflakes and ash for frost effect.
         */
        public static final double WHITE_ASH_SPAWN_PROBABILITY = 0.4;
    }

    /**
     * Addon registration constants - reusable values for built-in addon variants.
     *
     * <p>
     * These constants define standard addon variants that can be registered
     * across different addon types (e.g., kitsune ears, tails, legs).
     *
     * <p>
     * <b>Why centralized?</b> Prevents typos in color names and makes it easy
     * to add new variants consistently across all addon types.
     */
    public static final class Addon {
        /**
         * Standard kitsune color variants used across multiple addon types.
         *
         * <p>
         * These colors are used for:
         * <ul>
         * <li>Kitsune ears (kitsune_ears_white, kitsune_ears_black, etc.)</li>
         * <li>Kitsune tails (kitsune_tail_white, kitsune_tail_black, etc.)</li>
         * <li>Kitsune legs (kitsune_legs_white, kitsune_legs_black, etc.)</li>
         * </ul>
         *
         * <p>
         * <b>Order matters</b>: This array defines the canonical registration order.
         */
        public static final String[] KITSUNE_COLORS = {
                "white", "black", "blue", "yellow", "orange", "purple", "red", "gray"
        };
    }
}