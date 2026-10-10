package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * Ledge blocks that give way under you. Each block crumbles on its own {@value #CRUMBLE_TICKS} ticks
 * after a player first stands on it ({@value #MONK_CRUMBLE_TICKS} for a Monk, light on their feet), and
 * comes back {@value #REFORM_TICKS} ticks later (once nothing is in the way), so the route resets.
 * Sneaking doesn't help: standing on it is checked every tick, not by footsteps. No roll, nothing to
 * break; creative and spectator players don't set it off.
 */
public final class CrumblingLedgeType extends ObstacleType {
    public static final int CRUMBLE_TICKS = 10;
    public static final int MONK_CRUMBLE_TICKS = 40;
    public static final int REFORM_TICKS = 100;
    /** How often to look again while something stands where the block would reform. */
    private static final int REFORM_RETRY_TICKS = 20;

    public CrumblingLedgeType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return null;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public boolean interactive() {
        return false;
    }

    @Override
    public boolean ownWhoText() {
        return true;
    }

    @Override
    public boolean alarmsOnFumble() {
        return false;
    }

    public static int crumbleTicks(DndCharacter dndClass) {
        return dndClass == DndCharacter.MONK ? MONK_CRUMBLE_TICKS : CRUMBLE_TICKS;
    }

    @Override
    public void registerEvents() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isOnGround() && !player.isSpectator() && !player.isCreative()) {
                    checkFeet(world, player);
                }
            }
        });
    }

    /** Starts the crumble timer of each sealed ledge block under the player's feet. */
    private void checkFeet(ServerWorld world, ServerPlayerEntity player) {
        Box box = player.getBoundingBox();
        int y = MathHelper.floor(box.minY - 0.05);
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX - 1.0E-6); x++) {
            for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ - 1.0E-6); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(pos);
                if (!(state.getBlock() instanceof ObstacleBlock block) || block.type() != this
                        || state.get(ObstacleBlock.STATE) != ObstacleState.SEALED
                        || world.getBlockTickScheduler().isQueued(pos, block)) {
                    continue;
                }
                int ticks = crumbleTicks(Progression.classOf(player));
                world.scheduleBlockTick(pos, block, ticks);
                world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_STEP, SoundCategory.BLOCKS, 1.0f, 0.6f);
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST,
                        Blocks.STONE_BRICKS.getDefaultState()), x + 0.5, y, z + 0.5, 6, 0.3, 0.0, 0.3, 0.0);
                DnDClasses.LOGGER.info("[Obstacle] {} stepped on a crumbling ledge at {}: gives way in {} ticks",
                        player.getEntityName(), pos.toShortString(), ticks);
            }
        }
    }

    @Override
    public void scheduledTick(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.get(ObstacleBlock.STATE) == ObstacleState.SEALED) {
            world.setBlockState(pos, state.with(ObstacleBlock.STATE, ObstacleState.OPEN), Block.NOTIFY_ALL);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.getDefaultState()),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.35, 0.35, 0.35, 0.1);
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.0f, 0.6f);
            world.scheduleBlockTick(pos, state.getBlock(), REFORM_TICKS);
            return;
        }
        if (!world.getEntitiesByClass(LivingEntity.class, new Box(pos), e -> true).isEmpty()) {
            world.scheduleBlockTick(pos, state.getBlock(), REFORM_RETRY_TICKS);
            return;
        }
        world.setBlockState(pos, state.with(ObstacleBlock.STATE, ObstacleState.SEALED), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.6f, 0.8f);
    }
}
