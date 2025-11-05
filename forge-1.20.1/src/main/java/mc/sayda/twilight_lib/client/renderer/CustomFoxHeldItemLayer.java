package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.FoxModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomFoxHeldItemLayer extends RenderLayer<Fox, FoxModel<Fox>> {
    private final ItemInHandRenderer itemInHandRenderer;

    public CustomFoxHeldItemLayer(RenderLayerParent<Fox, FoxModel<Fox>> parent, ItemInHandRenderer itemRenderer) {
        super(parent);
        this.itemInHandRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Fox fox, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack itemStack = fox.getItemBySlot(EquipmentSlot.MAINHAND);
        if (itemStack.isEmpty()) return;

        poseStack.pushPose();

        // Position in fox's mouth (vanilla positioning)
        if (this.getParentModel().young) {
            poseStack.translate(0.0F, 0.6F, -0.5F);
        }

        this.getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.0625F, 0.0625F, -0.25F);

        // X rotation with position fix
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(0.0F, 0.06F, 0.21F);

        // Render the item
        this.itemInHandRenderer.renderItem(
            fox,
            itemStack,
            ItemDisplayContext.GROUND,
            false,
            poseStack,
            buffer,
            packedLight
        );

        poseStack.popPose();
    }
}