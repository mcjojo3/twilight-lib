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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Mermaid tail that replaces the legs. The armor layer reuses the same bones to
 * draw leggings on the tail and boots on the fin.
 *
 * Texture: hip front on rows 4-9, one row per slice below it, fin at (32, 0).
 */
@SuppressWarnings("null")
public class MermaidTailModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(TwilightLib.MODID, "mermaid_tail"), "main");
    public static final ModelLayerLocation ARMOR_LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(TwilightLib.MODID, "mermaid_tail_armor"), "main");

    /** One 1px slice: its real size and the size of its texture box. */
    private record Slice(float width, float depth, int uvWidth, int uvDepth) {
    }

    private static final int SLICE_COUNT = 20;
    private static final float TAIL_TOP_Y = 10.0F; // first slice, where short_torso ends
    private static final float WAIST_TOP_Y = 6.0F;
    private static final float HIPS_Y = 12.0F;
    private static final int HIP_SLICE = 2; // first visible slice, the ones above only carry armor

    private static final float TOP_WIDTH = 8.1F;
    private static final float TOP_DEPTH = 4.1F;
    private static final float BOTTOM_WIDTH = 3.6F;
    private static final float BOTTOM_DEPTH = 1.4F;
    private static final float SLICE_MIN_STEP = 0.1F; // stops neighbouring slices z-fighting
    private static final float SLICE_OVERLAP = 0.1F; // hides gaps when bending
    private static final int FIRST_TEXTURE_ROW = 8;

    // Fixed per slice so the texture layout never moves
    private static final int[] TEXTURE_WIDTHS = { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4 };
    private static final int[] TEXTURE_DEPTHS = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 2, 2, 2, 2, 1 };

    // Vanilla leg texture rows: leggings from 20, boots from 29
    private static final int FIRST_ARMOR_ROW = 20;
    private static final int ARMOR_ROW_COUNT = 12;
    private static final int FIRST_BOOTS_ROW = 29;

    private static final float WAIST_INFLATE = 0.1F;

    private static final float FLUKE_WIDTH = 6.0F;
    private static final float FLUKE_LENGTH = 10.0F;
    private static final float FLUKE_SPREAD = 0.8F;
    private static final float FLUKE_TWIST = 0.12F; // stops the lobes z-fighting where they cross

    private static final float WAIST_ARMOR_INFLATE = 0.5F;
    private static final float TAIL_ARMOR_INFLATE = 0.4F;
    private static final float FLUKE_ARMOR_INFLATE = 0.25F;
    private static final float FLUKE_ARMOR_THICKNESS = 0.5F;

    // Animation, in radians and radians per tick
    private static final float TAIL_CURL = 1.75F; // backward arc on land
    private static final float TAIL_CURL_BIAS = 0.9F; // lower = tighter curl near the fin
    private static final float FLUKE_CURL = 0.25F;
    private static final float WAVE_WEIGHT_TOP = 0.15F;
    private static final float WAVE_WEIGHT_BOTTOM = 0.28F;
    private static final float WAVE_WEIGHT_FLUKE = 2.0F;
    private static final float WAVE_PHASE_SPAN = 2.1F; // wave delay from hip to fin
    private static final float IDLE_SPEED = 0.08F;
    private static final float IDLE_AMPLITUDE = 0.04F;
    private static final float WALK_SPEED = 0.6662F; // same frequency as vanilla leg swing
    private static final float WALK_AMPLITUDE = 0.2F;
    private static final float SWIM_SPEED = 0.3F;
    private static final float SWIM_AMPLITUDE_IDLE = 0.1F;
    private static final float SWIM_AMPLITUDE_MOVING = 0.2F;

    private static final Slice[] SLICES = new Slice[SLICE_COUNT];
    private static final int FIRST_BOOTS_SLICE;

    // Per bone: the slices, then the fluke
    private static final float[] LAND_CURL = new float[SLICE_COUNT + 1];
    private static final float[] WAVE_WEIGHT = new float[SLICE_COUNT + 1];
    private static final float[] WAVE_OFFSET = new float[SLICE_COUNT + 1];

    static {
        int last = SLICE_COUNT - 1;
        int firstBoots = SLICE_COUNT;
        for (int i = 0; i < SLICE_COUNT; i++) {
            float t = (float) i / last;
            // Tapers faster toward the fin
            float width = TOP_WIDTH - SLICE_MIN_STEP * i
                    - (TOP_WIDTH - BOTTOM_WIDTH - SLICE_MIN_STEP * last) * (float) Math.pow(t, 2.2);
            float depth = TOP_DEPTH - SLICE_MIN_STEP * i
                    - (TOP_DEPTH - BOTTOM_DEPTH - SLICE_MIN_STEP * last) * t * t;
            SLICES[i] = new Slice(width, depth, TEXTURE_WIDTHS[i], TEXTURE_DEPTHS[i]);
            if (firstBoots == SLICE_COUNT && armorRow(i) >= FIRST_BOOTS_ROW) {
                firstBoots = i;
            }
        }
        FIRST_BOOTS_SLICE = firstBoots;

        int bendingSlices = SLICE_COUNT - HIP_SLICE;
        float curlSum = 0.0F;
        for (int i = HIP_SLICE; i < SLICE_COUNT; i++) {
            curlSum += TAIL_CURL_BIAS + (float) (i - HIP_SLICE) / (bendingSlices - 1);
        }
        for (int i = HIP_SLICE; i < SLICE_COUNT; i++) {
            float t = (float) (i - HIP_SLICE) / (bendingSlices - 1);
            LAND_CURL[i] = TAIL_CURL * (TAIL_CURL_BIAS + t) / curlSum;
            WAVE_WEIGHT[i] = Mth.lerp(t, WAVE_WEIGHT_TOP, WAVE_WEIGHT_BOTTOM);
            WAVE_OFFSET[i] = WAVE_PHASE_SPAN * (i - HIP_SLICE) / bendingSlices;
        }
        LAND_CURL[SLICE_COUNT] = FLUKE_CURL;
        WAVE_WEIGHT[SLICE_COUNT] = WAVE_WEIGHT_FLUKE;
        WAVE_OFFSET[SLICE_COUNT] = WAVE_PHASE_SPAN;
    }

    private final ModelPart waist;
    private final ModelPart rightLegProxy;
    private final ModelPart leftLegProxy;
    private final ModelPart[] slices;
    private final ModelPart[] chain; // slices + fluke, top to bottom

    public MermaidTailModel(ModelPart root) {
        this.body = root.getChild("Body");
        this.waist = body.getChild("Waist");
        this.rightLegProxy = root.getChild("RightLegProxy");
        this.leftLegProxy = root.getChild("LeftLegProxy");

        this.slices = new ModelPart[SLICE_COUNT];
        ModelPart parent = body;
        for (int i = 0; i < SLICE_COUNT; i++) {
            parent = parent.getChild(sliceName(i));
            slices[i] = parent;
        }
        this.chain = Arrays.copyOf(slices, SLICE_COUNT + 1);
        this.chain[SLICE_COUNT] = parent.getChild("Fluke");

        // PlayerAddonLayer syncs these from the player
        this.rightLeg = rightLegProxy;
        this.leftLeg = leftLegProxy;
    }

    public static LayerDefinition createBodyLayer() {
        return buildLayer(false);
    }

    /** Armor shell over the tail, using vanilla armor textures. */
    public static LayerDefinition createArmorLayer() {
        return buildLayer(true);
    }

    private static String sliceName(int index) {
        return "Slice" + index;
    }

    /** The row of the vanilla leg texture that slice {@code index} shows. */
    private static int armorRow(int index) {
        return FIRST_ARMOR_ROW + index * ARMOR_ROW_COUNT / SLICE_COUNT;
    }

    /** Shared by both layers so the tail and its armor always line up. */
    private static LayerDefinition buildLayer(boolean armor) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Only used to read the leg poses
        root.addOrReplaceChild("RightLegProxy", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("LeftLegProxy", CubeListBuilder.create(), PartPose.ZERO);

        // Synced from the player's body
        PartDefinition body = root.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.ZERO);
        body.addOrReplaceChild("Waist", armor ? waistArmor() : waistBase(), PartPose.ZERO);

        PartDefinition parent = body;
        for (int i = 0; i < SLICE_COUNT; i++) {
            parent = parent.addOrReplaceChild(sliceName(i), armor ? sliceArmor(i) : sliceBase(i),
                    PartPose.offset(0.0F, i == 0 ? TAIL_TOP_Y : 1.0F, 0.0F));
        }

        PartDefinition fluke = parent.addOrReplaceChild("Fluke", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.0F, 0.0F));
        fluke.addOrReplaceChild("FlukeRight", armor ? flukeArmorLobe(false) : flukeLobe(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, FLUKE_TWIST, FLUKE_SPREAD));
        fluke.addOrReplaceChild("FlukeLeft", armor ? flukeArmorLobe(true) : flukeLobe(true),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -FLUKE_TWIST, -FLUKE_SPREAD));

        return LayerDefinition.create(mesh, 64, armor ? 32 : 64);
    }

    private static CubeListBuilder waistBase() {
        // Front face is rows 4-9
        return CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, WAIST_TOP_Y, -2.0F, 8.0F, HIPS_Y - WAIST_TOP_Y, 4.0F,
                        new CubeDeformation(WAIST_INFLATE));
    }

    /** Leggings waistband, using the band vanilla leggings draw on the hips. */
    private static CubeListBuilder waistArmor() {
        return CubeListBuilder.create().texOffs(16, 23)
                .addBox(-4.0F, TAIL_TOP_Y - 5.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(WAIST_ARMOR_INFLATE));
    }

    /** A slice whose front face is row FIRST_TEXTURE_ROW + index, centered on u=8. */
    private static CubeListBuilder sliceBase(int index) {
        if (index < HIP_SLICE) {
            return CubeListBuilder.create(); // inside the hip overlay
        }
        Slice slice = SLICES[index];
        int texU = 8 - slice.uvWidth() / 2 - slice.uvDepth();
        int texV = FIRST_TEXTURE_ROW + index - slice.uvDepth();
        return CubeListBuilder.create().texOffs(texU, texV)
                .addBox(-slice.uvWidth() / 2.0F, 0.0F, -slice.uvDepth() / 2.0F,
                        slice.uvWidth(), 1.0F, slice.uvDepth(),
                        new CubeDeformation((slice.width() - slice.uvWidth()) / 2.0F, SLICE_OVERLAP,
                                (slice.depth() - slice.uvDepth()) / 2.0F));
    }

    private static CubeListBuilder sliceArmor(int index) {
        Slice slice = SLICES[index];
        // UV origin is 4 rows above the side faces
        int texV = armorRow(index) - 4;
        return index >= FIRST_BOOTS_SLICE
                ? bootColumn(texV, slice.width(), slice.depth())
                : legHalves(texV, slice.width(), slice.depth());
    }

    /** Leggings on a slice: a right and a mirrored left leg column side by side. */
    private static CubeListBuilder legHalves(int texV, float width, float depth) {
        float halfWidth = width / 2.0F + TAIL_ARMOR_INFLATE;
        float growX = (halfWidth - 4.0F) / 2.0F;
        float growZ = (depth + 2.0F * TAIL_ARMOR_INFLATE - 4.0F) / 2.0F;
        CubeDeformation deformation = new CubeDeformation(growX, SLICE_OVERLAP, growZ);
        // Halves meet at x=0 so they don't z-fight
        return CubeListBuilder.create()
                .texOffs(0, texV).addBox(growX - halfWidth, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, deformation)
                .texOffs(0, texV).mirror().addBox(growX, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, deformation);
    }

    /** One leg column, so the narrow end looks like a single boot. */
    private static CubeListBuilder bootColumn(int texV, float width, float depth) {
        CubeDeformation deformation = new CubeDeformation(
                (width + 2.0F * TAIL_ARMOR_INFLATE - 4.0F) / 2.0F, SLICE_OVERLAP,
                (depth + 2.0F * TAIL_ARMOR_INFLATE - 4.0F) / 2.0F);
        return CubeListBuilder.create().texOffs(0, texV)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, deformation);
    }

    /** Single-faced plane. Addons don't cull faces, so it shows from both sides. */
    private static CubeListBuilder flukeLobe(boolean mirror) {
        return CubeListBuilder.create().texOffs(32, 0).mirror(mirror)
                .addBox(-FLUKE_WIDTH / 2.0F, 0.0F, 0.0F, FLUKE_WIDTH, FLUKE_LENGTH, 0.0F,
                        EnumSet.of(Direction.NORTH));
    }

    /** Thin slab over a fin lobe, stretched from the boot part of the leg texture. */
    private static CubeListBuilder flukeArmorLobe(boolean mirror) {
        float growX = (FLUKE_WIDTH + 2.0F * FLUKE_ARMOR_INFLATE - 4.0F) / 2.0F;
        float growY = (FLUKE_LENGTH + 2.0F * FLUKE_ARMOR_INFLATE - 6.0F) / 2.0F;
        float growZ = (FLUKE_ARMOR_THICKNESS - 4.0F) / 2.0F;
        // Keeps the stretched slab starting at the lobe's root
        float originY = growY - FLUKE_ARMOR_INFLATE;
        return CubeListBuilder.create().texOffs(0, 22).mirror(mirror)
                .addBox(-2.0F, originY, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(growX, growY, growZ));
    }

    /** Picks which armor piece the armor layer draws. Reset with (true, true). */
    public void setArmorPieceVisibility(boolean leggings, boolean boots) {
        waist.visible = leggings;
        // skipDraw still draws children, so the boot slices can show on their own
        for (int i = 0; i < FIRST_BOOTS_SLICE; i++) {
            slices[i].skipDraw = !leggings;
        }
        slices[FIRST_BOOTS_SLICE].visible = boots;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        float swim = getSwimAmount(entity, ageInTicks);
        float moving = Math.min(limbSwingAmount, 1.0F);
        float bodyLean = body.xRot; // forward lean when sneaking

        float idlePhase = ageInTicks * IDLE_SPEED;
        float walkPhase = limbSwing * WALK_SPEED;
        float swimPhase = ageInTicks * SWIM_SPEED;
        float swimAmplitude = SWIM_AMPLITUDE_IDLE + SWIM_AMPLITUDE_MOVING * moving;
        // Sneaking already tilts the tail back, so curl it less
        float curlScale = 1.0F - bodyLean / TAIL_CURL;

        for (int i = 0; i < chain.length; i++) {
            float offset = WAVE_OFFSET[i];
            float weight = WAVE_WEIGHT[i];

            // Land: curl along the ground, sway when idle, wiggle when moving
            float landPitch = LAND_CURL[i] * curlScale
                    + Mth.sin(idlePhase - offset) * IDLE_AMPLITUDE * weight;
            float landRoll = Mth.sin(walkPhase - offset) * WALK_AMPLITUDE * moving * weight;
            // Swimming: straight with a dolphin kick
            float swimPitch = Mth.sin(swimPhase - offset) * swimAmplitude * weight;

            chain[i].xRot = Mth.lerp(swim, landPitch, swimPitch);
            chain[i].yRot = 0.0F;
            chain[i].zRot = landRoll * (1.0F - swim);
        }

        // Follow poses both legs share, like sitting in a boat
        ModelPart hip = chain[HIP_SLICE];
        hip.xRot += (rightLegProxy.xRot + leftLegProxy.xRot) * 0.5F;
        hip.yRot += (rightLegProxy.yRot + leftLegProxy.yRot) * 0.5F;
        hip.zRot += (rightLegProxy.zRot + leftLegProxy.zRot) * 0.5F;
    }

    private static float getSwimAmount(Entity entity, float ageInTicks) {
        if (!(entity instanceof LivingEntity living)) {
            return 0.0F;
        }
        // For living entities ageInTicks is tickCount + partialTick
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        return living.getSwimAmount(partialTick);
    }
}
