package mc.sayda.twilight_lib;

/**
 * Centralized constants for Twilight Lib
 * Prevents typos and makes refactoring easier
 */
public class TwilightConstants {

    // NBT Tag Names
    public static final String NBT_MORPH = "TwilightLibMorph";
    public static final String NBT_ADDONS = "TwilightLibAddons";
    public static final String NBT_TRAILS = "TwilightLibTrails";
    public static final String NBT_EFFECTS = "TwilightLibEffects";

    // Animation Timing Constants (extracted from magic numbers)
    public static final class Animation {
        // Tail animation parameters
        public static final float TAIL_SWING_BASE = 0.053F;
        public static final float TAIL_SWING_AMPLITUDE = 0.3F;
        public static final float TAIL_WAVE_MODIFIER = 0.04F;
    }

    // Entity Dimensions
    public static final float PLAYER_DEFAULT_HEIGHT = 1.8f;

    // Respawn Effect Parameters
    public static final class RespawnEffect {
        public static final int TWILIGHT_PARTICLE_COUNT = 50;
        public static final int SOUL_PARTICLE_COUNT = 20;
        public static final double MAX_RADIUS = 2.0;
        public static final double MAX_HEIGHT = 3.0;
        public static final double PARTICLE_VELOCITY_HORIZONTAL = 0.2;
        public static final double PARTICLE_VELOCITY_VERTICAL = 0.1;
        public static final double CENTER_SPAWN_RADIUS = 0.5;
    }

    // Trail Rendering Parameters
    public static final class Trail {
        public static final double MIN_HORIZONTAL_SPEED = 0.01;
        public static final double FEET_OFFSET_Y = 0.1;
        public static final double PARTICLE_SPREAD_HORIZONTAL = 0.4;
        public static final double PARTICLE_SPREAD_VERTICAL = 0.3;
    }
}