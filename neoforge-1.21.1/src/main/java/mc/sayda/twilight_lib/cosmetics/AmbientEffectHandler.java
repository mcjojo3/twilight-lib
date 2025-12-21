package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
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

    /**
     * Cached config value for particles per tick.
     * Updated once per tick to avoid repeated config access for each player/effect.
     * Performance: Reduces config access from O(players * effects) to O(1) per tick.
     */
    private static int cachedParticlesPerTick;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        // Cache config value once per tick (instead of per-player per-effect)
        cachedParticlesPerTick = TwilightConfig.AMBIENT_PARTICLES_PER_TICK.get();

        // Get all players in the client level (only includes players sent by server - typically within tracking range)
        List<AbstractClientPlayer> players = mc.level.players();

        for (AbstractClientPlayer player : players) {
            if (player == null || player.isInvisible()) continue;

            // Check if player has ambient effects active
            var effects = player.getData(ModAttachments.EFFECTS);
            if (effects == null) continue;

            for (String effectId : effects.getActiveEffects()) {
                EffectType effectType = EffectType.fromId(effectId);
                if (effectType != null && effectType.getCategory() == EffectCategory.AMBIENT) {
                    renderAmbientEffect(player, effectId, mc.level);
                }
            }
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
     * Renders a continuous particle circle around the player's feet
     * @param player The player to render around
     * @param level The level to spawn particles in
     * @param mainParticle The primary particle type
     * @param mainVelocity The upward velocity for primary particles
     * @param secondaryParticle Optional secondary particle type (can be null)
     * @param secondaryVelocity The upward velocity for secondary particles
     * @param secondaryProbability The spawn probability for secondary particles (0.0-1.0)
     */
    private static void renderParticleCircle(AbstractClientPlayer player, Level level,
                                              ParticleOptions mainParticle, double mainVelocity,
                                              ParticleOptions secondaryParticle, double secondaryVelocity,
                                              double secondaryProbability) {
        Vec3 playerPos = player.position();

        // Spawn particles in a circle around the player's feet (using cached config value)
        for (int i = 0; i < cachedParticlesPerTick; i++) {
            // Random angle for this particle
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;

            // Calculate position on the circle
            double offsetX = Math.cos(angle) * CIRCLE_RADIUS;
            double offsetZ = Math.sin(angle) * CIRCLE_RADIUS;

            double x = playerPos.x + offsetX;
            double y = playerPos.y + TwilightConstants.AmbientEffect.PARTICLE_HEIGHT_ABOVE_GROUND;
            double z = playerPos.z + offsetZ;

            // Spawn primary particle
            level.addParticle(mainParticle, x, y, z, 0.0, mainVelocity, 0.0);

            // Add secondary particles if configured
            if (secondaryParticle != null && ThreadLocalRandom.current().nextDouble() < secondaryProbability) {
                level.addParticle(secondaryParticle, x, y, z, 0.0, secondaryVelocity, 0.0);
            }
        }
    }

    /**
     * Renders a continuous flame circle around the player's feet
     */
    private static void renderFlameCircle(AbstractClientPlayer player, Level level) {
        renderParticleCircle(player, level,
            ParticleTypes.FLAME, TwilightConstants.AmbientEffect.FLAME_UPWARD_VELOCITY,
            ParticleTypes.SMALL_FLAME, TwilightConstants.AmbientEffect.SMALL_FLAME_UPWARD_VELOCITY,
            TwilightConstants.AmbientEffect.SMALL_FLAME_SPAWN_PROBABILITY);
    }

    /**
     * Renders a continuous frost circle around the player's feet
     */
    private static void renderFrostCircle(AbstractClientPlayer player, Level level) {
        renderParticleCircle(player, level,
            ParticleTypes.SNOWFLAKE, TwilightConstants.AmbientEffect.SNOWFLAKE_UPWARD_VELOCITY,
            ParticleTypes.WHITE_ASH, TwilightConstants.AmbientEffect.WHITE_ASH_UPWARD_VELOCITY,
            TwilightConstants.AmbientEffect.WHITE_ASH_SPAWN_PROBABILITY);
    }
}