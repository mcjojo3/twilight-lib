package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class NymphModel<T extends Entity> extends BaseAddonModel<T> {
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
                        new ResourceLocation(TwilightLib.MODID, "nymph_model"), "main");

        public NymphModel(ModelPart root) {
                this.leftArm = getChildSafe(root, "LeftArm");
                this.head = getChildSafe(root, "Head");
                this.body = getChildSafe(root, "Body");
                this.rightLeg = getChildSafe(root, "RightLeg");
                this.leftLeg = getChildSafe(root, "LeftLeg");
                this.rightArm = getChildSafe(root, "RightArm");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                PartDefinition Head = partdefinition.addOrReplaceChild("Head",
                                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                                new CubeDeformation(0.75F)),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                PartDefinition Body = partdefinition.addOrReplaceChild("Body",
                                CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                                                new CubeDeformation(0.5F)),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                PartDefinition ChestLayer_r1 = Body.addOrReplaceChild("ChestLayer_r1",
                                CubeListBuilder.create().texOffs(16, 37).addBox(-3.9F, -2.0F, -2.0F, 7.8F, 2.0F, 4.0F,
                                                new CubeDeformation(0.5F)),
                                PartPose.offsetAndRotation(0.0F, 3.5F, -0.9F, 1.1781F, 0.0F, 0.0F));

                PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm",
                                CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                                                new CubeDeformation(0.5F)),
                                PartPose.offset(-5.0F, 2.0F, 0.0F));

                PartDefinition LeftArm = partdefinition.addOrReplaceChild("LeftArm",
                                CubeListBuilder.create().texOffs(49, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F,
                                                new CubeDeformation(0.5F)),
                                PartPose.offset(5.0F, 2.0F, 0.0F));

                PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg",
                                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                                new CubeDeformation(0.5F)),
                                PartPose.offset(-1.9F, 12.0F, 0.0F));

                PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg",
                                CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                                new CubeDeformation(0.5F)),
                                PartPose.offset(1.9F, 12.0F, 0.0F));

                return LayerDefinition.create(meshdefinition, 64, 64);
        }
}
