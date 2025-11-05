package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.capabilities.IEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * Client-side ambient effect handler.
 * Renders continuous particle effects around players (e.g., circles at feet).
 */
@OnlyIn(Dist.CLIENT)
public class AmbientEffectHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double CIRCLE_RADIUS = 0.4; // Radius of the circle around player's feet
    private static final int PARTICLES_PER_TICK = 3; // How many particles to spawn each tick

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        // Get all players in render distance
        List<AbstractClientPlayer> players = mc.level.players();

        for (AbstractClientPlayer player : players) {
            if (player == null) continue;

            // Check if player has ambient effects active
            var effects = player.getData(ModAttachments.EFFECTS);
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
     * Renders a continuous flame circle around the player's feet
     */
    private static void renderFlameCircle(AbstractClientPlayer player, Level level) {
        Vec3 playerPos = player.position();

        // Spawn particles in a circle around the player's feet
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            // Random angle for this particle
            double angle = Math.random() * 2 * Math.PI;

            // Calculate position on the circle
            double offsetX = Math.cos(angle) * CIRCLE_RADIUS;
            double offsetZ = Math.sin(angle) * CIRCLE_RADIUS;

            double x = playerPos.x + offsetX;
            double y = playerPos.y + 0.05; // Slightly above ground
            double z = playerPos.z + offsetZ;

            // Spawn regular flame particles
            level.addParticle(ParticleTypes.FLAME,
                    x, y, z,
                    0.0, 0.02, 0.0); // Slight upward velocity

            // Add some small flame particles for depth
            if (Math.random() < 0.3) {
                level.addParticle(ParticleTypes.SMALL_FLAME,
                        x, y, z,
                        0.0, 0.01, 0.0);
            }
        }
    }

    /**
     * Renders a continuous frost circle around the player's feet
     */
    private static void renderFrostCircle(AbstractClientPlayer player, Level level) {
        Vec3 playerPos = player.position();

        // Spawn particles in a circle around the player's feet
        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            // Random angle for this particle
            double angle = Math.random() * 2 * Math.PI;

            // Calculate position on the circle
            double offsetX = Math.cos(angle) * CIRCLE_RADIUS;
            double offsetZ = Math.sin(angle) * CIRCLE_RADIUS;

            double x = playerPos.x + offsetX;
            double y = playerPos.y + 0.05; // Slightly above ground
            double z = playerPos.z + offsetZ;

            // Spawn snowflake particles
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    0.0, 0.01, 0.0); // Slight upward velocity

            // Add some white ash for sparkle effect
            if (Math.random() < 0.4) {
                level.addParticle(ParticleTypes.WHITE_ASH,
                        x, y, z,
                        0.0, 0.02, 0.0);
            }
        }
    }
}