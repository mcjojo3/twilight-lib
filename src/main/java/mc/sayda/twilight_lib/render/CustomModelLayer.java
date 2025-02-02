package mc.sayda.twilight_lib.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import mc.sayda.twilight_lib.ModelManager;

public class CustomModelLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation CUSTOM_MODEL_TEXTURE = new ResourceLocation("twilight_lib", "textures/entity/custommodel.png");

    // Accept PlayerRenderer (raw type, as it is not parameterized in 1.20.1)
    public CustomModelLayer(PlayerRenderer playerRenderer) {
        super(playerRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        System.out.println("CustomModelLayer.render called for " + player.getName().getString());
        String modelName = ModelManager.getPlayerModel(player.getUUID());
        if (modelName != null && ModelManager.hasModel(modelName)) {
            // Retrieve the custom model; no cast needed if the supplier already produces the correct type.
            EntityModel<AbstractClientPlayer> customModel = ModelManager.MODEL_REGISTRY.get(modelName.toLowerCase()).get().get();
            if (customModel != null) {
                customModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(CUSTOM_MODEL_TEXTURE));
                customModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
            }
        }
    }
}
