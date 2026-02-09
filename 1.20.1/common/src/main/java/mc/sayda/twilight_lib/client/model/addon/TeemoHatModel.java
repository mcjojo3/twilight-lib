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

public class TeemoHatModel<T extends Entity> extends BaseAddonModel<T> {
        public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
                        new ResourceLocation(TwilightLib.MODID, "teemo_hat"), "main");

        public TeemoHatModel(ModelPart root) {
                this.head = getChildSafe(root, "Head");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                PartDefinition Head = partdefinition.addOrReplaceChild("Head",
                                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                                new CubeDeformation(0.5F))
                                                .texOffs(0, 0).addBox(-3.5F, -7.5F, -3.5F, 7.0F, 7.0F, 7.0F,
                                                                new CubeDeformation(0.5F)),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                return LayerDefinition.create(meshdefinition, 64, 64);
        }
}
