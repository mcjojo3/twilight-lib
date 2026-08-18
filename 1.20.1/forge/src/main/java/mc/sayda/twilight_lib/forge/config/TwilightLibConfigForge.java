package mc.sayda.twilight_lib.forge.config;

import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class TwilightLibConfigForge {
    public static final ForgeConfigSpec COMMON_CONFIG;
    // Features
    public static final ForgeConfigSpec.BooleanValue ENABLE_TRAILS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADDONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MORPHS;
    public static final ForgeConfigSpec.BooleanValue HIDE_CHEST_IN_ARMOR;
    public static final ForgeConfigSpec.BooleanValue HIDE_CHEST_IN_CUSTOM_ARMOR;
    public static final ForgeConfigSpec.BooleanValue FORCE_LOAD_ALL_ADDONS;

    // Performance
    public static final ForgeConfigSpec.IntValue MAX_CACHED_ADDON_MODELS;
    public static final ForgeConfigSpec.IntValue TRAIL_UPDATE_FREQUENCY;
    public static final ForgeConfigSpec.IntValue MAX_SUPPORTER_JSON_SIZE;
    public static final ForgeConfigSpec.IntValue MAX_ENTITY_CACHE_SIZE;
    public static final ForgeConfigSpec.IntValue MAX_SUPPORTERS;
    public static final ForgeConfigSpec.IntValue MAX_NBT_LIST_SIZE;
    public static final ForgeConfigSpec.IntValue MAX_TINT_TEXTURE_CACHE_SIZE;
    public static final ForgeConfigSpec.IntValue TRAIL_CLEANUP_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_ADDONS_IN_REGISTRY;

    // Input Validation
    public static final ForgeConfigSpec.IntValue MAX_HEX_COLOR_LENGTH;
    public static final ForgeConfigSpec.IntValue MAX_COSMETIC_ID_LENGTH;

    // Network & Caching
    public static final ForgeConfigSpec.ConfigValue<String> SUPPORTER_BACKUP_URL;
    public static final ForgeConfigSpec.IntValue SUPPORTER_CONNECT_TIMEOUT_MS;
    public static final ForgeConfigSpec.IntValue SUPPORTER_READ_TIMEOUT_MS;
    public static final ForgeConfigSpec.IntValue SUPPORTER_CACHE_DURATION_MINUTES;
    public static final ForgeConfigSpec.IntValue SUPPORTER_FETCH_MAX_RETRIES;
    public static final ForgeConfigSpec.IntValue SUPPORTER_FETCH_RETRY_DELAY_MS;
    public static final ForgeConfigSpec.IntValue MORPH_CACHE_CLEANUP_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_MORPH_PROXY_CACHE_SIZE;
    public static final ForgeConfigSpec.IntValue MORPH_PROXY_TICK_THRESHOLD_TICKS;
    public static final ForgeConfigSpec.IntValue LOGIN_SYNC_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue NETWORK_MAX_COLLECTION_SIZE;
    public static final ForgeConfigSpec.IntValue NETWORK_MAX_STRING_LENGTH;
    public static final ForgeConfigSpec.IntValue MAX_JSON_FIELD_LENGTH;
    public static final ForgeConfigSpec.IntValue MAX_COSMETIC_LIST_SIZE;
    public static final ForgeConfigSpec.DoubleValue TRAIL_MOVEMENT_EPSILON;

    // Client Performance
    public static final ForgeConfigSpec.IntValue AMBIENT_PARTICLES_PER_TICK;
    public static final ForgeConfigSpec.IntValue FOOTPRINT_UPDATE_FREQUENCY;
    public static final ForgeConfigSpec.IntValue FOOTPRINT_LIFETIME_TICKS;
    public static final ForgeConfigSpec.IntValue SPAWN_EFFECT_DELAY_TICKS;
    public static final ForgeConfigSpec.DoubleValue TRANSLUCENT_ADDON_ALPHA;
    public static final ForgeConfigSpec.IntValue TRAIL_RENDER_DISTANCE;
    public static final ForgeConfigSpec.IntValue MAX_EFFECT_PARTICLES_PER_PLAYER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FOOTPRINT_TRAILS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PARTICLE_TRAILS;
    public static final ForgeConfigSpec.DoubleValue CUSTOM_PARTICLE_SIZE;
    public static final ForgeConfigSpec.IntValue CUSTOM_PARTICLE_LIFETIME;
    public static final ForgeConfigSpec.DoubleValue CUSTOM_PARTICLE_GRAVITY;

    // Gameplay & Balance
    public static final ForgeConfigSpec.DoubleValue MINING_WATER_SLOWDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MINING_FLIGHT_SLOWDOWN_MULTIPLIER;

    // Debug
    public static final ForgeConfigSpec.BooleanValue VERBOSE_LOGGING;
    public static final ForgeConfigSpec.BooleanValue LOG_COSMETIC_LOADS;
    public static final ForgeConfigSpec.BooleanValue LOG_SUPPORTER_FETCHES;

    // Morph Physics
    public static final ForgeConfigSpec.DoubleValue EYE_HEIGHT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MIN_MORPH_SCALE;
    public static final ForgeConfigSpec.DoubleValue MAX_MORPH_SCALE;
    public static final ForgeConfigSpec.DoubleValue PLAYER_DEFAULT_HEIGHT;

    // Animation
    public static final ForgeConfigSpec.DoubleValue TAIL_SWING_BASE;
    public static final ForgeConfigSpec.DoubleValue TAIL_SWING_AMPLITUDE;
    public static final ForgeConfigSpec.DoubleValue TAIL_WAVE_MODIFIER;

    // Spawn Effects
    public static final ForgeConfigSpec.IntValue SPAWN_PARTICLE_COUNT;
    public static final ForgeConfigSpec.IntValue SPAWN_SOUL_PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue SPAWN_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SPAWN_MAX_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue SPAWN_VELOCITY_HORIZONTAL;
    public static final ForgeConfigSpec.DoubleValue SPAWN_VELOCITY_VERTICAL;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CENTER_RADIUS;

    // Trails
    public static final ForgeConfigSpec.DoubleValue TRAIL_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue TRAIL_FEET_OFFSET_Y;
    public static final ForgeConfigSpec.DoubleValue TRAIL_SPREAD_HORIZONTAL;
    public static final ForgeConfigSpec.DoubleValue TRAIL_SPREAD_VERTICAL;
    public static final ForgeConfigSpec.DoubleValue TRAIL_FOOTPRINT_OFFSET_LATERAL;
    public static final ForgeConfigSpec.DoubleValue TRAIL_FOOTPRINT_OFFSET_Y;

    // Ambient Effects
    public static final ForgeConfigSpec.DoubleValue AMBIENT_CIRCLE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_HEIGHT_OFFSET;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_FLAME_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_SMALL_FLAME_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_SMALL_FLAME_CHANCE;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_SNOW_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_ASH_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_ASH_CHANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Twilight Lib Configuration")
                .comment("Changes require server restart")
                .push("features");

        builder.comment("Enable/disable entire cosmetic systems");
        ENABLE_TRAILS = builder.comment("Enable particle trails for supporters").define("enable_trails", true);
        ENABLE_ADDONS = builder.comment("Enable cosmetic addons (ears, tails, wings, etc.)").define("enable_addons",
                true);
        ENABLE_EFFECTS = builder.comment("Enable special effects (respawn effects, etc.)").define("enable_effects",
                true);
        ENABLE_MORPHS = builder.comment("Enable player morphing system").define("enable_morphs", true);
        HIDE_CHEST_IN_ARMOR = builder.comment("Hide chest addon when wearing chest armor").define("hide_chest_in_armor",
                false);
        HIDE_CHEST_IN_CUSTOM_ARMOR = builder.comment("Hide chest addon when wearing custom rendered chest armor").define("hide_chest_in_custom_armor",
                true);
        FORCE_LOAD_ALL_ADDONS = builder.comment("Force load all addons").define("force_load_all_addons", false);

        builder.pop();
        builder.push("performance");
        MAX_CACHED_ADDON_MODELS = builder.defineInRange("max_cached_addon_models", 150, 10, 500);
        TRAIL_UPDATE_FREQUENCY = builder.defineInRange("trail_update_frequency", 3, 1, 20);
        MAX_SUPPORTER_JSON_SIZE = builder.defineInRange("max_supporter_json_size", 10, 1, 100);
        MAX_ENTITY_CACHE_SIZE = builder.defineInRange("max_entity_cache_size", 10000, 100, 50000);
        MAX_SUPPORTERS = builder.defineInRange("max_supporters", 100000, 1000, 1000000);
        MAX_NBT_LIST_SIZE = builder.defineInRange("max_nbt_list_size", 1000, 100, 10000);
        MAX_TINT_TEXTURE_CACHE_SIZE = builder.defineInRange("max_tint_texture_cache_size", 64, 8, 512);
        TRAIL_CLEANUP_INTERVAL_TICKS = builder.defineInRange("trail_cleanup_interval_ticks", 100, 20, 2000);
        MAX_ADDONS_IN_REGISTRY = builder.defineInRange("max_addons_in_registry", 1000, 100, 10000);
        builder.pop();

        builder.push("input_validation");
        MAX_HEX_COLOR_LENGTH = builder.defineInRange("max_hex_color_length", 16, 6, 128);
        MAX_COSMETIC_ID_LENGTH = builder.defineInRange("max_cosmetic_id_length", 64, 8, 256);
        builder.pop();

        builder.push("network_and_caching");
        SUPPORTER_BACKUP_URL = builder.define("supporter_backup_url",
                "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json");
        SUPPORTER_CONNECT_TIMEOUT_MS = builder.defineInRange("supporter_connect_timeout_ms", 5000, 1000, 30000);
        SUPPORTER_READ_TIMEOUT_MS = builder.defineInRange("supporter_read_timeout_ms", 5000, 1000, 30000);
        SUPPORTER_CACHE_DURATION_MINUTES = builder.defineInRange("supporter_cache_duration_minutes", 60, 5, 1440);
        SUPPORTER_FETCH_MAX_RETRIES = builder.defineInRange("supporter_fetch_max_retries", 3, 0, 10);
        SUPPORTER_FETCH_RETRY_DELAY_MS = builder.defineInRange("supporter_fetch_retry_delay_ms", 2000, 500, 10000);
        MORPH_CACHE_CLEANUP_INTERVAL_TICKS = builder.defineInRange("morph_cache_cleanup_interval_ticks", 6000, 1200,
                72000);
        MAX_MORPH_PROXY_CACHE_SIZE = builder.defineInRange("max_morph_proxy_cache_size", 1000, 10, 10000);
        MORPH_PROXY_TICK_THRESHOLD_TICKS = builder.defineInRange("morph_proxy_tick_threshold_ticks", 100, 10, 1000);
        LOGIN_SYNC_DELAY_TICKS = builder.defineInRange("login_sync_delay_ticks", 40, 10, 100);
        NETWORK_MAX_COLLECTION_SIZE = builder.defineInRange("network_max_collection_size", 128, 8, 2048);
        NETWORK_MAX_STRING_LENGTH = builder.defineInRange("network_max_string_length", 128, 8, 1024);
        MAX_JSON_FIELD_LENGTH = builder.defineInRange("max_json_field_length", 1000, 10, 10000);
        MAX_COSMETIC_LIST_SIZE = builder.defineInRange("max_cosmetic_list_size", 500, 10, 5000);
        TRAIL_MOVEMENT_EPSILON = builder.defineInRange("trail_movement_epsilon", 0.001, 0.0001, 0.1);
        builder.pop();

        builder.push("client_performance");
        AMBIENT_PARTICLES_PER_TICK = builder.defineInRange("ambient_particles_per_tick", 3, 0, 10);
        FOOTPRINT_UPDATE_FREQUENCY = builder.defineInRange("footprint_update_frequency", 4, 1, 20);
        FOOTPRINT_LIFETIME_TICKS = builder.defineInRange("footprint_lifetime_ticks", 60, 20, 200);
        SPAWN_EFFECT_DELAY_TICKS = builder.defineInRange("spawn_effect_delay_ticks", 5, 0, 40);
        TRANSLUCENT_ADDON_ALPHA = builder.defineInRange("translucent_addon_alpha", 0.5, 0.0, 1.0);
        TRAIL_RENDER_DISTANCE = builder.defineInRange("trail_render_distance", 0, 0, 512);
        MAX_EFFECT_PARTICLES_PER_PLAYER = builder.defineInRange("max_effect_particles_per_player", 0, 0, 1000);
        ENABLE_FOOTPRINT_TRAILS = builder.define("enable_footprint_trails", true);
        ENABLE_PARTICLE_TRAILS = builder.define("enable_particle_trails", true);
        CUSTOM_PARTICLE_SIZE = builder.defineInRange("custom_particle_size", 0.8, 0.1, 2.0);
        CUSTOM_PARTICLE_LIFETIME = builder.defineInRange("custom_particle_lifetime", 10, 1, 100);
        CUSTOM_PARTICLE_GRAVITY = builder.defineInRange("custom_particle_gravity", -0.2, -1.0, 1.0);
        builder.pop();

        builder.push("gameplay_and_balance");
        MINING_WATER_SLOWDOWN_MULTIPLIER = builder.defineInRange("mining_water_slowdown_multiplier", 5.0, 1.0, 10.0);
        MINING_FLIGHT_SLOWDOWN_MULTIPLIER = builder.defineInRange("mining_flight_slowdown_multiplier", 5.0, 1.0, 10.0);
        builder.pop();

        builder.push("debug");
        VERBOSE_LOGGING = builder.define("verbose_logging", false);
        LOG_COSMETIC_LOADS = builder.define("log_cosmetic_loads", false);
        LOG_SUPPORTER_FETCHES = builder.define("log_supporter_fetches", false);
        builder.pop();

        builder.push("morph_physics");
        EYE_HEIGHT_MULTIPLIER = builder.defineInRange("eye_height_multiplier", 0.85, 0.0, 1.0);
        MIN_MORPH_SCALE = builder.defineInRange("min_morph_scale", 0.1, 0.01, 1.0);
        MAX_MORPH_SCALE = builder.defineInRange("max_morph_scale", 10.0, 1.0, 50.0);
        PLAYER_DEFAULT_HEIGHT = builder.defineInRange("player_default_height", 1.8, 0.5, 5.0);
        builder.pop();

        builder.push("animation");
        TAIL_SWING_BASE = builder.defineInRange("tail_swing_base", 0.053, 0.0, 1.0);
        TAIL_SWING_AMPLITUDE = builder.defineInRange("tail_swing_amplitude", 0.3, 0.0, 2.0);
        TAIL_WAVE_MODIFIER = builder.defineInRange("tail_wave_modifier", 0.04, 0.0, 1.0);
        builder.pop();

        builder.push("spawn_effects");
        SPAWN_PARTICLE_COUNT = builder.defineInRange("spawn_particle_count", 50, 0, 500);
        SPAWN_SOUL_PARTICLE_COUNT = builder.defineInRange("spawn_soul_particle_count", 20, 0, 500);
        SPAWN_MAX_RADIUS = builder.defineInRange("spawn_max_radius", 2.0, 0.1, 10.0);
        SPAWN_MAX_HEIGHT = builder.defineInRange("spawn_max_height", 3.0, 0.1, 10.0);
        SPAWN_VELOCITY_HORIZONTAL = builder.defineInRange("spawn_velocity_horizontal", 0.2, 0.0, 5.0);
        SPAWN_VELOCITY_VERTICAL = builder.defineInRange("spawn_velocity_vertical", 0.1, 0.0, 5.0);
        SPAWN_CENTER_RADIUS = builder.defineInRange("spawn_center_radius", 0.5, 0.0, 5.0);
        builder.pop();

        builder.push("trails");
        TRAIL_MIN_SPEED = builder.defineInRange("trail_min_speed", 0.01, 0.0, 1.0);
        TRAIL_FEET_OFFSET_Y = builder.defineInRange("trail_feet_offset_y", 0.1, -2.0, 2.0);
        TRAIL_SPREAD_HORIZONTAL = builder.defineInRange("trail_spread_horizontal", 0.4, 0.0, 5.0);
        TRAIL_SPREAD_VERTICAL = builder.defineInRange("trail_spread_vertical", 0.3, 0.0, 5.0);
        TRAIL_FOOTPRINT_OFFSET_LATERAL = builder.defineInRange("trail_footprint_offset_lateral", 0.15, -2.0, 2.0);
        TRAIL_FOOTPRINT_OFFSET_Y = builder.defineInRange("trail_footprint_offset_y", 0.02, -2.0, 2.0);
        builder.pop();

        builder.push("ambient_effects");
        AMBIENT_CIRCLE_RADIUS = builder.defineInRange("ambient_circle_radius", 0.4, 0.0, 5.0);
        AMBIENT_HEIGHT_OFFSET = builder.defineInRange("ambient_height_offset", 0.05, -2.0, 2.0);
        AMBIENT_FLAME_VELOCITY = builder.defineInRange("ambient_flame_velocity", 0.02, 0.0, 1.0);
        AMBIENT_SMALL_FLAME_VELOCITY = builder.defineInRange("ambient_small_flame_velocity", 0.01, 0.0, 1.0);
        AMBIENT_SMALL_FLAME_CHANCE = builder.defineInRange("ambient_small_flame_chance", 0.3, 0.0, 1.0);
        AMBIENT_SNOW_VELOCITY = builder.defineInRange("ambient_snow_velocity", 0.01, 0.0, 1.0);
        AMBIENT_ASH_VELOCITY = builder.defineInRange("ambient_ash_velocity", 0.02, 0.0, 1.0);
        AMBIENT_ASH_CHANCE = builder.defineInRange("ambient_ash_chance", 0.4, 0.0, 1.0);
        builder.pop();

        COMMON_CONFIG = builder.build();
    }

    public static void registerConfig() {
        // Unlike NeoForge (ModContainer.registerConfig via getActiveContainer()),
        // Forge 1.20.1 exposes registerConfig directly on ModLoadingContext.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);

        // Link Common TwilightConfig suppliers to Forge Config
        TwilightConfig.ENABLE_TRAILS = ENABLE_TRAILS::get;
        TwilightConfig.ENABLE_ADDONS = ENABLE_ADDONS::get;
        TwilightConfig.ENABLE_EFFECTS = ENABLE_EFFECTS::get;
        TwilightConfig.ENABLE_MORPHS = ENABLE_MORPHS::get;
        TwilightConfig.HIDE_CHEST_IN_ARMOR = HIDE_CHEST_IN_ARMOR::get;
        TwilightConfig.HIDE_CHEST_IN_CUSTOM_ARMOR = HIDE_CHEST_IN_CUSTOM_ARMOR::get;
        TwilightConfig.FORCE_LOAD_ALL_ADDONS = FORCE_LOAD_ALL_ADDONS::get;

        TwilightConfig.MAX_CACHED_ADDON_MODELS = MAX_CACHED_ADDON_MODELS::get;
        TwilightConfig.TRAIL_UPDATE_FREQUENCY = TRAIL_UPDATE_FREQUENCY::get;
        TwilightConfig.MAX_SUPPORTER_JSON_SIZE = MAX_SUPPORTER_JSON_SIZE::get;
        TwilightConfig.MAX_ENTITY_CACHE_SIZE = MAX_ENTITY_CACHE_SIZE::get;
        TwilightConfig.MAX_SUPPORTERS = MAX_SUPPORTERS::get;
        TwilightConfig.MAX_NBT_LIST_SIZE = MAX_NBT_LIST_SIZE::get;
        TwilightConfig.MAX_TINT_TEXTURE_CACHE_SIZE = MAX_TINT_TEXTURE_CACHE_SIZE::get;
        TwilightConfig.TRAIL_CLEANUP_INTERVAL_TICKS = TRAIL_CLEANUP_INTERVAL_TICKS::get;
        TwilightConfig.MAX_ADDONS_IN_REGISTRY = MAX_ADDONS_IN_REGISTRY::get;

        TwilightConfig.MAX_HEX_COLOR_LENGTH = MAX_HEX_COLOR_LENGTH::get;
        TwilightConfig.MAX_COSMETIC_ID_LENGTH = MAX_COSMETIC_ID_LENGTH::get;

        TwilightConfig.SUPPORTER_BACKUP_URL = SUPPORTER_BACKUP_URL::get;
        TwilightConfig.SUPPORTER_CONNECT_TIMEOUT_MS = SUPPORTER_CONNECT_TIMEOUT_MS::get;
        TwilightConfig.SUPPORTER_READ_TIMEOUT_MS = SUPPORTER_READ_TIMEOUT_MS::get;
        TwilightConfig.SUPPORTER_CACHE_DURATION_MINUTES = SUPPORTER_CACHE_DURATION_MINUTES::get;
        TwilightConfig.SUPPORTER_FETCH_MAX_RETRIES = SUPPORTER_FETCH_MAX_RETRIES::get;
        TwilightConfig.SUPPORTER_FETCH_RETRY_DELAY_MS = SUPPORTER_FETCH_RETRY_DELAY_MS::get;
        TwilightConfig.MORPH_CACHE_CLEANUP_INTERVAL_TICKS = MORPH_CACHE_CLEANUP_INTERVAL_TICKS::get;
        TwilightConfig.MAX_MORPH_PROXY_CACHE_SIZE = MAX_MORPH_PROXY_CACHE_SIZE::get;
        TwilightConfig.MORPH_PROXY_TICK_THRESHOLD_TICKS = MORPH_PROXY_TICK_THRESHOLD_TICKS::get;
        TwilightConfig.LOGIN_SYNC_DELAY_TICKS = LOGIN_SYNC_DELAY_TICKS::get;
        TwilightConfig.NETWORK_MAX_COLLECTION_SIZE = NETWORK_MAX_COLLECTION_SIZE::get;
        TwilightConfig.NETWORK_MAX_STRING_LENGTH = NETWORK_MAX_STRING_LENGTH::get;
        TwilightConfig.MAX_JSON_FIELD_LENGTH = MAX_JSON_FIELD_LENGTH::get;
        TwilightConfig.MAX_COSMETIC_LIST_SIZE = MAX_COSMETIC_LIST_SIZE::get;
        TwilightConfig.TRAIL_MOVEMENT_EPSILON = TRAIL_MOVEMENT_EPSILON::get;

        TwilightConfig.AMBIENT_PARTICLES_PER_TICK = AMBIENT_PARTICLES_PER_TICK::get;
        TwilightConfig.FOOTPRINT_UPDATE_FREQUENCY = FOOTPRINT_UPDATE_FREQUENCY::get;
        TwilightConfig.FOOTPRINT_LIFETIME_TICKS = FOOTPRINT_LIFETIME_TICKS::get;
        TwilightConfig.SPAWN_EFFECT_DELAY_TICKS = SPAWN_EFFECT_DELAY_TICKS::get;
        TwilightConfig.TRANSLUCENT_ADDON_ALPHA = TRANSLUCENT_ADDON_ALPHA::get;
        TwilightConfig.TRAIL_RENDER_DISTANCE = TRAIL_RENDER_DISTANCE::get;
        TwilightConfig.MAX_EFFECT_PARTICLES_PER_PLAYER = MAX_EFFECT_PARTICLES_PER_PLAYER::get;
        TwilightConfig.ENABLE_FOOTPRINT_TRAILS = ENABLE_FOOTPRINT_TRAILS::get;
        TwilightConfig.ENABLE_PARTICLE_TRAILS = ENABLE_PARTICLE_TRAILS::get;
        TwilightConfig.CUSTOM_PARTICLE_SIZE = CUSTOM_PARTICLE_SIZE::get;
        TwilightConfig.CUSTOM_PARTICLE_LIFETIME = CUSTOM_PARTICLE_LIFETIME::get;
        TwilightConfig.CUSTOM_PARTICLE_GRAVITY = CUSTOM_PARTICLE_GRAVITY::get;

        TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER = MINING_WATER_SLOWDOWN_MULTIPLIER::get;
        TwilightConfig.MINING_FLIGHT_SLOWDOWN_MULTIPLIER = MINING_FLIGHT_SLOWDOWN_MULTIPLIER::get;

        TwilightConfig.VERBOSE_LOGGING = VERBOSE_LOGGING::get;
        TwilightConfig.LOG_COSMETIC_LOADS = LOG_COSMETIC_LOADS::get;
        TwilightConfig.LOG_SUPPORTER_FETCHES = LOG_SUPPORTER_FETCHES::get;

        TwilightConfig.EYE_HEIGHT_MULTIPLIER = EYE_HEIGHT_MULTIPLIER::get;
        TwilightConfig.MIN_MORPH_SCALE = MIN_MORPH_SCALE::get;
        TwilightConfig.MAX_MORPH_SCALE = MAX_MORPH_SCALE::get;
        TwilightConfig.PLAYER_DEFAULT_HEIGHT = PLAYER_DEFAULT_HEIGHT::get;

        TwilightConfig.TAIL_SWING_BASE = TAIL_SWING_BASE::get;
        TwilightConfig.TAIL_SWING_AMPLITUDE = TAIL_SWING_AMPLITUDE::get;
        TwilightConfig.TAIL_WAVE_MODIFIER = TAIL_WAVE_MODIFIER::get;

        TwilightConfig.SPAWN_PARTICLE_COUNT = SPAWN_PARTICLE_COUNT::get;
        TwilightConfig.SPAWN_SOUL_PARTICLE_COUNT = SPAWN_SOUL_PARTICLE_COUNT::get;
        TwilightConfig.SPAWN_MAX_RADIUS = SPAWN_MAX_RADIUS::get;
        TwilightConfig.SPAWN_MAX_HEIGHT = SPAWN_MAX_HEIGHT::get;
        TwilightConfig.SPAWN_VELOCITY_HORIZONTAL = SPAWN_VELOCITY_HORIZONTAL::get;
        TwilightConfig.SPAWN_VELOCITY_VERTICAL = SPAWN_VELOCITY_VERTICAL::get;
        TwilightConfig.SPAWN_CENTER_RADIUS = SPAWN_CENTER_RADIUS::get;

        TwilightConfig.TRAIL_MIN_SPEED = TRAIL_MIN_SPEED::get;
        TwilightConfig.TRAIL_FEET_OFFSET_Y = TRAIL_FEET_OFFSET_Y::get;
        TwilightConfig.TRAIL_SPREAD_HORIZONTAL = TRAIL_SPREAD_HORIZONTAL::get;
        TwilightConfig.TRAIL_SPREAD_VERTICAL = TRAIL_SPREAD_VERTICAL::get;
        TwilightConfig.TRAIL_FOOTPRINT_OFFSET_LATERAL = TRAIL_FOOTPRINT_OFFSET_LATERAL::get;
        TwilightConfig.TRAIL_FOOTPRINT_OFFSET_Y = TRAIL_FOOTPRINT_OFFSET_Y::get;

        TwilightConfig.AMBIENT_CIRCLE_RADIUS = AMBIENT_CIRCLE_RADIUS::get;
        TwilightConfig.AMBIENT_HEIGHT_OFFSET = AMBIENT_HEIGHT_OFFSET::get;
        TwilightConfig.AMBIENT_FLAME_VELOCITY = AMBIENT_FLAME_VELOCITY::get;
        TwilightConfig.AMBIENT_SMALL_FLAME_VELOCITY = AMBIENT_SMALL_FLAME_VELOCITY::get;
        TwilightConfig.AMBIENT_SMALL_FLAME_CHANCE = AMBIENT_SMALL_FLAME_CHANCE::get;
        TwilightConfig.AMBIENT_SNOW_VELOCITY = AMBIENT_SNOW_VELOCITY::get;
        TwilightConfig.AMBIENT_ASH_VELOCITY = AMBIENT_ASH_VELOCITY::get;
        TwilightConfig.AMBIENT_ASH_CHANCE = AMBIENT_ASH_CHANCE::get;
    }
}
