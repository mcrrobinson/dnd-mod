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
import net.minecraft.util.math.MathHelper;

/**
 * A puff of a dragon's fire breath: the vanilla flame texture, glowing, flying exactly where it's
 * aimed and slowing down, swelling and fading from white-hot to dark red as it goes. Many of them
 * together make a flamethrower-like jet (FireBreath).
 */
public class DragonFlameParticle extends SpriteBillboardParticle {
    private final float startScale;
    private final float endScale;

    protected DragonFlameParticle(ClientWorld world, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.setSprite(spriteProvider);
        // The base constructor adds random motion; the jet needs exact aim
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.velocityMultiplier = ModParticles.DRAGON_FLAME_DRAG;
        this.gravityStrength = -0.02F; // hot air rises a little
        this.maxAge = ModParticles.DRAGON_FLAME_AGE - 2 + this.random.nextInt(5);
        this.startScale = 0.12F + this.random.nextFloat() * 0.08F;
        this.endScale = 0.55F + this.random.nextFloat() * 0.3F;
        this.scale = this.startScale;
        this.updateColor(0);
    }

    private void updateColor(float t) {
        // White-yellow at the mouth, orange in the middle, dark red at the end
        this.red = 1.0F - 0.45F * MathHelper.clamp((t - 0.6F) / 0.4F, 0, 1);
        this.green = MathHelper.lerp(MathHelper.clamp(t / 0.7F, 0, 1), 0.95F, 0.35F) - 0.2F * MathHelper.clamp((t - 0.7F) / 0.3F, 0, 1);
        this.blue = MathHelper.lerp(MathHelper.clamp(t / 0.3F, 0, 1), 0.7F, 0.1F);
        this.alpha = t < 0.65F ? 0.95F : 0.95F * (1 - (t - 0.65F) / 0.35F);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) this.age / this.maxAge;
        this.scale = MathHelper.lerp((float) Math.sqrt(t), this.startScale, this.endScale);
        this.updateColor(t);
    }

    @Override
    public int getBrightness(float tint) {
        return 0xF000F0; // full bright, like fire
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
            return new DragonFlameParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
