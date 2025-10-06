package mc.sayda.twilight_lib.client.model.addon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.twilight_lib.client.model.IAddonModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for addon models that simplifies addon creation.
 * Automatically handles rendering and part syncing for only the parts you use.
 */
public abstract class BaseAddonModel<T extends Entity> extends EntityModel<T> implements IAddonModel {
    protected ModelPart head;
    protected ModelPart body;
    protected ModelPart rightArm;
    protected ModelPart leftArm;
    protected ModelPart rightLeg;
    protected ModelPart leftLeg;

    /**
     * Try to get a child part from root, returning null if it doesn't exist.
     * This allows your addon to only define the parts it needs.
     */
    protected ModelPart getChildSafe(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Base animation is handled by syncing with player model parts in PlayerAddonLayer
        // This runs AFTER the sync, so player animations take priority

        // TODO: Add custom idle animations here (e.g., wagging tail, flapping wings)
        // Example for tail wagging:
        // if (body != null) {
        //     body.zRot += Mth.cos(ageInTicks * 0.2F) * 0.1F;  // Note the += to preserve player movement
        // }

        // Example for wing flapping:
        // if (rightArm != null && leftArm != null) {
        //     rightArm.zRot += Mth.cos(ageInTicks * 0.4F) * 0.3F;
        //     leftArm.zRot += -Mth.cos(ageInTicks * 0.4F) * 0.3F;
        // }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Render all non-null parts
        List<ModelPart> parts = new ArrayList<>();
        if (head != null) parts.add(head);
        if (body != null) parts.add(body);
        if (rightArm != null) parts.add(rightArm);
        if (leftArm != null) parts.add(leftArm);
        if (rightLeg != null) parts.add(rightLeg);
        if (leftLeg != null) parts.add(leftLeg);

        for (ModelPart part : parts) {
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    // IAddonModel getters - return Optional based on whether part exists
    @Override
    public Optional<ModelPart> getHead() {
        return Optional.ofNullable(head);
    }

    @Override
    public Optional<ModelPart> getBody() {
        return Optional.ofNullable(body);
    }

    @Override
    public Optional<ModelPart> getRightArm() {
        return Optional.ofNullable(rightArm);
    }

    @Override
    public Optional<ModelPart> getLeftArm() {
        return Optional.ofNullable(leftArm);
    }

    @Override
    public Optional<ModelPart> getRightLeg() {
        return Optional.ofNullable(rightLeg);
    }

    @Override
    public Optional<ModelPart> getLeftLeg() {
        return Optional.ofNullable(leftLeg);
    }
}