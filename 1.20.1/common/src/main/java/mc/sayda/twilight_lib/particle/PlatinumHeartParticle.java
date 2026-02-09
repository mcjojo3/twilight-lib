package mc.sayda.twilight_lib.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class PlatinumHeartParticle extends TextureSheetParticle {

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new PlatinumHeartParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    protected PlatinumHeartParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz,
            SpriteSet spriteSet) {
        super(world, x, y, z);
        this.setSize(0.2f, 0.2f);
        this.quadSize *= 0.8f;
        this.lifetime = 10;
        this.gravity = -0.3f;
        this.hasPhysics = true;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.pickSprite(spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
