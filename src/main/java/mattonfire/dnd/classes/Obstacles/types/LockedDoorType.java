package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleEvents;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * An iron door with a keyhole. Only a Rogue can pick it: the same Thieves' Tools roll as a locked chest
 * ({@code Lockpicking}), against the obstacle's tier DC, shown as "Lockpicking" on the HUD.
 * <ul>
 * <li>Failure: the lock holds. Natural 1: the pick snaps (the usual longer wait); no alarm.</li>
 * <li>Fallback: tear it down. Hardness 50 (like obsidian); breaking one block brings the whole door down,
 * which raises an alarm. A key from elsewhere in the structure is the other way in, once dungeons and
 * quests hand them out.</li>
 * </ul>
 */
public final class LockedDoorType extends ObstacleType {
    public LockedDoorType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.THIEVES_TOOLS;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.ROGUE ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public String rollLabel() {
        return D20.LOCKPICKING;
    }

    @Override
    public boolean breakable() {
        return true;
    }

    @Override
    public boolean alarmsOnFumble() {
        return false;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0f, 0.8f);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.8f, 1.4f);
    }

    /** Tearing one block out brings the whole door down, loudly. */
    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        int collapsed = 0;
        for (BlockPos part : ObstacleGroups.group(world, pos)) {
            if (!part.equals(pos)) {
                debris(world, part);
                world.setBlockState(part, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                collapsed++;
            }
        }
        debris(world, pos);
        world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 1.2f, 0.6f);
        player.sendMessage(Text.translatable(translationKey() + ".smashed"), true);
        DnDClasses.LOGGER.info("[Obstacle] {} tore down the locked door at {} ({} more blocks)",
                player.getEntityName(), pos.toShortString(), collapsed);
        ObstacleEvents.ALARM.invoker().onAlarm(world, pos, ObstacleEvents.ALARM_RADIUS, player);
    }

    private static void debris(ServerWorld world, BlockPos pos) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.6f, 1.6f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }
}
