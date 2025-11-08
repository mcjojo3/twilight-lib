package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerAddonLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * LRU cache for baked addon models.
     * Key: addon ID (e.g., "kitsune_ears_white")
     * Value: Baked EntityModel & IAddonModel instance
     * Access-ordered to evict least recently used models when capacity is exceeded.
     * Max size is configurable via TwilightConfig.MAX_CACHED_ADDON_MODELS.
     */
    private final Map<String, Object> bakedModels = new LinkedHashMap<String, Object>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            // No explicit cleanup needed - models don't hold native GPU resources
            // Minecraft's resource management handles texture/geometry lifecycle
            return size() > TwilightConfig.MAX_CACHED_ADDON_MODELS.get();
        }
    };

    public PlayerAddonLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {

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
                    // Get or bake the model
                    var addonModel = getOrBakeModel(addonId, addonInfo);

                    // Sync model parts to player model (only if they exist)
                    addonModel.getHead().ifPresent(part -> copyModelPart(playerModel.head, part));
                    addonModel.getBody().ifPresent(part -> copyModelPart(playerModel.body, part));
                    addonModel.getRightArm().ifPresent(part -> copyModelPart(playerModel.rightArm, part));
                    addonModel.getLeftArm().ifPresent(part -> copyModelPart(playerModel.leftArm, part));
                    addonModel.getRightLeg().ifPresent(part -> copyModelPart(playerModel.rightLeg, part));
                    addonModel.getLeftLeg().ifPresent(part -> copyModelPart(playerModel.leftLeg, part));

                    // Setup animations (runs AFTER sync so custom animations can use player movement)
                    @SuppressWarnings("unchecked")
                    EntityModel<Entity> entityModel = (EntityModel<Entity>) addonModel;
                    entityModel.setupAnim((Entity) player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                    // Render the addon - use player skin texture if specified, otherwise use addon's custom texture
                    var textureToUse = addonInfo.usePlayerSkin() ? player.getSkin().texture() : addonInfo.texture();

                    // Use translucent render type for transparent addons
                    RenderType renderType = addonInfo.translucent() ?
                        RenderType.entityTranslucent(textureToUse) :
                        RenderType.entityCutoutNoCull(textureToUse);
                    VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

                    // Apply transparency if translucent OR if forced by another addon - wrap vertex consumer to modify alpha
                    if (addonInfo.translucent() || forceAllTranslucent) {
                        final float targetAlpha = 0.5F;
                        VertexConsumer originalConsumer = vertexConsumer;
                        vertexConsumer = new VertexConsumer() {
                            @Override
                            public VertexConsumer addVertex(float x, float y, float z) {
                                return originalConsumer.addVertex(x, y, z);
                            }

                            @Override
                            public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                                // Force alpha to 50% (127 out of 255)
                                return originalConsumer.setColor(red, green, blue, (int)(targetAlpha * 255));
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
                    }

                    // Render with default white color
                    int color = FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
                    entityModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color);

                    // If this is a chest addon and player is wearing chest armor, render the armor overlay
                    if (addonModel instanceof ChestModel<?>) {
                        ItemStack chestArmor = player.getItemBySlot(EquipmentSlot.CHEST);
                        if (!chestArmor.isEmpty() && chestArmor.getItem() instanceof ArmorItem armorItem) {
                            // Bake the chest armor model
                            ChestArmorModel<?> chestArmorModel = getOrBakeChestArmorModel();

                            // Sync it to the player model
                            chestArmorModel.getBody().ifPresent(part -> copyModelPart(playerModel.body, part));

                            // Setup animations
                            @SuppressWarnings("unchecked")
                            EntityModel<Entity> armorEntityModel = (EntityModel<Entity>) chestArmorModel;
                            armorEntityModel.setupAnim((Entity) player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                            // Render with armor texture
                            ResourceLocation armorTexture = getArmorTexture(armorItem, false);
                            RenderType armorRenderType = RenderType.entityCutoutNoCull(armorTexture);
                            VertexConsumer armorVertexConsumer = buffer.getBuffer(armorRenderType);
                            armorEntityModel.renderToBuffer(poseStack, armorVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
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

    private ResourceLocation getArmorTexture(ArmorItem armorItem, boolean isLeggings) {
        // Get the armor material layers map and extract the key for the appropriate layer
        var layers = armorItem.getMaterial().value().layers();
        if (layers.isEmpty()) {
            return ResourceLocation.withDefaultNamespace("textures/models/armor/leather_layer_1.png");
        }

        // Get the first layer's ID (for chest armor, we want layer 1)
        var layer = layers.get(isLeggings ? 1 : 0);
        ResourceLocation layerId = layer.texture(false); // false = not dying overlay

        return layerId;
    }
}