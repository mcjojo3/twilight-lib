package mc.sayda.twilight_lib.cosmetics;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import mc.sayda.twilight_lib.particle.ModParticles;
import mc.sayda.twilight_lib.supporter.SupporterService;
import mc.sayda.twilight_lib.supporter.SupporterData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Random;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.LifecycleEvent;

/**
 * Client-side trail rendering for supporters
 */
public class TrailRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static final double MOVEMENT_EPSILON = 0.001; // Default fallback

    // Track last positions for accurate velocity calculation (for remote players)
    private static final java.util.Map<java.util.UUID, Vec3> lastPositions = new java.util.concurrent.ConcurrentHashMap<>();

    // Track which foot is next for each player (true = left, false = right)
    private static final java.util.Map<java.util.UUID, Boolean> footStepTracker = new java.util.concurrent.ConcurrentHashMap<>();

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            onClientTick();
        });
    }

    public static void onClientDisconnect() {
        lastPositions.clear();
        footStepTracker.clear();
        LOGGER.debug("Goodbye, my new friend! Clearing trail tracking data on disconnect.");
    }

    public static void onLevelUnload() {
        lastPositions.clear();
        footStepTracker.clear();
        LOGGER.debug("I hope this world survives... Clearing trail tracking data on level unload.");
    }

    private static void onClientTick() {
        // Check if trails are enabled in config
        if (!TwilightConfig.ENABLE_TRAILS.get())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused())
            return;

        tickCounter++;

        // Clean up tracking data periodically to prevent memory leaks
        if (tickCounter % TwilightConfig.TRAIL_CLEANUP_INTERVAL_TICKS.get() == 0) {
            java.util.Set<java.util.UUID> currentUUIDs = new java.util.HashSet<>();
            for (Player player : mc.level.players()) {
                currentUUIDs.add(player.getUUID());
            }
            lastPositions.keySet().retainAll(currentUUIDs);
            footStepTracker.keySet().retainAll(currentUUIDs);
        }

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

        // Check render distance limit (0 = unlimited)
        Minecraft mc = Minecraft.getInstance();
        int maxDistance = TwilightConfig.TRAIL_RENDER_DISTANCE.get();
        if (maxDistance > 0 && mc.player != null && !player.equals(mc.player)) {
            double distanceSq = mc.player.distanceToSqr(player);
            if (distanceSq > maxDistance * maxDistance) {
                return;
            }
        }

        ITrails trails = DataUtils.getTrailsData(player);
        if (trails == null) {
            LOGGER.warn("Or, what. Player {} has no trails data!", player.getName().getString());
            return;
        }

        // Get all active trails and render each one
        for (String activeTrailId : trails.getActiveTrails()) {
            TrailType trailType = TrailType.fromId(activeTrailId);
            if (trailType == null || !trailType.isAvailable())
                continue;

            renderSingleTrail(player, trailType);
        }

    }

    private static void renderSingleTrail(AbstractClientPlayer player, TrailType trailType) {
        TrailSpawnMode spawnMode = trailType.getSpawnMode();

        // Check if this trail type is enabled
        if (spawnMode == TrailSpawnMode.FOOTPRINT && !TwilightConfig.ENABLE_FOOTPRINT_TRAILS.get()) {
            return;
        }
        if (spawnMode != TrailSpawnMode.FOOTPRINT && !TwilightConfig.ENABLE_PARTICLE_TRAILS.get()) {
            return;
        }

        // Calculate velocity based on position change for more accurate movement
        // detection (especially for remote players)
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPositions.get(player.getUUID());
        double horizontalSpeed;
        double dx;
        double dz;

        if (lastPos != null) {
            // Calculate actual movement since last check (ticked every tick)
            dx = currentPos.x - lastPos.x;
            dz = currentPos.z - lastPos.z;
            horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        } else {
            // Fallback to delta movement (less accurate for remote players but better than
            // 0)
            Vec3 move = player.getDeltaMovement();
            dx = move.x;
            dz = move.z;
            horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        }

        // Update last position EVERY tick for accurate velocity next tick
        lastPositions.put(player.getUUID(), currentPos);

        // Don't render trails too frequently (configurable)
        // Exception: Footprint trails spawn more frequently for consistent footstep
        // spacing
        int updateFrequency = (spawnMode == TrailSpawnMode.FOOTPRINT) ? TwilightConfig.FOOTPRINT_UPDATE_FREQUENCY.get()
                : Math.max(1, TwilightConfig.TRAIL_UPDATE_FREQUENCY.get());
        if (tickCounter % updateFrequency != 0)
            return;

        // Apply spawn mode restrictions
        switch (spawnMode) {
            case MOVEMENT:
                // Standard movement trail - requires movement, works in air and on ground
                if (horizontalSpeed < mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_MIN_SPEED.get())
                    return;
                break;

            case FOOTPRINT:
                // Footprint trail - requires movement AND being on ground
                if (horizontalSpeed < mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_MIN_SPEED.get())
                    return;
                if (!player.onGround())
                    return; // Don't spawn footprints in air
                break;

            case GROUNDED:
                // Grounded trail - requires movement AND being on ground (but no foot
                // alternation)
                if (horizontalSpeed < mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_MIN_SPEED.get())
                    return;
                if (!player.onGround())
                    return; // Only spawn when on ground
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
                case "platinum" -> ModParticles.PLATINUM_HEART.get();
                case "gold" -> ModParticles.GOLD_HEART.get();
                case "silver" -> ModParticles.SILVER_HEART.get();
                case "bronze" -> ModParticles.BRONZE_HEART.get();
                default -> ModParticles.BRONZE_HEART.get();
            };
        } else {
            particleType = trailType.getParticleType();
        }

        if (particleType == null) {
            LOGGER.warn("How did I?! Uuuughh! Particle type is null for trail {}", trailType.getId());
            return;
        }

        if (TwilightConfig.VERBOSE_LOGGING.get()) {
            LOGGER.info("Twilight Lib: Spawning particle for trail {} for player {}", trailType.getId(),
                    player.getName().getString());
        }

        // Spawn particles at player's feet (accounts for morphs)
        Vec3 pos = player.position();
        double offsetY = mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_FEET_OFFSET_Y.get(); // Start at feet level
                                                                                                // (works for all entity
                                                                                                // heights)

        // Special handling for footprint trails - alternating left/right feet with
        // movement direction
        if (spawnMode == TrailSpawnMode.FOOTPRINT) {
            // Use position-based direction for consistency
            double dirX = dx;
            double dirZ = dz;

            // Normalize if moving (avoid division by zero)
            double length = horizontalSpeed;
            double epsilon = 0.001;
            try {
                epsilon = TwilightConfig.TRAIL_MOVEMENT_EPSILON.get();
            } catch (Exception e) {
            }

            if (length > epsilon) {
                dirX /= length;
                dirZ /= length;
            }

            // Alternate between left and right foot
            boolean isLeftFoot = footStepTracker.getOrDefault(player.getUUID(), true);
            footStepTracker.put(player.getUUID(), !isLeftFoot);

            // Calculate perpendicular offset for left/right foot placement
            // Perpendicular vector to movement direction (rotate 90 degrees)
            double perpX = -dirZ; // Perpendicular X
            double perpZ = dirX; // Perpendicular Z

            // Offset distance from center (paw width)
            double footOffset = mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_FOOTPRINT_OFFSET_LATERAL.get(); // Distance
                                                                                                                  // from
                                                                                                                  // center
                                                                                                                  // line
            double lateralOffset = isLeftFoot ? -footOffset : footOffset;

            // Calculate footprint position with left/right offset
            double footX = pos.x + (perpX * lateralOffset);
            double footZ = pos.z + (perpZ * lateralOffset);

            // Spawn footprint particles with movement direction
            for (int i = 0; i < trailType.getParticleCount(); i++) {
                player.level().addParticle(
                        particleType,
                        footX,
                        pos.y + mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_FOOTPRINT_OFFSET_Y.get(), // Just
                                                                                                            // barely
                                                                                                            // above
                                                                                                            // ground
                        footZ,
                        dirX, 0, dirZ // Pass movement direction
                );
            }
        } else {
            // Normal trail particles with random spread
            for (int i = 0; i < trailType.getParticleCount(); i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5)
                        * mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_SPREAD_HORIZONTAL.get();
                double offsetZ = (RANDOM.nextDouble() - 0.5)
                        * mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_SPREAD_HORIZONTAL.get();
                double randomY = RANDOM.nextDouble()
                        * mc.sayda.twilight_lib.config.TwilightConfig.TRAIL_SPREAD_VERTICAL.get();

                player.level().addParticle(
                        particleType,
                        pos.x + offsetX,
                        pos.y + offsetY + randomY,
                        pos.z + offsetZ,
                        0, 0, 0);
            }
        }
    }
}
