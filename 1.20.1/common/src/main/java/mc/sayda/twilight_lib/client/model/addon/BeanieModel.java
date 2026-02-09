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

public class BeanieModel<T extends Entity> extends BaseAddonModel<T> {
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
                        new ResourceLocation(TwilightLib.MODID, "beanie"), "main");

        public BeanieModel(ModelPart root) {
                this.head = getChildSafe(root, "head");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                PartDefinition head = partdefinition
                                .addOrReplaceChild("head",
                                                CubeListBuilder.create().texOffs(0, 26).addBox(-4.5F, -10.0F, -4.5F,
                                                                9.0F, 3.0F, 9.0F,
                                                                new CubeDeformation(0.0F)),
                                                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));

                PartDefinition bone = head.addOrReplaceChild("bone",
                                CubeListBuilder.create().texOffs(0, 15)
                                                .addBox(-0.0558F, 6.3149F, -3.5F, 6.0F, 3.0F, 7.0F,
                                                                new CubeDeformation(0.0F))
                                                .texOffs(0, 16)
                                                .addBox(2.1242F, 4.3149F, -3.5F, 4.0F, 2.0F, 7.0F,
                                                                new CubeDeformation(0.0F))
                                                .texOffs(0, 31)
                                                .addBox(3.1242F, 3.3149F, -1.0F, 2.0F, 2.0F, 2.0F,
                                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(0.0F, -18.0F, -1.0F, -1.5708F, -1.2654F, 1.5708F));

                PartDefinition bone2 = bone.addOrReplaceChild("bone2",
                                CubeListBuilder.create().texOffs(0, 16).addBox(-4.6822F, 7.4003F, -3.5F, 3.0F, 2.0F,
                                                7.0F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(-1.6033F, -2.3044F, 0.0F, 0.0F, 0.0F, -0.7418F));

                PartDefinition bone3 = bone2.addOrReplaceChild("bone3",
                                CubeListBuilder.create().texOffs(0, 16).addBox(-9.9559F, 0.1747F, -3.5F, 3.0F, 2.0F,
                                                7.0F,
                                                new CubeDeformation(0.0F)),
                                PartPose.offsetAndRotation(2.4362F, 2.8803F, 0.0F, 0.0F, 0.0F, -1.0908F));

                return LayerDefinition.create(meshdefinition, 38, 38);
        }
}
