package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * An iron portcullis. Nobody rolls against the gate itself: its {@link PortcullisWinchType winch}
 * raises it. Anyone can mine through instead (hardness 50, like obsidian, per bar), and a Fighter can
 * wedge it open for good from the winch.
 *
 * <p>The Fighter is its only PRIMARY (the wedge is their exclusive job, see {@code ObstacleCodex}), but the gate
 * itself takes no clicks: the wedge happens at the winch. Its hint names the winch and the Fighter.
 */
public final class PortcullisType extends ObstacleType {
    public PortcullisType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return null;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.FIGHTER ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    /** Worked from its winch, so a Fighter isn't told to right-click the gate. */
    @Override
    public boolean interactive() {
        return false;
    }

    @Override
    public boolean breakable() {
        return true;
    }

    @Override
    public boolean ownWhoText() {
        return true;
    }

    @Override
    public boolean alarmsOnFumble() {
        return false;
    }

    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.2f, 0.6f);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.2f, 0.6f);
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.2f, 0.5f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.2f, 0.6f);
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.4f, 0.7f);
        }
    }
}
