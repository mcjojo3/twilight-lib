package mc.sayda.twilight_lib.config.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FabricConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("twilight_lib-common.json").toFile();

    public static void load() {
        ConfigData data = new ConfigData();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                data = GSON.fromJson(reader, ConfigData.class);
                if (data == null)
                    data = new ConfigData();
            } catch (IOException e) {
                LOGGER.error("How did I?! Uuuughh! Failed to load Fabric config", e);
            }
        } else {
            save(data);
        }

        // Clamp divisor fields to prevent division-by-zero (mirrors Forge min=1
        // bounds)
        data.performance.trail_update_frequency = Math.max(1, data.performance.trail_update_frequency);
        data.network_and_caching.morph_cache_cleanup_interval_ticks = Math.max(1,
                data.network_and_caching.morph_cache_cleanup_interval_ticks);
        data.network_and_caching.login_sync_delay_ticks = Math.max(1,
                data.network_and_caching.login_sync_delay_ticks);

        // Clamp to a safe minimum: a value <= 0 makes TintTextureCompositor's LRU
        // evict every entry immediately after insertion, forcing a full recomposite
        // (NativeImage decode + per-pixel blend + GPU upload) on every single
        // getOrCreate() call for a tinted addon/fox instead of hitting the cache.
        data.performance.max_tint_texture_cache_size = Math.max(1, data.performance.max_tint_texture_cache_size);

        apply(data);
    }

    private static void save(ConfigData data) {
        try {
            if (CONFIG_FILE.getParentFile() != null) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("How did I?! Uuuughh! Failed to save Fabric config", e);
        }
    }

    private static void apply(ConfigData data) {
        // Features
        TwilightConfig.ENABLE_TRAILS = () -> data.features.enable_trails;
        TwilightConfig.ENABLE_ADDONS = () -> data.features.enable_addons;
        TwilightConfig.ENABLE_EFFECTS = () -> data.features.enable_effects;
        TwilightConfig.ENABLE_MORPHS = () -> data.features.enable_morphs;
        TwilightConfig.HIDE_CHEST_IN_ARMOR = () -> data.features.hide_chest_in_armor;
        TwilightConfig.HIDE_CHEST_IN_CUSTOM_ARMOR = () -> data.features.hide_chest_in_custom_armor;
        TwilightConfig.FORCE_LOAD_ALL_ADDONS = () -> data.features.force_load_all_addons;

        // Performance
        TwilightConfig.MAX_CACHED_ADDON_MODELS = () -> data.performance.max_cached_addon_models;
        TwilightConfig.TRAIL_UPDATE_FREQUENCY = () -> data.performance.trail_update_frequency;
        TwilightConfig.MAX_SUPPORTER_JSON_SIZE = () -> data.performance.max_supporter_json_size;
        TwilightConfig.MAX_ENTITY_CACHE_SIZE = () -> data.performance.max_entity_cache_size;
        TwilightConfig.MAX_SUPPORTERS = () -> data.performance.max_supporters;
        TwilightConfig.MAX_NBT_LIST_SIZE = () -> data.performance.max_nbt_list_size;
        TwilightConfig.MAX_TINT_TEXTURE_CACHE_SIZE = () -> data.performance.max_tint_texture_cache_size;
        TwilightConfig.TRAIL_CLEANUP_INTERVAL_TICKS = () -> data.performance.trail_cleanup_interval_ticks;
        TwilightConfig.MAX_ADDONS_IN_REGISTRY = () -> data.performance.max_addons_in_registry;

        // Input Validation
        TwilightConfig.MAX_HEX_COLOR_LENGTH = () -> data.input_validation.max_hex_color_length;
        TwilightConfig.MAX_COSMETIC_ID_LENGTH = () -> data.input_validation.max_cosmetic_id_length;

        // Network & Caching
        TwilightConfig.SUPPORTER_BACKUP_URL = () -> data.network_and_caching.supporter_backup_url;
        TwilightConfig.SUPPORTER_CONNECT_TIMEOUT_MS = () -> data.network_and_caching.supporter_connect_timeout_ms;
        TwilightConfig.SUPPORTER_READ_TIMEOUT_MS = () -> data.network_and_caching.supporter_read_timeout_ms;
        TwilightConfig.SUPPORTER_CACHE_DURATION_MINUTES = () -> data.network_and_caching.supporter_cache_duration_minutes;
        TwilightConfig.SUPPORTER_FETCH_MAX_RETRIES = () -> data.network_and_caching.supporter_fetch_max_retries;
        TwilightConfig.SUPPORTER_FETCH_RETRY_DELAY_MS = () -> data.network_and_caching.supporter_fetch_retry_delay_ms;
        TwilightConfig.MORPH_CACHE_CLEANUP_INTERVAL_TICKS = () -> data.network_and_caching.morph_cache_cleanup_interval_ticks;
        TwilightConfig.MAX_MORPH_PROXY_CACHE_SIZE = () -> data.network_and_caching.max_morph_proxy_cache_size;
        TwilightConfig.MORPH_PROXY_TICK_THRESHOLD_TICKS = () -> data.network_and_caching.morph_proxy_tick_threshold_ticks;
        TwilightConfig.LOGIN_SYNC_DELAY_TICKS = () -> data.network_and_caching.login_sync_delay_ticks;
        TwilightConfig.NETWORK_MAX_COLLECTION_SIZE = () -> data.network_and_caching.network_max_collection_size;
        TwilightConfig.NETWORK_MAX_STRING_LENGTH = () -> data.network_and_caching.network_max_string_length;
        TwilightConfig.MAX_JSON_FIELD_LENGTH = () -> data.network_and_caching.max_json_field_length;
        TwilightConfig.MAX_COSMETIC_LIST_SIZE = () -> data.network_and_caching.max_cosmetic_list_size;
        TwilightConfig.TRAIL_MOVEMENT_EPSILON = () -> data.network_and_caching.trail_movement_epsilon;

        // Client Performance
        TwilightConfig.AMBIENT_PARTICLES_PER_TICK = () -> data.client_performance.ambient_particles_per_tick;
        TwilightConfig.FOOTPRINT_UPDATE_FREQUENCY = () -> data.client_performance.footprint_update_frequency;
        TwilightConfig.FOOTPRINT_LIFETIME_TICKS = () -> data.client_performance.footprint_lifetime_ticks;
        TwilightConfig.SPAWN_EFFECT_DELAY_TICKS = () -> data.client_performance.spawn_effect_delay_ticks;
        TwilightConfig.TRANSLUCENT_ADDON_ALPHA = () -> data.client_performance.translucent_addon_alpha;
        TwilightConfig.TRAIL_RENDER_DISTANCE = () -> data.client_performance.trail_render_distance;
        TwilightConfig.MAX_EFFECT_PARTICLES_PER_PLAYER = () -> data.client_performance.max_effect_particles_per_player;
        TwilightConfig.ENABLE_FOOTPRINT_TRAILS = () -> data.client_performance.enable_footprint_trails;
        TwilightConfig.ENABLE_PARTICLE_TRAILS = () -> data.client_performance.enable_particle_trails;
        TwilightConfig.CUSTOM_PARTICLE_SIZE = () -> data.client_performance.custom_particle_size;
        TwilightConfig.CUSTOM_PARTICLE_LIFETIME = () -> data.client_performance.custom_particle_lifetime;
        TwilightConfig.CUSTOM_PARTICLE_GRAVITY = () -> data.client_performance.custom_particle_gravity;

        // Gameplay & Balance
        TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER = () -> data.gameplay_and_balance.mining_water_slowdown_multiplier;
        TwilightConfig.MINING_FLIGHT_SLOWDOWN_MULTIPLIER = () -> data.gameplay_and_balance.mining_flight_slowdown_multiplier;

        // Debug
        TwilightConfig.VERBOSE_LOGGING = () -> data.debug.verbose_logging;
        TwilightConfig.LOG_COSMETIC_LOADS = () -> data.debug.log_cosmetic_loads;
        TwilightConfig.LOG_SUPPORTER_FETCHES = () -> data.debug.log_supporter_fetches;

        // Morph Physics
        TwilightConfig.EYE_HEIGHT_MULTIPLIER = () -> data.morph_physics.eye_height_multiplier;
        TwilightConfig.MIN_MORPH_SCALE = () -> data.morph_physics.min_morph_scale;
        TwilightConfig.MAX_MORPH_SCALE = () -> data.morph_physics.max_morph_scale;
        TwilightConfig.PLAYER_DEFAULT_HEIGHT = () -> data.morph_physics.player_default_height;

        // Animation
        TwilightConfig.TAIL_SWING_BASE = () -> data.animation.tail_swing_base;
        TwilightConfig.TAIL_SWING_AMPLITUDE = () -> data.animation.tail_swing_amplitude;
        TwilightConfig.TAIL_WAVE_MODIFIER = () -> data.animation.tail_wave_modifier;

        // Spawn Effects
        TwilightConfig.SPAWN_PARTICLE_COUNT = () -> data.spawn_effects.spawn_particle_count;
        TwilightConfig.SPAWN_SOUL_PARTICLE_COUNT = () -> data.spawn_effects.spawn_soul_particle_count;
        TwilightConfig.SPAWN_MAX_RADIUS = () -> data.spawn_effects.spawn_max_radius;
        TwilightConfig.SPAWN_MAX_HEIGHT = () -> data.spawn_effects.spawn_max_height;
        TwilightConfig.SPAWN_VELOCITY_HORIZONTAL = () -> data.spawn_effects.spawn_velocity_horizontal;
        TwilightConfig.SPAWN_VELOCITY_VERTICAL = () -> data.spawn_effects.spawn_velocity_vertical;
        TwilightConfig.SPAWN_CENTER_RADIUS = () -> data.spawn_effects.spawn_center_radius;

        // Trails
        TwilightConfig.TRAIL_MIN_SPEED = () -> data.trails.trail_min_speed;
        TwilightConfig.TRAIL_FEET_OFFSET_Y = () -> data.trails.trail_feet_offset_y;
        TwilightConfig.TRAIL_SPREAD_HORIZONTAL = () -> data.trails.trail_spread_horizontal;
        TwilightConfig.TRAIL_SPREAD_VERTICAL = () -> data.trails.trail_spread_vertical;
        TwilightConfig.TRAIL_FOOTPRINT_OFFSET_LATERAL = () -> data.trails.trail_footprint_offset_lateral;
        TwilightConfig.TRAIL_FOOTPRINT_OFFSET_Y = () -> data.trails.trail_footprint_offset_y;

        // Ambient Effects
        TwilightConfig.AMBIENT_CIRCLE_RADIUS = () -> data.ambient_effects.ambient_circle_radius;
        TwilightConfig.AMBIENT_HEIGHT_OFFSET = () -> data.ambient_effects.ambient_height_offset;
        TwilightConfig.AMBIENT_FLAME_VELOCITY = () -> data.ambient_effects.ambient_flame_velocity;
        TwilightConfig.AMBIENT_SMALL_FLAME_VELOCITY = () -> data.ambient_effects.ambient_small_flame_velocity;
        TwilightConfig.AMBIENT_SMALL_FLAME_CHANCE = () -> data.ambient_effects.ambient_small_flame_chance;
        TwilightConfig.AMBIENT_SNOW_VELOCITY = () -> data.ambient_effects.ambient_snow_velocity;
        TwilightConfig.AMBIENT_ASH_VELOCITY = () -> data.ambient_effects.ambient_ash_velocity;
        TwilightConfig.AMBIENT_ASH_CHANCE = () -> data.ambient_effects.ambient_ash_chance;
    }

    public static class ConfigData {
        public Features features = new Features();
        public Performance performance = new Performance();
        public InputValidation input_validation = new InputValidation();
        public NetworkAndCaching network_and_caching = new NetworkAndCaching();
        public ClientPerformance client_performance = new ClientPerformance();
        public GameplayAndBalance gameplay_and_balance = new GameplayAndBalance();
        public Debug debug = new Debug();
        public MorphPhysics morph_physics = new MorphPhysics();
        public Animation animation = new Animation();
        public SpawnEffects spawn_effects = new SpawnEffects();
        public Trails trails = new Trails();
        public AmbientEffects ambient_effects = new AmbientEffects();

        public static class Features {
            public boolean enable_trails = true;
            public boolean enable_addons = true;
            public boolean enable_effects = true;
            public boolean enable_morphs = true;
            public boolean hide_chest_in_armor = false;
            public boolean hide_chest_in_custom_armor = true;
            public boolean force_load_all_addons = false;
        }

        public static class Performance {
            public int max_cached_addon_models = 150;
            public int trail_update_frequency = 3;
            public int max_supporter_json_size = 10;
            public int max_entity_cache_size = 10000;
            public int max_supporters = 100000;
            public int max_nbt_list_size = 1000;
            public int max_tint_texture_cache_size = 64;
            public int trail_cleanup_interval_ticks = 100;
            public int max_addons_in_registry = 1000;
        }

        public static class InputValidation {
            public int max_hex_color_length = 16;
            public int max_cosmetic_id_length = 64;
        }

        public static class NetworkAndCaching {
            public String supporter_backup_url = "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json";
            public int supporter_connect_timeout_ms = 5000;
            public int supporter_read_timeout_ms = 5000;
            public int supporter_cache_duration_minutes = 60;
            public int supporter_fetch_max_retries = 3;
            public int supporter_fetch_retry_delay_ms = 2000;
            public int morph_cache_cleanup_interval_ticks = 6000;
            public int max_morph_proxy_cache_size = 1000;
            public int morph_proxy_tick_threshold_ticks = 100;
            public int login_sync_delay_ticks = 40;
            public int network_max_collection_size = 128;
            public int network_max_string_length = 128;
            public int max_json_field_length = 1000;
            public int max_cosmetic_list_size = 500;
            public double trail_movement_epsilon = 0.001;
        }

        public static class ClientPerformance {
            public int ambient_particles_per_tick = 3;
            public int footprint_update_frequency = 4;
            public int footprint_lifetime_ticks = 60;
            public int spawn_effect_delay_ticks = 5;
            public double translucent_addon_alpha = 0.5;
            public int trail_render_distance = 0;
            public int max_effect_particles_per_player = 0;
            public boolean enable_footprint_trails = true;
            public boolean enable_particle_trails = true;
            public double custom_particle_size = 0.8;
            public int custom_particle_lifetime = 10;
            public double custom_particle_gravity = -0.2;
        }

        public static class GameplayAndBalance {
            public double mining_water_slowdown_multiplier = 5.0;
            public double mining_flight_slowdown_multiplier = 5.0;
        }

        public static class Debug {
            public boolean verbose_logging = false;
            public boolean log_cosmetic_loads = false;
            public boolean log_supporter_fetches = false;
        }

        public static class MorphPhysics {
            public double eye_height_multiplier = 0.85;
            public double min_morph_scale = 0.1;
            public double max_morph_scale = 10.0;
            public double player_default_height = 1.8;
        }

        public static class Animation {
            public double tail_swing_base = 0.053;
            public double tail_swing_amplitude = 0.3;
            public double tail_wave_modifier = 0.04;
        }

        public static class SpawnEffects {
            public int spawn_particle_count = 50;
            public int spawn_soul_particle_count = 20;
            public double spawn_max_radius = 2.0;
            public double spawn_max_height = 3.0;
            public double spawn_velocity_horizontal = 0.2;
            public double spawn_velocity_vertical = 0.1;
            public double spawn_center_radius = 0.5;
        }

        public static class Trails {
            public double trail_min_speed = 0.01;
            public double trail_feet_offset_y = 0.1;
            public double trail_spread_horizontal = 0.4;
            public double trail_spread_vertical = 0.3;
            public double trail_footprint_offset_lateral = 0.15;
            public double trail_footprint_offset_y = 0.02;
        }

        public static class AmbientEffects {
            public double ambient_circle_radius = 0.4;
            public double ambient_height_offset = 0.05;
            public double ambient_flame_velocity = 0.02;
            public double ambient_small_flame_velocity = 0.01;
            public double ambient_small_flame_chance = 0.3;
            public double ambient_snow_velocity = 0.01;
            public double ambient_ash_velocity = 0.02;
            public double ambient_ash_chance = 0.4;
        }
    }
}
