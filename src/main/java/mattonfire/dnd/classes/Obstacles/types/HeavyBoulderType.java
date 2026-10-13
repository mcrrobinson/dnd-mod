package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleEvents;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A boulder wedged in a corridor. Only a Barbarian can heave it aside (Athletics; Rage gives
 * advantage through the character sheet). Nobody can mine it, but its blast resistance is only
 * {@value #BLAST_RESISTANCE} (like cobblestone), so a single TNT clears the whole boulder, and the bang
 * raises an alarm.
 */
public final class HeavyBoulderType extends ObstacleType {
    public static final float BLAST_RESISTANCE = 6.0f;
    public static final int FAIL_SLOWNESS_TICKS = 100;
    public static final int FAIL_SLOWNESS_AMPLIFIER = 1;

    public HeavyBoulderType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.ATHLETICS;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.BARBARIAN ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public float blastResistance() {
        return BLAST_RESISTANCE;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FAIL_SLOWNESS_TICKS,
                FAIL_SLOWNESS_AMPLIFIER));
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        // It rocks back onto you
        player.damage(world.getDamageSources().fallingBlock(null), FUMBLE_DAMAGE);
        world.playSound(null, pos, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    /** One block blown away takes the rest of the boulder with it. */
    @Override
    public void onExploded(ServerWorld world, BlockPos pos) {
        int cleared = 0;
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.offset(direction);
            if (!isMine(world.getBlockState(next))) {
                continue;
            }
            for (BlockPos part : ObstacleGroups.group(world, next)) {
                dust(world, part);
                world.setBlockState(part, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                cleared++;
            }
        }
        DnDClasses.LOGGER.info("[Obstacle] heavy boulder at {} blown apart ({} more blocks)", pos.toShortString(),
                cleared);
        ObstacleEvents.ALARM.invoker().onAlarm(world, pos, ObstacleEvents.ALARM_RADIUS, null);
    }

    private static void dust(ServerWorld world, BlockPos pos) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STONE.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.15);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        dust(world, pos);
        world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                4, 0.4, 0.1, 0.4, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1.5f, 0.5f);
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.5f, 0.5f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.2f, 0.5f);
        }
    }
}
