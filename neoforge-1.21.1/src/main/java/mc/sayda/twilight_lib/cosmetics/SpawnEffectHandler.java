package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side spawn effects for players with spawn effects active
 * Creates beautiful particle bursts when spawning (login, respawn, dimension change, etc)
 *
 * Supported effects:
 * - spawn_ethereal: Soul + portal + enchanting particles (purple/blue ethereal theme)
 * - spawn_rainbow: Rainbow cycling particles (vibrant multi-color)
 * - spawn_portal: End portal particles with reverse gravity (mysterious void theme)
 * - spawn_frost: Snowflake particles (icy winter theme)
 * - spawn_flame: Soul fire particles (blazing fire theme)
 * - spawn_nature: Spore blossom particles (natural floral theme)
 */
public class SpawnEffectHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    // Thread-safe map to prevent ConcurrentModificationException from network thread
    private static final Map<UUID, Integer> PENDING_EFFECTS = new ConcurrentHashMap<>();

    /**
     * Get the effective particle count, clamped by config limit (if set).
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
     * Triggers on any spawn event: login, respawn after death, dimension change, etc.
     */
    public static void scheduleSpawnEffect(UUID playerId) {
        PENDING_EFFECTS.put(playerId, mc.sayda.twilight_lib.config.TwilightConfig.SPAWN_EFFECT_DELAY_TICKS.get());
        LOGGER.debug("Something good is going to happen. With sparkles! Scheduled spawn effect for player UUID: {}", playerId);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Check if effects are enabled in config
        if (!mc.sayda.twilight_lib.config.TwilightConfig.ENABLE_EFFECTS.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Process pending effects using computeIfPresent for thread-safe atomic updates
        // Create snapshot of keys to avoid modification during iteration
        List<UUID> keys = new ArrayList<>(PENDING_EFFECTS.keySet());

        for (UUID playerId : keys) {
            PENDING_EFFECTS.computeIfPresent(playerId, (id, ticksLeft) -> {
                if (ticksLeft <= 0) {
                    // Time to trigger effect
                    Player player = mc.level.getPlayerByUUID(id);
                    if (player != null && !player.isInvisible()) {
                        var effects = player.getData(ModAttachments.EFFECTS);
                        if (effects == null) return null; // Remove entry if no effects data

                        // Check which spawn effects are active and trigger them
                        if (effects.isEffectActive("spawn_ethereal")) {
                            spawnEtherealEffect(player);
                            LOGGER.debug("More sparkles, now! Triggered spawn_ethereal effect for player {}", player.getName().getString());
                        }
                        if (effects.isEffectActive("spawn_rainbow")) {
                            spawnRainbowEffect(player);
                            LOGGER.debug("More sparkles, now! Triggered spawn_rainbow effect for player {}", player.getName().getString());
                        }
                        if (effects.isEffectActive("spawn_portal")) {
                            spawnPortalEffect(player);
                            LOGGER.debug("More sparkles, now! Triggered spawn_portal effect for player {}", player.getName().getString());
                        }
                        if (effects.isEffectActive("spawn_frost")) {
                            spawnFrostEffect(player);
                            LOGGER.debug("More sparkles, now! Triggered spawn_frost effect for player {}", player.getName().getString());
                        }
                        if (effects.isEffectActive("spawn_flame")) {
                            spawnFlameEffect(player);
                            LOGGER.debug("More sparkles, now! Triggered spawn_flame effect for player {}", player.getName().getString());
                        }
                        if (effects.isEffectActive("spawn_nature")) {
                            spawnNatureEffect(player);
                            LOGGER.debug("More sparkles, now! Triggered spawn_nature effect for player {}", player.getName().getString());
                        }
                    }
                    return null; // Remove this entry
                } else {
                    return ticksLeft - 1; // Decrement counter atomically
                }
            });
        }
    }

    /**
     * Cleanup pending effects when a player disconnects to prevent memory leaks.
     * Called on client side when any player (including the local player) disconnects.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        if (PENDING_EFFECTS.remove(playerId) != null) {
            LOGGER.debug("Goodbye, my new friend! Cleaned up pending spawn effect for disconnected player UUID: {}", playerId);
        }
    }

    /**
     * Cleanup all pending effects when the level unloads to prevent memory leaks.
     * Called on client side when disconnecting from server or changing dimensions.
     * This ensures we don't accumulate stale player UUIDs across world changes.
     */
    @SubscribeEvent
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        // Only clear on client side
        if (event.getLevel().isClientSide()) {
            int size = PENDING_EFFECTS.size();
            if (size > 0) {
                PENDING_EFFECTS.clear();
                LOGGER.debug("I hope this world survives... Cleared {} pending spawn effects on level unload", size);
            }
        }
    }

    /**
     * spawn_ethereal: Soul + portal + enchanting particles
     * Purple and blue particles spiraling upward with soul particles at the center
     */
    private static void spawnEtherealEffect(Player player) {
        Vec3 pos = player.position();

        // Spawn a beautiful ethereal burst
        // Purple and blue particles spiraling upward
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.PARTICLE_COUNT); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS;
            double height = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // Purple enchanting sparkles
            player.level().addParticle(
                ParticleTypes.ENCHANT,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL,
                TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL,
                (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL
            );

            // Blue portal particles
            if (i % 2 == 0) {
                player.level().addParticle(
                    ParticleTypes.PORTAL,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * (TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL * 1.5),
                    TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL * 1.5,
                    (RANDOM.nextDouble() - 0.5) * (TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL * 1.5)
                );
            }
        }

        // Add soul particles at the center
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.SOUL_PARTICLE_COUNT); i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS;
            double offsetY = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS;

            player.level().addParticle(
                ParticleTypes.SOUL,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                0,
                TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL,
                0
            );
        }
    }

    /**
     * spawn_rainbow: Rainbow cycling particles
     * Actual rainbow-colored dust particles in RGB spectrum
     */
    private static void spawnRainbowEffect(Player player) {
        Vec3 pos = player.position();

        // Rainbow colors: Red -> Orange -> Yellow -> Green -> Blue -> Purple
        Vector3f[] rainbowColors = {
            new Vector3f(1.0f, 0.0f, 0.0f),      // Red
            new Vector3f(1.0f, 0.5f, 0.0f),      // Orange
            new Vector3f(1.0f, 1.0f, 0.0f),      // Yellow
            new Vector3f(0.0f, 1.0f, 0.0f),      // Green
            new Vector3f(0.0f, 0.5f, 1.0f),      // Blue
            new Vector3f(0.5f, 0.0f, 1.0f)       // Purple
        };

        // Create rainbow burst with colored dust particles
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.PARTICLE_COUNT); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS;
            double height = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT;

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
                (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL,
                TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL,
                (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_HORIZONTAL
            );
        }
    }

    /**
     * spawn_portal: End portal particles with reverse gravity
     * Mysterious void-themed particles floating upward from the ground
     */
    private static void spawnPortalEffect(Player player) {
        Vec3 pos = player.position();

        // End portal particles rising from below
        for (int i = 0; i < (int)(getClampedParticleCount(TwilightConstants.SpawnEffect.PARTICLE_COUNT) * 1.5); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS * 1.2;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = RANDOM.nextDouble() * 0.5; // Start near ground

            player.level().addParticle(
                ParticleTypes.REVERSE_PORTAL,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                0,
                TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL * 2,
                0
            );
        }

        // Add some dragon breath for extra mystique
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.SOUL_PARTICLE_COUNT); i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS;
            double offsetY = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS;

            player.level().addParticle(
                ParticleTypes.DRAGON_BREATH,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                0,
                0.05,
                0
            );
        }
    }

    /**
     * spawn_frost: Snowflake particles
     * Icy winter-themed particles with snowflakes and white sparkles
     */
    private static void spawnFrostEffect(Player player) {
        Vec3 pos = player.position();

        // Snowflakes falling and floating around
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.PARTICLE_COUNT); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS;
            double height = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT;

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
                (RANDOM.nextDouble() - 0.5) * 0.05
            );

            // White sparkles
            if (i % 3 == 0) {
                player.level().addParticle(
                    ParticleTypes.WAX_OFF,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.1,
                    TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL,
                    (RANDOM.nextDouble() - 0.5) * 0.1
                );
            }
        }
    }

    /**
     * spawn_flame: Soul fire particles
     * Blazing fire-themed particles with soul flames and sparks
     */
    private static void spawnFlameEffect(Player player) {
        Vec3 pos = player.position();

        // Soul fire burst
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.PARTICLE_COUNT); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS;
            double height = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT;

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
                TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL * 1.5,
                (RANDOM.nextDouble() - 0.5) * 0.05
            );

            // Add some lava sparks
            if (i % 4 == 0) {
                player.level().addParticle(
                    ParticleTypes.LAVA,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.2,
                    TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL,
                    (RANDOM.nextDouble() - 0.5) * 0.2
                );
            }
        }

        // Smoke rising from the center
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.SOUL_PARTICLE_COUNT); i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS;
            double offsetY = RANDOM.nextDouble();
            double offsetZ = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS;

            player.level().addParticle(
                ParticleTypes.LARGE_SMOKE,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                0,
                TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL * 0.5,
                0
            );
        }
    }

    /**
     * spawn_nature: Spore blossom particles
     * Natural floral-themed particles with spores and happy villager particles
     */
    private static void spawnNatureEffect(Player player) {
        Vec3 pos = player.position();

        // Spore blossom particles floating around
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.PARTICLE_COUNT); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_RADIUS;
            double height = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT;

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
                (RANDOM.nextDouble() - 0.5) * 0.05
            );

            // Happy villager particles (green sparkles)
            if (i % 2 == 0) {
                player.level().addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * 0.1,
                    TwilightConstants.SpawnEffect.PARTICLE_VELOCITY_VERTICAL,
                    (RANDOM.nextDouble() - 0.5) * 0.1
                );
            }
        }

        // Cherry leaves falling in the center
        for (int i = 0; i < getClampedParticleCount(TwilightConstants.SpawnEffect.SOUL_PARTICLE_COUNT); i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS * 2;
            double offsetY = RANDOM.nextDouble() * TwilightConstants.SpawnEffect.MAX_HEIGHT;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * TwilightConstants.SpawnEffect.CENTER_SPAWN_RADIUS * 2;

            player.level().addParticle(
                ParticleTypes.CHERRY_LEAVES,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                (RANDOM.nextDouble() - 0.5) * 0.05,
                -0.05,
                (RANDOM.nextDouble() - 0.5) * 0.05
            );
        }
    }
}