package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Harpy legs addon - digitigrade bird-like legs.
 * Made with Blockbench 5.0.4
 */
public class HarpyLegsModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "harpy_legs"), "main");

    public HarpyLegsModel(ModelPart root) {
        this.rightLeg = getChildSafe(root, "RightLeg");
        this.leftLeg = getChildSafe(root, "LeftLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create().texOffs(42, 0).addBox(-2.1F, 11.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 9).addBox(-1.1F, 11.0F, 1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 6).addBox(-0.1F, 11.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 3).addBox(-2.1F, 11.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 8.0F, 0.0F));

        PartDefinition RightLeg_r1 = RightLeg.addOrReplaceChild("RightLeg_r1", CubeListBuilder.create().texOffs(42, 5).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 7.5F, 2.25F, -0.6981F, 0.0F, 0.0F));

        PartDefinition RightLeg_r2 = RightLeg.addOrReplaceChild("RightLeg_r2", CubeListBuilder.create().texOffs(16, 0).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 2.9667F, -1.4677F, -0.6981F, 0.0F, 0.0F));

        PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create().texOffs(42, 14).addBox(-0.9F, 11.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 23).addBox(0.1F, 11.0F, 1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 17).addBox(-0.9F, 11.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 20).addBox(1.1F, 11.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 8.0F, 0.0F));

        PartDefinition LeftLeg_r1 = LeftLeg.addOrReplaceChild("LeftLeg_r1", CubeListBuilder.create().texOffs(42, 19).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 7.5F, 2.25F, -0.6981F, 0.0F, 0.0F));

        PartDefinition LeftLeg_r2 = LeftLeg.addOrReplaceChild("LeftLeg_r2", CubeListBuilder.create().texOffs(16, 14).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 2.9667F, -1.4677F, -0.6981F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Legs are synced with player movement automatically via PlayerAddonLayer
    }
}
