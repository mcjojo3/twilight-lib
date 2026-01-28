package mc.sayda.twilight_lib.client.renderer;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.slf4j.Logger;
import mc.sayda.twilight_lib.addon.AddonModelInfo;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    private final Map<String, Object> bakedModels = Collections.synchronizedMap(
            new LinkedHashMap<String, Object>(16, 0.75f, true) {
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
        var addons = player.getData(ModAttachments.ADDONS);
        PlayerModel<AbstractClientPlayer> playerModel = this.getParentModel();

        // Check if any addon forces all addons to be translucent
        boolean forceAllTranslucent = addons.getActiveAddons().stream()
                .anyMatch(addonId -> AddonRegistry.getAddon(addonId)
                        .map(AddonModelInfo::forceAllTranslucent)
                        .orElse(false));

        // Render each active addon
        for (String addonId : addons.getActiveAddons()) {
            AddonRegistry.getAddon(addonId).ifPresent(addonInfo -> {
                // Filter addons based on mod requirements and config (FORCE_LOAD_ALL_ADDONS)
                // This happens during rendering when the config is guaranteed to be loaded
                if (!mc.sayda.twilight_lib.cosmetics.ModRequirement.shouldLoad(addonInfo.modTags())) {
                    return;
                }

                // Get or bake the model
                var addonModel = getOrBakeModel(addonId, addonInfo);

                // Special handling for chest addons - optionally hide when wearing armor
                if (addonModel instanceof ChestModel<?>) {
                    ItemStack chestArmor = player.getItemBySlot(EquipmentSlot.CHEST);
                    if (!chestArmor.isEmpty() && TwilightConfig.HIDE_CHEST_IN_ARMOR.get()) {
                        return; // Skip chest addon - config set to hide when wearing armor
                    }
                }

                // Sync model parts to player model (only if they exist)
                addonModel.getHead().ifPresent(part -> copyModelPart(playerModel.head, part));
                addonModel.getBody().ifPresent(part -> copyModelPart(playerModel.body, part));
                addonModel.getRightArm().ifPresent(part -> copyModelPart(playerModel.rightArm, part));
                addonModel.getLeftArm().ifPresent(part -> copyModelPart(playerModel.leftArm, part));
                addonModel.getRightLeg().ifPresent(part -> copyModelPart(playerModel.rightLeg, part));
                addonModel.getLeftLeg().ifPresent(part -> copyModelPart(playerModel.leftLeg, part));

                // Setup animations (runs AFTER sync so custom animations can use player
                // movement)
                // Validate type before unchecked cast
                if (!(addonModel instanceof EntityModel<?>)) {
                    LOGGER.error(
                            "Is this the best physical representation you can manifest? Addon model {} is not an EntityModel: {}",
                            addonId, addonModel.getClass());
                    return;
                }
                @SuppressWarnings("unchecked")
                EntityModel<Entity> entityModel = (EntityModel<Entity>) addonModel;
                entityModel.setupAnim((Entity) player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                // Render the addon - use player skin texture if specified, otherwise use
                // addon's custom texture
                var textureToUse = addonInfo.usePlayerSkin() ? player.getSkin().texture() : addonInfo.texture();

                // Check if THIS addon is the one forcing translucency
                boolean thisAddonForcesTranslucency = addonInfo.forceAllTranslucent();

                // Translucency rules (spotlight effect):
                // 1. If THIS addon is naturally translucent → make it translucent
                // 2. If THIS addon is forcing others translucent → keep THIS opaque (spotlight:
                // forces others but stays solid)
                // 3. If ANOTHER addon is forcing translucency → make THIS translucent (follow
                // the force)
                boolean shouldBeTranslucent = addonInfo.translucent()
                        || (forceAllTranslucent && !thisAddonForcesTranslucency);

                // Use translucent render type for transparent addons
                RenderType renderType = shouldBeTranslucent ? RenderType.entityTranslucent(textureToUse)
                        : RenderType.entityCutoutNoCull(textureToUse);
                VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

                // Use vanilla's official overlay calculation for damage effects
                int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

                // Get tint color for this addon (stored as 0xRRGGBB)
                int tintColor = addons.getAddonTint(addonId);
                // Mask to ensure only RGB, no alpha or sign extension issues
                tintColor = tintColor & 0x00FFFFFF;
                final float tintRed = ((tintColor >> 16) & 0xFF) / 255.0F;
                final float tintGreen = ((tintColor >> 8) & 0xFF) / 255.0F;
                final float tintBlue = (tintColor & 0xFF) / 255.0F;

                // Wrap vertex consumer to apply tint color AND transparency
                final float targetAlpha = shouldBeTranslucent
                        ? Math.max(0.0F, Math.min(1.0F, TwilightConfig.TRANSLUCENT_ADDON_ALPHA.get().floatValue()))
                        : 1.0F;
                VertexConsumer originalConsumer = vertexConsumer;
                vertexConsumer = new VertexConsumer() {
                    @Override
                    public VertexConsumer addVertex(float x, float y, float z) {
                        return originalConsumer.addVertex(x, y, z);
                    }

                    @Override
                    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                        // Apply tint color multiplication
                        int tintedRed = (int) (red * tintRed);
                        int tintedGreen = (int) (green * tintGreen);
                        int tintedBlue = (int) (blue * tintBlue);
                        int finalAlpha = (int) (alpha * targetAlpha);
                        return originalConsumer.setColor(tintedRed, tintedGreen, tintedBlue, finalAlpha);
                    }

                    @Override
                    public VertexConsumer setUv(float u, float v) {
                        return originalConsumer.setUv(u, v);
                    }

                    @Override
                    public VertexConsumer setUv1(int u, int v) {
                        return originalConsumer.setUv1(u, v);
                    }

                    @Override
                    public VertexConsumer setUv2(int u, int v) {
                        return originalConsumer.setUv2(u, v);
                    }

                    @Override
                    public VertexConsumer setNormal(float x, float y, float z) {
                        return originalConsumer.setNormal(x, y, z);
                    }
                };

                // Render with wrapped vertex consumer (white color since tint is applied in
                // wrapper)
                entityModel.renderToBuffer(poseStack, vertexConsumer, packedLight, overlay,
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

                                RenderType armorRenderType = RenderType.entityCutoutNoCull(armorTexture);
                                VertexConsumer armorVertexConsumer = buffer.getBuffer(armorRenderType);
                                // Use the same vanilla overlay for armor as the addon
                                armorEntityModel.renderToBuffer(poseStack, armorVertexConsumer, packedLight, overlay,
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
            });
        }
    }

    @SuppressWarnings("unchecked")
    private IAddonModel getOrBakeModel(String addonId, AddonModelInfo addonInfo) {
        return (IAddonModel) bakedModels.computeIfAbsent(addonId, id -> {
            var context = Minecraft.getInstance().getEntityModels();
            var modelPart = context.bakeLayer(addonInfo.layerLocation());
            return addonInfo.modelFactory().apply(modelPart);
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

}