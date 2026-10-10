package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The winch on the far side of a {@link ChasmType chasm}. No roll: whoever gets across (in practice a
 * Monk's triple jump) right-clicks it and the bridge swings down for everyone. The first lowering earns
 * the chasm's tier XP.
 */
public final class BridgeWinchType extends WinchType {
    /** The chasm must be within this many blocks of a hand-placed winch. */
    public static final int LINK_RADIUS = 8;

    public BridgeWinchType(Identifier id, ChasmType chasm) {
        super(id, chasm);
    }

    @Override
    public int linkRadius() {
        return LINK_RADIUS;
    }

    @Override
    public Skill skill() {
        return null;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return Eligibility.UNTRAINED;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        BlockPos bridge = target(world, pos);
        if (bridge == null) {
            solver.sendMessage(Text.translatable(translationKey() + ".no_bridge"), true);
            return false;
        }
        int lowered = ObstacleGroups.open(world, bridge);
        if (lowered == 0) {
            solver.sendMessage(Text.translatable(translationKey() + ".already"), true);
            return false;
        }
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        solver.sendMessage(Text.translatable(translationKey() + ".lowered"), true);
        DnDClasses.LOGGER.info("[Obstacle] {} lowered the bridge at {} ({} blocks)", solver.getEntityName(),
                bridge.toShortString(), lowered);
        return true;
    }
}
