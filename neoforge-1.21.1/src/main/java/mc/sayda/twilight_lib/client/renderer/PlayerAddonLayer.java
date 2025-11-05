package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.twilight_lib.addon.AddonModelInfo;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.client.model.IAddonModel;
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
import net.minecraft.world.entity.Entity;

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

                    // Render the addon
                    VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(addonInfo.texture()));
                    entityModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
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

    private void copyModelPart(ModelPart from, ModelPart to) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
    }
}