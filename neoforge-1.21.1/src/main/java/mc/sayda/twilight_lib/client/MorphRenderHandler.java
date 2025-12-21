package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// In 1.21.1, bus parameter is deprecated - use modid for GAME bus events
@EventBusSubscriber(modid = mc.sayda.twilight_lib.TwilightLib.MODID, value = Dist.CLIENT)
public class MorphRenderHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, LivingEntity> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000; // Reasonable upper bound to prevent memory exhaustion
    private static int tickCounter = 0;

    // Cached config value to avoid repeated config access on every tick
    private static int cachedCleanupInterval = 6000; // Default value

    public static void register() {
        // Initialize cached config value
        try {
            cachedCleanupInterval = mc.sayda.twilight_lib.config.TwilightConfig.MORPH_CACHE_CLEANUP_INTERVAL_TICKS.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, use default
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut evt) {
        // ConcurrentHashMap handles thread safety - no synchronization needed
        CACHE.values().forEach(LivingEntity::discard);
        CACHE.clear();
        tickCounter = 0; // Reset cleanup timer
        LOGGER.debug("Goodbye, my new friend! Clearing morph cache on disconnect.");
    }

    @SubscribeEvent
    public static void onEntityLeavelevel(net.neoforged.neoforge.event.level.LevelEvent.Unload evt) {
        // ConcurrentHashMap handles thread safety - no synchronization needed
        CACHE.values().forEach(LivingEntity::discard);
        CACHE.clear();
        tickCounter = 0; // Reset cleanup timer
        LOGGER.debug("I hope this world survives... Clearing morph cache on level unload.");
    }

    /** Tick proxies so animations and timers advance client-side. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // Periodic cleanup: remove stale cache entries based on config interval
        tickCounter++;
        if (tickCounter >= cachedCleanupInterval) {
            tickCounter = 0;
            cleanupStaleEntries(level);
        }

        // Create a snapshot of values to prevent ConcurrentModificationException
        // ConcurrentHashMap.values() returns a thread-safe view
        java.util.List<LivingEntity> entities = new java.util.ArrayList<>(CACHE.values());

        for (LivingEntity le : entities) {
            // Check if entity is still valid before ticking (prevent issues if another thread discards it)
            if (!le.isRemoved()) {
                le.tickCount++;
                if (le instanceof Mob mob) mob.tick();
                else le.baseTick();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre evt) {
        Player player = evt.getEntity();

        // Check if morphs are enabled in config
        if (!mc.sayda.twilight_lib.config.TwilightConfig.ENABLE_MORPHS.get()) {
            return;
        }

        // Don't render morphs for invisible players
        if (player.isInvisible()) {
            return;
        }

        IMorph morph = player.getData(ModAttachments.MORPH);
        Optional<ResourceLocation> rlOpt = morph.getEntityType();
        if (rlOpt.isEmpty()) {
            // Discard old entity before removing from cache
            LivingEntity old = CACHE.remove(player.getUUID());
            if (old != null) {
                old.discard();
            }
            return;
        }

        LivingEntity proxy = getOrCreateProxy(player, rlOpt.get());
        if (proxy == null) return;

        final float pt = evt.getPartialTick();

        // === NAMETAG ===
        // Copy player's display name (includes team colors, prefixes, etc.) to proxy
        // and show it unless hideNametag is enabled
        boolean shouldShowNametag = !morph.isNametagHidden();
        if (shouldShowNametag) {
            proxy.setCustomName(player.getDisplayName());
            proxy.setCustomNameVisible(true);
        } else {
            proxy.setCustomName(null); // Remove custom name entirely when hidden
            proxy.setCustomNameVisible(false);
        }

        // === POSITION & ROTATION ===
        proxy.xo = (float) player.xo;
        proxy.yo = (float) player.yo;
        proxy.zo = (float) player.zo;
        proxy.yRotO = player.yRotO;
        proxy.xRotO = player.xRotO;

        // Calculate Y offset for entities with different poses
        double yOffset = 0.0;

        proxy.setPos(player.getX(), player.getY() + yOffset, player.getZ());
        proxy.setYRot(player.getYRot());
        proxy.setXRot(player.getXRot());
        proxy.yBodyRot = player.yBodyRot;
        proxy.yBodyRotO = player.yBodyRotO; // Body rotation interpolation
        proxy.yHeadRot = player.yHeadRot;
        proxy.yHeadRotO = player.yHeadRotO; // Head rotation interpolation

        // === MOVEMENT STATE ===
        proxy.setOnGround(player.onGround());
        proxy.setSwimming(player.isSwimming());
        proxy.setSprinting(player.isSprinting());
        proxy.setPose(player.getPose());
        proxy.setDeltaMovement(player.getDeltaMovement());
        proxy.setHealth(Math.max(1.0f, player.getHealth()));

        // === ANIMATION STATE (NEW!) ===
        // Arm swing animation
        proxy.attackAnim = player.attackAnim;
        proxy.oAttackAnim = player.oAttackAnim;

        // Swing progress for items
        proxy.swinging = player.swinging;
        proxy.swingTime = player.swingTime;

        // Item usage (eating, blocking, drawing bow, etc.)
        if (player.isUsingItem()) {
            proxy.startUsingItem(player.getUsedItemHand());
        } else {
            proxy.stopUsingItem();
        }

        // Hurt animation (red tint when damaged)
        proxy.hurtTime = player.hurtTime;
        proxy.hurtDuration = player.hurtDuration;

        // Death animation
        proxy.deathTime = player.deathTime;

        // Walk/run animation progress
        proxy.walkAnimation.setSpeed(player.walkAnimation.speed());
        proxy.walkAnimation.position(player.walkAnimation.position());

        // Invulnerability ticks (flashing effect)
        proxy.invulnerableTime = player.invulnerableTime;

        // === EQUIPMENT (so held items render) ===
        // Copy all equipment slots
        for (var slot : EquipmentSlot.values()) {
            proxy.setItemSlot(slot, player.getItemBySlot(slot));
        }

        // Special handling for foxes - animations and item display
        double foxYOffset = 0.0;
        if (proxy instanceof Fox fox) {
            var mainHandItem = player.getMainHandItem();
            if (!mainHandItem.isEmpty()) {
                // Foxes display items in their mouth via NBT
                fox.setItemSlot(EquipmentSlot.MAINHAND, mainHandItem);
            }

            // Sync fox-specific states based on player pose
            boolean isSleeping = player.getPose() == Pose.SLEEPING;
            boolean isCrouching = player.getPose() == Pose.CROUCHING;
            boolean shouldSit = player.isPassenger() && player.getDeltaMovement().lengthSqr() < 0.01;

            // For CustomFoxEntity, we can directly control the sleeping state
            if (fox instanceof mc.sayda.twilight_lib.entity.CustomFoxEntity customFox) {
                customFox.setForceSleeping(isSleeping);
            }

            // Calculate Y offset based on fox state
            if (shouldSit) {
                // Fox sitting is lower - raise it up for vehicles
                foxYOffset = 0.55;
            } else if (isSleeping) {
                // Fox sleeping needs to be lowered significantly and rotated
                foxYOffset = -0.4;
            } else if (isCrouching) {
                // Fox crouching is slightly lower
                foxYOffset = 0;
            }
        }

        // Render proxy instead of player
        evt.setCanceled(true);
        EntityRenderDispatcher disp = Minecraft.getInstance().getEntityRenderDispatcher();
        var poseStack = evt.getPoseStack();
        var buffer = evt.getMultiBufferSource();
        int packedLight = disp.getPackedLightCoords(proxy, pt);

        // Special handling for fox rendering with offsets and rotations
        if (proxy instanceof Fox fox) {
            boolean isSleeping = player.getPose() == Pose.SLEEPING;

            poseStack.pushPose();
            // Apply Y offset for fox poses
            poseStack.translate(0.0, foxYOffset, 0.0);

            if (isSleeping) {
                // Calculate bed-relative offset based on player's yaw
                // Player sleeps with head pointing in the direction of yaw
                float yaw = (float) Math.toRadians(player.getYRot());
                double xOffset = Math.sin(yaw) * 0.15; // Reduced from 0.2, inverted direction
                double zOffset = -Math.cos(yaw) * 0.15; // Reduced from 0.2, inverted direction

                // Apply bed-relative centering offset
                poseStack.translate(xOffset, 0.0, zOffset);

                // Rotate fox 270 degrees around Z axis to lie on side
                // Move to rotation center, rotate, then move back
                poseStack.translate(0.0, 0.3, 0.0); // Adjusted rotation center
                poseStack.mulPose(Axis.ZP.rotationDegrees(270.0f));
                poseStack.translate(0.0, -0.3, 0.0);
            }

            disp.render(proxy, 0.0, 0.0, 0.0, player.getYRot(), pt, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }

        disp.render(proxy, 0.0, 0.0, 0.0, player.getYRot(), pt, poseStack, buffer, packedLight);
    }

    /**
     * Periodic cleanup to prevent unbounded cache growth.
     * Removes entries for players that are no longer in the level.
     */
    private static void cleanupStaleEntries(net.minecraft.client.multiplayer.ClientLevel level) {
        int removed = 0;
        java.util.Iterator<Map.Entry<UUID, LivingEntity>> iterator = CACHE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            LivingEntity proxy = entry.getValue();

            // Check if player is still in the level
            Player player = level.getPlayerByUUID(playerUuid);
            if (player == null) {
                // Player no longer in level - clean up
                proxy.discard();
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            LOGGER.debug("While I wait, I will stay happy! Periodic cleanup removed {} stale morph cache entries", removed);
        }
    }

    private static LivingEntity getOrCreateProxy(Player player, ResourceLocation rl) {
        // Check cache size before adding new entries
        if (CACHE.size() >= MAX_CACHE_SIZE && !CACHE.containsKey(player.getUUID())) {
            LOGGER.error("Really?! Morph cache exceeded maximum size of {}. This indicates a configuration or cleanup issue.", MAX_CACHE_SIZE);
            return null; // Refuse to grow beyond limit
        }

        return CACHE.compute(player.getUUID(), (uuid, cached) -> {
            // If cached entity exists and matches the requested type, return it
            if (cached != null && EntityType.getKey(cached.getType()).equals(rl)) {
                return cached;
            }

            // Create new entity BEFORE discarding old one (to keep old on failure)
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Hello? Hellooo? Level is null...");
                return cached; // Keep old entity if level unavailable
            }

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (type == null) {
                LOGGER.warn("I wonder who's around. Unknown entity type: {}", rl);
                return cached; // Keep old entity if type doesn't exist
            }

            Entity e = type.create(level);

            if (e == null) {
                LOGGER.error("How did I?! Uuuughh! Failed to create entity for morph: {}. Player: {}. " +
                             "This may indicate a mod incompatibility or corrupted entity registry.",
                             rl, uuid);
                return cached; // Keep old entity on failure
            }

            if (!(e instanceof LivingEntity le)) {
                LOGGER.warn("Or, what. Entity {} is not a LivingEntity (actual type: {}). Player: {}. " +
                            "Morphing requires LivingEntity subclasses.",
                            rl, e.getClass().getName(), uuid);
                e.discard(); // Discard failed entity
                return cached; // Keep old entity on failure
            }

            // Only discard old entity after successful creation
            if (cached != null) {
                cached.discard();
            }

            // Setup new entity
            if (le instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setAggressive(false);
            }
            le.setSilent(true);
            le.noPhysics = true;
            le.setCustomNameVisible(false);

            // Log successful morph creation (debug level for production)
            LOGGER.debug("Want to see something neat? Created new morph proxy: {} for player {}", rl, uuid);

            return le;
        });
    }

}