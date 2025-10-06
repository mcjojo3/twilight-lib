package mc.sayda.twilight_lib.client.model.addon;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class PlayerTailModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        new ResourceLocation(TwilightLib.MODID, "player_tail"), "main");

    public PlayerTailModel(ModelPart root) {
        // Only get the Body part since that's all this addon uses
        this.body = getChildSafe(root, "Body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Body = partdefinition.addOrReplaceChild("Body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, 8.0F, 2.0F, 4.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    // Note: setupAnim and renderToBuffer are handled by BaseAddonModel
    // Note: getters are handled by BaseAddonModel
}