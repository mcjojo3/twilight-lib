package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
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
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Random;

/**
 * Client-side trail rendering for supporters
 */
public class TrailRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    // Track last positions for accurate velocity calculation (for remote players)
    private static final java.util.Map<java.util.UUID, Vec3> lastPositions = new java.util.concurrent.ConcurrentHashMap<>();

    // Track which foot is next for each player (true = left, false = right)
    private static final java.util.Map<java.util.UUID, Boolean> footStepTracker = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut evt) {
        // Clean up tracking maps to prevent memory leak
        lastPositions.clear();
        footStepTracker.clear();
        LOGGER.debug("See ya real soon! Clearing trail tracking data on disconnect.");
    }

    @SubscribeEvent
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload evt) {
        // Clean up tracking maps when level unloads
        lastPositions.clear();
        footStepTracker.clear();
        LOGGER.debug("Take care of the place for me, okay? Clearing trail tracking data on level unload.");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Check if trails are enabled in config
        if (!TwilightConfig.ENABLE_TRAILS.get()) return;

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

            TrailType trailType = TrailType.fromId(trails.getActiveTrail());
            if (trailType == null) return;

            TrailSpawnMode spawnMode = trailType.getSpawnMode();

            // Don't render trails too frequently (configurable)
            // Exception: Footprint trails spawn more frequently for consistent footstep spacing
            int updateFrequency = (spawnMode == TrailSpawnMode.FOOTPRINT) ?
                TwilightConfig.FOOTPRINT_UPDATE_FREQUENCY.get() : Math.max(1, TwilightConfig.TRAIL_UPDATE_FREQUENCY.get());
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

            // Apply spawn mode restrictions
            switch (spawnMode) {
                case MOVEMENT:
                    // Standard movement trail - requires movement, works in air and on ground
                    if (horizontalSpeed < TwilightConstants.Trail.MIN_HORIZONTAL_SPEED) return;
                    break;

                case FOOTPRINT:
                    // Footprint trail - requires movement AND being on ground
                    if (horizontalSpeed < TwilightConstants.Trail.MIN_HORIZONTAL_SPEED) return;
                    if (!player.onGround()) return; // Don't spawn footprints in air
                    break;

                case GROUNDED:
                    // Grounded trail - requires movement AND being on ground (but no foot alternation)
                    if (horizontalSpeed < TwilightConstants.Trail.MIN_HORIZONTAL_SPEED) return;
                    if (!player.onGround()) return; // Only spawn when on ground
                    break;

                case CONTINUOUS:
                    // Continuous trail - always spawns, no restrictions
                    // No checks needed - spawn regardless of movement or ground state
                    break;
            }

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

            // Special handling for footprint trails - alternating left/right feet with movement direction
            if (spawnMode == TrailSpawnMode.FOOTPRINT) {
                // Calculate movement direction vector
                Vec3 movement = player.getDeltaMovement();
                double dirX = movement.x;
                double dirZ = movement.z;

                // Normalize if moving (avoid division by zero)
                double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (length > 0.001) {
                    dirX /= length;
                    dirZ /= length;
                }

                // Alternate between left and right foot
                boolean isLeftFoot = footStepTracker.getOrDefault(player.getUUID(), true);
                footStepTracker.put(player.getUUID(), !isLeftFoot);

                // Calculate perpendicular offset for left/right foot placement
                // Perpendicular vector to movement direction (rotate 90 degrees)
                double perpX = -dirZ;  // Perpendicular X
                double perpZ = dirX;   // Perpendicular Z

                // Offset distance from center (paw width)
                double footOffset = 0.15; // Distance from center line
                double lateralOffset = isLeftFoot ? -footOffset : footOffset;

                // Calculate footprint position with left/right offset
                double footX = pos.x + (perpX * lateralOffset);
                double footZ = pos.z + (perpZ * lateralOffset);

                // Spawn footprint particles with movement direction
                for (int i = 0; i < trailType.getParticleCount(); i++) {
                    player.level().addParticle(
                        particleType,
                        footX,
                        pos.y + offsetY - 0.05, // Slightly below feet level for ground contact
                        footZ,
                        dirX, 0, dirZ // Pass movement direction
                    );
                }
            } else {
                // Normal trail particles with random spread
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
            }
        });
    }
}