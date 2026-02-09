package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

public class CustomFoxHeldItemLayer extends RenderLayer<Fox, FoxModel<Fox>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ItemInHandRenderer itemInHandRenderer;

    // Item positioning constants
    private static final float YOUNG_FOX_Y_OFFSET = 0.6F; // Vertical offset for young foxes
    private static final float YOUNG_FOX_Z_OFFSET = -0.5F; // Forward offset for young foxes
    private static final float HEAD_OFFSET = 0.0625F; // Standard head position offset (1/16 block)
    private static final float MOUTH_Z_OFFSET = -0.25F; // Forward offset to mouth position
    private static final float ITEM_X_ROTATION = -90.0F; // Rotation to orient item correctly
    private static final float ITEM_Y_POSITION_FIX = 0.06F; // Fine-tune Y position
    private static final float ITEM_Z_POSITION_FIX = 0.21F; // Fine-tune Z position

    public CustomFoxHeldItemLayer(RenderLayerParent<Fox, FoxModel<Fox>> parent, ItemInHandRenderer itemRenderer) {
        super(parent);
        this.itemInHandRenderer = itemRenderer;
        LOGGER.debug("Yeah? Well... Initialized CustomFoxHeldItemLayer with custom item rotation");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Fox fox, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        ItemStack itemStack = fox.getItemBySlot(EquipmentSlot.MAINHAND);
        if (itemStack.isEmpty()) return;

        // Defensive null check for parent model
        FoxModel<Fox> parentModel = this.getParentModel();
        if (parentModel == null || parentModel.head == null) {
            return; // Cannot render without model/head
        }

        poseStack.pushPose();

        // Position in fox's mouth (vanilla positioning)
        if (parentModel.young) {
            poseStack.translate(0.0F, YOUNG_FOX_Y_OFFSET, YOUNG_FOX_Z_OFFSET);
        }

        parentModel.head.translateAndRotate(poseStack);
        poseStack.translate(HEAD_OFFSET, HEAD_OFFSET, MOUTH_Z_OFFSET);

        // X rotation with position fix
        poseStack.mulPose(Axis.XP.rotationDegrees(ITEM_X_ROTATION));
        poseStack.translate(0.0F, ITEM_Y_POSITION_FIX, ITEM_Z_POSITION_FIX);

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
