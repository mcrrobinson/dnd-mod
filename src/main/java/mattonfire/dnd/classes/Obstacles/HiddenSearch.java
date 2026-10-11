package mattonfire.dnd.classes.Obstacles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.types.HiddenDoorType;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Searching a wall for hidden doors: sneak + right-click the side of any plain full block with both hands empty. Rolls
 * Investigation (the HUD shows it as "Search the wall") with no DC of its own, on a
 * {@value #COOLDOWN_TICKS}-tick cooldown, and reveals every Hidden Door within {@value #RADIUS} blocks of the
 * clicked block whose tier DC the total meets (a natural 20 finds them all, a natural 1 nothing). The roll
 * shows either way, so it doesn't give a door away.
 *
 * <p>Sneak-clicking a known obstacle is "take your time" instead, and blocks that do something when used
 * (a crafting table, a note block) are left alone.
 */
public final class HiddenSearch {
    public static final int COOLDOWN_TICKS = 100;
    public static final int RADIUS = 3;
    public static final String LABEL = "skill.dndclasses.search_wall";

    /** Full blocks that do something on a plain right-click, so sneak + right-click isn't a search on them. */
    private static final Set<Block> USABLE = Set.of(Blocks.CRAFTING_TABLE, Blocks.CARTOGRAPHY_TABLE,
            Blocks.SMITHING_TABLE, Blocks.LOOM, Blocks.NOTE_BLOCK, Blocks.RESPAWN_ANCHOR, Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.FLETCHING_TABLE, Blocks.TNT);

    /** Player UUID to the world time they may search again. */
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private HiddenSearch() {
    }

    static void register() {
        UseBlockCallback.EVENT.register(HiddenSearch::onUse);
    }

    public static void forget(UUID player) {
        NEXT.remove(player);
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND || !player.isSneaking() || player.isSpectator() || player.isCreative()
                || !player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty()) {
            return ActionResult.PASS;
        }
        // A wall: a side face, not the floor or the ceiling
        if (!hit.getSide().getAxis().isHorizontal()) {
            return ActionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!isWall(world, pos, state, player)) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        search(serverWorld, pos, serverPlayer);
        return ActionResult.SUCCESS;
    }

    /** A plain full block, or a hidden door this player doesn't know about (which looks like one). */
    private static boolean isWall(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (state.getBlock() instanceof ObstacleBlock block) {
            return block.type().hiddenFrom(world, pos, player);
        }
        return state.isFullCube(world, pos) && !state.hasBlockEntity() && !USABLE.contains(state.getBlock());
    }

    /** Rolls the search at {@code pos}. Returns the roll, or null while on cooldown. */
    public static D20.Roll search(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        long now = world.getTime();
        Long next = NEXT.get(player.getUuid());
        if (next != null && now < next) {
            player.sendMessage(Text.translatable("obstacle.dndclasses.search.wait", (next - now + 19) / 20), true);
            return null;
        }
        NEXT.put(player.getUuid(), now + COOLDOWN_TICKS);
        D20.Roll roll = SkillCheck.builder(player, Skill.INVESTIGATION, 0, "search").label(LABEL).roll();
        int found = 0;
        for (BlockPos near : ObstacleIndex.get(world).within(pos, RADIUS)) {
            BlockState state = world.getBlockState(near);
            if (!ObstacleBlock.isSealed(state)
                    || !(((ObstacleBlock) state.getBlock()).type() instanceof HiddenDoorType door)
                    || !door.hiddenFrom(world, near, player)) {
                continue;
            }
            Tier tier = world.getBlockEntity(near) instanceof ObstacleBlockEntity be ? be.tier() : Tier.MEDIUM;
            boolean spotted = roll.outcome() == D20.Outcome.CRITICAL
                    || roll.outcome() != D20.Outcome.FUMBLE && roll.total() >= tier.dc;
            DnDClasses.LOGGER.info("[Obstacle] {} searches the wall at {}: {} total {} vs DC {} -> {}",
                    player.getEntityName(), pos.toShortString(), door.id(), roll.total(), tier.dc,
                    spotted ? "found" : "missed");
            if (spotted) {
                door.reveal(world, near, player, HiddenDoorType.How.SEARCH);
                found++;
            }
        }
        if (found == 0) {
            DnDClasses.LOGGER.info("[Obstacle] {} searches the wall at {}: {} total {}, nothing found",
                    player.getEntityName(), pos.toShortString(), roll.natural(), roll.total());
        }
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 0.6f, 1.0f);
        D20.show(player, roll, Text.translatable(found > 0 ? "obstacle.dndclasses.search.found"
                : "obstacle.dndclasses.search.nothing"));
        return roll;
    }
}
