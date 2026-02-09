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

public class ChestModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "chest"), "main");

    public ChestModel(ModelPart root) {
        this.body = getChildSafe(root, "Body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Chest layer (uses player skin - 64x64 texture)
        PartDefinition Chest_r1 = Body.addOrReplaceChild("Chest_r1",
                CubeListBuilder.create().texOffs(16, 21).addBox(-3.9F, -2.0F, -2.0F, 7.8F, 2.0F, 4.0F,
                        new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.5F, -0.9F, 1.1781F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
