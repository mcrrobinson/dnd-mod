package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.particle.ModParticles;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * The Frost Drake's breath: the same wind-up, cone and timing as {@link FireBreath}, but a jet of
 * frost. It deals freezing damage, slows and briefly freezes what it hits instead of setting it
 * alight, and with mobGriefing on it freezes water and drifts snow where it lands.
 */
public class FrostBreath extends FireBreath {
    /** Slowness II for 5 seconds (2.5 on a successful save). */
    public static final int SLOWNESS_TICKS = 100;
    public static final int SLOWNESS_AMPLIFIER = 1;
    /** The Freeze effect (held in place) for 2 seconds; a successful save avoids it. */
    public static final int FREEZE_TICKS = 40;
    private static final BlockState WATER_SOURCE = Blocks.WATER.getDefaultState();

    public FrostBreath(TameableEntity dragon, DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey,
            double range, float damage) {
        super(dragon, head, ticksKey, aimKey, range, damage);
    }

    @Override
    protected void playStartSound() {
        this.dragon.playSound(SoundEvents.ENTITY_PLAYER_HURT_FREEZE, 2.0F, 0.5F);
    }

    @Override
    protected void playLoopSound() {
        this.dragon.world.playSound(null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(),
                SoundEvents.BLOCK_POWDER_SNOW_BREAK, SoundCategory.HOSTILE, 1.5F,
                0.5F + this.dragon.getRandom().nextFloat() * 0.2F);
    }

    /** A successful DEX save halves the damage and the Slowness and avoids the Freeze. */
    @Override
    protected void hit(LivingEntity living) {
        DamageSource source = ModDamageTypes.of(this.dragon.world, ModDamageTypes.FROST_BREATH, this.dragon);
        if (DragonSaves.isUnaffected(living, source)) {
            return;
        }
        SaveResult save = this.save(living);
        if (living.damage(source, save.damage(this.damage))) {
            living.extinguish();
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, save.duration(SLOWNESS_TICKS),
                    SLOWNESS_AMPLIFIER), this.dragon);
            if (save.failed()) {
                living.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, FREEZE_TICKS, 0), this.dragon);
            }
        }
    }

    @Override
    protected String saveLabel() {
        return DragonSaves.FROST_BREATH;
    }

    @Override
    protected String saveEffect() {
        return DragonSaves.FROST_BREATH_EFFECT;
    }

    /** Sometimes freezes the water it lands on (frosted ice, like Frost Walker) or drifts snow onto the ground. */
    @Override
    protected void affectGround(BlockHitResult hit) {
        World world = this.dragon.world;
        if (this.dragon.getRandom().nextInt(3) != 0) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (world.getBlockState(pos) == WATER_SOURCE) {
            // A small patch of the surface round where it lands
            for (BlockPos at : BlockPos.iterate(pos.add(-1, 0, -1), pos.add(1, 0, 1))) {
                if (world.getBlockState(at) == WATER_SOURCE && world.getBlockState(at.up()).isAir()
                        && world.canPlace(Blocks.FROSTED_ICE.getDefaultState(), at, ShapeContext.absent())) {
                    world.setBlockState(at, Blocks.FROSTED_ICE.getDefaultState());
                    world.scheduleBlockTick(at, Blocks.FROSTED_ICE, MathHelper.nextInt(this.dragon.getRandom(), 60, 120));
                }
            }
            return;
        }
        if (hit.getSide() == Direction.UP) {
            BlockPos above = pos.up();
            if (world.getBlockState(above).isAir() && Blocks.SNOW.getDefaultState().canPlaceAt(world, above)) {
                world.setBlockState(above, Blocks.SNOW.getDefaultState());
            }
        }
    }

    @Override
    protected void spawnParticles() {
        Vec3d mouth = this.getMouthPos();
        Vec3d aim = this.getAim();
        var random = this.dragon.getRandom();
        // The jet stops at the first block (or water surface) in its way
        BlockHitResult hit = this.dragon.world.raycast(new RaycastContext(mouth, mouth.add(aim.multiply(this.range)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, this.dragon));
        double length = hit.getPos().distanceTo(mouth);
        Vec3d side = aim.crossProduct(Math.abs(aim.y) > 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize();
        Vec3d up = side.crossProduct(aim);

        // Frost puffs launched like the fire breath's flame puffs (same drag and lifetime), so the jet
        // has the same reach and shape
        double reach = (1 - Math.pow(ModParticles.DRAGON_FLAME_DRAG, ModParticles.DRAGON_FLAME_AGE)) / (1 - ModParticles.DRAGON_FLAME_DRAG);
        for (int i = 0; i < 30; i++) {
            double along = random.nextDouble() * Math.min(1.5, length * 0.15);
            double speed = (length - along) / reach * (0.85 + random.nextDouble() * 0.25);
            double a = random.nextGaussian() * CONE_TAN * 0.6;
            double b = random.nextGaussian() * CONE_TAN * 0.6;
            Vec3d pos = mouth.add(aim.multiply(along));
            Vec3d velocity = aim.add(side.multiply(a)).add(up.multiply(b)).multiply(speed);
            this.dragon.world.addParticle(ModParticles.DRAGON_FROST, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
        // Snowflakes swirling round the jet
        for (int i = 0; i < 6; i++) {
            double along = length * random.nextDouble();
            double radius = 0.3 + along * CONE_TAN;
            Vec3d pos = mouth.add(aim.multiply(along)).add(side.multiply(random.nextGaussian() * radius))
                    .add(up.multiply(random.nextGaussian() * radius));
            this.dragon.world.addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, aim.x * 0.1, aim.y * 0.1, aim.z * 0.1);
        }
        // Ice shards bursting off whatever it hits
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3d at = hit.getPos();
            for (int i = 0; i < 4; i++) {
                this.dragon.world.addParticle(ParticleTypes.ITEM_SNOWBALL, at.x, at.y, at.z,
                        random.nextGaussian() * 0.08, random.nextDouble() * 0.08, random.nextGaussian() * 0.08);
            }
        }
    }
}
