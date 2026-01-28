package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;

/**
 * Generic wing model for 10x14 textures.
 * Supports idle and flying animations.
 */
public class WingsModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(TwilightLib.MODID, "wings"), "main");

    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public WingsModel(ModelPart root) {
        this.body = getChildSafe(root, "Body");
        // Get the wings which are children of the body
        if (this.body != null) {
            this.leftWing = getChildSafe(this.body, "left_wing");
            this.rightWing = getChildSafe(this.body, "right_wing");
        } else {
            this.leftWing = null;
            this.rightWing = null;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Body root
        PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Right Wing
        // Using texOffs(0, 0) - Front: 0-10, Back: 10-20
        Body.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-10.0F, -14.0F, 0.0F, 10.0F, 14.0F, 0.0F, new CubeDeformation(0.01F)),
                PartPose.offsetAndRotation(-0.5F, 11.0F, 2.0F, 0.0F, 1.0472F, 0.0F));

        // Left Wing
        // Using mirror() and texOffs(0, 0) ensures it samples the same way as the right
        // wing but flipped
        Body.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 0)
                        .addBox(0.0F, -14.0F, 0.0F, 10.0F, 14.0F, 0.0F, new CubeDeformation(0.01F)),
                PartPose.offsetAndRotation(0.5F, 11.0F, 2.0F, 0.0F, -1.0472F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (leftWing == null || rightWing == null)
            return;

        boolean isFlying = false;
        if (entity instanceof net.minecraft.world.entity.LivingEntity le) {
            isFlying = le.isFallFlying() || (!entity.onGround() && !entity.isInWater());
        } else {
            isFlying = !entity.onGround() && !entity.isInWater();
        }

        if (isFlying) {
            // Fast flapping animation
            float speed = 1.2F;

            float baseRot = 0.9F; // ~51 degrees
            float flapRange = 0.5F; // +/- 28 degrees -> Range [0.4, 1.4] radians (~23 to 80 degrees)

            float currentFlap = baseRot + (Mth.cos(ageInTicks * speed) * flapRange);

            this.rightWing.yRot = currentFlap;
            this.leftWing.yRot = -currentFlap;
        } else {
            // Idle animation
            // Base rotation: 60 deg (1.0472F)
            float speed = 0.05F;
            float degree = 0.25F;
            float sway = Mth.cos(ageInTicks * speed) * degree;

            this.rightWing.yRot = 1.0472F + sway;
            this.leftWing.yRot = -1.0472F - sway;
        }
    }
}
