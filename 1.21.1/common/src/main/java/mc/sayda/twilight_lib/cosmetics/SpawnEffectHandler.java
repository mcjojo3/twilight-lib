package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.architectury.event.events.client.ClientTickEvent;

/**
 * Client-side spawn effects for players with spawn effects active
 * Creates beautiful particle bursts when spawning (login, respawn, dimension
 * change, etc)
 *
 * Supported effects:
 * - spawn_ethereal: Soul + portal + enchanting particles (purple/blue ethereal
 * theme)
 * - spawn_rainbow: Rainbow cycling particles (vibrant multi-color)
 * - spawn_portal: End portal particles with reverse gravity (mysterious void
 * theme)
 * - spawn_frost: Snowflake particles (icy winter theme)
 * - spawn_flame: Soul fire particles (blazing fire theme)
 * - spawn_nature: Spore blossom particles (natural floral theme)
 */
@Environment(EnvType.CLIENT)
public class SpawnEffectHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    // Thread-safe map to prevent ConcurrentModificationException from network
    // thread
    private static final Map<UUID, Integer> PENDING_EFFECTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RETRY_COUNTS = new ConcurrentHashMap<>();
    private static final int MAX_RETRIES = 20;

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            onClientTick();
        });
    }

    /**
     * Clears all pending spawn effects.
     * Called on client disconnect to prevent memory leaks and stale effects.
     */
    public static void clear() {
        PENDING_EFFECTS.clear();
        RETRY_COUNTS.clear();
        LOGGER.debug("Goodbye, my new friend! Cleared pending spawn effects from cache.");
    }

    public static void onClientDisconnect() {
        clear();
    }

    public static void onLevelUnload() {
        clear();
    }

    /**
     * Get the effective particle count, clamped by config limit (if set).
     * 
     * @param baseCount The default particle count from TwilightConstants
     * @return The clamped particle count (0 = unlimited uses base count)
     */
    private static int getClampedParticleCount(int baseCount) {
        int maxParticles = mc.sayda.twilight_lib.config.TwilightConfig.MAX_EFFECT_PARTICLES_PER_PLAYER.get();
        if (maxParticles <= 0) {
            return baseCount; // 0 = unlimited
        }
        return Math.min(baseCount, maxParticles);
    }

    /**
     * Called by the network handler when effects are synced on spawn.
     * Triggers on any spawn event: login, respawn after death, dimension change,
     * etc.
     */
    public static void scheduleSpawnEffect(UUID playerId) {
        PENDING_EFFECTS.put(playerId, mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_EFFECT_DELAY_TICKS.get());
        LOGGER.debug("Something good is going to happen. With sparkles! Scheduled spawn effect for player UUID: {}",
                playerId);
    }

    private static void onClientTick() {
        // Check if effects are enabled in config
        if (!mc.sayda.twilight_lib.config.TwilightConfig.ENABLE_EFFECTS.get())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        // Process pending effects using computeIfPresent for thread-safe atomic updates
        // Create snapshot of keys to avoid modification during iteration
        List<UUID> keys = new ArrayList<>(PENDING_EFFECTS.keySet());

        for (UUID playerId : keys) {
            PENDING_EFFECTS.computeIfPresent(playerId, (id, ticksLeft) -> {
                if (ticksLeft <= 0) {
                    Player player = mc.level.getPlayerByUUID(id);
                    if (player == null) {
                        // Retry logic for dimension transitions/loading delays
                        int retries = RETRY_COUNTS.getOrDefault(id, 0);
                        if (retries < MAX_RETRIES) {
                            RETRY_COUNTS.put(id, retries + 1);
                            return 5; // Wait 5 ticks before retrying
                        }
                        RETRY_COUNTS.remove(id);
                        return null; // Give up
                    }

                    // Trigger and cleanup
                    RETRY_COUNTS.remove(id);
                    if (!player.isInvisible()) {
                        triggerSpawnEffects(player);
                    }
                    return null; // Remove this entry
                } else {
                    return ticksLeft - 1; // Decrement counter atomically
                }
            });
        }
    }

    private static void triggerSpawnEffects(Player player) {
        var effects = DataUtils.getEffectsData(player);
        if (effects == null)
            return;

        // Check which spawn effects are active and trigger them
        if (effects.isEffectActive("spawn_ethereal")) {
            try {
                spawnEtherealEffect(player);
                LOGGER.debug("More sparkles, now! Triggered spawn_ethereal effect for player {}",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to spawn ethereal effect for player {}",
                        player.getName().getString(), e);
            }
        }
        if (effects.isEffectActive("spawn_rainbow")) {
            try {
                spawnRainbowEffect(player);
                LOGGER.debug("More sparkles, now! Triggered spawn_rainbow effect for player {}",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to spawn rainbow effect for player {}",
                        player.getName().getString(), e);
            }
        }
        if (effects.isEffectActive("spawn_portal")) {
            try {
                spawnPortalEffect(player);
                LOGGER.debug("More sparkles, now! Triggered spawn_portal effect for player {}",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to spawn portal effect for player {}",
                        player.getName().getString(), e);
            }
        }
        if (effects.isEffectActive("spawn_frost")) {
            try {
                spawnFrostEffect(player);
                LOGGER.debug("More sparkles, now! Triggered spawn_frost effect for player {}",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to spawn frost effect for player {}",
                        player.getName().getString(), e);
            }
        }
        if (effects.isEffectActive("spawn_flame")) {
            try {
                spawnFlameEffect(player);
                LOGGER.debug("More sparkles, now! Triggered spawn_flame effect for player {}",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to spawn flame effect for player {}",
                        player.getName().getString(), e);
            }
        }
        if (effects.isEffectActive("spawn_nature")) {
            try {
                spawnNatureEffect(player);
                LOGGER.debug("More sparkles, now! Triggered spawn_nature effect for player {}",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to spawn nature effect for player {}",
                        player.getName().getString(), e);
            }
        }
    }

    /**
     * spawn_ethereal: Soul + portal + enchanting particles
     * Purple and blue particles spiraling upward with soul particles at the center
     */
    private static void spawnEtherealEffect(Player player) {
        Vec3 pos = player.position();

        // Cache particle counts to avoid repeated method calls
        int particleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_PARTICLE_COUNT.get());
        int soulParticleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_SOUL_PARTICLE_COUNT.get());

        // Spawn a beautiful ethereal burst
        // Purple and blue particles spiraling upward
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get();
            double height = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get();

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // Purple enchanting sparkles
            player.level().addParticle(
                    ParticleTypes.ENCHANT,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5)
                            * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get(),
                    mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get(),
                    (RANDOM.nextDouble() - 0.5)
                            * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get());

            // Blue portal particles
            if (i % 2 == 0) {
                player.level().addParticle(
                        ParticleTypes.PORTAL,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        (RANDOM.nextDouble() - 0.5)
                                * (mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get() * 1.5),
                        mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get() * 1.5,
                        (RANDOM.nextDouble() - 0.5)
                                * (mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get() * 1.5));
            }
        }

        // Add soul particles at the center
        for (int i = 0; i < soulParticleCount; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get();
            double offsetY = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get();
            double offsetZ = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get();

            player.level().addParticle(
                    ParticleTypes.SOUL,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    0,
                    mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get(),
                    0);
        }
    }

    /**
     * spawn_rainbow: Rainbow cycling particles
     * Actual rainbow-colored dust particles in RGB spectrum
     */
    private static void spawnRainbowEffect(Player player) {
        Vec3 pos = player.position();

        // Cache particle count to avoid repeated method calls
        int particleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_PARTICLE_COUNT.get());

        // Rainbow colors: Red -> Orange -> Yellow -> Green -> Blue -> Purple
        Vector3f[] rainbowColors = {
                new Vector3f(1.0f, 0.0f, 0.0f), // Red
                new Vector3f(1.0f, 0.5f, 0.0f), // Orange
                new Vector3f(1.0f, 1.0f, 0.0f), // Yellow
                new Vector3f(0.0f, 1.0f, 0.0f), // Green
                new Vector3f(0.0f, 0.5f, 1.0f), // Blue
                new Vector3f(0.5f, 0.0f, 1.0f) // Purple
        };

        // Create rainbow burst with colored dust particles
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get();
            double height = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get();

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // Cycle through rainbow colors
            Vector3f color = rainbowColors[i % rainbowColors.length];
            DustParticleOptions dustOptions = new DustParticleOptions(color, 1.0f);

            player.level().addParticle(
                    dustOptions,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5)
                            * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get(),
                    mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get(),
                    (RANDOM.nextDouble() - 0.5)
                            * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_HORIZONTAL.get());
        }
    }

    /**
     * spawn_portal: End portal particles with reverse gravity
     * Mysterious void-themed particles floating upward from the ground
     */
    private static void spawnPortalEffect(Player player) {
        Vec3 pos = player.position();

        // Cache particle counts to avoid repeated method calls
        int particleCount = (int) (getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_PARTICLE_COUNT.get()) * 1.5);
        int soulParticleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_SOUL_PARTICLE_COUNT.get());

        // End portal particles rising from below
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get()
                    * 1.2;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = RANDOM.nextDouble() * 0.5; // Start near ground

            player.level().addParticle(
                    ParticleTypes.REVERSE_PORTAL,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    0,
                    mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get() * 2,
                    0);
        }

        // Add some dragon breath for extra mystique
        for (int i = 0; i < soulParticleCount; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get();
            double offsetY = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get()
                    * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get();

            player.level().addParticle(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    0,
                    0.05,
                    0);
        }
    }

    /**
     * spawn_frost: Snowflake particles
     * Icy winter-themed particles with snowflakes and white sparkles
     */
    private static void spawnFrostEffect(Player player) {
        Vec3 pos = player.position();

        // Cache particle count to avoid repeated method calls
        int particleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_PARTICLE_COUNT.get());

        // Snowflakes falling and floating around
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get();
            double height = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get();

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // Snowflakes
            player.level().addParticle(
                    ParticleTypes.SNOWFLAKE,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.05,
                    -0.05,
                    (RANDOM.nextDouble() - 0.5) * 0.05);

            // White sparkles
            if (i % 3 == 0) {
                player.level().addParticle(
                        ParticleTypes.WAX_OFF,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get(),
                        (RANDOM.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    /**
     * spawn_flame: Soul fire particles
     * Blazing fire-themed particles with soul flames and sparks
     */
    private static void spawnFlameEffect(Player player) {
        Vec3 pos = player.position();

        // Cache particle counts to avoid repeated method calls
        int particleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_PARTICLE_COUNT.get());
        int soulParticleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_SOUL_PARTICLE_COUNT.get());

        // Soul fire burst
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get();
            double height = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get();

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height * 0.5; // Keep flames lower

            // Soul fire flames
            player.level().addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.05,
                    mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get() * 1.5,
                    (RANDOM.nextDouble() - 0.5) * 0.05);

            // Add some lava sparks
            if (i % 4 == 0) {
                player.level().addParticle(
                        ParticleTypes.LAVA,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        (RANDOM.nextDouble() - 0.5) * 0.2,
                        mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get(),
                        (RANDOM.nextDouble() - 0.5) * 0.2);
            }
        }

        // Smoke rising from the center
        for (int i = 0; i < soulParticleCount; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get();
            double offsetY = RANDOM.nextDouble();
            double offsetZ = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get();

            player.level().addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    0,
                    mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get() * 0.5,
                    0);
        }
    }

    /**
     * spawn_nature: Spore blossom particles
     * Natural floral-themed particles with spores and happy villager particles
     */
    private static void spawnNatureEffect(Player player) {
        Vec3 pos = player.position();

        // Cache particle counts to avoid repeated method calls
        int particleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_PARTICLE_COUNT.get());
        int soulParticleCount = getClampedParticleCount(
                mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_SOUL_PARTICLE_COUNT.get());

        // Spore blossom particles floating around
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_RADIUS.get();
            double height = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get();

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // Spore blossom particles
            player.level().addParticle(
                    ParticleTypes.SPORE_BLOSSOM_AIR,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.05,
                    -0.02,
                    (RANDOM.nextDouble() - 0.5) * 0.05);

            // Happy villager particles (green sparkles)
            if (i % 2 == 0) {
                player.level().addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_VELOCITY_VERTICAL.get(),
                        (RANDOM.nextDouble() - 0.5) * 0.1);
            }
        }

        // Cherry leaves falling in the center
        for (int i = 0; i < soulParticleCount; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get() * 2;
            double offsetY = RANDOM.nextDouble() * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_MAX_HEIGHT.get();
            double offsetZ = (RANDOM.nextDouble() - 0.5)
                    * mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_CENTER_RADIUS.get() * 2;

            player.level().addParticle(
                    ParticleTypes.CHERRY_LEAVES,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.05,
                    -0.05,
                    (RANDOM.nextDouble() - 0.5) * 0.05);
        }
    }
}
