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
 * <p>
 * The frost variant (FrostBreath) flies the same way but is a white-blue puff of the generic smoke
 * texture that sinks a little, fading from white to icy blue.
 */
public class DragonFlameParticle extends SpriteBillboardParticle {
    private final float startScale;
    private final float endScale;
    private final boolean frost;

    protected DragonFlameParticle(ClientWorld world, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, SpriteProvider spriteProvider, boolean frost) {
        super(world, x, y, z);
        this.frost = frost;
        this.setSprite(spriteProvider);
        // The base constructor adds random motion; the jet needs exact aim
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.velocityMultiplier = ModParticles.DRAGON_FLAME_DRAG;
        this.gravityStrength = frost ? 0.02F : -0.02F; // hot air rises a little, cold air sinks
        this.maxAge = ModParticles.DRAGON_FLAME_AGE - 2 + this.random.nextInt(5);
        this.startScale = 0.12F + this.random.nextFloat() * 0.08F;
        this.endScale = 0.55F + this.random.nextFloat() * 0.3F;
        this.scale = this.startScale;
        this.updateColor(0);
    }

    private void updateColor(float t) {
        if (this.frost) {
            // White at the mouth, pale cyan in the middle, icy blue at the end
            this.red = MathHelper.lerp(MathHelper.clamp(t / 0.8F, 0, 1), 0.95F, 0.55F);
            this.green = MathHelper.lerp(MathHelper.clamp(t / 0.8F, 0, 1), 0.98F, 0.8F);
            this.blue = 1.0F;
            this.alpha = t < 0.6F ? 0.85F : 0.85F * (1 - (t - 0.6F) / 0.4F);
            return;
        }
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
            return new DragonFlameParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider, false);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class FrostFactory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public FrostFactory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new DragonFlameParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider, true);
        }
    }
}
