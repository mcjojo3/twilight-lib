package mc.sayda.twilight_lib.client.model.addon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mc.sayda.twilight_lib.client.model.IAddonModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
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

    // Pre-allocated list for rendering - reused every frame to avoid allocations
    private final List<ModelPart> renderParts = new ArrayList<>(6);
    private boolean renderPartsInitialized = false;

    /**
     * Try to get a child part from root, returning null if it doesn't exist.
     * This allows your addon to only define the parts it needs.
     */
    protected ModelPart getChildSafe(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (NoSuchElementException e) {
            // Expected - part doesn't exist in this model
            return null;
        }
        // Let other exceptions propagate - they indicate real bugs!
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Base animation is handled by syncing with player model parts in PlayerAddonLayer
        // This runs AFTER the sync, so player animations take priority
        // Override this method in your addon model to add custom idle animations (e.g., wagging tail, flapping wings)
    }

    /**
     * Initialize the render parts list with all non-null parts.
     * Called lazily on first render to avoid allocations every frame.
     */
    private void initializeRenderParts() {
        renderParts.clear();
        if (head != null) renderParts.add(head);
        if (body != null) renderParts.add(body);
        if (rightArm != null) renderParts.add(rightArm);
        if (leftArm != null) renderParts.add(leftArm);
        if (rightLeg != null) renderParts.add(rightLeg);
        if (leftLeg != null) renderParts.add(leftLeg);
        renderPartsInitialized = true;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Initialize render parts list on first render (lazy initialization)
        if (!renderPartsInitialized) {
            initializeRenderParts();
        }

        // Render all non-null parts using pre-allocated list
        for (ModelPart part : renderParts) {
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