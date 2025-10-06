package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class KitsuneEarsModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        new ResourceLocation(TwilightLib.MODID, "kitsune_ears"), "main");

    public KitsuneEarsModel(ModelPart root) {
        // Only get the Head part since ears are attached to the head
        this.head = getChildSafe(root, "Head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Head = partdefinition.addOrReplaceChild("Head",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition LeftEarTip_r1 = Head.addOrReplaceChild("LeftEarTip_r1",
            CubeListBuilder.create()
                .texOffs(1, 10)
                .addBox(-1.75F, -1.75F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.75F, -10.25F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition RightEarTip_r1 = Head.addOrReplaceChild("RightEarTip_r1",
            CubeListBuilder.create()
                .texOffs(1, 10)
                .addBox(-1.25F, -1.75F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5)
                .addBox(-2.25F, -0.75F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-3.75F, -10.25F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition LeftEar_r1 = Head.addOrReplaceChild("LeftEar_r1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.25F, -0.75F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.75F, -10.25F, 0.0F, 3.1416F, 0.0F, -2.7489F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    // Note: setupAnim and renderToBuffer are handled by BaseAddonModel
    // Note: getters are handled by BaseAddonModel
}