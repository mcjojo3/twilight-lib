package mc.sayda.twilight_lib.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.texture.OverlayTexture;
import mc.sayda.twilight_lib.ModelManager;
import net.minecraft.client.model.EntityModel;

public class CustomModelLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {

    private static final ResourceLocation CUSTOM_MODEL_TEXTURE = new ResourceLocation("twilight_lib", "textures/entity/custommodel.png");

    public CustomModelLayer(RenderLayerParent<T, PlayerModel<T>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        String modelName = ModelManager.getPlayerModel(player.getUUID());
        if (modelName != null && ModelManager.hasModel(modelName)) {
            EntityModel<Player> customModel = ModelManager.MODEL_REGISTRY.get(modelName.toLowerCase()).get().get();
            if (customModel != null) {
                customModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(CUSTOM_MODEL_TEXTURE));
                customModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
            }
        }
    }
}


/*package mc.sayda.twilight_lib.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.texture.OverlayTexture;
import mc.sayda.twilight_lib.ModelManager;
import net.minecraft.client.model.EntityModel;

public class CustomModelLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {

    // Define a dedicated texture for your custom model.
    // This texture file must be located at:
    // src/main/resources/assets/twilight_lib/textures/entity/custommodel.png
    private static final ResourceLocation CUSTOM_MODEL_TEXTURE = new ResourceLocation("twilight_lib", "textures/entity/custommodel.png");

    public CustomModelLayer(RenderLayerParent<T, PlayerModel<T>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        // Retrieve the model name for the player (set via your commands)
        String modelName = ModelManager.getPlayerModel(player.getUUID());
        if (modelName != null && ModelManager.hasModel(modelName)) {
            // Obtain the custom model instance from your registry.
            EntityModel<Player> customModel = ModelManager.MODEL_REGISTRY.get(modelName.toLowerCase()).get().get();
            if (customModel != null) {
                // Synchronize animations with the player.
                customModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                // Use the dedicated texture when rendering the model.
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(CUSTOM_MODEL_TEXTURE));
                customModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
            }
        }
    }
}*/
