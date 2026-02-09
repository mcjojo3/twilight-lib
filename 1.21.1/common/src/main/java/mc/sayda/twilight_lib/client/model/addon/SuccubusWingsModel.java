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

public class SuccubusWingsModel<T extends Entity> extends BaseAddonModel<T> {
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
                        ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "succubus_wings"), "main");

        public SuccubusWingsModel(ModelPart root) {
                this.head = getChildSafe(root, "head");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(),
                                PartPose.offset(0.0F, -6.0F, 0.0F));

                PartDefinition left_wing = head.addOrReplaceChild("left_wing",
                                CubeListBuilder.create().texOffs(0, 10).addBox(-1.7039F, -9.0433F, 0.0F, 8.0F, 7.0F,
                                                0.1F,
                                                new CubeDeformation(0.01F)),
                                PartPose.offsetAndRotation(8.0F, -2.5F, 0.0F, 3.1416F, 0.0F, -2.7489F));

                PartDefinition right_wing = head.addOrReplaceChild("right_wing", CubeListBuilder.create(),
                                PartPose.offsetAndRotation(-8.0F, -2.5F, 0.0F, 0.0F, 0.0F, -0.3927F));

                PartDefinition wing_r1 = right_wing.addOrReplaceChild("wing_r1",
                                CubeListBuilder.create().texOffs(0, 10).mirror()
                                                .addBox(-4.0F, -3.5F, 0.0F, 8.0F, 7.0F, 0.1F,
                                                                new CubeDeformation(0.01F))
                                                .mirror(false),
                                PartPose.offsetAndRotation(2.2961F, -5.5433F, 0.0F, 0.0F, 3.1416F, 0.0F));

                return LayerDefinition.create(meshdefinition, 19, 19);
        }
}
