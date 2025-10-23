package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side spawn effects for players with the respawn_twilight effect
 * Creates a beautiful twilight particle burst when spawning (login, respawn, etc)
 */
public class RespawnEffectHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    // Thread-safe map to prevent ConcurrentModificationException from network thread
    private static final Map<UUID, Integer> PENDING_EFFECTS = new ConcurrentHashMap<>();
    private static final int EFFECT_DELAY_TICKS = 5; // Wait 5 ticks after spawn

    /**
     * Called by the network handler when effects are synced on spawn.
     * Triggers on any spawn event: login, respawn after death, etc.
     */
    public static void scheduleSpawnEffect(UUID playerId) {
        PENDING_EFFECTS.put(playerId, EFFECT_DELAY_TICKS);
        LOGGER.debug("Something good is going to happen. With sparkles! Scheduled spawn effect for player UUID: {}", playerId);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

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
                        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                            if (effects.isEffectActive("respawn_twilight")) {
                                spawnTwilightSpawnEffect(player);
                                LOGGER.debug("More sparkles, now! Triggered spawn effect for player {}", player.getName().getString());
                            }
                        });
                    }
                    return null; // Remove this entry
                } else {
                    return ticksLeft - 1; // Decrement counter atomically
                }
            });
        }
    }

    private static void spawnTwilightSpawnEffect(Player player) {
        Vec3 pos = player.position();

        // Spawn a beautiful twilight burst
        // Purple and blue particles spiraling upward
        for (int i = 0; i < TwilightConstants.RespawnEffect.TWILIGHT_PARTICLE_COUNT; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * TwilightConstants.RespawnEffect.MAX_RADIUS;
            double height = RANDOM.nextDouble() * TwilightConstants.RespawnEffect.MAX_HEIGHT;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = height;

            // Purple twilight sparkles
            player.level().addParticle(
                ParticleTypes.ENCHANT,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                (RANDOM.nextDouble() - 0.5) * TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_HORIZONTAL,
                TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_HORIZONTAL,
                (RANDOM.nextDouble() - 0.5) * TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_HORIZONTAL
            );

            // Blue portal particles
            if (i % 2 == 0) {
                player.level().addParticle(
                    ParticleTypes.PORTAL,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    (RANDOM.nextDouble() - 0.5) * (TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_HORIZONTAL * 1.5),
                    TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_HORIZONTAL * 1.5,
                    (RANDOM.nextDouble() - 0.5) * (TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_HORIZONTAL * 1.5)
                );
            }
        }

        // Add some soul particles at the center
        for (int i = 0; i < TwilightConstants.RespawnEffect.SOUL_PARTICLE_COUNT; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * TwilightConstants.RespawnEffect.CENTER_SPAWN_RADIUS;
            double offsetY = RANDOM.nextDouble() * TwilightConstants.RespawnEffect.MAX_RADIUS;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * TwilightConstants.RespawnEffect.CENTER_SPAWN_RADIUS;

            player.level().addParticle(
                ParticleTypes.SOUL,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                0,
                TwilightConstants.RespawnEffect.PARTICLE_VELOCITY_VERTICAL,
                0
            );
        }
    }
}
