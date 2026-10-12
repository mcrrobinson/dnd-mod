package mattonfire.dnd.classes.Obstacles.types;

import java.util.LinkedHashSet;
import java.util.Set;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.classes.SkillChecks.LootRuin;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Amber resin over a reagent cache or a vault. Only an Alchemist can dissolve it (Alchemist's Supplies);
 * every attempt uses up a potion.
 * <ul>
 * <li>Failure: it fizzes. Natural 1: an acid splash, {@value #ACID_DAMAGE} damage and Poison I for
 * {@value #ACID_POISON_TICKS} ticks, and an alarm.</li>
 * <li>While sealed, a container touching the resin is glued shut.</li>
 * <li>Fallback: break it (hardness {@value #HARDNESS}). The whole seal shatters, and every container
 * touching it (including diagonally) rolls its loot and loses each stack with chance
 * {@link LootRuin#RUIN_CHANCE}, exactly like a smashed locked chest.</li>
 * </ul>
 */
public final class AlchemicalSealType extends ObstacleType {
    public static final float HARDNESS = 3.0f;
    public static final float ACID_DAMAGE = 3.0f;
    public static final int ACID_POISON_TICKS = 100;
    private static final Identifier GLUE_PHASE = new Identifier(DnDClasses.MOD_ID, "alchemical_seal_glue");

    public AlchemicalSealType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.ALCHEMISTS_SUPPLIES;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.ALCHEMIST ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return true;
    }

    @Override
    public float hardness() {
        return HARDNESS;
    }

    /** An empty hand, or the potion it takes. */
    @Override
    public boolean accepts(PlayerEntity player, ItemStack held) {
        return held.isEmpty() || Reagents.isPotion(held);
    }

    @Override
    public boolean payForAttempt(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        if (!Reagents.takePotion(player)) {
            player.sendMessage(Text.translatable("obstacle.dndclasses.needs_potion"), true);
            return false;
        }
        DnDClasses.LOGGER.info("[Obstacle] {} poured a potion on the {} at {}", player.getEntityName(), id(),
                pos.toShortString());
        return true;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.6f, 1.6f);
        world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        onFailure(world, pos, player);
        world.spawnParticles(ParticleTypes.FALLING_HONEY, player.getX(), player.getY() + 1.0, player.getZ(),
                12, 0.4, 0.4, 0.4, 0.0);
        player.damage(world.getDamageSources().magic(), ACID_DAMAGE);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, ACID_POISON_TICKS, 0));
    }

    /** The broken block stays in place, open with the rest of the seal, so a reset puts the seal back whole. */
    @Override
    public boolean opensWhenBroken() {
        return true;
    }

    /** The whole seal shatters, and the cache behind it is forced: some of it is ruined. */
    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        Set<BlockPos> containers = new LinkedHashSet<>();
        for (BlockPos part : ObstacleGroups.group(world, pos)) {
            for (BlockPos near : BlockPos.iterate(part.add(-1, -1, -1), part.add(1, 1, 1))) {
                if (world.getBlockEntity(near) instanceof LootableContainerBlockEntity) {
                    containers.add(near.toImmutable());
                }
            }
        }
        ObstacleGroups.open(world, pos);
        int ruined = 0;
        for (BlockPos at : containers) {
            if (world.getBlockEntity(at) instanceof LootableContainerBlockEntity container) {
                ruined += LootRuin.forceOpen(container, player);
            }
        }
        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0f, 0.7f);
        player.sendMessage(Text.translatable(translationKey() + ".smashed", ruined), true);
        DnDClasses.LOGGER.info("[Obstacle] {} broke the alchemical seal at {}: {} container(s), {} stack(s) ruined",
                player.getEntityName(), pos.toShortString(), containers.size(), ruined);
    }

    /**
     * A container with a sealed Alchemical Seal on any side is glued shut (survival only). Checked in an early
     * phase, before lockpicking, so a Rogue can't pick a glued chest either.
     */
    @Override
    public void registerEvents() {
        UseBlockCallback.EVENT.addPhaseOrdering(GLUE_PHASE, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(GLUE_PHASE, (player, world, hand, hit) -> {
            if (world.isClient || player.isCreative() || player.isSpectator()) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!(world.getBlockEntity(pos) instanceof LootableContainerBlockEntity) || !gluedShut(world, pos)) {
                return ActionResult.PASS;
            }
            player.sendMessage(Text.translatable(translationKey() + ".glued"), true);
            world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_STEP, SoundCategory.BLOCKS, 1.0f, 0.8f);
            return ActionResult.FAIL;
        });
    }

    private boolean gluedShut(World world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState state = world.getBlockState(pos.offset(direction));
            if (ObstacleBlock.isSealed(state) && ((ObstacleBlock) state.getBlock()).type() == this) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.DRIPPING_HONEY, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.35, 0.35, 0.35, 0.0);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
            world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.8f, 1.2f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
    }
}
