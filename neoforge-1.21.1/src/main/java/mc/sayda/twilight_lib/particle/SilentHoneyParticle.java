package mc.sayda.twilight_lib.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Silent version of falling honey particle.
 * Uses vanilla honey textures but doesn't play landing sounds.
 * Matches vanilla behavior: translucent, hangs in air briefly, then falls.
 */
@OnlyIn(Dist.CLIENT)
public class SilentHoneyParticle extends TextureSheetParticle {

    public static SilentHoneyParticleProvider provider(SpriteSet spriteSet) {
        return new SilentHoneyParticleProvider(spriteSet);
    }

    public static class SilentHoneyParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public SilentHoneyParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SilentHoneyParticle(worldIn, x, y, z, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;
    private int hangTime;

    protected SilentHoneyParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;

        // Match vanilla falling honey particle properties
        this.setSize(0.01f, 0.01f);
        this.gravity = 0.06f;
        this.lifetime = (int)(128.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;

        // Honey color (orange-ish) with slight transparency
        this.rCol = 0.976f;
        this.gCol = 0.714f;
        this.bCol = 0.051f;
        this.alpha = 0.8f; // Slightly transparent

        // Hang in air for about 10-15 ticks (0.5-0.75 seconds) before falling
        this.hangTime = 10 + world.random.nextInt(6);

        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.lifetime-- <= 0) {
            this.remove();
            return;
        }

        // Hang in air first, then start falling
        if (this.hangTime > 0) {
            this.hangTime--;
            // Very slow drift while hanging
            this.xd *= 0.02;
            this.zd *= 0.02;
        } else {
            // Apply gravity after hang time
            this.yd -= this.gravity;
        }

        this.move(this.xd, this.yd, this.zd);

        // Slow down over time
        this.xd *= 0.98;
        this.yd *= 0.98;
        this.zd *= 0.98;

        // Remove if on ground (but silently, no sound)
        if (this.onGround) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
