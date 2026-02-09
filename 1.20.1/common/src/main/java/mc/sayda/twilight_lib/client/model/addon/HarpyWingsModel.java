package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Harpy wings addon - feathered wings attached to arms with chest feathers.
 * Made with Blockbench 5.0.4
 */
public class HarpyWingsModel<T extends Entity> extends BaseAddonModel<T> {
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
                        new ResourceLocation(TwilightLib.MODID, "harpy_wings"), "main");

        public HarpyWingsModel(ModelPart root) {
                this.body = getChildSafe(root, "Body");
                this.rightArm = getChildSafe(root, "RightArm");
                this.leftArm = getChildSafe(root, "LeftArm");
                this.head = getChildSafe(root, "Head");
        }

        @SuppressWarnings("null")
        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                PartDefinition Body = partdefinition.addOrReplaceChild("Body",
                                CubeListBuilder.create().texOffs(6, 14).addBox(
                                                -4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.5F)),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                Body.addOrReplaceChild("ChestLayer_r1",
                                CubeListBuilder.create().texOffs(6, 18).addBox(-4.0F, -2.0F, -2.0F,
                                                8.0F, 2.0F, 4.0F, new CubeDeformation(0.5F)),
                                PartPose.offsetAndRotation(0.0F, 3.5F, -0.9F, 1.1781F, 0.0F, 0.0F));

                PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm", CubeListBuilder.create(),
                                PartPose.offset(-5.0F, 2.0F, 0.0F));

                RightArm.addOrReplaceChild("RightWing_r1",
                                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.5F, 0.0F,
                                                7.0F, 15.0F, 0.1F, new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(-4.5F, 2.5F, 0.0F, 0.0F, 3.1416F, 0.0F));

                partdefinition.addOrReplaceChild("LeftArm",
                                CubeListBuilder.create().texOffs(14, 0).addBox(1.0F, -5.0F, 0.0F,
                                                7.0F, 15.0F, 0.1F, new CubeDeformation(0.0F)),
                                PartPose.offset(5.0F, 2.0F, 0.0F));

                PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create(),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                Head.addOrReplaceChild("RightEar_r1",
                                CubeListBuilder.create().texOffs(6, 31).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 0.1F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(-4.0F, -5.0F, 1.0F, -0.3927F, 0.3927F, -0.3927F));

                Head.addOrReplaceChild("LeftEar_r1",
                                CubeListBuilder.create().texOffs(14, 31).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 0.1F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(4.0F, -5.0F, 1.0F, -0.4215F, -0.3614F, 0.5499F));

                return LayerDefinition.create(meshdefinition, 64, 64);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                        float headPitch) {
                super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                // Arms and body synced with player movement automatically via PlayerAddonLayer
        }
}
