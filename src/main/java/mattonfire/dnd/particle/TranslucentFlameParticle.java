package mattonfire.dnd.particle;

import net.minecraft.client.particle.SpriteBillboardParticle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.client.particle.SpriteProvider;

public class TranslucentFlameParticle extends SpriteBillboardParticle {
    protected TranslucentFlameParticle(ClientWorld world, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.setSprite(spriteProvider);
        this.scale = 0.1F;
        this.maxAge = 20;
        this.alpha = 0.2F; // Opacity (0.0 = transparent, 1.0 = opaque)
    }

    @Override
    public void tick() {
        super.tick();
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new TranslucentFlameParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider);
        }
    }
}