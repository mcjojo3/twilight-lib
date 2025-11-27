package mc.sayda.twilight_lib.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TwilightConfig {
    public static final ModConfigSpec COMMON_CONFIG;

    // Features
    public static final ModConfigSpec.BooleanValue ENABLE_TRAILS;
    public static final ModConfigSpec.BooleanValue ENABLE_ADDONS;
    public static final ModConfigSpec.BooleanValue ENABLE_EFFECTS;
    public static final ModConfigSpec.BooleanValue ENABLE_MORPHS;
    public static final ModConfigSpec.BooleanValue HIDE_CHEST_IN_ARMOR;

    // Performance
    public static final ModConfigSpec.IntValue MAX_CACHED_ADDON_MODELS;
    public static final ModConfigSpec.IntValue TRAIL_UPDATE_FREQUENCY;
    public static final ModConfigSpec.IntValue MAX_SUPPORTER_JSON_SIZE;
    public static final ModConfigSpec.IntValue MAX_ENTITY_CACHE_SIZE;
    public static final ModConfigSpec.IntValue MAX_SUPPORTERS;

    // Network & Caching
    public static final ModConfigSpec.ConfigValue<String> SUPPORTER_BACKUP_URL;
    public static final ModConfigSpec.IntValue SUPPORTER_CONNECT_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue SUPPORTER_READ_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue SUPPORTER_CACHE_DURATION_MINUTES;
    public static final ModConfigSpec.IntValue MORPH_CACHE_CLEANUP_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue LOGIN_SYNC_DELAY_TICKS;

    // Client Performance
    public static final ModConfigSpec.IntValue AMBIENT_PARTICLES_PER_TICK;
    public static final ModConfigSpec.IntValue FOOTPRINT_UPDATE_FREQUENCY;
    public static final ModConfigSpec.IntValue FOOTPRINT_LIFETIME_TICKS;
    public static final ModConfigSpec.IntValue SPAWN_EFFECT_DELAY_TICKS;

    // Gameplay & Balance
    public static final ModConfigSpec.DoubleValue MINING_WATER_SLOWDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MINING_FLIGHT_SLOWDOWN_MULTIPLIER;

    // Morph Physics
    public static final ModConfigSpec.DoubleValue BASE_STEP_HEIGHT;
    public static final ModConfigSpec.DoubleValue MIN_STEP_SCALE;
    public static final ModConfigSpec.DoubleValue MAX_STEP_SCALE;
    public static final ModConfigSpec.DoubleValue EYE_HEIGHT_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Twilight Lib Configuration")
                .comment("Changes require server restart")
                .push("features");

        builder.comment("Enable/disable entire cosmetic systems");
        ENABLE_TRAILS = builder
                .comment("Enable particle trails for supporters")
                .define("enable_trails", true);
        ENABLE_ADDONS = builder
                .comment("Enable cosmetic addons (ears, tails, wings, etc.)")
                .define("enable_addons", true);
        ENABLE_EFFECTS = builder
                .comment("Enable special effects (respawn effects, etc.)")
                .define("enable_effects", true);
        ENABLE_MORPHS = builder
                .comment("Enable player morphing system")
                .define("enable_morphs", true);
        HIDE_CHEST_IN_ARMOR = builder
                .comment("Hide chest addon when wearing chest armor (prevents clipping with custom armor models)")
                .define("hide_chest_in_armor", false);

        builder.pop();

        builder.push("performance");
        builder.comment("Performance and optimization settings");
        MAX_CACHED_ADDON_MODELS = builder
                .comment("Maximum cached addon models before LRU eviction")
                .defineInRange("max_cached_addon_models", 150, 10, 500);
        TRAIL_UPDATE_FREQUENCY = builder
                .comment("How often trails update in ticks (higher = better performance)")
                .defineInRange("trail_update_frequency", 3, 1, 20);
        MAX_SUPPORTER_JSON_SIZE = builder
                .comment("Maximum supporter JSON size in MB (prevents OOM attacks)")
                .defineInRange("max_supporter_json_size", 10, 1, 100);
        MAX_ENTITY_CACHE_SIZE = builder
                .comment("Maximum entity types cached for morph suggestions (prevents memory issues in heavily modded servers)")
                .defineInRange("max_entity_cache_size", 10000, 100, 50000);
        MAX_SUPPORTERS = builder
                .comment("Maximum supporters in cache (prevents unbounded growth from malicious JSON)")
                .defineInRange("max_supporters", 100000, 1000, 1000000);

        builder.pop();

        builder.push("network_and_caching");
        builder.comment("Network timeouts and cache duration settings");
        SUPPORTER_BACKUP_URL = builder
                .comment("Backup URL for supporter data (used if primary URL fails or times out)")
                .define("supporter_backup_url", "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json");
        SUPPORTER_CONNECT_TIMEOUT_MS = builder
                .comment("HTTP connection timeout for supporter fetch in milliseconds")
                .defineInRange("supporter_connect_timeout_ms", 5000, 1000, 30000);
        SUPPORTER_READ_TIMEOUT_MS = builder
                .comment("HTTP read timeout for supporter fetch in milliseconds")
                .defineInRange("supporter_read_timeout_ms", 5000, 1000, 30000);
        SUPPORTER_CACHE_DURATION_MINUTES = builder
                .comment("How long to cache supporter data before refetching from GitHub")
                .defineInRange("supporter_cache_duration_minutes", 60, 5, 1440);
        MORPH_CACHE_CLEANUP_INTERVAL_TICKS = builder
                .comment("How often to clean up stale morph entities from cache (prevents memory leaks)")
                .defineInRange("morph_cache_cleanup_interval_ticks", 6000, 1200, 72000);
        LOGIN_SYNC_DELAY_TICKS = builder
                .comment("Delay before syncing cosmetics after player login (allows entities to load)")
                .defineInRange("login_sync_delay_ticks", 40, 10, 100);

        builder.pop();

        builder.push("client_performance");
        builder.comment("Client-side rendering and particle settings");
        AMBIENT_PARTICLES_PER_TICK = builder
                .comment("Particles spawned per tick for ambient effects (0 = disable ambient effects)")
                .defineInRange("ambient_particles_per_tick", 3, 0, 10);
        FOOTPRINT_UPDATE_FREQUENCY = builder
                .comment("How often footprint trails update in ticks (higher = better performance)")
                .defineInRange("footprint_update_frequency", 4, 1, 20);
        FOOTPRINT_LIFETIME_TICKS = builder
                .comment("How long footprint particles last in ticks")
                .defineInRange("footprint_lifetime_ticks", 60, 20, 200);
        SPAWN_EFFECT_DELAY_TICKS = builder
                .comment("Delay before playing spawn effects after respawn")
                .defineInRange("spawn_effect_delay_ticks", 5, 0, 40);

        builder.pop();

        builder.push("gameplay_and_balance");
        builder.comment("Gameplay mechanics and balance settings");
        MINING_WATER_SLOWDOWN_MULTIPLIER = builder
                .comment("Mining speed penalty multiplier when in water without aqua affinity")
                .defineInRange("mining_water_slowdown_multiplier", 5.0, 1.0, 10.0);
        MINING_FLIGHT_SLOWDOWN_MULTIPLIER = builder
                .comment("Mining speed penalty multiplier when not on ground")
                .defineInRange("mining_flight_slowdown_multiplier", 5.0, 1.0, 10.0);

        builder.pop();

        builder.push("morph_physics");
        builder.comment("Physics settings for morphed players");
        BASE_STEP_HEIGHT = builder
                .comment("Base step height for players")
                .defineInRange("base_step_height", 0.6, 0.0, 2.0);
        MIN_STEP_SCALE = builder
                .comment("Minimum step height scaling based on morph size")
                .defineInRange("min_step_scale", 0.3, 0.0, 1.0);
        MAX_STEP_SCALE = builder
                .comment("Maximum step height scaling based on morph size")
                .defineInRange("max_step_scale", 2.0, 1.0, 5.0);
        EYE_HEIGHT_MULTIPLIER = builder
                .comment("Eye height as percentage of morph height")
                .defineInRange("eye_height_multiplier", 0.85, 0.0, 1.0);

        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}