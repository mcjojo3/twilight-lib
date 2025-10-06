// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class kitsuneTail2<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "kitsunetail2"), "main");
	private final ModelPart Body;
	private final ModelPart Tail1;
	private final ModelPart Tail2;
	private final ModelPart Tail3;
	private final ModelPart Tail4;
	private final ModelPart Tail5;
	private final ModelPart Tail6;
	private final ModelPart Tail7;
	private final ModelPart Tail8;
	private final ModelPart Tail9;

	public kitsuneTail2(ModelPart root) {
		this.Body = root.getChild("Body");
		this.Tail1 = this.Body.getChild("Tail1");
		this.Tail2 = this.Body.getChild("Tail2");
		this.Tail3 = this.Body.getChild("Tail3");
		this.Tail4 = this.Body.getChild("Tail4");
		this.Tail5 = this.Body.getChild("Tail5");
		this.Tail6 = this.Body.getChild("Tail6");
		this.Tail7 = this.Body.getChild("Tail7");
		this.Tail8 = this.Body.getChild("Tail8");
		this.Tail9 = this.Body.getChild("Tail9");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Tail1 = Body.addOrReplaceChild("Tail1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.2217F, 0.0F, 0.0F));

		PartDefinition Tail_r1 = Tail1.addOrReplaceChild("Tail_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail2 = Body.addOrReplaceChild("Tail2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.0917F, 0.1647F, 0.4997F));

		PartDefinition Tail_r2 = Tail2.addOrReplaceChild("Tail_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail3 = Body.addOrReplaceChild("Tail3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.0917F, -0.1647F, -0.4997F));

		PartDefinition Tail_r3 = Tail3.addOrReplaceChild("Tail_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail4 = Body.addOrReplaceChild("Tail4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.3908F, -0.3007F, -1.0199F));

		PartDefinition Tail_r4 = Tail4.addOrReplaceChild("Tail_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail5 = Body.addOrReplaceChild("Tail5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.3996F, -0.3265F, -1.5718F));

		PartDefinition Tail_r5 = Tail5.addOrReplaceChild("Tail_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail6 = Body.addOrReplaceChild("Tail6", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.7508F, -0.3007F, -2.1217F));

		PartDefinition Tail_r6 = Tail6.addOrReplaceChild("Tail_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail7 = Body.addOrReplaceChild("Tail7", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.3908F, 0.3007F, 1.0199F));

		PartDefinition Tail_r7 = Tail7.addOrReplaceChild("Tail_r7", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail8 = Body.addOrReplaceChild("Tail8", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.4007F, 0.3229F, 1.5719F));

		PartDefinition Tail_r8 = Tail8.addOrReplaceChild("Tail_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		PartDefinition Tail9 = Body.addOrReplaceChild("Tail9", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 1.7508F, 0.3007F, 2.1217F));

		PartDefinition Tail_r9 = Tail9.addOrReplaceChild("Tail_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.0F, 0.0F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}