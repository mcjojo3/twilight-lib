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

public class KitsuneEarsModel<T extends Entity> extends BaseAddonModel<T> {
    // Model layer locations for standard and alt variants
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "kitsune_ears"), "main");
    public static final ModelLayerLocation LAYER_LOCATION_ALT = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "kitsune_ears_alt"), "main");

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

        PartDefinition RightEarTip2_r1 = Head.addOrReplaceChild("RightEarTip2_r1",
            CubeListBuilder.create()
                .texOffs(2, 10)
                .addBox(-0.25F, -2.75F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(10, 7)
                .addBox(-1.25F, -1.75F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5)
                .addBox(-2.25F, -0.75F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-3.75F, -9.25F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition LeftEarTip2_r1 = Head.addOrReplaceChild("LeftEarTip2_r1",
            CubeListBuilder.create()
                .texOffs(10, 2)
                .addBox(-1.75F, -1.75F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 10)
                .addBox(-1.75F, -2.75F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.75F, -9.25F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition LeftEar_r1 = Head.addOrReplaceChild("LeftEar_r1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.25F, -0.75F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.75F, -9.25F, 0.0F, 3.1416F, 0.0F, -2.7489F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public static LayerDefinition createBodyLayerAlt() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Head = partdefinition.addOrReplaceChild("Head",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition LeftEarTip2_r1 = Head.addOrReplaceChild("LeftEarTip2_r1",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(0.75F, -2.75F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12)
                .addBox(-0.25F, -0.75F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 1)
                .addBox(-1.25F, 0.25F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12)
                .addBox(-0.25F, -1.75F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.25F, -10.75F, 0.0F, 3.1416F, 0.0F, -2.9234F));

        PartDefinition RightEarTip2_r1 = Head.addOrReplaceChild("RightEarTip2_r1",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(0.75F, -2.75F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 17)
                .addBox(-0.25F, -1.75F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 1)
                .addBox(-1.25F, 0.25F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 17)
                .addBox(-0.25F, -0.75F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-3.25F, -10.75F, 0.0F, 0.0F, 0.0F, -0.2182F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    // Note: setupAnim and renderToBuffer are handled by BaseAddonModel
    // Note: getters are handled by BaseAddonModel
}