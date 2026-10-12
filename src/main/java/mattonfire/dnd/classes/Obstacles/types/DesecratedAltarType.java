package mattonfire.dnd.classes.Obstacles.types;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A cracked blackstone altar burning with soul fire. Only a Cleric can consecrate it (Religion):
 * <ul>
 * <li>Every mob spawner within {@value #SPAWNER_RADIUS} blocks stops for {@value #CONSECRATED_TICKS}
 * ticks (10 minutes), and the Cleric and their party within {@value #HEAL_RADIUS} blocks are healed
 * {@value #HEAL_AMOUNT} health. The altar then slowly desecrates itself again (it reseals), and the
 * spawners wake. Only the first consecration earns class XP.</li>
 * <li>Failure: Wither I for {@value #FAIL_WITHER_TICKS} ticks. Natural 1: {@value #FUMBLE_RISEN} zombies
 * rise beside it, and an alarm.</li>
 * <li>Fallback (it's optional): anyone can smash it (hardness {@value #HARDNESS}), but the spawners stay
 * on and {@value #BREAK_RISEN} undead rise.</li>
 * </ul>
 * A spawner is suppressed while a consecrated (open) altar is within range; {@code MobSpawnerLogicMixin}
 * asks {@link #suppresses}.
 */
public final class DesecratedAltarType extends ObstacleType {
    public static final int SPAWNER_RADIUS = 12;
    public static final int CONSECRATED_TICKS = 20 * 60 * 10;
    public static final int HEAL_RADIUS = 8;
    public static final float HEAL_AMOUNT = 8.0f;
    public static final int FAIL_WITHER_TICKS = 100;
    public static final int FUMBLE_RISEN = 2;
    public static final float HARDNESS = 5.0f;
    public static final int BREAK_RISEN = 4;
    private static final String CONSECRATED_KEY = "Consecrated";

    public DesecratedAltarType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.RELIGION;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.CLERIC ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return true;
    }

    @Override
    public float hardness() {
        return HARDNESS;
    }

    /** It's an altar: you can stand on it either way. */
    @Override
    public boolean solid(ObstacleState state) {
        return true;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        if (ObstacleGroups.openFor(world, pos, CONSECRATED_TICKS) == 0) {
            return false;
        }
        List<ServerPlayerEntity> healed = new ArrayList<>(PartyManager.nearbyMembers(solver, HEAL_RADIUS));
        healed.add(solver);
        for (ServerPlayerEntity player : healed) {
            player.heal(HEAL_AMOUNT);
            world.spawnParticles(ParticleTypes.HEART, player.getX(), player.getBodyY(0.8), player.getZ(),
                    5, 0.4, 0.3, 0.4, 0.0);
        }
        int spawners = 0;
        for (BlockPos near : BlockPos.iterate(pos.add(-SPAWNER_RADIUS, -SPAWNER_RADIUS, -SPAWNER_RADIUS),
                pos.add(SPAWNER_RADIUS, SPAWNER_RADIUS, SPAWNER_RADIUS))) {
            if (world.getBlockState(near).isOf(net.minecraft.block.Blocks.SPAWNER)) {
                spawners++;
                world.spawnParticles(ParticleTypes.END_ROD, near.getX() + 0.5, near.getY() + 0.5, near.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.02);
            }
        }
        solver.sendMessage(Text.translatable(translationKey() + ".consecrated", spawners,
                CONSECRATED_TICKS / 20 / 60), true);
        DnDClasses.LOGGER.info("[Obstacle] {} consecrated the altar at {}: {} spawners quiet, {} healed",
                solver.getEntityName(), pos.toShortString(), spawners, healed.size());
        // XP for the first consecration only, so waiting out the 10 minutes isn't a way to level
        if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be && !be.data().getBoolean(CONSECRATED_KEY)) {
            for (BlockPos part : ObstacleGroups.group(world, pos)) {
                if (world.getBlockEntity(part) instanceof ObstacleBlockEntity partBe) {
                    partBe.data().putBoolean(CONSECRATED_KEY, true);
                    partBe.markDirty();
                }
            }
            return true;
        }
        return false;
    }

    /** Whether a consecrated altar keeps the spawner at {@code spawner} quiet. */
    public boolean suppresses(World world, BlockPos spawner) {
        return consecratedNear(world, spawner, SPAWNER_RADIUS);
    }

    /** Whether a consecrated (open) altar is within {@code radius} blocks of {@code center} (the Lich asks too). */
    public boolean consecratedNear(World world, BlockPos center, int radius) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        for (BlockPos pos : ObstacleIndex.get(serverWorld).within(center, radius)) {
            if (!world.isChunkLoaded(pos)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ObstacleBlock block && block.type() == this
                    && state.get(ObstacleBlock.STATE) == ObstacleState.OPEN) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, FAIL_WITHER_TICKS, 0));
        world.playSound(null, pos, SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.BLOCKS, 1.5f, 0.6f);
        soulFlare(world, pos, 10);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        onFailure(world, pos, player);
        RisenUndead.raise(world, pos, EntityType.ZOMBIE, FUMBLE_RISEN, player);
    }

    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        soulFlare(world, pos, 30);
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.BLOCKS, 0.8f, 0.6f);
        player.sendMessage(Text.translatable(translationKey() + ".smashed"), true);
        for (int i = 0; i < BREAK_RISEN; i++) {
            RisenUndead.raise(world, pos, i % 2 == 0 ? EntityType.ZOMBIE : EntityType.SKELETON, 1, player);
        }
    }

    private static void soulFlare(ServerWorld world, BlockPos pos, int count) {
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.3, 0.3, 0.3, 0.03);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                24, 0.4, 0.6, 0.4, 0.05);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.2f, 1.0f);
            world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        soulFlare(world, pos, 12);
        if (first) {
            world.playSound(null, pos, SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.BLOCKS, 1.5f, 0.5f);
        }
    }

    @Override
    public List<String> progressKeys() {
        return List.of(CONSECRATED_KEY);
    }
}
