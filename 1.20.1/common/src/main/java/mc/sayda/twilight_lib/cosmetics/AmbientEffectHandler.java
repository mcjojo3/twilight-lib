package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import dev.architectury.event.events.client.ClientTickEvent;

/**
 * Client-side ambient effect handler.
 * Renders continuous particle effects around players (e.g., circles at feet).
 */
public class AmbientEffectHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int cachedParticlesPerTick = 10;

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            onClientTick();
        });
    }

    private static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused())
            return;

        // Cache config value once per tick (instead of per-player per-effect)
        cachedParticlesPerTick = TwilightConfig.AMBIENT_PARTICLES_PER_TICK.get();

        // Get all players in the client level (only includes players sent by server -
        // typically within tracking range)
        List<AbstractClientPlayer> players = mc.level.players();

        for (AbstractClientPlayer player : players) {
            if (player == null || player.isInvisible())
                continue;

            // Check if player has ambient effects active
            var effects = DataUtils.getEffectsData(player);
            if (effects == null)
                continue;

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
     * 
     * @param player               The player to render around
     * @param level                The level to spawn particles in
     * @param mainParticle         The primary particle type
     * @param mainVelocity         The upward velocity for primary particles
     * @param secondaryParticle    Optional secondary particle type (can be null)
     * @param secondaryVelocity    The upward velocity for secondary particles
     * @param secondaryProbability The spawn probability for secondary particles
     *                             (0.0-1.0)
     */
    private static void renderParticleCircle(AbstractClientPlayer player, Level level,
            ParticleOptions mainParticle, double mainVelocity,
            ParticleOptions secondaryParticle, double secondaryVelocity,
            double secondaryProbability) {
        Vec3 playerPos = player.position();

        // Spawn particles in a circle around the player's feet (using cached config
        // value)
        for (int i = 0; i < cachedParticlesPerTick; i++) {
            // Random angle for this particle
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;

            // Calculate position on the circle
            double offsetX = Math.cos(angle) * TwilightConfig.AMBIENT_CIRCLE_RADIUS.get();
            double offsetZ = Math.sin(angle) * TwilightConfig.AMBIENT_CIRCLE_RADIUS.get();

            double x = playerPos.x + offsetX;
            double y = playerPos.y + TwilightConfig.AMBIENT_HEIGHT_OFFSET.get();
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
                ParticleTypes.FLAME, TwilightConfig.AMBIENT_FLAME_VELOCITY.get(),
                ParticleTypes.SMALL_FLAME, TwilightConfig.AMBIENT_SMALL_FLAME_VELOCITY.get(),
                TwilightConfig.AMBIENT_SMALL_FLAME_CHANCE.get());
    }

    /**
     * Renders a continuous frost circle around the player's feet
     */
    private static void renderFrostCircle(AbstractClientPlayer player, Level level) {
        renderParticleCircle(player, level,
                ParticleTypes.SNOWFLAKE, TwilightConfig.AMBIENT_SNOW_VELOCITY.get(),
                ParticleTypes.WHITE_ASH, TwilightConfig.AMBIENT_ASH_VELOCITY.get(),
                TwilightConfig.AMBIENT_ASH_CHANCE.get());
    }
}
