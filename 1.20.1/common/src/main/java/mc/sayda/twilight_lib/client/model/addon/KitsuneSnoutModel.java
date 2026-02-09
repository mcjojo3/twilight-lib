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

public class KitsuneSnoutModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(TwilightLib.MODID, "kitsune_snout"), "main");

    public KitsuneSnoutModel(ModelPart root) {
        // Only get the Head part since snout is attached to the head
        this.head = getChildSafe(root, "Head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Head = partdefinition.addOrReplaceChild("Head",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition Snout_r1 = Head.addOrReplaceChild("Snout_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, 0.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -3.0F, -4.0F, 3.1416F, 0.0F, -3.1416F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    // Note: setupAnim and renderToBuffer are handled by BaseAddonModel
    // Note: getters are handled by BaseAddonModel
}
