package mc.sayda.twilight_lib.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TwilightConfig {
    public static final ModConfigSpec COMMON_CONFIG;

    // Features
    public static final ModConfigSpec.BooleanValue ENABLE_TRAILS;
    public static final ModConfigSpec.BooleanValue ENABLE_ADDONS;
    public static final ModConfigSpec.BooleanValue ENABLE_EFFECTS;
    public static final ModConfigSpec.BooleanValue ENABLE_MORPHS;

    // Performance
    public static final ModConfigSpec.IntValue MAX_CACHED_ADDON_MODELS;
    public static final ModConfigSpec.IntValue TRAIL_UPDATE_FREQUENCY;
    public static final ModConfigSpec.IntValue MAX_SUPPORTER_JSON_SIZE;

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