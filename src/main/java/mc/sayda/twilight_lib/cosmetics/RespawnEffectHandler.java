package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

/**
 * Client-side respawn effects for Gold supporters
 * Creates a beautiful twilight particle burst when respawning
 */
public class RespawnEffectHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        // Only handle on client side
        if (!player.level().isClientSide) return;

        // Check if player has respawn_twilight effect
        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (effects.hasEffect("respawn_twilight")) {
                spawnTwilightRespawnEffect(player);
            }
        });
    }

    private static void spawnTwilightRespawnEffect(Player player) {
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
