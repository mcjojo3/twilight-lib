package mc.sayda.twilight_lib.client.renderer;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.slf4j.Logger;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.client.model.IAddonModel;
import mc.sayda.twilight_lib.client.model.addon.ChestModel;
import mc.sayda.twilight_lib.client.model.addon.ChestArmorModel;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerAddonLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * LRU cache for baked addon models.
     * Key: addon ID (e.g., "kitsune_ears_white")
     * Value: Baked EntityModel & IAddonModel instance
     * Access-ordered to evict least recently used models when capacity is exceeded.
     * Max size is configurable via TwilightConfig.MAX_CACHED_ADDON_MODELS.
     *
     * <p>
     * <b>Thread Safety</b>: Wrapped with Collections.synchronizedMap() to prevent
     * ConcurrentModificationException when multiple players are rendered
     * simultaneously
     * on multi-threaded renderers. The removeEldestEntry check is synchronized
     * internally.
     */
    private final Map<String, Object> bakedModels = Collections
            .synchronizedMap(new LinkedHashMap<String, Object>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                    // No additional synchronization needed - Collections.synchronizedMap() handles
                    // thread safety
                    // Config access outside any manual synchronized blocks to prevent potential
                    // deadlock
                    // No explicit cleanup needed - models don't hold native GPU resources
                    // Minecraft's resource management handles texture/geometry lifecycle
                    return size() > TwilightConfig.MAX_CACHED_ADDON_MODELS.get();
                }
            });

    public PlayerAddonLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {

        // Check if addons are enabled in config
        if (!TwilightConfig.ENABLE_ADDONS.get()) {
            return;
        }

        // Don't render addons for invisible players
        if (player.isInvisible()) {
            return;
        }

        // Get player's equipped addons
        var addons = mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
        if (addons == null) return;
        PlayerModel<AbstractClientPlayer> playerModel = this.getParentModel();

        // Check if any addon forces all addons to be translucent
        boolean forceAllTranslucent = addons.getActiveAddons().stream()
                .map(id -> {
                    if (id == null)
                        return ResourceLocation.fromNamespaceAndPath("twilight_lib", "unknown");
                    ResourceLocation resLoc = ResourceLocation.tryParse(id);
                    return resLoc != null ? resLoc
                            : ResourceLocation.fromNamespaceAndPath("twilight_lib", id.toLowerCase());
                })
                .anyMatch(id -> AddonRegistry.getInstance().get(id)
                        .map(mc.sayda.twilight_lib.api.addon.IAddon::forceAllTranslucent)
                        .orElse(false));

        // Render each active addon
        for (String addonIdString : addons.getActiveAddons()) {
            if (addonIdString == null)
                continue;
            ResourceLocation addonId = ResourceLocation.tryParse(addonIdString);
            if (addonId == null || (addonId != null && addonId.getNamespace().equals("minecraft"))) {
                addonId = ResourceLocation.fromNamespaceAndPath("twilight_lib", addonIdString.toLowerCase());
            }

            // Capture effectively final ID for lambda
            final ResourceLocation finalAddonId = addonId;

            var addonOptional = AddonRegistry.getInstance().get(finalAddonId);
            if (addonOptional.isEmpty()) {
                // Throttle this warning or only show on debug
                if (TwilightConfig.VERBOSE_LOGGING.get()) {
                    LOGGER.warn("Or, what. Addon not found in registry: {}", finalAddonId);
                }
            }

            addonOptional.ifPresent(addon -> {
                // Filter addons based on mod requirements and config (FORCE_LOAD_ALL_ADDONS)
                // This happens during rendering when the config is guaranteed to be loaded
                if (!mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(addon.getRequiredModIds())) {
                    return;
                }

                // Get or bake the model
                var addonModel = getOrBakeModel(finalAddonId, addon);

                // Special handling for chest addons - optionally hide when wearing armor
                if (addonModel instanceof ChestModel<?>) {
                    ItemStack chestArmor = player.getItemBySlot(EquipmentSlot.CHEST);
                    if (!chestArmor.isEmpty()) {
                        boolean hideAddon = false;

                        if (chestArmor.getItem() instanceof ArmorItem armorItem) {
                            // Check for custom armor using NeoForge layers
                            var layers = armorItem.getMaterial().value().layers();
                            if (!layers.isEmpty()) {
                                ResourceLocation armorTexture = layers.get(0).texture(false);
                                if (Minecraft.getInstance().getResourceManager().getResource(armorTexture).isEmpty()) {
                                    // It's custom armor (texture missing from standard location)
                                    if (TwilightConfig.HIDE_CHEST_IN_CUSTOM_ARMOR.get()) {
                                        hideAddon = true;
                                    }
                                } else {
                                    // Standard vanilla armor
                                    if (TwilightConfig.HIDE_CHEST_IN_ARMOR.get()) {
                                        hideAddon = true;
                                    }
                                }
                            } else {
                                // No layers defined, assume custom armor
                                if (TwilightConfig.HIDE_CHEST_IN_CUSTOM_ARMOR.get()) {
                                    hideAddon = true;
                                }
                            }
                        } else {
                            // Not an ArmorItem at all (like Elytra)
                            if (TwilightConfig.HIDE_CHEST_IN_CUSTOM_ARMOR.get()) {
                                hideAddon = true;
                            }
                        }

                        if (hideAddon) {
                            return; // Skip rendering the chest addon
                        }
                    }
                }

                // Sync model parts to player model (only if they exist)
                addonModel.getHead().ifPresent(part -> copyModelPart(playerModel.head, part));
                addonModel.getBody().ifPresent(part -> copyModelPart(playerModel.body, part));
                addonModel.getRightArm().ifPresent(part -> copyModelPart(playerModel.rightArm, part));
                addonModel.getLeftArm().ifPresent(part -> copyModelPart(playerModel.leftArm, part));
                addonModel.getRightLeg().ifPresent(part -> copyModelPart(playerModel.rightLeg, part));
                addonModel.getLeftLeg().ifPresent(part -> copyModelPart(playerModel.leftLeg, part));

                // Handle body part hiding
                Map<ModelPart, Boolean> visibilityState = new java.util.HashMap<>();
                if (addon.hidesPlayerModel()) {
                    hideAll(playerModel, visibilityState);
                } else {
                    for (mc.sayda.twilight_lib.addon.BodyPart part : addon.getHiddenBodyParts()) {
                        hidePart(playerModel, part, visibilityState);
                    }
                }

                try {
                    // Setup animations (runs AFTER sync so custom animations can use player
                    // movement)
                    // Validate type before unchecked cast
                    if (!(addonModel instanceof EntityModel<?>)) {
                        // Only log error once per session ideally, but for now just leave it as it
                        // indicates a real setup issue
                        LOGGER.error(
                                "Is this the best physical representation you can manifest? Addon model {} is not an EntityModel: {}",
                                finalAddonId, addonModel.getClass());
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    EntityModel<Entity> entityModel = (EntityModel<Entity>) addonModel;
                    entityModel.setupAnim((Entity) player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw,
                            headPitch);

                    // Render the addon - use player skin texture if specified, otherwise use
                    // addon's custom texture
                    var textureToUse = addon.usePlayerSkin() ? player.getSkin().texture() : addon.getTexture();

                    // Check if THIS addon is the one forcing translucency
                    boolean thisAddonForcesTranslucency = addon.forceAllTranslucent();

                    // Translucency rules (spotlight effect):
                    // 1. If THIS addon is naturally translucent → make it translucent
                    // 2. If THIS addon is forcing others translucent → keep THIS opaque (spotlight:
                    // forces others but stays solid)
                    // 3. If ANOTHER addon is forcing translucency → make THIS translucent (follow
                    // the force)
                    boolean shouldBeTranslucent = addon.isTranslucent()
                            || (forceAllTranslucent && !thisAddonForcesTranslucency);

                    // Use translucent render type for transparent addons
                    @SuppressWarnings("null")
                    RenderType renderType = shouldBeTranslucent ? RenderType.entityTranslucent(textureToUse)
                            : RenderType.entityCutoutNoCull(textureToUse);
                    @SuppressWarnings("null")
                    VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

                    // Use vanilla's official overlay calculation for damage effects
                    int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

                    // Get tint color for this addon (stored as 0xRRGGBB)
                    int tintColor = addons.getAddonTint(addonIdString);
                    // Mask to ensure only RGB, no alpha or sign extension issues
                    tintColor = tintColor & 0x00FFFFFF;
                    final float tintRed = ((tintColor >> 16) & 0xFF) / 255.0F;
                    final float tintGreen = ((tintColor >> 8) & 0xFF) / 255.0F;
                    final float tintBlue = (tintColor & 0xFF) / 255.0F;

                    // Wrap vertex consumer to apply tint color AND transparency
                    final float targetAlpha = shouldBeTranslucent
                            ? Math.max(0.0F, Math.min(1.0F, TwilightConfig.TRANSLUCENT_ADDON_ALPHA.get().floatValue()))
                            : 1.0F;
                    // Create a wrapped vertex consumer to handle tinting and alpha transparency
                    VertexConsumer finalConsumer = new TintedVertexConsumer(vertexConsumer, tintRed, tintGreen,
                            tintBlue,
                            targetAlpha);

                    // Render with wrapped vertex consumer (white color since tint is applied in
                    // wrapper)
                    entityModel.renderToBuffer(poseStack, finalConsumer, packedLight, overlay,
                            FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F));

                    // If this is a chest addon and player is wearing chest armor, render the armor
                    // overlay
                    if (addonModel instanceof ChestModel<?>) {
                        ItemStack chestArmor = player.getItemBySlot(EquipmentSlot.CHEST);
                        if (!chestArmor.isEmpty() && chestArmor.getItem() instanceof ArmorItem armorItem) {
                            try {
                                // Bake the chest armor model
                                ChestArmorModel<?> chestArmorModel = getOrBakeChestArmorModel();

                                // Sync it to the player model
                                chestArmorModel.getBody().ifPresent(part -> copyModelPart(playerModel.body, part));

                                // Setup animations
                                // Validate type before unchecked cast
                                if (!(chestArmorModel instanceof EntityModel<?>)) {
                                    LOGGER.error(
                                            "Is this the best physical representation you can manifest? Chest armor model is not an EntityModel: {}",
                                            chestArmorModel.getClass());
                                    return;
                                }
                                @SuppressWarnings("unchecked")
                                EntityModel<Entity> armorEntityModel = (EntityModel<Entity>) chestArmorModel;
                                armorEntityModel.setupAnim((Entity) player, limbSwing, limbSwingAmount, ageInTicks,
                                        netHeadYaw, headPitch);

                                // Render with armor texture - use NeoForge's data-driven layer system
                                var layers = armorItem.getMaterial().value().layers();
                                if (!layers.isEmpty()) {
                                    // Get first layer's texture (chest armor layer)
                                    var layer = layers.get(0);
                                    ResourceLocation armorTexture = layer.texture(false); // false = not dying overlay

                                    // Check if the texture exists before rendering
                                    if (Minecraft.getInstance().getResourceManager().getResource(armorTexture).isEmpty()) {
                                            return; // Fallback: skip armor extension if texture is missing
                                    }

                                    @SuppressWarnings("null")
                                    RenderType armorRenderType = RenderType.entityCutoutNoCull(armorTexture);
                                    @SuppressWarnings("null")
                                    VertexConsumer armorVertexConsumer = buffer.getBuffer(armorRenderType);
                                    // Use the same vanilla overlay for armor as the addon
                                    armorEntityModel.renderToBuffer(poseStack, armorVertexConsumer, packedLight,
                                            overlay,
                                            FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F));
                                }
                            } catch (Exception e) {
                                // If armor rendering fails for any reason, just skip it - the chest addon will
                                // still render
                                LOGGER.warn("How did I?! Uuuughh! Failed to render armor overlay for {}, skipping",
                                        chestArmor.getItem(), e);
                            }
                        }
                    }
                } finally {
                    // Restore visibility
                    visibilityState.forEach((part, visible) -> part.visible = visible);
                }
            });
        }

    }

    @SuppressWarnings("unchecked")
    private IAddonModel getOrBakeModel(ResourceLocation addonId, mc.sayda.twilight_lib.api.addon.IAddon addon) {
        return (IAddonModel) bakedModels.computeIfAbsent(addonId.toString(), id -> {
            var context = Minecraft.getInstance().getEntityModels();
            var modelPart = context.bakeLayer(addon.getLayerLocation());
            return addon.createModel(modelPart);
        });
    }

    @SuppressWarnings("unchecked")
    private ChestArmorModel<?> getOrBakeChestArmorModel() {
        return (ChestArmorModel<?>) bakedModels.computeIfAbsent("__chest_armor_internal__", id -> {
            var context = Minecraft.getInstance().getEntityModels();
            var modelPart = context.bakeLayer(ChestArmorModel.LAYER_LOCATION);
            return new ChestArmorModel<>(modelPart);
        });
    }

    private void copyModelPart(ModelPart from, ModelPart to) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
    }

    private void hideAll(PlayerModel<AbstractClientPlayer> model, Map<ModelPart, Boolean> state) {
        state.put(model.head, model.head.visible);
        state.put(model.hat, model.hat.visible);
        state.put(model.body, model.body.visible);
        state.put(model.jacket, model.jacket.visible);
        state.put(model.rightArm, model.rightArm.visible);
        state.put(model.rightSleeve, model.rightSleeve.visible);
        state.put(model.leftArm, model.leftArm.visible);
        state.put(model.leftSleeve, model.leftSleeve.visible);
        state.put(model.rightLeg, model.rightLeg.visible);
        state.put(model.rightPants, model.rightPants.visible);
        state.put(model.leftLeg, model.leftLeg.visible);
        state.put(model.leftPants, model.leftPants.visible);

        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.jacket.visible = false;
        model.rightArm.visible = false;
        model.rightSleeve.visible = false;
        model.leftArm.visible = false;
        model.leftSleeve.visible = false;
        model.rightLeg.visible = false;
        model.rightPants.visible = false;
        model.leftLeg.visible = false;
        model.leftPants.visible = false;
    }

    private void hidePart(PlayerModel<AbstractClientPlayer> model, mc.sayda.twilight_lib.addon.BodyPart part,
            Map<ModelPart, Boolean> state) {
        switch (part) {
            case HEAD -> {
                state.put(model.head, model.head.visible);
                state.put(model.hat, model.hat.visible);
                model.head.visible = false;
                model.hat.visible = false;
            }
            case HAT -> {
                state.put(model.hat, model.hat.visible);
                model.hat.visible = false;
            }
            case BODY -> {
                state.put(model.body, model.body.visible);
                state.put(model.jacket, model.jacket.visible);
                model.body.visible = false;
                model.jacket.visible = false;
            }
            case RIGHT_ARM -> {
                state.put(model.rightArm, model.rightArm.visible);
                state.put(model.rightSleeve, model.rightSleeve.visible);
                model.rightArm.visible = false;
                model.rightSleeve.visible = false;
            }
            case LEFT_ARM -> {
                state.put(model.leftArm, model.leftArm.visible);
                state.put(model.leftSleeve, model.leftSleeve.visible);
                model.leftArm.visible = false;
                model.leftSleeve.visible = false;
            }
            case RIGHT_LEG -> {
                state.put(model.rightLeg, model.rightLeg.visible);
                state.put(model.rightPants, model.rightPants.visible);
                model.rightLeg.visible = false;
                model.rightPants.visible = false;
            }
            case LEFT_LEG -> {
                state.put(model.leftLeg, model.leftLeg.visible);
                state.put(model.leftPants, model.leftPants.visible);
                model.leftLeg.visible = false;
                model.leftPants.visible = false;
            }
            case JACKET -> {
                state.put(model.jacket, model.jacket.visible);
                model.jacket.visible = false;
            }
            case LEFT_PANTS -> {
                state.put(model.leftPants, model.leftPants.visible);
                model.leftPants.visible = false;
            }
            case RIGHT_PANTS -> {
                state.put(model.rightPants, model.rightPants.visible);
                model.rightPants.visible = false;
            }
            case LEFT_SLEEVE -> {
                state.put(model.leftSleeve, model.leftSleeve.visible);
                model.leftSleeve.visible = false;
            }
            case RIGHT_SLEEVE -> {
                state.put(model.rightSleeve, model.rightSleeve.visible);
                model.rightSleeve.visible = false;
            }
        }
    }

    /**
     * Internal VertexConsumer wrapper that applies a color/alpha multiplier.
     */
    private static record TintedVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a)
            implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return delegate.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return delegate.setColor(
                    (int) (red * r),
                    (int) (green * g),
                    (int) (blue * b),
                    (int) (alpha * a));
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return delegate.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return delegate.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return delegate.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return delegate.setNormal(x, y, z);
        }
    }
}