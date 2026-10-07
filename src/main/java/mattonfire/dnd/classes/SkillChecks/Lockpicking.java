package mattonfire.dnd.classes.SkillChecks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.mixin.LootableContainerBlockEntityAccessor;
import mattonfire.dnd.entity.MountainDwarfEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Dungeon and lair loot chests start locked: any chest still holding an unrolled {@code chests/...}
 * loot table (except village, hobbit and bonus chests). Only a Rogue can open one, by picking the lock
 * (d20 + 5 against DC 10, or DC 15 for the richest hoards). Anyone else, a Rogue included, can break
 * the chest open instead, but smashing the lock ruins some of the loot inside.
 *
 * A picked lock rolls the loot, so the chest is unlocked for good; it swings open a moment later, once
 * the roll has shown on the HUD.
 */
public final class Lockpicking {
    /** Rogue: Dexterity +3 and expertise with thieves' tools. */
    public static final int ROGUE_MODIFIER = 5;
    public static final int DC = 10;
    public static final int HARD_DC = 15;
    /** Chance that each stack in a locked chest is ruined when it's broken open. */
    public static final float RUIN_CHANCE = 0.4f;
    private static final int FAIL_COOLDOWN_TICKS = 30;
    private static final int FUMBLE_COOLDOWN_TICKS = 100;
    /** Long enough for the HUD's die to land and the result to be read. */
    private static final int OPEN_DELAY_TICKS = 26;

    private record PendingOpen(ServerPlayerEntity player, BlockPos pos, long openAt) {
    }

    private static final List<PendingOpen> PENDING_OPEN = new ArrayList<>();

    /** The best-guarded hoards. */
    private static final Set<String> HARD_TABLES = Set.of(
            "dndclasses:chests/dragon_lair",
            "dndclasses:chests/dwarven_fortress_treasury",
            "minecraft:chests/end_city_treasure",
            "minecraft:chests/bastion_treasure",
            "minecraft:chests/ancient_city",
            "minecraft:chests/woodland_mansion",
            "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing",
            "minecraft:chests/stronghold_library");

    /** Player UUID to the world time their hands are steady enough to try again. */
    private static final Map<UUID, Long> RETRY_AT = new HashMap<>();

    private Lockpicking() {
    }

    static void register() {
        UseBlockCallback.EVENT.register(Lockpicking::onUse);
        PlayerBlockBreakEvents.BEFORE.register(Lockpicking::onBreak);
        ServerTickEvents.END_SERVER_TICK.register(server -> PENDING_OPEN.removeIf(Lockpicking::tryOpen));
    }

    /** Opens a picked chest for its Rogue once the roll has shown. Returns true when done with it. */
    private static boolean tryOpen(PendingOpen pending) {
        ServerPlayerEntity player = pending.player();
        World world = player.getWorld();
        if (player.isRemoved() || !player.isAlive()) {
            return true;
        }
        if (world.getTime() < pending.openAt()) {
            return false;
        }
        BlockState state = world.getBlockState(pending.pos());
        if (player.currentScreenHandler != player.playerScreenHandler
                || !(state.getBlock() instanceof ChestBlock)
                || player.squaredDistanceTo(Vec3d.ofCenter(pending.pos())) > 64.0) {
            return true;
        }
        NamedScreenHandlerFactory factory = state.createScreenHandlerFactory(world, pending.pos());
        if (factory != null) {
            player.openHandledScreen(factory);
            player.incrementStat(Stats.CUSTOM.getOrCreateStat(Stats.OPEN_CHEST));
            PiglinBrain.onGuardedBlockInteracted(player, true);
            MountainDwarfEntity.witness(player, pending.pos());
        }
        return true;
    }

    /** Whether this loot table belongs to a locked chest. */
    public static boolean isLockedTable(Identifier table) {
        String path = table.getPath();
        return path.startsWith("chests/")
                && !path.contains("village")
                && !path.startsWith("chests/hobbit_")
                && !path.equals("chests/spawn_bonus_chest");
    }

    public static int dcFor(Identifier table) {
        return HARD_TABLES.contains(table.toString()) ? HARD_DC : DC;
    }

    /** The locked loot table of the chest at pos (null if it isn't a locked chest). */
    private static Identifier lockedTableAt(World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            Identifier table = ((LootableContainerBlockEntityAccessor) chest).dndclasses$getLootTableId();
            if (table != null && isLockedTable(table)) {
                return table;
            }
        }
        return null;
    }

    /** The other half of a double chest, or null. */
    private static BlockPos otherHalf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            return pos.offset(ChestBlock.getFacing(state));
        }
        return null;
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient || player.isSpectator() || player.isCreative()) {
            return ActionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return ActionResult.PASS;
        }
        // Sneaking with something in hand places it instead of opening the chest
        if (player.shouldCancelInteraction()
                && !(player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty())) {
            return ActionResult.PASS;
        }
        Identifier table = lockedTableAt(world, pos);
        BlockPos other = otherHalf(state, pos);
        if (table == null && other != null) {
            table = lockedTableAt(world, other);
        }
        if (table == null) {
            return ActionResult.PASS;
        }
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.FAIL;
        }

        if (D20.classOf(player) != DndCharacter.ROGUE) {
            player.sendMessage(Text.translatable("skill.dndclasses.lockpicking.locked"), true);
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return ActionResult.FAIL;
        }

        long now = world.getTime();
        Long retryAt = RETRY_AT.get(player.getUuid());
        if (retryAt != null && now < retryAt) {
            player.sendMessage(Text.translatable("skill.dndclasses.lockpicking.steady"), true);
            return ActionResult.FAIL;
        }

        D20.Roll roll = D20.check(player, D20.Skill.LOCKPICKING, ROGUE_MODIFIER, dcFor(table));
        if (roll.outcome().succeeded()) {
            RETRY_AT.remove(player.getUuid());
            D20.show(player, roll, Text.translatable(roll.outcome() == D20.Outcome.CRITICAL
                    ? "skill.dndclasses.lockpicking.critical"
                    : "skill.dndclasses.lockpicking.success"));
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.6f, 1.6f);
            // Roll the loot now, which unlocks both halves for good. The chest swings open once the
            // player has seen the roll (an open screen would hide it).
            unlock(world, pos, player);
            if (other != null) {
                unlock(world, other, player);
            }
            PENDING_OPEN.add(new PendingOpen((ServerPlayerEntity) player, pos, now + OPEN_DELAY_TICKS));
            return ActionResult.SUCCESS;
        }

        boolean fumble = roll.outcome() == D20.Outcome.FUMBLE;
        RETRY_AT.put(player.getUuid(), now + (fumble ? FUMBLE_COOLDOWN_TICKS : FAIL_COOLDOWN_TICKS));
        D20.show(player, roll, Text.translatable(fumble
                ? "skill.dndclasses.lockpicking.fumble"
                : "skill.dndclasses.lockpicking.failure"));
        world.playSound(null, pos, fumble ? SoundEvents.ENTITY_ITEM_BREAK : SoundEvents.BLOCK_CHEST_LOCKED,
                SoundCategory.BLOCKS, 0.8f, fumble ? 1.4f : 1.0f);
        return ActionResult.FAIL;
    }

    private static void unlock(World world, BlockPos pos, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.checkLootInteraction(player);
        }
    }

    /** Breaking a locked chest open rolls its loot, then ruins some of it. */
    private static boolean onBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
            BlockEntity blockEntity) {
        if (world.isClient || player.isCreative() || !(blockEntity instanceof ChestBlockEntity chest)
                || lockedTableAt(world, pos) == null) {
            return true;
        }
        chest.checkLootInteraction(player);
        int ruined = 0;
        for (int i = 0; i < chest.size(); i++) {
            ItemStack stack = chest.getStack(i);
            if (!stack.isEmpty() && world.random.nextFloat() < RUIN_CHANCE) {
                chest.setStack(i, ItemStack.EMPTY);
                ruined++;
            }
        }
        player.sendMessage(Text.translatable("skill.dndclasses.lockpicking.smashed", ruined), true);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
        return true;
    }
}
