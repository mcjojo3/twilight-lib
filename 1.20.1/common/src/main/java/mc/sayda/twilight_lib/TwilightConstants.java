package mc.sayda.twilight_lib;

/**
 * Shared constants, grouped by feature.
 *
 * @author SaydaGames (mc_jojo3)
 */
public class TwilightConstants {

    /** NBT keys for saved cosmetic data. Prefixed so they don't clash with other mods. */
    public static final String NBT_MORPH = "TwilightLibMorph";
    public static final String NBT_ADDONS = "TwilightLibAddons";
    public static final String NBT_TRAILS = "TwilightLibTrails";
    public static final String NBT_EFFECTS = "TwilightLibEffects";
    public static final String NBT_MODEL_VARIANT = "TwilightLibModelVariant";
    public static final java.util.UUID MORPH_STEP_HEIGHT_MODIFIER_UUID = java.util.UUID
            .fromString("d8c9e2c7-ac2b-4c2b-ac2b-2b3c4d5e6f7a");

    /** Tail and addon animation values. */
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

    /** Default player height in blocks, used to scale morphs. */
    public static final float PLAYER_DEFAULT_HEIGHT = 1.8f;

    /** Spawn effect particle settings. */
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

    /** Trail particle settings. */
    public static final class Trail {
        /** Minimum horizontal speed (blocks per tick) for trails, so they don't pile up while idle. */
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

    /** Limits for fetching supporter data. The config can override them. */
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

    /** Ambient particle settings. These run every tick, so keep counts low. */
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

        /** Chance (0-1) of a small flame instead of a regular one. */
        public static final double SMALL_FLAME_SPAWN_PROBABILITY = 0.3;

        /** Upward velocity for snowflake particles (blocks per tick). */
        public static final double SNOWFLAKE_UPWARD_VELOCITY = 0.01;

        /** Upward velocity for white ash particles (blocks per tick). */
        public static final double WHITE_ASH_UPWARD_VELOCITY = 0.02;

        /** Chance (0-1) of white ash instead of a snowflake. */
        public static final double WHITE_ASH_SPAWN_PROBABILITY = 0.4;
    }

    /** Built-in addon variant names. */
    public static final class Addon {
        /** Kitsune colors shared by the ears, tails and legs, in registration order. */
        public static final String[] KITSUNE_COLORS = {
                "white", "black", "blue", "yellow", "orange", "purple", "red", "gray"
        };
    }
}
