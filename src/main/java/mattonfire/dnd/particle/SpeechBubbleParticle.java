package mattonfire.dnd.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** The speech bubble over an NPC the DM speaks through ({@code /dm say}): hangs, drifts up a little, fades. */
@Environment(EnvType.CLIENT)
public class SpeechBubbleParticle extends SpriteBillboardParticle {
    private static final int AGE = 60;
    private static final int FADE_TICKS = 15;

    protected SpeechBubbleParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setSprite(sprites);
        this.scale = 0.35F;
        this.maxAge = AGE;
        this.gravityStrength = 0.0F;
        this.velocityX = 0.0;
        this.velocityY = 0.004;
        this.velocityZ = 0.0;
        this.collidesWithWorld = false;
    }

    @Override
    public void tick() {
        super.tick();
        int left = this.maxAge - this.age;
        this.alpha = left < FADE_TICKS ? left / (float) FADE_TICKS : 1.0F;
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new SpeechBubbleParticle(world, x, y, z, sprites);
        }
    }
}
