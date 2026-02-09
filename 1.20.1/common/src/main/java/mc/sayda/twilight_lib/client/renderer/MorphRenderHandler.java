package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.config.TwilightConfig;
import mc.sayda.twilight_lib.entity.CustomFoxEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles rendering of player morphs by creating and managing proxy entities.
 * Ported from legacy Forge implementation to common module.
 */
public class MorphRenderHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, LivingEntity> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_RENDERED = new ConcurrentHashMap<>();
    private static int tickCounter = 0;
    private static int cachedCleanupInterval = 400;

    public static void init() {
        try {
            cachedCleanupInterval = TwilightConfig.MORPH_CACHE_CLEANUP_INTERVAL_TICKS.get();
        } catch (Exception e) {
            // Config not loaded yet
        }
    }

    public static void onClientDisconnect() {
        CACHE.values().forEach(Entity::discard);
        CACHE.clear();
        LAST_RENDERED.clear();
        tickCounter = 0;
    }

    public static void onLevelUnload() {
        CACHE.values().forEach(Entity::discard);
        CACHE.clear();
        tickCounter = 0;
    }

    public static void onClientTick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null)
            return;

        tickCounter++;
        if (tickCounter >= cachedCleanupInterval) {
            tickCounter = 0;
            cleanupStaleEntries(level);
        }

        for (Map.Entry<UUID, LivingEntity> entry : CACHE.entrySet()) {
            LivingEntity le = entry.getValue();
            if (!le.isRemoved()) {
                // Only tick if recently rendered
                Long lastRender = LAST_RENDERED.get(entry.getKey());
                long threshold = 100;
                try {
                    threshold = TwilightConfig.MORPH_PROXY_TICK_THRESHOLD_TICKS.get();
                } catch (Exception e) {
                }

                if (lastRender != null && (level.getGameTime() - lastRender) < threshold) {
                    le.tickCount++;
                    if (le instanceof Mob mob)
                        mob.tick();
                    else
                        le.baseTick();
                }
            }
        }
    }

    /**
     * Called from PlayerRendererMixin before rendering the player.
     * 
     * @return true if a morph was rendered and original rendering should be
     *         cancelled.
     */
    public static boolean onRenderPlayerPre(Player player, PlayerRenderer renderer, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!TwilightConfig.ENABLE_MORPHS.get() || player.isInvisible()) {
            return false;
        }

        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null)
            return false;

        Optional<mc.sayda.twilight_lib.api.morph.IMorph> activeMorph = morph.getMorph();
        if (activeMorph.isEmpty()) {
            LivingEntity old = CACHE.remove(player.getUUID());
            if (old != null)
                old.discard();
            return false;
        }

        mc.sayda.twilight_lib.api.morph.IMorph m = activeMorph.get();
        LivingEntity proxy = getOrCreateProxy(player, m.getId());
        if (proxy == null)
            return false;

        syncProxyState(player, proxy, morph, m);
        LAST_RENDERED.put(player.getUUID(), player.level().getGameTime());

        renderProxy(player, proxy, partialTicks, poseStack, buffer);
        return true;
    }

    private static void syncProxyState(Player player, LivingEntity proxy, IMorph morphCap,
            mc.sayda.twilight_lib.api.morph.IMorph activeMorph) {
        // Nametag
        boolean shouldShowNametag = !morphCap.isNametagHidden() && !activeMorph.shouldHideNametag();
        if (shouldShowNametag) {
            proxy.setCustomName(player.getDisplayName());
            proxy.setCustomNameVisible(true);
        } else {
            proxy.setCustomName(null);
            proxy.setCustomNameVisible(false);
        }

        // Position & Rotation
        proxy.xo = player.xo;
        proxy.yo = player.yo;
        proxy.zo = player.zo;
        proxy.yRotO = player.yRotO;
        proxy.xRotO = player.xRotO;
        proxy.setPos(player.getX(), player.getY(), player.getZ());
        proxy.setYRot(player.getYRot());
        proxy.setXRot(player.getXRot());
        proxy.yBodyRot = player.yBodyRot;
        proxy.yBodyRotO = player.yBodyRotO;
        proxy.yHeadRot = player.yHeadRot;
        proxy.yHeadRotO = player.yHeadRotO;

        // Movement & Pose
        proxy.setOnGround(player.onGround());
        proxy.setSwimming(player.isSwimming());
        proxy.setSprinting(player.isSprinting());
        proxy.setPose(player.getPose());
        proxy.setDeltaMovement(player.getDeltaMovement());
        proxy.setHealth(Math.max(1.0f, player.getHealth()));

        // Animations
        proxy.attackAnim = player.attackAnim;
        proxy.oAttackAnim = player.oAttackAnim;
        proxy.swinging = player.swinging;
        proxy.swingTime = player.swingTime;

        if (player.isUsingItem())
            proxy.startUsingItem(player.getUsedItemHand());
        else
            proxy.stopUsingItem();

        proxy.hurtTime = player.hurtTime;
        proxy.hurtDuration = player.hurtDuration;
        proxy.deathTime = player.deathTime;
        proxy.walkAnimation.setSpeed(player.walkAnimation.speed());
        proxy.walkAnimation.position(player.walkAnimation.position());
        proxy.invulnerableTime = player.invulnerableTime;

        // Equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            proxy.setItemSlot(slot, player.getItemBySlot(slot));
        }

        // Fox specifics
        if (proxy instanceof Fox fox) {
            var mainHandItem = player.getMainHandItem();
            if (!mainHandItem.isEmpty())
                fox.setItemSlot(EquipmentSlot.MAINHAND, mainHandItem);

            if (fox instanceof CustomFoxEntity customFox) {
                customFox.setForceSleeping(player.getPose() == Pose.SLEEPING);
            }
        }
    }

    private static void renderProxy(Player player, LivingEntity proxy, float pt, PoseStack poseStack,
            MultiBufferSource buffer) {
        EntityRenderDispatcher disp = Minecraft.getInstance().getEntityRenderDispatcher();
        int packedLight = disp.getPackedLightCoords(proxy, pt);

        double yOffset = 0.0;
        if (proxy instanceof Fox) {
            boolean isSleeping = player.getPose() == Pose.SLEEPING;
            boolean shouldSit = player.isPassenger() && player.getDeltaMovement().lengthSqr() < 0.01;

            if (shouldSit)
                yOffset = 0.55;
            else if (isSleeping)
                yOffset = -0.4;

            poseStack.pushPose();
            poseStack.translate(0.0, yOffset, 0.0);

            if (isSleeping) {
                float yaw = (float) Math.toRadians(player.getYRot());
                double xOffset = Math.sin(yaw) * 0.15;
                double zOffset = -Math.cos(yaw) * 0.15;
                poseStack.translate(xOffset, 0.0, zOffset);
                poseStack.translate(0.0, 0.3, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(270.0f));
                poseStack.translate(0.0, -0.3, 0.0);
            }
            disp.render(proxy, 0.0, 0.0, 0.0, player.getYRot(), pt, poseStack, buffer, packedLight);
            poseStack.popPose();
        } else {
            disp.render(proxy, 0.0, 0.0, 0.0, player.getYRot(), pt, poseStack, buffer, packedLight);
        }
    }

    private static void cleanupStaleEntries(ClientLevel level) {
        int removed = 0;
        var iterator = CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (level.getPlayerByUUID(entry.getKey()) == null) {
                entry.getValue().discard();
                iterator.remove();
                LAST_RENDERED.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0)
            LOGGER.debug("While I wait, I will stay happy! Periodic cleanup removed {} stale morph cache entries",
                    removed);
    }

    private static LivingEntity getOrCreateProxy(Player player, ResourceLocation rl) {
        int maxCacheSize = 1000;
        try {
            maxCacheSize = TwilightConfig.MAX_MORPH_PROXY_CACHE_SIZE.get();
        } catch (Exception e) {
        }

        if (CACHE.size() >= maxCacheSize && !CACHE.containsKey(player.getUUID())) {
            LOGGER.error("Or, what. Morph cache exceeded maximum size of {}", maxCacheSize);
            return null;
        }

        return CACHE.compute(player.getUUID(), (uuid, cached) -> {
            if (cached != null && EntityType.getKey(cached.getType()).equals(rl)) {
                return cached;
            }

            ClientLevel level = Minecraft.getInstance().level;
            if (level == null)
                return cached;

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (type == null)
                return cached;

            Entity e = type.create(level);
            if (!(e instanceof LivingEntity le)) {
                if (e != null)
                    e.discard();
                return cached;
            }

            if (cached != null)
                cached.discard();

            if (le instanceof Mob mob) {
                mob.setNoAi(true);
            }
            le.setSilent(true);
            le.noPhysics = true;
            le.setCustomNameVisible(false);

            return le;
        });
    }
}
