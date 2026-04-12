package mc.sayda.twilight_lib.neoforge.config;

import mc.sayda.twilight_lib.config.TwilightConfig;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class TwilightLibConfigNeoForge {
    public static final ModConfigSpec COMMON_CONFIG;
    // Features
    public static final ModConfigSpec.BooleanValue ENABLE_TRAILS;
    public static final ModConfigSpec.BooleanValue ENABLE_ADDONS;
    public static final ModConfigSpec.BooleanValue ENABLE_EFFECTS;
    public static final ModConfigSpec.BooleanValue ENABLE_MORPHS;
    public static final ModConfigSpec.BooleanValue HIDE_CHEST_IN_ARMOR;
    public static final ModConfigSpec.BooleanValue HIDE_CHEST_IN_CUSTOM_ARMOR;
    public static final ModConfigSpec.BooleanValue FORCE_LOAD_ALL_ADDONS;

    // Performance
    public static final ModConfigSpec.IntValue MAX_CACHED_ADDON_MODELS;
    public static final ModConfigSpec.IntValue TRAIL_UPDATE_FREQUENCY;
    public static final ModConfigSpec.IntValue MAX_SUPPORTER_JSON_SIZE;
    public static final ModConfigSpec.IntValue MAX_ENTITY_CACHE_SIZE;
    public static final ModConfigSpec.IntValue MAX_SUPPORTERS;
    public static final ModConfigSpec.IntValue MAX_NBT_LIST_SIZE;

    // Input Validation
    public static final ModConfigSpec.IntValue MAX_HEX_COLOR_LENGTH;
    public static final ModConfigSpec.IntValue MAX_COSMETIC_ID_LENGTH;

    // Network & Caching
    public static final ModConfigSpec.ConfigValue<String> SUPPORTER_BACKUP_URL;
    public static final ModConfigSpec.IntValue SUPPORTER_CONNECT_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue SUPPORTER_READ_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue SUPPORTER_CACHE_DURATION_MINUTES;
    public static final ModConfigSpec.IntValue SUPPORTER_FETCH_MAX_RETRIES;
    public static final ModConfigSpec.IntValue SUPPORTER_FETCH_RETRY_DELAY_MS;
    public static final ModConfigSpec.IntValue MORPH_CACHE_CLEANUP_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue LOGIN_SYNC_DELAY_TICKS;

    // Client Performance
    public static final ModConfigSpec.IntValue AMBIENT_PARTICLES_PER_TICK;
    public static final ModConfigSpec.IntValue FOOTPRINT_UPDATE_FREQUENCY;
    public static final ModConfigSpec.IntValue FOOTPRINT_LIFETIME_TICKS;
    public static final ModConfigSpec.IntValue SPAWN_EFFECT_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue TRANSLUCENT_ADDON_ALPHA;
    public static final ModConfigSpec.IntValue TRAIL_RENDER_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_EFFECT_PARTICLES_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue ENABLE_FOOTPRINT_TRAILS;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLE_TRAILS;
    public static final ModConfigSpec.DoubleValue CUSTOM_PARTICLE_SIZE;
    public static final ModConfigSpec.IntValue CUSTOM_PARTICLE_LIFETIME;
    public static final ModConfigSpec.DoubleValue CUSTOM_PARTICLE_GRAVITY;

    // Gameplay & Balance
    public static final ModConfigSpec.DoubleValue MINING_WATER_SLOWDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MINING_FLIGHT_SLOWDOWN_MULTIPLIER;

    // Debug
    public static final ModConfigSpec.BooleanValue VERBOSE_LOGGING;
    public static final ModConfigSpec.BooleanValue LOG_COSMETIC_LOADS;
    public static final ModConfigSpec.BooleanValue LOG_SUPPORTER_FETCHES;

    // Morph Physics
    public static final ModConfigSpec.DoubleValue EYE_HEIGHT_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MIN_MORPH_SCALE;
    public static final ModConfigSpec.DoubleValue MAX_MORPH_SCALE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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
        LOGIN_SYNC_DELAY_TICKS = builder.defineInRange("login_sync_delay_ticks", 40, 10, 100);
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
        builder.pop();

        COMMON_CONFIG = builder.build();
    }

    public static void registerConfig() {
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);

        // Link Common TwilightConfig suppliers to NeoForge Config
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

        TwilightConfig.MAX_HEX_COLOR_LENGTH = MAX_HEX_COLOR_LENGTH::get;
        TwilightConfig.MAX_COSMETIC_ID_LENGTH = MAX_COSMETIC_ID_LENGTH::get;

        TwilightConfig.SUPPORTER_BACKUP_URL = SUPPORTER_BACKUP_URL::get;
        TwilightConfig.SUPPORTER_CONNECT_TIMEOUT_MS = SUPPORTER_CONNECT_TIMEOUT_MS::get;
        TwilightConfig.SUPPORTER_READ_TIMEOUT_MS = SUPPORTER_READ_TIMEOUT_MS::get;
        TwilightConfig.SUPPORTER_CACHE_DURATION_MINUTES = SUPPORTER_CACHE_DURATION_MINUTES::get;
        TwilightConfig.SUPPORTER_FETCH_MAX_RETRIES = SUPPORTER_FETCH_MAX_RETRIES::get;
        TwilightConfig.SUPPORTER_FETCH_RETRY_DELAY_MS = SUPPORTER_FETCH_RETRY_DELAY_MS::get;
        TwilightConfig.MORPH_CACHE_CLEANUP_INTERVAL_TICKS = MORPH_CACHE_CLEANUP_INTERVAL_TICKS::get;
        TwilightConfig.LOGIN_SYNC_DELAY_TICKS = LOGIN_SYNC_DELAY_TICKS::get;

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
    }
}
