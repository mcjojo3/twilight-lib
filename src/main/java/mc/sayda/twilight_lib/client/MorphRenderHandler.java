package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MorphRenderHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, LivingEntity> CACHE = new ConcurrentHashMap<>();

    public static void register() { /* no-op - static subscriber */ }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut evt) {
        // Discard all cached entities before clearing to release resources
        CACHE.values().forEach(LivingEntity::discard);
        CACHE.clear();
        LOGGER.debug("Goodbye, my new friend! Clearing morph cache on disconnect.");
    }

    @SubscribeEvent
    public static void onEntityLeavelevel(net.minecraftforge.event.level.LevelEvent.Unload evt) {
        // Discard all cached entities before clearing to release resources
        CACHE.values().forEach(LivingEntity::discard);
        CACHE.clear();
        LOGGER.debug("I hope this world survives... Clearing morph cache on level unload.");
    }

    /** Tick proxies so animations and timers advance client-side. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (LivingEntity le : CACHE.values()) {
            le.tickCount++;
            if (le instanceof Mob mob) mob.tick();
            else le.baseTick();
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre evt) {
        Player player = evt.getEntity();
        LazyOptional<IMorph> cap = player.getCapability(MorphProvider.MORPH_CAP);
        if (!cap.isPresent()) {
            // LOGGER.debug("No morph capability for player {}", player.getName().getString());
            return;
        }

        IMorph morph = cap.orElse(null);
        if (morph == null) {
            // LOGGER.debug("Morph capability is null for player {}", player.getName().getString());
            return;
        }
        Optional<ResourceLocation> rlOpt = morph.getEntityType();
        // LOGGER.debug("Player {} has morph type: {}", player.getName().getString(), rlOpt);
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
        proxy.yBodyRotO = player.yBodyRotO; // NEW: Body rotation interpolation
        proxy.yHeadRot = player.yHeadRot;
        proxy.yHeadRotO = player.yHeadRotO; // NEW: Head rotation interpolation

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
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            proxy.setItemSlot(slot, player.getItemBySlot(slot));
        }

        // Special handling for foxes - animations and item display
        double foxYOffset = 0.0;
        if (proxy instanceof net.minecraft.world.entity.animal.Fox fox) {
            var mainHandItem = player.getMainHandItem();
            if (!mainHandItem.isEmpty()) {
                // Foxes display items in their mouth via NBT
                fox.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, mainHandItem);
            }

            // Sync fox-specific states based on player pose using entity data (accessible)
            boolean isSleeping = player.getPose() == net.minecraft.world.entity.Pose.SLEEPING;
            boolean isCrouching = player.getPose() == net.minecraft.world.entity.Pose.CROUCHING;
            boolean shouldSit = player.isPassenger() && player.getDeltaMovement().lengthSqr() < 0.01;

            setFoxState(fox, "Crouching", isCrouching);
            setFoxState(fox, "Sleeping", isSleeping);
            setFoxState(fox, "Sitting", shouldSit);

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
        if (proxy instanceof net.minecraft.world.entity.animal.Fox fox) {
            boolean isSleeping = player.getPose() == net.minecraft.world.entity.Pose.SLEEPING;

            poseStack.pushPose();
            // Apply Y offset for fox poses
            poseStack.translate(0.0, foxYOffset, 0.0);

            if (isSleeping) {
                // Calculate bed-relative offset based on player's yaw
                float yaw = (float) Math.toRadians(player.getYRot());
                double xOffset = -Math.sin(yaw) * 0.2;
                double zOffset = Math.cos(yaw) * 0.2;

                // Apply bed-relative centering offset
                poseStack.translate(xOffset, 0.0, zOffset);

                // Rotate fox 270 degrees (90 + 180) to lie on its back properly
                poseStack.translate(0.0, 0.35, 0.0);
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(270.0f));
                poseStack.translate(0.0, -0.35, 0.0);
            }

            disp.render(proxy, 0.0, 0.0, 0.0, player.getYRot(), pt, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }

        disp.render(proxy, 0.0, 0.0, 0.0, player.getYRot(), pt, poseStack, buffer, packedLight);
    }

    private static LivingEntity getOrCreateProxy(Player player, ResourceLocation rl) {
        return CACHE.compute(player.getUUID(), (uuid, cached) -> {
            // If cached entity exists and matches the requested type, return it
            if (cached != null && EntityType.getKey(cached.getType()).equals(rl)) {
                return cached;
            }

            // Discard old entity before creating new one to prevent memory leak
            if (cached != null) {
                cached.discard();
            }

            // Otherwise create new entity
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("This virtual reality is so lifelike! But level is null...");
                return null;
            }

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            Entity e = type.create(level);

            if (e == null) {
                LOGGER.error("Oh, dung beetles! Failed to create entity for morph: {}", rl);
                return null;
            }

            if (!(e instanceof LivingEntity le)) {
                LOGGER.warn("Is this the best physical representation you can manifest? Entity {} is not a LivingEntity", rl);
                return null;
            }

            if (le instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setAggressive(false);
            }
            le.setSilent(true);
            le.noPhysics = true;
            le.setCustomNameVisible(false);

            return le;
        });
    }

    /**
     * Sets fox animation states using reflection to access package-private methods.
     * Falls back gracefully if reflection fails.
     */
    private static void setFoxState(net.minecraft.world.entity.animal.Fox fox, String stateName, boolean value) {
        try {
            String methodName = "set" + stateName;
            Method method = net.minecraft.world.entity.animal.Fox.class.getDeclaredMethod(methodName, boolean.class);
            method.setAccessible(true);
            method.invoke(fox, value);
        } catch (Exception e) {
            // Silently fail - animation states are cosmetic only
        }
    }
}