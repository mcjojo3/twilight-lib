package mc.sayda.twilight_lib.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class WolfPrintParticle extends TextureSheetParticle {

    public static WolfPrintParticleProvider provider(SpriteSet spriteSet) {
        return new WolfPrintParticleProvider(spriteSet);
    }

    public static class WolfPrintParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public WolfPrintParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new WolfPrintParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;
    private final float rotationYaw; // Store the yaw rotation for footprint direction

    protected WolfPrintParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.25f, 0.25f);
        this.quadSize *= 1f; // Small footprint size
        this.lifetime = mc.sayda.twilight_lib.config.TwilightConfig.FOOTPRINT_LIFETIME_TICKS.get();
        this.gravity = 0.0f; // No gravity - stays on ground
        this.hasPhysics = false; // No collision

        // No movement - footprints stay in place
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        // Store rotation from velocity vector (direction player was moving)
        // vx and vz represent the player's movement direction
        // Subtract PI/2 to align texture top (north) with movement direction
        this.rotationYaw = (float) (Math.atan2(vz, vx) - Math.PI / 2);

        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Fade out as it ages
        float ageRatio = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0f - ageRatio; // Gradually fade to transparent
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Custom rendering to make footprint lay flat on ground (in XZ plane, not billboard)
        float quadSize = this.getQuadSize(partialTicks);

        // Calculate world position
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x);
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y); // Exactly on ground
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z);

        // Create corners for horizontal quad (laying on ground in XZ plane)
        // The quad is horizontal (parallel to ground), rotated around Y axis to face movement direction
        float cos = (float) Math.cos(this.rotationYaw);
        float sin = (float) Math.sin(this.rotationYaw);

        // Define corners in local space (horizontal quad)
        Vector3f[] corners = new Vector3f[4];

        // Corner 0: back-left
        float x0 = -quadSize;
        float z0 = -quadSize;
        corners[0] = new Vector3f(
            x + (x0 * cos - z0 * sin),
            y,
            z + (x0 * sin + z0 * cos)
        );

        // Corner 1: front-left
        float x1 = -quadSize;
        float z1 = quadSize;
        corners[1] = new Vector3f(
            x + (x1 * cos - z1 * sin),
            y,
            z + (x1 * sin + z1 * cos)
        );

        // Corner 2: front-right
        float x2 = quadSize;
        float z2 = quadSize;
        corners[2] = new Vector3f(
            x + (x2 * cos - z2 * sin),
            y,
            z + (x2 * sin + z2 * cos)
        );

        // Corner 3: back-right
        float x3 = quadSize;
        float z3 = -quadSize;
        corners[3] = new Vector3f(
            x + (x3 * cos - z3 * sin),
            y,
            z + (x3 * sin + z3 * cos)
        );

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();
        int light = this.getLightColor(partialTicks);

        // In 1.21.1, VertexConsumer API changed - vertex() now requires position as separate method
        // Render quad with proper winding order
        buffer.addVertex(corners[0].x(), corners[0].y(), corners[0].z())
                .setUv(minU, maxV).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(corners[1].x(), corners[1].y(), corners[1].z())
                .setUv(minU, minV).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(corners[2].x(), corners[2].y(), corners[2].z())
                .setUv(maxU, minV).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(corners[3].x(), corners[3].y(), corners[3].z())
                .setUv(maxU, maxV).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}