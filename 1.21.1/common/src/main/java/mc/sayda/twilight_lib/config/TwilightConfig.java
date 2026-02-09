package mc.sayda.twilight_lib.config;

import java.util.function.Supplier;

public class TwilightConfig {

        // Features
        public static Supplier<Boolean> ENABLE_TRAILS = () -> true;
        public static Supplier<Boolean> ENABLE_ADDONS = () -> true;
        public static Supplier<Boolean> ENABLE_EFFECTS = () -> true;
        public static Supplier<Boolean> ENABLE_MORPHS = () -> true;
        public static Supplier<Boolean> HIDE_CHEST_IN_ARMOR = () -> false;
        public static Supplier<Boolean> FORCE_LOAD_ALL_ADDONS = () -> false;

        // Performance
        public static Supplier<Integer> MAX_CACHED_ADDON_MODELS = () -> 150;
        public static Supplier<Integer> TRAIL_UPDATE_FREQUENCY = () -> 3;
        public static Supplier<Integer> MAX_SUPPORTER_JSON_SIZE = () -> 10;
        public static Supplier<Integer> MAX_ENTITY_CACHE_SIZE = () -> 10000;
        public static Supplier<Integer> MAX_SUPPORTERS = () -> 100000;
        public static Supplier<Integer> MAX_NBT_LIST_SIZE = () -> 1000;
        public static Supplier<Integer> TRAIL_CLEANUP_INTERVAL_TICKS = () -> 100;

        // Registry & Limits
        public static Supplier<Integer> MAX_COSMETIC_ID_LENGTH = () -> 128; // Standardized to 128
        public static Supplier<Integer> MAX_ADDONS_IN_REGISTRY = () -> 1000;
        public static Supplier<Integer> MAX_HEX_COLOR_LENGTH = () -> 16;

        // Network & Caching
        public static Supplier<String> SUPPORTER_BACKUP_URL = () -> "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json";
        public static Supplier<Integer> SUPPORTER_CONNECT_TIMEOUT_MS = () -> 5000;
        public static Supplier<Integer> SUPPORTER_READ_TIMEOUT_MS = () -> 5000;
        public static Supplier<Integer> SUPPORTER_CACHE_DURATION_MINUTES = () -> 60;
        public static Supplier<Integer> SUPPORTER_FETCH_MAX_RETRIES = () -> 3;
        public static Supplier<Integer> SUPPORTER_FETCH_RETRY_DELAY_MS = () -> 2000;
        public static Supplier<Integer> MORPH_CACHE_CLEANUP_INTERVAL_TICKS = () -> 400; // Updated default
        public static Supplier<Integer> MAX_MORPH_PROXY_CACHE_SIZE = () -> 1000;
        public static Supplier<Integer> MORPH_PROXY_TICK_THRESHOLD_TICKS = () -> 100;
        public static Supplier<Integer> LOGIN_SYNC_DELAY_TICKS = () -> 40;
        public static Supplier<Integer> NETWORK_MAX_COLLECTION_SIZE = () -> 128;
        public static Supplier<Integer> NETWORK_MAX_STRING_LENGTH = () -> 128;
        public static Supplier<Integer> MAX_JSON_FIELD_LENGTH = () -> 1000;
        public static Supplier<Integer> MAX_COSMETIC_LIST_SIZE = () -> 500;
        public static Supplier<Double> TRAIL_MOVEMENT_EPSILON = () -> 0.001;

        // Client Performance
        public static Supplier<Integer> AMBIENT_PARTICLES_PER_TICK = () -> 3;
        public static Supplier<Integer> FOOTPRINT_UPDATE_FREQUENCY = () -> 4;
        public static Supplier<Integer> FOOTPRINT_LIFETIME_TICKS = () -> 60;
        public static Supplier<Integer> SPAWN_EFFECT_DELAY_TICKS = () -> 5;
        public static Supplier<Double> TRANSLUCENT_ADDON_ALPHA = () -> 0.5;
        public static Supplier<Integer> TRAIL_RENDER_DISTANCE = () -> 0;
        public static Supplier<Integer> MAX_EFFECT_PARTICLES_PER_PLAYER = () -> 0;
        public static Supplier<Boolean> ENABLE_FOOTPRINT_TRAILS = () -> true;
        public static Supplier<Boolean> ENABLE_PARTICLE_TRAILS = () -> true;
        public static Supplier<Double> CUSTOM_PARTICLE_SIZE = () -> 0.8;
        public static Supplier<Integer> CUSTOM_PARTICLE_LIFETIME = () -> 10;
        public static Supplier<Double> CUSTOM_PARTICLE_GRAVITY = () -> -0.2;

        // Gameplay & Balance
        public static Supplier<Double> MINING_WATER_SLOWDOWN_MULTIPLIER = () -> 5.0;
        public static Supplier<Double> MINING_FLIGHT_SLOWDOWN_MULTIPLIER = () -> 5.0;

        // Debug
        public static Supplier<Boolean> VERBOSE_LOGGING = () -> false;
        public static Supplier<Boolean> LOG_COSMETIC_LOADS = () -> false;
        public static Supplier<Boolean> LOG_SUPPORTER_FETCHES = () -> false;

        // Morph Physics
        public static Supplier<Double> EYE_HEIGHT_MULTIPLIER = () -> 0.85;
        public static Supplier<Double> MIN_MORPH_SCALE = () -> 0.1;
        public static Supplier<Double> MAX_MORPH_SCALE = () -> 10.0;
        public static Supplier<Double> PLAYER_DEFAULT_HEIGHT = () -> 1.8;

        // Animation
        public static Supplier<Double> TAIL_SWING_BASE = () -> 0.053;
        public static Supplier<Double> TAIL_SWING_AMPLITUDE = () -> 0.3;
        public static Supplier<Double> TAIL_WAVE_MODIFIER = () -> 0.04;

        // Spawn Effects
        public static Supplier<Integer> SPAWN_PARTICLE_COUNT = () -> 50;
        public static Supplier<Integer> SPAWN_SOUL_PARTICLE_COUNT = () -> 20;
        public static Supplier<Double> SPAWN_MAX_RADIUS = () -> 2.0;
        public static Supplier<Double> SPAWN_MAX_HEIGHT = () -> 3.0;
        public static Supplier<Double> SPAWN_VELOCITY_HORIZONTAL = () -> 0.2;
        public static Supplier<Double> SPAWN_VELOCITY_VERTICAL = () -> 0.1;
        public static Supplier<Double> SPAWN_CENTER_RADIUS = () -> 0.5;

        // Trails
        public static Supplier<Double> TRAIL_MIN_SPEED = () -> 0.01;
        public static Supplier<Double> TRAIL_FEET_OFFSET_Y = () -> 0.1;
        public static Supplier<Double> TRAIL_SPREAD_HORIZONTAL = () -> 0.4;
        public static Supplier<Double> TRAIL_SPREAD_VERTICAL = () -> 0.3;
        public static Supplier<Double> TRAIL_FOOTPRINT_OFFSET_LATERAL = () -> 0.15;
        public static Supplier<Double> TRAIL_FOOTPRINT_OFFSET_Y = () -> 0.02;

        // Ambient Effects
        public static Supplier<Double> AMBIENT_CIRCLE_RADIUS = () -> 0.4;
        public static Supplier<Double> AMBIENT_HEIGHT_OFFSET = () -> 0.05;
        public static Supplier<Double> AMBIENT_FLAME_VELOCITY = () -> 0.02;
        public static Supplier<Double> AMBIENT_SMALL_FLAME_VELOCITY = () -> 0.01;
        public static Supplier<Double> AMBIENT_SMALL_FLAME_CHANCE = () -> 0.3;
        public static Supplier<Double> AMBIENT_SNOW_VELOCITY = () -> 0.01;
        public static Supplier<Double> AMBIENT_ASH_VELOCITY = () -> 0.02;
        public static Supplier<Double> AMBIENT_ASH_CHANCE = () -> 0.4;

}
