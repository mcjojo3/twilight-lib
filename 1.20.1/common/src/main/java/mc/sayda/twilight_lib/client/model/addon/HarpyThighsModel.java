package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Harpy thighs addon - upper leg thighs only.
 * Split from harpy_legs model.
 * Made with Blockbench 5.0.4
 */
public class HarpyThighsModel<T extends Entity> extends BaseAddonModel<T> {
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
                        new ResourceLocation(TwilightLib.MODID, "harpy_thighs"), "main");

        public HarpyThighsModel(ModelPart root) {
                this.rightLeg = getChildSafe(root, "RightLeg");
                this.leftLeg = getChildSafe(root, "LeftLeg");
                this.body = getChildSafe(root, "Body");
        }

        @SuppressWarnings("null")
        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create(),
                                PartPose.offset(-1.9F, 8.0F, 0.0F));

                RightLeg.addOrReplaceChild("RightThigh_r1",
                                CubeListBuilder.create().texOffs(8, 26).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 9.0F, 0.0F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(-4.6F, -2.75F, 0.25F, -2.4435F, 0.0F, -3.1416F));

                RightLeg.addOrReplaceChild("RightThigh_r2",
                                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.5F, -2.0F, 4.0F, 9.0F, 4.0F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(-0.6F, -2.75F, 0.25F, -0.6981F, 0.0F, 0.0F));

                PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create(),
                                PartPose.offset(1.9F, 8.0F, 0.0F));

                LeftLeg.addOrReplaceChild("LeftThigh_r1",
                                CubeListBuilder.create().texOffs(0, 26)
                                                .addBox(-2.0F, -1.5F, 0.0F, 4.0F, 9.0F, 0.0F, new CubeDeformation(0.0F))
                                                .texOffs(0, 13).addBox(-6.0F, -1.5F, -2.0F, 4.0F, 9.0F, 4.0F,
                                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(4.6F, -2.75F, 0.25F, -0.6981F, 0.0F, 0.0F));

                PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(),
                                PartPose.offset(0.0F, 24.0F, 0.0F));

                Body.addOrReplaceChild("Body_r1",
                                CubeListBuilder.create().texOffs(10, 37).addBox(-2.0F, 0.0F, -6.0F, 4.0F, 0.0F, 6.0F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(-2.0F, 8.0F, 2.0F, 2.2516F, -0.7171F, 0.0915F));

                Body.addOrReplaceChild("Body_r2",
                                CubeListBuilder.create().texOffs(2, 37).addBox(-2.0F, 0.0F, -6.0F, 4.0F, 0.0F, 6.0F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(2.0F, 8.0F, 2.0F, 2.2516F, 0.7171F, -0.0915F));

                Body.addOrReplaceChild("Body_r3",
                                CubeListBuilder.create().texOffs(-6, 37).addBox(-2.0F, 0.0F, -6.0F, 4.0F,
                                                0.0F, 6.0F, new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(0.0F, 8.0F, 2.0F, 2.7053F, 0.0F, 0.0F));

                return LayerDefinition.create(meshdefinition, 64, 64);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                        float headPitch) {
                super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                // Legs are synced with player movement automatically via PlayerAddonLayer
        }
}
