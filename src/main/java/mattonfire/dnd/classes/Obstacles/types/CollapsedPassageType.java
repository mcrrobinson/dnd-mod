package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.BlockState;
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

/**
 * A plug of rubble choking a passage. Cleared with an Athletics check by Barbarians and Fighters
 * (primary) or Paladins (secondary); a raging Barbarian rolls with advantage (from the character sheet).
 * Anyone can mine through it instead: hardness {@value #HARDNESS}, and each block mined has a
 * {@value #CAVE_IN_CHANCE} chance of bringing gravel down for {@value #CAVE_IN_DAMAGE} damage.
 */
public final class CollapsedPassageType extends ObstacleType {
    public static final float HARDNESS = 30.0f;
    public static final float CAVE_IN_CHANCE = 0.25f;
    public static final float CAVE_IN_DAMAGE = 3.0f;
    /** Failure: winded from heaving at it. */
    public static final int FAIL_HUNGER_TICKS = 200;
    public static final int FAIL_SLOWNESS_TICKS = 60;

    public CollapsedPassageType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.ATHLETICS;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return switch (dndClass) {
            case BARBARIAN, FIGHTER -> Eligibility.PRIMARY;
            case PALADIN -> Eligibility.SECONDARY;
            default -> Eligibility.NONE;
        };
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
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, FAIL_HUNGER_TICKS, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FAIL_SLOWNESS_TICKS, 0));
        world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_HIT, SoundCategory.BLOCKS, 1.0f, 0.7f);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        caveIn(world, player);
    }

    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        if (world.getRandom().nextFloat() < CAVE_IN_CHANCE) {
            caveIn(world, player);
        }
    }

    /** Gravel comes down on the player's head. */
    private static void caveIn(ServerWorld world, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().fallingBlock(null), CAVE_IN_DAMAGE);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.getDefaultState()),
                player.getX(), player.getY() + 2.2, player.getZ(), 30, 0.5, 0.2, 0.5, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.2f, 0.6f);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        BlockState dust = (pos.getY() % 2 == 0 ? Blocks.GRAVEL : Blocks.COBBLESTONE).getDefaultState();
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, dust),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.1);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.4, 0.1, 0.4, 0.0);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.5f, 0.6f);
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1.2f, 0.7f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_PLACE, SoundCategory.BLOCKS, 1.2f, 0.7f);
        }
    }
}
