package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleEvents;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
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
 * A gate of stacked bone with a skull keystone. Only a Necromancer can command it open (Intimidation:
 * "command the dead").
 * <ul>
 * <li>Failure: the skull cackles. Natural 1: {@value #FUMBLE_RISEN} skeletons rise, and an alarm.</li>
 * <li>Fallback: anyone can break it (hardness {@value #HARDNESS}, like bone). Breaking any block brings
 * the whole gate down, and {@value #BREAK_SKELETONS} skeletons and {@value #BREAK_WITHER_SKELETONS}
 * wither skeleton rise from the bones, with an alarm.</li>
 * </ul>
 * Undead already ignore Necromancers, so a Necromancer who smashes it is still left alone.
 */
public final class OssuaryGateType extends ObstacleType {
    public static final float HARDNESS = 2.0f;
    public static final int FUMBLE_RISEN = 2;
    public static final int BREAK_SKELETONS = 4;
    public static final int BREAK_WITHER_SKELETONS = 1;

    public OssuaryGateType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.INTIMIDATION;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.NECROMANCER ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return true;
    }

    @Override
    public float hardness() {
        return HARDNESS;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.ENTITY_SKELETON_AMBIENT, SoundCategory.BLOCKS, 1.5f, 0.5f);
        world.playSound(null, pos, SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.BLOCKS, 0.8f, 0.6f);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        onFailure(world, pos, player);
        RisenUndead.raise(world, pos, EntityType.SKELETON, FUMBLE_RISEN, player);
    }

    /** One broken block brings the whole gate down, and the bones get up. */
    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        int collapsed = 0;
        for (BlockPos part : ObstacleGroups.group(world, pos)) {
            if (!part.equals(pos)) {
                bones(world, part);
                world.setBlockState(part, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                collapsed++;
            }
        }
        bones(world, pos);
        world.playSound(null, pos, SoundEvents.ENTITY_SKELETON_DEATH, SoundCategory.BLOCKS, 1.5f, 0.5f);
        player.sendMessage(Text.translatable(translationKey() + ".smashed"), true);
        DnDClasses.LOGGER.info("[Obstacle] {} smashed the ossuary gate at {} ({} more blocks)",
                player.getEntityName(), pos.toShortString(), collapsed);
        RisenUndead.raise(world, pos, EntityType.SKELETON, BREAK_SKELETONS, player);
        RisenUndead.raise(world, pos, EntityType.WITHER_SKELETON, BREAK_WITHER_SKELETONS, player);
        ObstacleEvents.ALARM.invoker().onAlarm(world, pos, ObstacleEvents.ALARM_RADIUS, player);
    }

    private static void bones(ServerWorld world, BlockPos pos) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.1);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        bones(world, pos);
        world.spawnParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                4, 0.3, 0.3, 0.3, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.ENTITY_SKELETON_HURT, SoundCategory.BLOCKS, 1.2f, 0.5f);
            world.playSound(null, pos, SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.BLOCKS, 1.5f, 0.7f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_BONE_BLOCK_PLACE, SoundCategory.BLOCKS, 1.5f, 0.6f);
        }
    }
}
