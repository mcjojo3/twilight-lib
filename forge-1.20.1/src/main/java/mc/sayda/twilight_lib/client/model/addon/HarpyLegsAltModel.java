package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Harpy legs alt addon - digitigrade bird-like legs (alternate style).
 * Made with Blockbench 5.0.4
 */
public class HarpyLegsAltModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        new ResourceLocation(TwilightLib.MODID, "harpy_legs_alt"), "main");

    public HarpyLegsAltModel(ModelPart root) {
        this.rightLeg = getChildSafe(root, "RightLeg");
        this.leftLeg = getChildSafe(root, "LeftLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create().texOffs(41, 0).addBox(-2.1F, 11.0F, -1.0F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(48, 10).addBox(-1.1F, 11.0F, 2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 7).addBox(-0.1F, 11.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 4).addBox(-2.1F, 11.0F, -3.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 8.0F, 0.0F));

        PartDefinition RightLeg_r1 = RightLeg.addOrReplaceChild("RightLeg_r1", CubeListBuilder.create().texOffs(17, 1).addBox(-6.0F, -1.9F, -4.35F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.4F, 5.4F, -0.65F, -1.5708F, 0.0F, 0.0F));

        PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create().texOffs(41, 13).addBox(-0.9F, 11.0F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(48, 23).addBox(0.1F, 11.0F, 1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 17).addBox(-0.9F, 11.0F, -3.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 20).addBox(1.1F, 11.0F, -3.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 8.0F, 0.0F));

        PartDefinition LeftLeg_r1 = LeftLeg.addOrReplaceChild("LeftLeg_r1", CubeListBuilder.create().texOffs(17, 14).addBox(-1.0F, -0.4F, -4.85F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 5.9F, 0.6F, -1.5708F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Legs are synced with player movement automatically via PlayerAddonLayer
    }
}