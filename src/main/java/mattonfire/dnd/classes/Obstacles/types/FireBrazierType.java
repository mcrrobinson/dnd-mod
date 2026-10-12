package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The soul-fire brazier on the far side of a {@link FireCurtainType Fire Curtain}. No roll: whoever gets
 * through (a Warlock, or anyone under Fire Resistance) right-clicks it with an empty hand, it gutters out
 * and the curtain dies for everyone. The first dousing earns the brazier's tier XP. Resetting the brazier
 * ({@code /dndobstacle reset}) relights it and its curtain.
 */
public final class FireBrazierType extends WinchType {
    /** The curtain must be within this many blocks of a hand-placed brazier. */
    public static final int LINK_RADIUS = 8;

    public FireBrazierType(Identifier id, FireCurtainType curtain) {
        super(id, curtain);
    }

    @Override
    public int linkRadius() {
        return LINK_RADIUS;
    }

    @Override
    public Skill skill() {
        return null;
    }

    /** It's the Warlock's job, but anyone who gets through the fire can douse it. */
    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.WARLOCK ? Eligibility.PRIMARY : Eligibility.UNTRAINED;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        BlockPos curtain = target(world, pos);
        if (curtain == null) {
            solver.sendMessage(Text.translatable(translationKey() + ".no_curtain"), true);
            return false;
        }
        if (!ObstacleBlock.isSealed(world.getBlockState(curtain))) {
            solver.sendMessage(Text.translatable(translationKey() + ".already"), true);
            return false;
        }
        int doused = ObstacleGroups.open(world, curtain);
        ObstacleGroups.open(world, pos);
        solver.sendMessage(Text.translatable(translationKey() + ".doused"), true);
        DnDClasses.LOGGER.info("[Obstacle] {} doused the brazier at {}: the fire curtain at {} is out ({} blocks)",
                solver.getEntityName(), pos.toShortString(), curtain.toShortString(), doused);
        return true;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.2, 0.3, 0.2, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.9f);
        }
    }

    /** Relighting the brazier relights its curtain. */
    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (!first) {
            return;
        }
        world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        BlockPos curtain = target(world, pos);
        if (curtain != null) {
            ObstacleGroups.seal(world, curtain);
        }
    }
}
