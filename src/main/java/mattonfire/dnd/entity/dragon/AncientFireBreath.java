package mattonfire.dnd.entity.dragon;

import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.DragonSaves;
import mattonfire.dnd.entity.FireBreath;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * An Ancient Red's fire breath (design 10, 5.5): a {@value #RANGE}-block cone, 15 degrees either side, after a
 * {@value #WINDUP_TICKS}-tick inhale with embers at the throat; the flame pours for {@value #FLAME_TICKS} ticks.
 * {@value #DAMAGE} fire damage per hit ({@code dndclasses:ancient_fire_breath}, tagged {@code is_fire}); one DEX
 * save (DC {@value AncientSaves#FIRE_BREATH_DC}) per breath halves it, and burns {@value #BURN_SAVED_SECONDS} s
 * instead of {@value #BURN_SECONDS} s. In its Molten Fury the flame sets fires wherever it lands.
 */
public class AncientFireBreath extends FireBreath {
    public static final double RANGE = 28.0;
    public static final float DAMAGE = 7.0F;
    public static final int WINDUP_TICKS = 20;
    public static final int FLAME_TICKS = 50;
    public static final int BURN_SECONDS = 8;
    public static final int BURN_SECONDS_SAVED = 3;
    /** One breath, wind-up and flame, is one exposure. */
    public static final int SAVE_WINDOW = WINDUP_TICKS + FLAME_TICKS + 10;
    private static final double CONE_TAN = Math.tan(Math.toRadians(15.0));

    private final AncientDragonEntity ancient;

    public AncientFireBreath(AncientDragonEntity dragon, DragonPart head, TrackedData<Integer> ticksKey,
            TrackedData<Vector3f> aimKey) {
        super(dragon, head, ticksKey, aimKey, RANGE, DAMAGE);
        this.ancient = dragon;
    }

    @Override
    protected double coneTan() {
        return CONE_TAN;
    }

    @Override
    protected int windupTicks() {
        return WINDUP_TICKS;
    }

    @Override
    protected int flameTicks() {
        return FLAME_TICKS;
    }

    @Override
    protected void playWindupSound() {
        this.dragon.playSound(ModSounds.ANCIENT_BREATH_INHALE, 4.0F, 1.0F);
    }

    @Override
    protected void playStartSound() {
        this.dragon.playSound(ModSounds.ANCIENT_ROAR, 5.0F, 1.2F);
        this.dragon.playSound(SoundEvents.ITEM_FIRECHARGE_USE, 3.0F, 0.5F);
    }

    @Override
    protected void spawnWindupParticles() {
        Vec3d mouth = this.getMouthPos();
        var random = this.dragon.getRandom();
        for (int i = 0; i < 4; i++) {
            this.dragon.world.addParticle(ParticleTypes.FLAME, mouth.x + random.nextGaussian() * 0.8,
                    mouth.y - 1.0 + random.nextGaussian() * 0.6, mouth.z + random.nextGaussian() * 0.8,
                    0.0, 0.02, 0.0);
        }
        this.dragon.world.addParticle(ParticleTypes.LAVA, mouth.x, mouth.y, mouth.z, 0, 0, 0);
        this.dragon.world.addParticle(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 0, 0.05, 0);
    }

    @Override
    protected void hit(LivingEntity living) {
        DamageSource source = ModDamageTypes.of(this.dragon.world, ModDamageTypes.ANCIENT_FIRE_BREATH, this.dragon);
        if (DragonSaves.isUnaffected(living, source)) {
            return;
        }
        SaveResult save = this.save(living);
        if (living.damage(source, save.damage(this.damage))) {
            living.setOnFireFor(save.succeeded() ? BURN_SECONDS_SAVED : BURN_SECONDS);
        }
    }

    @Override
    protected SaveResult save(LivingEntity living) {
        return SavingThrow.of(living, AncientSaves.FIRE_BREATH)
                .source(this.dragon)
                .exposure(this.dragon, "breath", SAVE_WINDOW)
                .tags("breath", "dragon", "fire")
                .onSuccess(Text.translatable(AncientSaves.FIRE_BREATH.effectKey()))
                .roll();
    }

    @Override
    protected int saveDc() {
        return AncientSaves.FIRE_BREATH_DC;
    }

    @Override
    protected void affectGround(BlockHitResult hit) {
        if (!this.ancient.isMoltenFury()) {
            super.affectGround(hit);
            return;
        }
        // Molten Fury: every block the jet touches catches, where fire can burn
        BlockPos firePos = hit.getBlockPos().offset(hit.getSide());
        if (AbstractFireBlock.canPlaceAt(this.dragon.world, firePos, hit.getSide().getOpposite())) {
            this.dragon.world.setBlockState(firePos, AbstractFireBlock.getState(this.dragon.world, firePos));
        }
    }
}
