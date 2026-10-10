package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.joml.Vector3f;

/**
 * The Lightning Chaser's icy cousin, which only lives in frost lairs on Frozen Peaks: frost breath
 * instead of fire and a hailstorm instead of lightning. Immune to freezing, weak to fire.
 */
public class FrostDrakeEntity extends LairDragonEntity {
    /**
     * Each hailstone: 5 damage to anything within 3 blocks (and up to 3 above), and frozen for 2 seconds. A
     * successful DEX save halves the damage and avoids the Freeze.
     */
    private static final float HAIL_DAMAGE = 5.0F;
    private static final double HAIL_RADIUS = 3.0;
    private static final int HAIL_FREEZE_TICKS = 40;
    /** Fire hurts it this much more. */
    private static final float FIRE_WEAKNESS = 1.5F;

    public static final RegistryKey<Structure> LAIR_STRUCTURE =
            RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "frost_lair"));

    public FrostDrakeEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world, BossBar.Color.BLUE);
    }

    @Override
    protected FireBreath createBreath(DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey) {
        return new FrostBreath(this, head, ticksKey, aimKey, 12.0, 4.0F);
    }

    @Override
    protected RegistryKey<Structure> lairStructure() {
        return LAIR_STRUCTURE;
    }

    // Powder snow doesn't freeze it, and freezing damage (its own breath included) doesn't hurt it
    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isIn(DamageTypeTags.IS_FREEZING) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            amount *= FIRE_WEAKNESS;
        }
        return super.damage(source, amount);
    }

    /** Calls a hailstorm down on the target: one hailstone on it and two more close by. */
    @Override
    public void shoot(LivingEntity target) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        this.hail(serverWorld, target.getX(), target.getY(), target.getZ());
        for (int i = 0; i < 2; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + this.random.nextDouble() * STORM_SPREAD;
            this.hail(serverWorld, target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
        }
    }

    /**
     * A hailstone crashing down: damage the drake deals itself (credited to it, scaled with
     * difficulty), shards of ice and snow, and the sound of breaking ice. It never breaks blocks.
     */
    private void hail(ServerWorld world, double x, double y, double z) {
        DamageSource source = ModDamageTypes.of(this.world, ModDamageTypes.HAILSTONE, this);
        Box area = new Box(x - HAIL_RADIUS, y - HAIL_RADIUS, z - HAIL_RADIUS,
                x + HAIL_RADIUS, y + 3.0D + HAIL_RADIUS, z + HAIL_RADIUS);
        for (LivingEntity victim : this.world.getEntitiesByClass(LivingEntity.class, area, this::isStormVictim)) {
            if (DragonSaves.isUnaffected(victim, source)) {
                continue;
            }
            // One DEX save for the whole hailstorm: the three hailstones land in the same tick
            SaveResult save = DragonSaves.save(victim, this, DragonSaves.HAILSTORM, DragonSaves.STORM_DC,
                    DragonSaves.HAILSTORM_EFFECT, "storm", DragonSaves.STORM_WINDOW);
            if (victim.damage(source, save.damage(HAIL_DAMAGE)) && save.failed()) {
                victim.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, HAIL_FREEZE_TICKS, 0), this);
            }
        }

        // A streak of falling ice, then the impact
        for (int i = 0; i < 12; i++) {
            world.spawnParticles(ParticleTypes.SNOWFLAKE, x, y + 1.0D + i * 0.6D, z, 2, 0.15D, 0.2D, 0.15D, 0.0D);
        }
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
                x, y + 0.2D, z, 60, 0.8D, 0.3D, 0.8D, 0.15D);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.getDefaultState()),
                x, y + 0.2D, z, 30, 1.2D, 0.2D, 1.2D, 0.1D);
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, x, y + 0.3D, z, 20, 1.0D, 0.3D, 1.0D, 0.1D);
        world.playSound(null, x, y, z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.0F,
                0.6F + this.random.nextFloat() * 0.3F);
        world.playSound(null, x, y, z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.HOSTILE, 1.5F,
                0.5F + this.random.nextFloat() * 0.2F);
    }
}
