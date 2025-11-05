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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class KitsuneTailsModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "kitsune_tails"), "main");

    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final ModelPart tail5;
    private final ModelPart tail6;
    private final ModelPart tail7;
    private final ModelPart tail8;
    private final ModelPart tail9;

    // Store base rotations for each tail
    private float[] baseRotX = new float[9];
    private float[] baseRotZ = new float[9];
    private boolean initialized = false;

    public KitsuneTailsModel(ModelPart root) {
        this.body = getChildSafe(root, "Body");

        // Get tail parts from body
        if (body != null) {
            this.tail1 = getChildSafe(body, "Tail1");
            this.tail2 = getChildSafe(body, "Tail2");
            this.tail3 = getChildSafe(body, "Tail3");
            this.tail4 = getChildSafe(body, "Tail4");
            this.tail5 = getChildSafe(body, "Tail5");
            this.tail6 = getChildSafe(body, "Tail6");
            this.tail7 = getChildSafe(body, "Tail7");
            this.tail8 = getChildSafe(body, "Tail8");
            this.tail9 = getChildSafe(body, "Tail9");
        } else {
            this.tail1 = this.tail2 = this.tail3 = this.tail4 = this.tail5 =
            this.tail6 = this.tail7 = this.tail8 = this.tail9 = null;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        ModelPart[] tails = {tail1, tail2, tail3, tail4, tail5, tail6, tail7, tail8, tail9};

        // Store base rotations on first frame (after PlayerAddonLayer syncs body)
        if (!initialized) {
            for (int i = 0; i < 9; i++) {
                if (tails[i] != null) {
                    baseRotX[i] = tails[i].xRot;
                    baseRotZ[i] = tails[i].zRot;
                }
            }
            initialized = true;
        }

        // Apply smooth flowing idle animation using sine waves on top of base positions
        // Tail 1 - Center bottom
        if (tail1 != null) {
            tail1.xRot = baseRotX[0] + Mth.cos(ageInTicks * 0.067F) * 0.05F;
            tail1.zRot = baseRotZ[0] + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        }

        // Tail 2 - Left top
        if (tail2 != null) {
            tail2.xRot = baseRotX[1] + Mth.cos(ageInTicks * 0.08F + 1.0F) * 0.06F;
            tail2.zRot = baseRotZ[1] + Mth.sin(ageInTicks * 0.06F + 0.5F) * 0.04F;
        }

        // Tail 3 - Center middle
        if (tail3 != null) {
            tail3.xRot = baseRotX[2] + Mth.cos(ageInTicks * 0.05F + 0.5F) * 0.045F;
            tail3.zRot = baseRotZ[2] + Mth.sin(ageInTicks * 0.045F) * 0.035F;
        }

        // Tail 4 - Center top
        if (tail4 != null) {
            tail4.xRot = baseRotX[3] + Mth.cos(ageInTicks * 0.055F + 2.0F) * 0.055F;
            tail4.zRot = baseRotZ[3] + Mth.sin(ageInTicks * 0.048F + 1.5F) * 0.038F;
        }

        // Tail 5 - Right bottom
        if (tail5 != null) {
            tail5.xRot = baseRotX[4] + Mth.cos(ageInTicks * 0.072F + 0.8F) * 0.05F;
            tail5.zRot = baseRotZ[4] + Mth.sin(ageInTicks * 0.053F + 0.3F) * 0.04F;
        }

        // Tail 6 - Right middle
        if (tail6 != null) {
            tail6.xRot = baseRotX[5] + Mth.cos(ageInTicks * 0.065F + 1.5F) * 0.06F;
            tail6.zRot = baseRotZ[5] + Mth.sin(ageInTicks * 0.058F + 1.0F) * 0.045F;
        }

        // Tail 7 - Right top
        if (tail7 != null) {
            tail7.xRot = baseRotX[6] + Mth.cos(ageInTicks * 0.075F + 0.3F) * 0.055F;
            tail7.zRot = baseRotZ[6] + Mth.sin(ageInTicks * 0.051F + 2.0F) * 0.038F;
        }

        // Tail 8 - Left bottom
        if (tail8 != null) {
            tail8.xRot = baseRotX[7] + Mth.cos(ageInTicks * 0.062F + 1.2F) * 0.05F;
            tail8.zRot = baseRotZ[7] + Mth.sin(ageInTicks * 0.056F + 0.8F) * 0.04F;
        }

        // Tail 9 - Left middle
        if (tail9 != null) {
            tail9.xRot = baseRotX[8] + Mth.cos(ageInTicks * 0.07F + 2.5F) * 0.06F;
            tail9.zRot = baseRotZ[8] + Mth.sin(ageInTicks * 0.049F + 1.8F) * 0.035F;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Body = partdefinition.addOrReplaceChild("Body",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition Tail1 = Body.addOrReplaceChild("Tail1",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.2217F, 0.0F, 0.0F));

        PartDefinition Tail_r1 = Tail1.addOrReplaceChild("Tail_r1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail2 = Body.addOrReplaceChild("Tail2",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.0917F, 0.1647F, 0.4997F));

        PartDefinition Tail_r2 = Tail2.addOrReplaceChild("Tail_r2",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail3 = Body.addOrReplaceChild("Tail3",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.0917F, -0.1647F, -0.4997F));

        PartDefinition Tail_r3 = Tail3.addOrReplaceChild("Tail_r3",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail4 = Body.addOrReplaceChild("Tail4",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.3908F, -0.3007F, -1.0199F));

        PartDefinition Tail_r4 = Tail4.addOrReplaceChild("Tail_r4",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail5 = Body.addOrReplaceChild("Tail5",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.3996F, -0.3265F, -1.5718F));

        PartDefinition Tail_r5 = Tail5.addOrReplaceChild("Tail_r5",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail6 = Body.addOrReplaceChild("Tail6",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.7508F, -0.3007F, -2.1217F));

        PartDefinition Tail_r6 = Tail6.addOrReplaceChild("Tail_r6",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail7 = Body.addOrReplaceChild("Tail7",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.3908F, 0.3007F, 1.0199F));

        PartDefinition Tail_r7 = Tail7.addOrReplaceChild("Tail_r7",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail8 = Body.addOrReplaceChild("Tail8",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.4007F, 0.3229F, 1.5719F));

        PartDefinition Tail_r8 = Tail8.addOrReplaceChild("Tail_r8",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        PartDefinition Tail9 = Body.addOrReplaceChild("Tail9",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.7508F, 0.3007F, 2.1217F));

        PartDefinition Tail_r9 = Tail9.addOrReplaceChild("Tail_r9",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    // Note: setupAnim handles custom tail animations
    // Note: renderToBuffer and getters are handled by BaseAddonModel
}