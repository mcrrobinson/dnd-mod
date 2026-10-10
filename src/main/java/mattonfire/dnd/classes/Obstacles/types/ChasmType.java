package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The top layer of a chasm's gap: the retracted bridge. Sealed, it's invisible and you fall straight
 * through, but it's a sealed obstacle, so nobody can build across or pillar up within a block of it
 * (the protected volume). Open, it's the lowered bridge: solid spruce planks anyone can walk over.
 *
 * <p>Nobody rolls for it. Getting across is movement: a Monk's triple jump clears the
 * {@value #LENGTH}-block gap a sprint jump (about 4 blocks) can't, and whoever reaches the far side
 * lowers the bridge from its {@link BridgeWinchType winch}. The structure puts a pit under it (falling
 * in is the failure) with a way back up; an ender pearl also works.
 */
public final class ChasmType extends ObstacleType {
    /** The gap a chasm should span (a Monk clears it, a sprint jump doesn't). */
    public static final int LENGTH = 7;

    public ChasmType(Identifier id) {
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

    /** Only the lowered bridge has collision. */
    @Override
    public boolean solid(ObstacleState state) {
        return state == ObstacleState.OPEN;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.05);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.5f, 0.6f);
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.2f, 0.7f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1.5f, 0.6f);
        }
    }
}
