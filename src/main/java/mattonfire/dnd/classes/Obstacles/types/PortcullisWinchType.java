package mattonfire.dnd.classes.Obstacles.types;

import java.util.List;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PartyRole;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The winch beside a portcullis. Anyone can try to crank it (Athletics, untrained); Tank-role classes
 * (Barbarian, Fighter, Paladin) are its primaries and hold it longer:
 * <ul>
 * <li>Success raises the portcullis for {@value #RAISE_TICKS} ticks ({@value #TANK_RAISE_TICKS} for a Tank),
 * then it drops again. The winch earns class XP once until it's reset: the first raise (or wedge).</li>
 * <li><b>Wedge</b> (Fighter only): a second success while the gate is up jams the winch, and the gate stays
 * up for good. The winch shows as wedged ({@code state=open}) until the obstacle is reset. It earns no
 * XP beyond the raise's.</li>
 * <li>Failure: the gate stays down. Natural 1: the handle whips back for {@value #FUMBLE_DAMAGE} damage.</li>
 * </ul>
 * Resetting a wedged winch drops its portcullis.
 */
public final class PortcullisWinchType extends WinchType {
    public static final int RAISE_TICKS = 80;
    public static final int TANK_RAISE_TICKS = 100;
    public static final float FUMBLE_DAMAGE = 2.0f;
    /** The portcullis must be within this many blocks of a hand-placed winch. */
    public static final int LINK_RADIUS = 6;
    /** Set once the winch has earned its XP (first raise or wedge); cleared by a reset. */
    private static final String RAISED_KEY = "Raised";

    public PortcullisWinchType(Identifier id, PortcullisType portcullis) {
        super(id, portcullis);
    }

    @Override
    public int linkRadius() {
        return LINK_RADIUS;
    }

    @Override
    public Skill skill() {
        return Skill.ATHLETICS;
    }

    public static boolean isTank(DndCharacter dndClass) {
        ClassInfo info = dndClass == DndCharacter.NONE ? null : ClassInfo.get(dndClass);
        return info != null && info.role() == PartyRole.TANK;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return isTank(dndClass) ? Eligibility.PRIMARY : Eligibility.UNTRAINED;
    }

    @Override
    public boolean alarmsOnFumble() {
        return false;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        BlockPos gate = target(world, pos);
        if (gate == null) {
            solver.sendMessage(Text.translatable(translationKey() + ".no_gate"), true);
            return false;
        }
        DndCharacter dndClass = D20.classOf(solver);
        if (!ObstacleBlock.isSealed(world.getBlockState(gate)) && !ObstacleGroups.openOnTimer(world, gate)) {
            solver.sendMessage(Text.translatable(translationKey() + ".already_open"), true);
            return false;
        }
        if (dndClass == DndCharacter.FIGHTER && ObstacleGroups.openOnTimer(world, gate) && !solver.isCreative()) {
            ObstacleGroups.setResealTicks(world, gate, 0);
            BlockState state = world.getBlockState(pos);
            world.setBlockState(pos, state.with(ObstacleBlock.STATE, ObstacleState.OPEN), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.8f, 1.4f);
            solver.sendMessage(Text.translatable(translationKey() + ".wedged"), true);
            DnDClasses.LOGGER.info("[Obstacle] {} wedged the portcullis at {} open", solver.getEntityName(),
                    gate.toShortString());
            // The winch earns its XP once per reset, so a wedge after the raise earns nothing more
            return firstTime(world, pos, RAISED_KEY);
        }
        int ticks = isTank(dndClass) ? TANK_RAISE_TICKS : RAISE_TICKS;
        ObstacleGroups.openFor(world, gate, ticks);
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        solver.sendMessage(Text.translatable(translationKey() + ".raised", ticks / 20), true);
        DnDClasses.LOGGER.info("[Obstacle] {} raised the portcullis at {} for {} ticks", solver.getEntityName(),
                gate.toShortString(), ticks);
        // XP only for the first raise, so cranking it again and again isn't a way to level
        return firstTime(world, pos, RAISED_KEY);
    }

    /** Sets the winch's {@code key} flag; true if it wasn't set yet. Cleared by {@code ObstacleGroups.reset}. */
    private static boolean firstTime(ServerWorld world, BlockPos pos, String key) {
        if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be && !be.data().getBoolean(key)) {
            be.data().putBoolean(key, true);
            be.markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.BLOCKS, 1.0f, 0.6f);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().generic(), FUMBLE_DAMAGE);
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    /** A wedged winch being reset lets its gate drop. */
    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        BlockPos gate = target(world, pos);
        if (first && gate != null) {
            ObstacleGroups.seal(world, gate);
        }
    }

    @Override
    public List<String> progressKeys() {
        return List.of(RAISED_KEY);
    }
}
