package mattonfire.dnd.dm;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.LockDcHolder;
import mattonfire.dnd.classes.SkillChecks.Lockpicking;
import mattonfire.dnd.entity.MimicEntity;
import mattonfire.dnd.magic.MagicLoot;
import mattonfire.dnd.magic.MagicTier;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.LootCommand;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The DM's treasure commands:
 *
 * <pre>
 * /dm loot give &lt;players&gt; &lt;loot table&gt;             roll a table straight into their inventories
 * /dm loot give &lt;players&gt; magic &lt;rarity&gt;           one roll on the magic item table of that rarity
 * /dm loot plant &lt;loot table&gt; [&lt;pos&gt;] [dc &lt;n&gt;]    (op only) an unrolled chest at the block you look at
 * /dm loot plant mimic [&lt;loot table&gt;] [&lt;pos&gt;]      (op only) a dormant Mimic holding that loot
 * </pre>
 *
 * A planted chest with a {@code chests/...} table is locked for {@link Lockpicking} like any dungeon chest; giving a
 * {@code dc} sets its lock DC (stored as {@link LockDcHolder#NBT_KEY}) and locks it whatever the table.
 */
final class DmLoot {
    public static final int RAYCAST = 64;
    public static final Identifier DEFAULT_MIMIC_LOOT = new Identifier("minecraft", "chests/simple_dungeon");

    private static final DynamicCommandExceptionType UNKNOWN_TABLE = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown loot table " + id));
    private static final DynamicCommandExceptionType UNKNOWN_RARITY = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown rarity " + id + " (uncommon, rare, very_rare or legendary)"));
    private static final SimpleCommandExceptionType NO_TARGET_BLOCK = new SimpleCommandExceptionType(
            Text.literal("Look at a block within " + RAYCAST + " blocks, or give a position"));
    private static final DynamicCommandExceptionType NOT_FREE = new DynamicCommandExceptionType(
            pos -> Text.literal("No room for a chest at " + pos));

    private DmLoot() {
    }

    static LiteralArgumentBuilder<ServerCommandSource> loot() {
        return CommandManager.literal("loot")
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .then(CommandManager.literal("magic")
                                        .then(CommandManager.argument("rarity", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        magicTiers().stream().map(tier -> tier.id), builder))
                                                .executes(DmLoot::giveMagic)))
                                .then(CommandManager.argument("table", IdentifierArgumentType.identifier())
                                        .suggests(LootCommand.SUGGESTION_PROVIDER)
                                        .executes(DmLoot::give))))
                .then(CommandManager.literal("plant").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("mimic")
                                .executes(context -> plantMimic(context, DEFAULT_MIMIC_LOOT, null))
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(context -> plantMimic(context, DEFAULT_MIMIC_LOOT,
                                                BlockPosArgumentType.getBlockPos(context, "pos"))))
                                .then(CommandManager.argument("table", IdentifierArgumentType.identifier())
                                        .suggests(LootCommand.SUGGESTION_PROVIDER)
                                        .executes(context -> plantMimic(context, table(context), null))
                                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(context -> plantMimic(context, table(context),
                                                        BlockPosArgumentType.getBlockPos(context, "pos"))))))
                        .then(CommandManager.argument("table", IdentifierArgumentType.identifier())
                                .suggests(LootCommand.SUGGESTION_PROVIDER)
                                .executes(context -> plant(context, null, 0))
                                .then(CommandManager.literal("dc")
                                        .then(CommandManager.argument("dc", IntegerArgumentType.integer(1, 30))
                                                .executes(context -> plant(context, null,
                                                        IntegerArgumentType.getInteger(context, "dc")))))
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(context -> plant(context,
                                                BlockPosArgumentType.getBlockPos(context, "pos"), 0))
                                        .then(CommandManager.literal("dc")
                                                .then(CommandManager.argument("dc", IntegerArgumentType.integer(1, 30))
                                                        .executes(context -> plant(context,
                                                                BlockPosArgumentType.getBlockPos(context, "pos"),
                                                                IntegerArgumentType.getInteger(context, "dc"))))))));
    }

    /** The tiers that have a {@code dndclasses:magic/<tier>} table (no Common table yet). */
    static List<MagicTier> magicTiers() {
        return List.of(MagicTier.UNCOMMON, MagicTier.RARE, MagicTier.VERY_RARE, MagicTier.LEGENDARY);
    }

    private static Identifier table(CommandContext<ServerCommandSource> context) {
        return IdentifierArgumentType.getIdentifier(context, "table");
    }

    private static LootTable lootTable(ServerCommandSource source, Identifier id) throws CommandSyntaxException {
        if (!source.getServer().getLootManager().getTableIds().contains(id)) {
            throw UNKNOWN_TABLE.create(id);
        }
        return source.getServer().getLootManager().getTable(id);
    }

    // ------------------------------------------------------------ give

    private static int give(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier id = table(context);
        return give(context.getSource(), EntityArgumentType.getPlayers(context, "players"), id, id.toString());
    }

    private static int giveMagic(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String rarity = StringArgumentType.getString(context, "rarity");
        MagicTier tier = MagicTier.byId(rarity);
        if (tier == null || !magicTiers().contains(tier)) {
            throw UNKNOWN_RARITY.create(rarity);
        }
        return give(context.getSource(), EntityArgumentType.getPlayers(context, "players"),
                MagicLoot.tierTable(tier), "a " + tier.id + " magic item");
    }

    private static int give(ServerCommandSource source, Collection<ServerPlayerEntity> players, Identifier id,
            String what) throws CommandSyntaxException {
        LootTable table = lootTable(source, id);
        int total = 0;
        for (ServerPlayerEntity player : players) {
            List<ItemStack> stacks = table.generateLoot(new LootContext.Builder(player.getWorld())
                    .parameter(LootContextParameters.ORIGIN, player.getPos())
                    .parameter(LootContextParameters.THIS_ENTITY, player)
                    .luck(player.getLuck())
                    .random(player.getRandom())
                    .build(LootContextTypes.CHEST));
            StringBuilder names = new StringBuilder();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    continue;
                }
                if (!names.isEmpty()) {
                    names.append(", ");
                }
                names.append(stack.getCount()).append(" ").append(stack.getName().getString());
                total++;
                player.getInventory().offerOrDrop(stack);
            }
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP,
                    SoundCategory.PLAYERS, 0.4F, 1.2F);
            DnDClasses.LOGGER.info("[DM] loot give {} from {}: {}", player.getEntityName(), id, names);
            source.sendFeedback(Text.literal("Gave " + player.getEntityName() + " " + what + ": "
                    + (names.isEmpty() ? "nothing (the roll came up empty)" : names)).formatted(Formatting.GOLD),
                    true);
        }
        return total;
    }

    // ------------------------------------------------------------ plant

    private static BlockPos where(ServerCommandSource source, BlockPos pos) throws CommandSyntaxException {
        if (pos != null) {
            return pos;
        }
        Entity entity = source.getEntity();
        if (entity == null) {
            throw NO_TARGET_BLOCK.create();
        }
        HitResult hit = entity.raycast(RAYCAST, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            throw NO_TARGET_BLOCK.create();
        }
        return blockHit.getBlockPos().offset(blockHit.getSide());
    }

    /** Faces the chest (or mimic) towards whoever planted it. */
    private static Direction facing(ServerCommandSource source) {
        Entity entity = source.getEntity();
        return entity == null ? Direction.NORTH : entity.getHorizontalFacing().getOpposite();
    }

    private static int plant(CommandContext<ServerCommandSource> context, BlockPos at, int dc)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Identifier id = table(context);
        lootTable(source, id);
        ServerWorld world = source.getWorld();
        BlockPos pos = where(source, at);
        if (!world.getBlockState(pos).isReplaceable()) {
            throw NOT_FREE.create(pos.toShortString());
        }
        world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing(source)));
        if (!(world.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            throw NOT_FREE.create(pos.toShortString());
        }
        chest.setLootTable(id, world.random.nextLong());
        if (dc > 0) {
            ((LockDcHolder) chest).dndclasses$setLockDc(dc);
        }
        boolean locked = Lockpicking.isLocked(world, pos);
        String lock = locked ? "locked, DC " + Lockpicking.dcAt(world, pos, null, id) : "not locked";
        DnDClasses.LOGGER.info("[DM] loot plant {} at {} ({})", id, pos.toShortString(), lock);
        source.sendFeedback(Text.literal("Planted a chest of " + id + " at " + pos.toShortString() + " (" + lock
                + ")").formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int plantMimic(CommandContext<ServerCommandSource> context, Identifier id, BlockPos at)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        lootTable(source, id);
        ServerWorld world = source.getWorld();
        BlockPos pos = where(source, at);
        if (!world.getBlockState(pos).isReplaceable()) {
            throw NOT_FREE.create(pos.toShortString());
        }
        MimicEntity mimic = MimicEntity.disguised(world, pos, facing(source), id);
        if (mimic == null || !world.spawnEntity(mimic)) {
            throw NOT_FREE.create(pos.toShortString());
        }
        DnDClasses.LOGGER.info("[DM] loot plant mimic {} at {} with {}", mimic.getUuidAsString(), pos.toShortString(),
                id);
        source.sendFeedback(Text.literal("Planted a dormant Mimic (" + id + ") at " + pos.toShortString())
                .formatted(Formatting.GOLD), true);
        return 1;
    }
}
