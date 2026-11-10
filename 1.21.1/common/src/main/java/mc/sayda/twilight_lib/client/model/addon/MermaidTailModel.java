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
 * Mermaid tail addon - replaces both legs with a long, smoothly tapering tail
 * and a swallowtail fluke.
 *
 * <p>
 * One class, two layers that share the same bones, pivots and animation:
 * <ul>
 * <li>{@link #LAYER_LOCATION} / {@link #createBodyLayer()} - the tail itself
 * (64x64 addon texture)</li>
 * <li>{@link #ARMOR_LAYER_LOCATION} / {@link #createArmorLayer()} - an armor
 * shell drawn over it with vanilla 64x32 armor textures: leggings on the hips
 * and tail, boots on the stalk and fluke</li>
 * </ul>
 * Both run the same setupAnim with the same inputs, so the armor always follows
 * the tail.
 *
 * <p>
 * Shape: a hip overlay on the lower torso blends the body into the tail, then
 * twenty 1px slices narrow along a curve. Each slice is a little smaller than
 * the one above, so the outline reads as a smooth line instead of steps, and
 * slices can overlap slightly (hiding gaps while bending) without z-fighting.
 * The fluke is two flat planes angled outward, so its edges are straight
 * diagonal lines.
 *
 * <p>
 * The tail starts 2px above the hips, where the short_torso addon's torso ends.
 * It lines up with that addon when both are worn, and without it the top slices
 * simply wrap the lower torso. On land it arcs back along the ground, since it
 * is longer than the distance to the floor.
 *
 * <p>
 * The tail hangs from the torso, not the legs, so it stays attached when
 * sneaking moves the two apart. The legs are only read through two empty proxy
 * parts: averaging them keeps poses both legs share (like sitting in a boat)
 * while cancelling out the walking swing.
 *
 * <p>
 * Base texture layout (64x64): the front faces of the hip overlay (rows 4-7)
 * and every slice (rows 8-27, one row each) form one continuous column centered
 * on u=8, so the tail front can be painted as a single shape. Each row also
 * holds that slice's sides and back. The fluke lobe is a flat plane at (32, 0);
 * the left lobe mirrors the right, and its outline comes from the texture's
 * transparency.
 */
@SuppressWarnings("null")
public class MermaidTailModel<T extends Entity> extends BaseAddonModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "mermaid_tail"), "main");
    public static final ModelLayerLocation ARMOR_LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "mermaid_tail_armor"), "main");

    /**
     * One 1px slice of the tail.
     *
     * @param width   real width in pixels
     * @param depth   real depth in pixels
     * @param uvWidth width of its texture box (even, so the front stays centered)
     * @param uvDepth depth of its texture box
     */
    private record Slice(float width, float depth, int uvWidth, int uvDepth) {
    }

    private static final int SLICE_COUNT = 20;
    private static final float TAIL_TOP_Y = 10.0F; // torso height the tail starts at (where short_torso ends)
    private static final int HIP_SLICE = 2; // first slice below the torso; the ones above never bend

    // Size at the top and bottom of the tail. The top must stay wider than the skin's
    // jacket layer (8.5 x 4.5) to cover the lower torso. Every slice shrinks by at
    // least SLICE_MIN_STEP so neighbouring faces are never coplanar.
    private static final float TOP_WIDTH = 8.8F;
    private static final float TOP_DEPTH = 4.8F;
    private static final float BOTTOM_WIDTH = 3.6F;
    private static final float BOTTOM_DEPTH = 1.4F;
    private static final float SLICE_MIN_STEP = 0.1F;
    private static final float SLICE_OVERLAP = 0.1F; // hides gaps when the tail bends
    private static final int FIRST_TEXTURE_ROW = 8; // front row of the first slice

    // Leggings fill leg rows 20-28 of a vanilla armor texture and boots rows 29-31.
    // The 12 rows are spread evenly down the slices.
    private static final int FIRST_ARMOR_ROW = 20;
    private static final int ARMOR_ROW_COUNT = 12;
    private static final int FIRST_BOOTS_ROW = 29;

    // Hip overlay sits outside the skin's jacket layer (0.25) to avoid z-fighting, and
    // outside the first slice (half size 4.4 x 2.4), whose top face would otherwise
    // stick out below it as a thin bright line
    private static final float WAIST_INFLATE = 0.45F;

    private static final float FLUKE_WIDTH = 6.0F;
    private static final float FLUKE_LENGTH = 10.0F;
    private static final float FLUKE_SPREAD = 0.8F; // how far each lobe angles outward (radians)
    private static final float FLUKE_TWIST = 0.12F; // slight twist so the lobes don't z-fight where they cross

    // How far the armor shell sticks out past the tail. The waistband stays wider than
    // the first armored slice (half size 4.8 x 2.8), for the same reason as WAIST_INFLATE
    private static final float WAIST_ARMOR_INFLATE = 0.85F;
    private static final float TAIL_ARMOR_INFLATE = 0.4F;
    private static final float FLUKE_ARMOR_INFLATE = 0.25F;
    private static final float FLUKE_ARMOR_THICKNESS = 0.5F;

    // Animation tuning. Rotations in radians, speeds in radians per tick.
    private static final float TAIL_CURL = 1.75F; // total backward arc on land, spread over the slices
    private static final float TAIL_CURL_BIAS = 0.9F; // lower = the arc tightens more toward the fluke
    private static final float FLUKE_CURL = 0.25F; // lifts the fluke slightly off the ground
    private static final float WAVE_WEIGHT_TOP = 0.15F; // how strongly the top slice joins the waves
    private static final float WAVE_WEIGHT_BOTTOM = 0.28F; // ...and the bottom slice
    private static final float WAVE_WEIGHT_FLUKE = 2.0F; // the fluke flaps the most
    private static final float WAVE_PHASE_SPAN = 2.1F; // wave delay from hip to fluke
    private static final float IDLE_SPEED = 0.08F;
    private static final float IDLE_AMPLITUDE = 0.04F;
    private static final float WALK_SPEED = 0.6662F; // same frequency as vanilla leg swing
    private static final float WALK_AMPLITUDE = 0.2F;
    private static final float SWIM_SPEED = 0.3F;
    private static final float SWIM_AMPLITUDE_IDLE = 0.1F;
    private static final float SWIM_AMPLITUDE_MOVING = 0.2F;

    private static final Slice[] SLICES = new Slice[SLICE_COUNT];
    private static final int FIRST_BOOTS_SLICE;

    // Per bone (slices, then fluke): resting curl, wave weight and wave delay
    private static final float[] LAND_CURL = new float[SLICE_COUNT + 1];
    private static final float[] WAVE_WEIGHT = new float[SLICE_COUNT + 1];
    private static final float[] WAVE_OFFSET = new float[SLICE_COUNT + 1];

    static {
        int last = SLICE_COUNT - 1;
        int firstBoots = SLICE_COUNT;
        for (int i = 0; i < SLICE_COUNT; i++) {
            float t = (float) i / last;
            // A steady shrink plus a curve, so the taper speeds up toward the fluke
            float width = TOP_WIDTH - SLICE_MIN_STEP * i
                    - (TOP_WIDTH - BOTTOM_WIDTH - SLICE_MIN_STEP * last) * (float) Math.pow(t, 2.2);
            float depth = TOP_DEPTH - SLICE_MIN_STEP * i
                    - (TOP_DEPTH - BOTTOM_DEPTH - SLICE_MIN_STEP * last) * t * t;
            int uvWidth = Mth.clamp(2 * Math.round(width / 2.0F), 2, 8);
            int uvDepth = Mth.clamp(Math.round(depth), 1, 4);
            SLICES[i] = new Slice(width, depth, uvWidth, uvDepth);
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

    /**
     * Armor shell over the tail, textured with a vanilla 64x32 armor texture.
     */
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

    /**
     * Builds the bone hierarchy shared by both layers, so pivots and names always
     * match between the tail and its armor.
     */
    private static LayerDefinition buildLayer(boolean armor) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Empty parts that only receive the leg poses (see class docs)
        root.addOrReplaceChild("RightLegProxy", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("LeftLegProxy", CubeListBuilder.create(), PartPose.ZERO);

        // Synced from the player's body; everything else hangs off it
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
        // Front face lands on rows 4-7, right above the slices' front column
        return CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, TAIL_TOP_Y - 4.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(WAIST_INFLATE));
    }

    /**
     * The leggings' waistband: rows 27-31 of the body area (UV origin 16,16), the
     * same band vanilla leggings draw on the hips, ending where the tail starts.
     */
    private static CubeListBuilder waistArmor() {
        return CubeListBuilder.create().texOffs(16, 23)
                .addBox(-4.0F, TAIL_TOP_Y - 5.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(WAIST_ARMOR_INFLATE));
    }

    /**
     * One slice with its own whole-pixel texture box, placed so its front face is
     * row {@code FIRST_TEXTURE_ROW + index}, centered on u=8. CubeDeformation trims
     * the box to the slice's real size.
     */
    private static CubeListBuilder sliceBase(int index) {
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
        // The leg area's UV origin is 4 rows above where its side faces start
        int texV = armorRow(index) - 4;
        return index >= FIRST_BOOTS_SLICE
                ? bootColumn(texV, slice.width(), slice.depth())
                : legHalves(texV, slice.width(), slice.depth());
    }

    /**
     * Two side-by-side boxes over one slice: the right half textured like a right
     * leg and the left half mirrored like a left leg, so leggings look like both
     * pant legs fused together. The UV box is always one leg column (4x1x4) and
     * CubeDeformation stretches it to the slice's size.
     */
    private static CubeListBuilder legHalves(int texV, float width, float depth) {
        float halfWidth = width / 2.0F + TAIL_ARMOR_INFLATE;
        float growX = (halfWidth - 4.0F) / 2.0F;
        float growZ = (depth + 2.0F * TAIL_ARMOR_INFLATE - 4.0F) / 2.0F;
        CubeDeformation deformation = new CubeDeformation(growX, SLICE_OVERLAP, growZ);
        // The halves meet exactly at x=0 so their front faces don't overlap and z-fight
        return CubeListBuilder.create()
                .texOffs(0, texV).addBox(growX - halfWidth, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, deformation)
                .texOffs(0, texV).mirror().addBox(growX, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, deformation);
    }

    /**
     * One leg column over a narrow stalk slice, so it looks like a single boot.
     */
    private static CubeListBuilder bootColumn(int texV, float width, float depth) {
        CubeDeformation deformation = new CubeDeformation(
                (width + 2.0F * TAIL_ARMOR_INFLATE - 4.0F) / 2.0F, SLICE_OVERLAP,
                (depth + 2.0F * TAIL_ARMOR_INFLATE - 4.0F) / 2.0F);
        return CubeListBuilder.create().texOffs(0, texV)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F, deformation);
    }

    /**
     * A flat, single-faced plane. Addon render types don't cull faces, so it shows
     * from both sides without a second face to z-fight with.
     */
    private static CubeListBuilder flukeLobe(boolean mirror) {
        return CubeListBuilder.create().texOffs(32, 0).mirror(mirror)
                .addBox(-FLUKE_WIDTH / 2.0F, 0.0F, 0.0F, FLUKE_WIDTH, FLUKE_LENGTH, 0.0F,
                        EnumSet.of(Direction.NORTH));
    }

    /**
     * A thin slab around a fluke lobe textured with the 4x6 boot area of the leg
     * (rows 26-31), stretched to the lobe's size: its front gets the boot's front
     * and its back the boot's back.
     */
    private static CubeListBuilder flukeArmorLobe(boolean mirror) {
        float growX = (FLUKE_WIDTH + 2.0F * FLUKE_ARMOR_INFLATE - 4.0F) / 2.0F;
        float growY = (FLUKE_LENGTH + 2.0F * FLUKE_ARMOR_INFLATE - 6.0F) / 2.0F;
        float growZ = (FLUKE_ARMOR_THICKNESS - 4.0F) / 2.0F;
        // Shifted down so the stretched slab still starts at the lobe's root
        float originY = growY - FLUKE_ARMOR_INFLATE;
        return CubeListBuilder.create().texOffs(0, 22).mirror(mirror)
                .addBox(-2.0F, originY, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(growX, growY, growZ));
    }

    /**
     * Picks which armor piece the next render draws. Only meaningful on the armor
     * layer. Call with (true, true) afterwards to restore.
     *
     * @param leggings draw the waistband and the upper slices
     * @param boots    draw the stalk slices and fluke
     */
    public void setArmorPieceVisibility(boolean leggings, boolean boots) {
        waist.visible = leggings;
        // skipDraw hides a part's own cubes but still draws its children, so the
        // stalk and fluke (children of the upper slices) can be drawn on their own
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
        float bodyLean = body.xRot; // the torso's forward lean, set by the sync when sneaking

        float idlePhase = ageInTicks * IDLE_SPEED;
        float walkPhase = limbSwing * WALK_SPEED;
        float swimPhase = ageInTicks * SWIM_SPEED;
        float swimAmplitude = SWIM_AMPLITUDE_IDLE + SWIM_AMPLITUDE_MOVING * moving;
        // When sneaking the tail already starts tilted back with the torso, so it needs
        // that much less arc to end up pointing the same way as when standing
        float curlScale = 1.0F - bodyLean / TAIL_CURL;

        for (int i = 0; i < chain.length; i++) {
            float offset = WAVE_OFFSET[i];
            float weight = WAVE_WEIGHT[i];

            // On land: arc back along the ground with the fluke lifted, a gentle idle
            // sway, and a side-to-side wiggle while moving
            float landPitch = LAND_CURL[i] * curlScale
                    + Mth.sin(idlePhase - offset) * IDLE_AMPLITUDE * weight;
            float landRoll = Mth.sin(walkPhase - offset) * WALK_AMPLITUDE * moving * weight;
            // Swimming: straight tail with an up-and-down dolphin kick
            float swimPitch = Mth.sin(swimPhase - offset) * swimAmplitude * weight;

            chain[i].xRot = Mth.lerp(swim, landPitch, swimPitch);
            chain[i].yRot = 0.0F;
            chain[i].zRot = landRoll * (1.0F - swim);
        }

        // Follow poses both legs share, at the hip
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
