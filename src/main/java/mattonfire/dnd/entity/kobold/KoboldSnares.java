package mattonfire.dnd.entity.kobold;

import java.util.Iterator;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Blocks.KoboldSnareBlock;
import mattonfire.dnd.classes.Blocks.KoboldSnareBlockEntity;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Kobold Snares (design 10, 3.4): Trapmakers setting them in a fight, and what stepping into one does.
 * <ul>
 * <li>Setting: every {@value #INTERVAL} ticks in a fight, a Trapmaker hides one on a floor tile on the straight
 * line between it and its target, {@value #MIN_DISTANCE}-{@value #MAX_DISTANCE} blocks from the target. At most
 * {@value #MAX_ACTIVE} each; they fall apart after {@value #LIFETIME} ticks.</li>
 * <li>Springing: {@value #DAMAGE} trap damage and a DEX save (DC {@value KoboldSaves#SNARE_DC}, tag {@code trap}).
 * A failure is Restrained for up to {@value #RESTRAIN_TICKS} ticks, with a STR {@value KoboldSaves#SNARE_DC} save
 * every {@value #REPEAT_TICKS} ticks to break free. The snare is used up either way.</li>
 * </ul>
 */
public final class KoboldSnares {
    public static final int INTERVAL = 20 * 20;
    public static final int MAX_ACTIVE = 3;
    public static final int LIFETIME = 20 * 60;
    public static final int MIN_DISTANCE = 3;
    public static final int MAX_DISTANCE = 6;
    public static final float DAMAGE = 2.0F;
    public static final int RESTRAIN_TICKS = 100;
    public static final int REPEAT_TICKS = 20;
    public static final String CONDITION_KEY = "kobold_snare";

    private KoboldSnares() {
    }

    /** {@code trapmaker} hides a snare in {@code target}'s path; false if there's nowhere to put one. */
    public static boolean placeNear(ServerWorld world, KoboldTrapmakerEntity trapmaker, LivingEntity target) {
        Iterator<BlockPos> it = trapmaker.snares().iterator();
        while (it.hasNext()) {
            if (!world.getBlockState(it.next()).isOf(Kobolds.KOBOLD_SNARE_BLOCK)) {
                it.remove();
            }
        }
        if (trapmaker.snares().size() >= MAX_ACTIVE) {
            return false;
        }
        Vec3d from = target.getPos();
        Vec3d towards = trapmaker.getPos().subtract(from).multiply(1.0D, 0.0D, 1.0D);
        if (towards.lengthSquared() < 1.0E-4D) {
            return false;
        }
        towards = towards.normalize();
        int first = MIN_DISTANCE + trapmaker.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
        for (int i = 0; i <= MAX_DISTANCE - MIN_DISTANCE; i++) {
            int d = MIN_DISTANCE + (first - MIN_DISTANCE + i) % (MAX_DISTANCE - MIN_DISTANCE + 1);
            BlockPos column = BlockPos.ofFloored(from.add(towards.multiply(d)));
            for (int dy : new int[] {0, 1, -1}) {
                BlockPos pos = column.up(dy);
                if (canSetAt(world, pos)) {
                    place(world, pos, trapmaker);
                    trapmaker.snares().add(pos);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canSetAt(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        boolean water = state.isOf(Blocks.WATER) && state.getFluidState().isStill();
        if (!state.isAir() && !water) {
            return false;
        }
        return Kobolds.KOBOLD_SNARE_BLOCK.getDefaultState().canPlaceAt(world, pos);
    }

    /** Sets a kobold's snare at {@code pos} (also used by K2's warrens and by commands). */
    public static void place(ServerWorld world, BlockPos pos, LivingEntity kobold) {
        boolean water = world.getFluidState(pos).getFluid() == Fluids.WATER;
        world.setBlockState(pos, Kobolds.KOBOLD_SNARE_BLOCK.getDefaultState().with(KoboldSnareBlock.WATERLOGGED, water));
        if (world.getBlockEntity(pos) instanceof KoboldSnareBlockEntity snare) {
            snare.setKoboldOwner(kobold.getUuid(), world.getTime() + LIFETIME);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, SoundCategory.HOSTILE, 0.6F, 1.2F);
        MobActions.log(kobold, "snare at " + pos.toShortString(), kobold instanceof net.minecraft.entity.mob.MobEntity mob ? mob.getTarget() : null);
        DnDClasses.LOGGER.info("[Kobold] {} sets a snare at {}", kobold.getName().getString(), pos.toShortString());
    }

    /** {@code victim} stepped into the snare at {@code pos}. */
    public static void spring(ServerWorld world, BlockPos pos, LivingEntity victim) {
        if (!(world.getBlockEntity(pos) instanceof KoboldSnareBlockEntity snare) || snare.spares(victim)) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        world.setBlockState(pos, state.get(KoboldSnareBlock.WATERLOGGED) ? Blocks.WATER.getDefaultState()
                : Blocks.AIR.getDefaultState());
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), pos.getX() + 0.5D,
                pos.getY() + 0.1D, pos.getZ() + 0.5D, 12, 0.3D, 0.05D, 0.3D, 0.05D);
        world.playSound(null, pos, SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, SoundCategory.BLOCKS, 1.0F, 0.8F);
        DnDClasses.LOGGER.info("[Kobold] {} steps in the snare at {}", victim.getName().getString(), pos.toShortString());
        victim.damage(ModDamageTypes.of(world, ModDamageTypes.TRAP), DAMAGE);
        SaveResult save = SavingThrow.of(victim, KoboldSaves.SNARE).label("save.dndclasses.kobold_snare").tags("trap")
                .onSuccess(Text.translatable("save.dndclasses.kobold_snare.success"))
                .onFailure(Text.translatable("save.dndclasses.kobold_snare.failure")).roll();
        if (save.failed() && victim.isAlive()) {
            Conditions.of(victim, Condition.RESTRAINED).key(CONDITION_KEY).sourceName(Text.translatable("block.dndclasses.kobold_snare"))
                    .ticks(RESTRAIN_TICKS).repeatSave(Ability.STR, KoboldSaves.SNARE_DC, REPEAT_TICKS).apply();
        }
    }
}
