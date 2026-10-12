package mattonfire.dnd.classes.Obstacles.types;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShearsItem;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A wall of thorned vines. Only a Druid can part it, with a Nature check: the vines draw back for
 * {@value #PART_TICKS} ticks and then regrow. Everyone else has two ways through:
 * <ul>
 * <li><b>Burn it:</b> flint and steel or a fire charge on the wall, or any fire lit beside it (a fireball, fire
 * breath, lava). It burns for {@value #BURN_SECONDS} seconds, setting fire round it (which spreads to nearby
 * wooden props) and burning whoever stands in it, then it's gone until the obstacle is reset.</li>
 * <li><b>Cut it:</b> shears or an axe cut one block in {@value #CUT_TICKS} ticks, for {@value #CUT_DAMAGE} thorns
 * damage per block. Cut blocks stay cut until the wall regrows or is reset.</li>
 * </ul>
 */
public final class ThornwallType extends ObstacleType {
    public static final int PART_TICKS = 60 * 20;
    public static final int BURN_SECONDS = 20;
    public static final int CUT_TICKS = 40;
    public static final float CUT_DAMAGE = 1.0f;
    public static final float STING_DAMAGE = 2.0f;
    public static final float FUMBLE_DAMAGE = 4.0f;
    public static final int FUMBLE_SLOWNESS_TICKS = 60;
    /** Chance per block per second of setting the air beside it alight while it burns. */
    public static final float SPREAD_CHANCE = 0.3f;
    public static final int BURNING_ENTITY_SECONDS = 4;
    /** Seconds of burning left, on every block of a burning wall. */
    public static final String BURN_KEY = "BurnLeft";

    public ThornwallType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.NATURE;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.DRUID ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    /** Cut by hand (shears or an axe); see {@link #breakingDelta}. */
    @Override
    public boolean breakable() {
        return true;
    }

    @Override
    public float breakingDelta(PlayerEntity player, float vanilla) {
        ItemStack held = player.getMainHandStack();
        return held.getItem() instanceof ShearsItem || held.getItem() instanceof AxeItem ? 1.0f / CUT_TICKS : 0.0f;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        return ObstacleGroups.openFor(world, pos, PART_TICKS) > 0;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().sweetBerryBush(), STING_DAMAGE);
        world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 0.6f);
    }

    /** A natural 1: the vines lash and snag you. No alarm: it's only a hedge. */
    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().sweetBerryBush(), FUMBLE_DAMAGE);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FUMBLE_SLOWNESS_TICKS, 1));
        world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 0.4f);
    }

    @Override
    public boolean alarmsOnFumble() {
        return false;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        leaves(world, pos, 8);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.BLOCKS, 1.2f, 0.7f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        leaves(world, pos, 6);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, SoundCategory.BLOCKS, 1.2f, 0.6f);
        }
    }

    private static void leaves(ServerWorld world, BlockPos pos, int count) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, count, 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public List<String> progressKeys() {
        return List.of(BURN_KEY);
    }

    // ---- Burning ----

    @Override
    public void neighborChanged(ServerWorld world, BlockPos pos, BlockPos from) {
        BlockState there = world.getBlockState(from);
        if (there.isIn(BlockTags.FIRE) || there.isOf(Blocks.LAVA)) {
            ignite(world, pos, null);
        }
    }

    /** Sets the wall at {@code pos} burning, if it's sealed and not burning already. Returns whether it caught. */
    public boolean ignite(ServerWorld world, BlockPos pos, @Nullable PlayerEntity by) {
        if (!ObstacleBlock.isSealed(world.getBlockState(pos)) || ObstacleGroups.dataInt(world, pos, BURN_KEY) > 0) {
            return false;
        }
        ObstacleGroups.setDataInt(world, pos, BURN_KEY, BURN_SECONDS);
        world.scheduleBlockTick(pos, world.getBlockState(pos).getBlock(), 20);
        world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        if (by != null) {
            by.sendMessage(Text.translatable(translationKey() + ".ignited", BURN_SECONDS), true);
        }
        DnDClasses.LOGGER.info("[Obstacle] thornwall at {} caught fire{}", pos.toShortString(),
                by == null ? "" : " (" + by.getEntityName() + ")");
        return true;
    }

    /** Once a second while it burns: flames, fire round it, burning whoever's in it; then it's gone. */
    @Override
    public void scheduledTick(ServerWorld world, BlockPos pos, BlockState state) {
        int left = ObstacleGroups.dataInt(world, pos, BURN_KEY);
        if (left <= 0) {
            return;
        }
        List<BlockPos> group = ObstacleGroups.group(world, pos);
        if (state.get(ObstacleBlock.STATE) != ObstacleState.SEALED) {
            // This block was cut or the wall parted: burn on from a block still standing, or stop
            for (BlockPos block : group) {
                if (ObstacleBlock.isSealed(world.getBlockState(block))) {
                    world.scheduleBlockTick(block, state.getBlock(), 1);
                    return;
                }
            }
            ObstacleGroups.setDataInt(world, pos, BURN_KEY, 0);
            return;
        }
        Box area = null;
        for (BlockPos block : group) {
            world.spawnParticles(ParticleTypes.FLAME, block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5, 6,
                    0.4, 0.4, 0.4, 0.01);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, block.getX() + 0.5, block.getY() + 0.8,
                    block.getZ() + 0.5, 1, 0.3, 0.2, 0.3, 0.01);
            for (Direction direction : Direction.values()) {
                BlockPos side = block.offset(direction);
                if (world.getRandom().nextFloat() < SPREAD_CHANCE / 6 && world.isAir(side)) {
                    BlockState fire = AbstractFireBlock.getState(world, side);
                    if (fire.canPlaceAt(world, side)) {
                        world.setBlockState(side, fire, Block.NOTIFY_ALL);
                    }
                }
            }
            Box box = new Box(block).expand(0.5);
            area = area == null ? box : area.union(box);
        }
        if (area != null) {
            for (Entity entity : world.getEntitiesByClass(LivingEntity.class, area, Entity::isAlive)) {
                entity.setOnFireFor(BURNING_ENTITY_SECONDS);
            }
        }
        if (left % 5 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.5f, 0.8f);
        }
        left--;
        if (left > 0) {
            ObstacleGroups.setDataInt(world, pos, BURN_KEY, left);
            world.scheduleBlockTick(pos, state.getBlock(), 20);
            return;
        }
        // Burnt away: open for good (the fallback: no class XP) until the obstacle is reset
        ObstacleGroups.setDataInt(world, pos, BURN_KEY, 0);
        ObstacleGroups.setResealTicks(world, pos, 0);
        ObstacleGroups.open(world, pos);
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.8f);
        DnDClasses.LOGGER.info("[Obstacle] thornwall at {} burnt away", pos.toShortString());
    }

    // ---- Cutting ----

    /** Cuts one sealed block open: it stays cut (no regrow timer of its own). */
    private void cut(ServerWorld world, BlockPos pos, BlockState state, PlayerEntity player) {
        world.setBlockState(pos, state.with(ObstacleBlock.STATE, ObstacleState.OPEN), Block.NOTIFY_ALL);
        if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be) {
            be.setResealTicks(0);
        }
        player.damage(world.getDamageSources().sweetBerryBush(), CUT_DAMAGE);
        player.getMainHandStack().damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
        leaves(world, pos, 12);
        world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0f, 0.8f);
        DnDClasses.LOGGER.info("[Obstacle] {} cut thornwall at {}: health {}", player.getEntityName(),
                pos.toShortString(), player.getHealth());
    }

    @Override
    public void registerEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld) || player.isCreative()
                    || !(state.getBlock() instanceof ObstacleBlock block) || block.type() != this
                    || state.get(ObstacleBlock.STATE) != ObstacleState.SEALED) {
                return true;
            }
            cut(serverWorld, pos, state, player);
            return false;
        });
        UseBlockCallback.EVENT.register(this::useFire);
    }

    /** Flint and steel or a fire charge on the wall sets it burning. */
    private ActionResult useFire(PlayerEntity player, World world, Hand hand,
            net.minecraft.util.hit.BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);
        if (player.isSpectator() || !held.isOf(Items.FLINT_AND_STEEL) && !held.isOf(Items.FIRE_CHARGE)) {
            return ActionResult.PASS;
        }
        BlockState state = world.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof ObstacleBlock block) || block.type() != this
                || state.get(ObstacleBlock.STATE) != ObstacleState.SEALED) {
            return ActionResult.PASS;
        }
        if (world instanceof ServerWorld serverWorld && ignite(serverWorld, hit.getBlockPos(), player)
                && !player.isCreative()) {
            if (held.isOf(Items.FIRE_CHARGE)) {
                held.decrement(1);
            } else {
                held.damage(1, player, p -> p.sendToolBreakStatus(hand));
            }
        }
        return ActionResult.SUCCESS;
    }
}
