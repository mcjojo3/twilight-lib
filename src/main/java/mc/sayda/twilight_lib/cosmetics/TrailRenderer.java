package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.particle.ModParticles;
import mc.sayda.twilight_lib.supporter.SupporterService;
import mc.sayda.twilight_lib.supporter.SupporterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;
import java.util.Random;

/**
 * Client-side trail rendering for supporters
 * Purely cosmetic - no gameplay impact!
 */
public class TrailRenderer {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        tickCounter++;

        // Render trails for all players (including the local player)
        for (Player player : mc.level.players()) {
            if (player instanceof AbstractClientPlayer clientPlayer) {
                renderTrailForPlayer(clientPlayer);
            }
        }
    }

    private static void renderTrailForPlayer(AbstractClientPlayer player) {
        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            if (!trails.isTrailEnabled() || trails.getActiveTrail() == null) {
                return;
            }

            // Don't render trails too frequently
            if (tickCounter % 3 != 0) return;

            // Don't render trails when player is standing still
            // Check horizontal movement only (ignore Y for gravity/jumping)
            Vec3 velocity = player.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed < 0.01) return; // Standing still or barely moving

            TrailType trailType = TrailType.fromId(trails.getActiveTrail());
            if (trailType == null) return;

            // Get particle type (tier-based for hearts, fixed for others)
            ParticleOptions particleType;
            if (trailType.isTierBased()) {
                // Hearts - look up tier from supporter service
                Optional<SupporterData> supporterData = SupporterService.getSupporterData(player.getStringUUID());
                String tier = supporterData.map(SupporterData::getTier).orElse("bronze").toLowerCase();

                particleType = switch (tier) {
                    case "gold" -> ModParticles.GOLD_HEART.get();
                    case "silver" -> ModParticles.SILVER_HEART.get();
                    case "bronze" -> ModParticles.BRONZE_HEART.get();
                    default -> ModParticles.BRONZE_HEART.get();
                };
            } else {
                particleType = trailType.getParticleType();
            }

            if (particleType == null) return;

            // Spawn particles at player's feet (accounts for morphs)
            Vec3 pos = player.position();
            double offsetY = 0.1; // Start at feet level (works for all entity heights)

            for (int i = 0; i < trailType.getParticleCount(); i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 0.4;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.4;
                double randomY = RANDOM.nextDouble() * 0.3;

                player.level().addParticle(
                    particleType,
                    pos.x + offsetX,
                    pos.y + offsetY + randomY,
                    pos.z + offsetZ,
                    0, 0, 0
                );
            }
        });
    }
}