package net.nanaky.frost_lava_walker.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class LavaPopParticle extends SingleQuadParticle {

    private final SpriteSet sprites;

    protected LavaPopParticle(ClientLevel level,
                              double x, double y, double z,
                              SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.get(0, 1));
        this.sprites = sprites;
        this.gravity  = 0.0F;
        this.friction = 1.0F;
        this.xd       = 0.0;
        this.yd       = 1.0;
        this.zd       = 0.0;
        this.lifetime = 34;
        this.quadSize *= 3.0F;
        this.setSpriteFromAge(sprites); // frame 0 on spawn
    }

    @Override
    public Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    public void tick() {
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(sprites); // advances frame each tick
    }

    @Override
    public float getQuadSize(float partialTick) {
        float t = (this.age + partialTick) / (float) this.lifetime;
        return this.quadSize * (1.0F - t * t);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz,
                                       RandomSource random) {
            return new LavaPopParticle(level, x, y, z, sprites); // pass the set, not a single sprite
        }
    }
}