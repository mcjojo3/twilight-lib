package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.twilight_lib.addon.AddonModelInfo;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
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

        // Check if addons are enabled in config
        if (!TwilightConfig.ENABLE_ADDONS.get()) {
            return;
        }

        // Don't render addons for invisible players
        if (player.isInvisible()) {
            return;
        }

        // Get player's equipped addons
        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            PlayerModel<AbstractClientPlayer> playerModel = this.getParentModel();

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
                    var textureToUse = addonInfo.usePlayerSkin() ? player.getSkinTextureLocation() : addonInfo.texture();

                    // Use translucent render type for transparent addons
                    RenderType renderType = addonInfo.translucent() ?
                        RenderType.entityTranslucent(textureToUse) :
                        RenderType.entityCutoutNoCull(textureToUse);
                    VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

                    // Apply transparency if translucent - wrap vertex consumer to modify alpha
                    if (addonInfo.translucent()) {
                        final float targetAlpha = 0.5F;
                        VertexConsumer originalConsumer = vertexConsumer;
                        vertexConsumer = new VertexConsumer() {
                            @Override
                            public VertexConsumer vertex(double x, double y, double z) {
                                return originalConsumer.vertex(x, y, z);
                            }

                            @Override
                            public VertexConsumer color(int red, int green, int blue, int alpha) {
                                // Force alpha to 50% (127 out of 255)
                                return originalConsumer.color(red, green, blue, (int)(targetAlpha * 255));
                            }

                            @Override
                            public VertexConsumer uv(float u, float v) {
                                return originalConsumer.uv(u, v);
                            }

                            @Override
                            public VertexConsumer overlayCoords(int u, int v) {
                                return originalConsumer.overlayCoords(u, v);
                            }

                            @Override
                            public VertexConsumer uv2(int u, int v) {
                                return originalConsumer.uv2(u, v);
                            }

                            @Override
                            public VertexConsumer normal(float x, float y, float z) {
                                return originalConsumer.normal(x, y, z);
                            }

                            @Override
                            public void endVertex() {
                                originalConsumer.endVertex();
                            }

                            @Override
                            public void defaultColor(int r, int g, int b, int a) {
                                originalConsumer.defaultColor(r, g, b, a);
                            }

                            @Override
                            public void unsetDefaultColor() {
                                originalConsumer.unsetDefaultColor();
                            }
                        };
                    }

                    // Render with default white color
                    entityModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

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
                            armorEntityModel.renderToBuffer(poseStack, armorVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                        }
                    }
                });
            }
        });
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
        String armorMaterial = armorItem.getMaterial().getName();
        String layer = isLeggings ? "layer_2" : "layer_1";
        return new ResourceLocation("minecraft", "textures/models/armor/" + armorMaterial + "_" + layer + ".png");
    }
}