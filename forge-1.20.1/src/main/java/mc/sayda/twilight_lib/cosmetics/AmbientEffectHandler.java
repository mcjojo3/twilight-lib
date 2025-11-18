package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.capabilities.IEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side ambient effect handler.
 * Renders continuous particle effects around players (e.g., circles at feet).
 */
@OnlyIn(Dist.CLIENT)
public class AmbientEffectHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double CIRCLE_RADIUS = 0.4; // Radius of the circle around player's feet

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        // Get all players in render distance
        List<AbstractClientPlayer> players = mc.level.players();

        for (AbstractClientPlayer player : players) {
            if (player == null) continue;

            // Check if player has ambient effects active
            player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                for (String effectId : effects.getActiveEffects()) {
                    EffectType effectType = EffectType.fromId(effectId);
                    if (effectType != null && effectType.getCategory() == EffectCategory.AMBIENT) {
                        renderAmbientEffect(player, effectId, mc.level);
                    }
                }
            });
        }
    }

    private static void renderAmbientEffect(AbstractClientPlayer player, String effectId, Level level) {
        switch (effectId) {
            case "ambient_flame" -> renderFlameCircle(player, level);
            case "ambient_frost" -> renderFrostCircle(player, level);
            default -> LOGGER.warn("Or, what. Unknown ambient effect: {}", effectId);
        }
    }

    /**
     * Renders a continuous flame circle around the player's feet
     */
    private static void renderFlameCircle(AbstractClientPlayer player, Level level) {
        Vec3 playerPos = player.position();

        // Spawn particles in a circle around the player's feet
        int particlesPerTick = mc.sayda.twilight_lib.config.TwilightConfig.AMBIENT_PARTICLES_PER_TICK.get();
        for (int i = 0; i < particlesPerTick; i++) {
            // Random angle for this particle
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;

            // Calculate position on the circle
            double offsetX = Math.cos(angle) * CIRCLE_RADIUS;
            double offsetZ = Math.sin(angle) * CIRCLE_RADIUS;

            double x = playerPos.x + offsetX;
            double y = playerPos.y + TwilightConstants.AmbientEffect.PARTICLE_HEIGHT_ABOVE_GROUND;
            double z = playerPos.z + offsetZ;

            // Spawn regular flame particles
            level.addParticle(ParticleTypes.FLAME,
                    x, y, z,
                    0.0, TwilightConstants.AmbientEffect.FLAME_UPWARD_VELOCITY, 0.0);

            // Add some small flame particles for depth
            if (ThreadLocalRandom.current().nextDouble() < TwilightConstants.AmbientEffect.SMALL_FLAME_SPAWN_PROBABILITY) {
                level.addParticle(ParticleTypes.SMALL_FLAME,
                        x, y, z,
                        0.0, TwilightConstants.AmbientEffect.SMALL_FLAME_UPWARD_VELOCITY, 0.0);
            }
        }
    }

    /**
     * Renders a continuous frost circle around the player's feet
     */
    private static void renderFrostCircle(AbstractClientPlayer player, Level level) {
        Vec3 playerPos = player.position();

        // Spawn particles in a circle around the player's feet
        int particlesPerTick = mc.sayda.twilight_lib.config.TwilightConfig.AMBIENT_PARTICLES_PER_TICK.get();
        for (int i = 0; i < particlesPerTick; i++) {
            // Random angle for this particle
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;

            // Calculate position on the circle
            double offsetX = Math.cos(angle) * CIRCLE_RADIUS;
            double offsetZ = Math.sin(angle) * CIRCLE_RADIUS;

            double x = playerPos.x + offsetX;
            double y = playerPos.y + TwilightConstants.AmbientEffect.PARTICLE_HEIGHT_ABOVE_GROUND;
            double z = playerPos.z + offsetZ;

            // Spawn snowflake particles
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    0.0, TwilightConstants.AmbientEffect.SNOWFLAKE_UPWARD_VELOCITY, 0.0);

            // Add some white ash for sparkle effect
            if (ThreadLocalRandom.current().nextDouble() < TwilightConstants.AmbientEffect.WHITE_ASH_SPAWN_PROBABILITY) {
                level.addParticle(ParticleTypes.WHITE_ASH,
                        x, y, z,
                        0.0, TwilightConstants.AmbientEffect.WHITE_ASH_UPWARD_VELOCITY, 0.0);
            }
        }
    }
}