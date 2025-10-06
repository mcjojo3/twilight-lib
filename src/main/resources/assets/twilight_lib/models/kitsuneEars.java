// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class playerEars<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "playerears"), "main");
	private final ModelPart Head;

	public playerEars(ModelPart root) {
		this.Head = root.getChild("Head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition LeftEarTip_r1 = Head.addOrReplaceChild("LeftEarTip_r1", CubeListBuilder.create().texOffs(1, 10).addBox(-1.75F, -1.75F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.75F, -10.25F, 0.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition RightEarTip_r1 = Head.addOrReplaceChild("RightEarTip_r1", CubeListBuilder.create().texOffs(1, 10).addBox(-1.25F, -1.75F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 5).addBox(-2.25F, -0.75F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.75F, -10.25F, 0.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition LeftEar_r1 = Head.addOrReplaceChild("LeftEar_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.25F, -0.75F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.75F, -10.25F, 0.0F, 3.1416F, 0.0F, -2.7489F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}