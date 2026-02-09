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

/**
 * Armor overlay model for ChestModel - uses 64x32 armor texture dimensions
 */
public class ChestArmorModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "chest_armor"), "main");

    public ChestArmorModel(ModelPart root) {
        this.body = getChildSafe(root, "Body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Armor chest layer - uses 64x32 armor texture dimensions
        PartDefinition Chest_armor = Body.addOrReplaceChild("Chest_armor",
                CubeListBuilder.create().texOffs(16, 21).addBox(-3.9F, -2.0F, -2.0F, 7.8F, 2.0F, 4.0F,
                        new CubeDeformation(0.75F)), // 0.75F to stick out properly over armor, texOffs matches armor atlas (20,21) front face
                PartPose.offsetAndRotation(0.0F, 3.5F, -0.9F, 1.1781F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }
}
