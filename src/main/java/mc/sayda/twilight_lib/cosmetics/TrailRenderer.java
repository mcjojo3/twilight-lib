package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.config.TwilightConfig;
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
 */
public class TrailRenderer {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    // Track last positions for accurate velocity calculation (for remote players)
    private static final java.util.Map<java.util.UUID, Vec3> lastPositions = new java.util.concurrent.ConcurrentHashMap<>();

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
        // Don't render trails for invisible players
        if (player.isInvisible()) {
            return;
        }

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            if (!trails.isTrailEnabled() || trails.getActiveTrail() == null) {
                return;
            }

            // Don't render trails too frequently (configurable)
            int updateFrequency = Math.max(1, TwilightConfig.TRAIL_UPDATE_FREQUENCY.get());
            if (tickCounter % updateFrequency != 0) return;

            // Calculate velocity based on position change for more accurate movement detection
            Vec3 currentPos = player.position();
            Vec3 lastPos = lastPositions.get(player.getUUID());

            double horizontalSpeed;
            if (lastPos != null) {
                // Calculate actual movement since last check
                double dx = currentPos.x - lastPos.x;
                double dz = currentPos.z - lastPos.z;
                horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
            } else {
                // First time seeing this player, use getDeltaMovement as fallback
                Vec3 velocity = player.getDeltaMovement();
                horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            }

            // Update last position for next tick
            lastPositions.put(player.getUUID(), currentPos);

            // Don't render trails when player is standing still
            if (horizontalSpeed < TwilightConstants.Trail.MIN_HORIZONTAL_SPEED) return; // Standing still or barely moving

            TrailType trailType = TrailType.fromId(trails.getActiveTrail());
            if (trailType == null) return;

            // Get particle type (tier-based for hearts, fixed for others)
            ParticleOptions particleType;
            if (trailType.isTierBased()) {
                // Hearts - look up tier from supporter service
                Optional<SupporterData> supporterData = SupporterService.getSupporterData(player.getStringUUID());
                String tier = supporterData
                    .map(SupporterData::getTier)
                    .filter(t -> t != null && !t.equalsIgnoreCase("none"))
                    .orElse("bronze")
                    .toLowerCase();

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
            double offsetY = TwilightConstants.Trail.FEET_OFFSET_Y; // Start at feet level (works for all entity heights)

            for (int i = 0; i < trailType.getParticleCount(); i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * TwilightConstants.Trail.PARTICLE_SPREAD_HORIZONTAL;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * TwilightConstants.Trail.PARTICLE_SPREAD_HORIZONTAL;
                double randomY = RANDOM.nextDouble() * TwilightConstants.Trail.PARTICLE_SPREAD_VERTICAL;

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