package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dm.encounter.Encounter;
import mattonfire.dnd.dm.encounter.EncounterSpawner;
import mattonfire.dnd.dm.encounter.Encounters;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * The Dungeon Master commands. Ops (permission level 2) can run all of them; players given {@code /dm grant} can
 * run everything except {@code grant} and {@code revoke}.
 *
 * <pre>
 * /dm on | off                                   DM mode for yourself (off also lifts the veil)
 * /dm status                                     who is DM, veiled or granted
 * /dm grant | revoke &lt;player&gt;                    (op only) let a non-op use /dm
 * /dm veil [on|off]                              hide from non-DM players (turns DM mode on)
 * /dm encounter list                             encounter files and live encounters
 * /dm encounter spawn &lt;id&gt; [&lt;pos&gt;] [frozen]       at the block you look at (64 blocks), or pos
 * /dm encounter clear &lt;n&gt; | all                  remove an encounter's mobs, no drops
 * /dm freeze &lt;targets&gt; | radius [r]              pause mobs and players (default r 24)
 * /dm unfreeze &lt;targets&gt; | radius [r] | all
 * </pre>
 */
public final class DmCommand {
    public static final String ENCOUNTER_TAG = "dndclasses.dm_encounter";
    public static final String ENCOUNTER_TAG_PREFIX = "dndclasses.enc.";
    private static final int RAYCAST = 64;
    private static final int DEFAULT_RADIUS = 24;

    private static final DynamicCommandExceptionType UNKNOWN_ENCOUNTER = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown encounter " + id));
    private static final DynamicCommandExceptionType NO_SUCH_LIVE = new DynamicCommandExceptionType(
            n -> Text.literal("No encounter #" + n));
    private static final SimpleCommandExceptionType NO_TARGET_BLOCK = new SimpleCommandExceptionType(
            Text.literal("Look at a block within " + RAYCAST + " blocks, or give a position"));

    private DmCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dm")
                .requires(DungeonMaster::canUse)
                .then(CommandManager.literal("on").executes(context -> setDm(context, true)))
                .then(CommandManager.literal("off").executes(context -> setDm(context, false)))
                .then(CommandManager.literal("status").executes(DmCommand::status))
                .then(CommandManager.literal("grant").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.players())
                                .executes(context -> grant(context, true))))
                .then(CommandManager.literal("revoke").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.players())
                                .executes(context -> grant(context, false))))
                .then(CommandManager.literal("veil")
                        .executes(context -> veil(context, null))
                        .then(CommandManager.literal("on").executes(context -> veil(context, true)))
                        .then(CommandManager.literal("off").executes(context -> veil(context, false))))
                .then(CommandManager.literal("encounter")
                        .then(CommandManager.literal("list").executes(DmCommand::listEncounters))
                        .then(CommandManager.literal("spawn")
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Encounters.all().stream().map(e -> shortId(e.id())), builder))
                                        .executes(context -> spawn(context, null, false))
                                        .then(CommandManager.literal("frozen")
                                                .executes(context -> spawn(context, null, true)))
                                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(context -> spawn(context,
                                                        BlockPosArgumentType.getBlockPos(context, "pos"), false))
                                                .then(CommandManager.literal("frozen")
                                                        .executes(context -> spawn(context,
                                                                BlockPosArgumentType.getBlockPos(context, "pos"),
                                                                true))))))
                        .then(CommandManager.literal("clear")
                                .then(CommandManager.literal("all").executes(context -> clear(context, -1)))
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(1))
                                        .executes(context -> clear(context,
                                                IntegerArgumentType.getInteger(context, "n"))))))
                .then(CommandManager.literal("freeze")
                        .then(CommandManager.literal("radius")
                                .executes(context -> freezeRadius(context, DEFAULT_RADIUS, true))
                                .then(CommandManager.argument("r", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> freezeRadius(context,
                                                IntegerArgumentType.getInteger(context, "r"), true))))
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .executes(context -> freezeTargets(context, true))))
                .then(CommandManager.literal("unfreeze")
                        .then(CommandManager.literal("all").executes(DmCommand::unfreezeAll))
                        .then(CommandManager.literal("radius")
                                .executes(context -> freezeRadius(context, DEFAULT_RADIUS, false))
                                .then(CommandManager.argument("r", IntegerArgumentType.integer(1, 256))
                                        .executes(context -> freezeRadius(context,
                                                IntegerArgumentType.getInteger(context, "r"), false))))
                        .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .executes(context -> freezeTargets(context, false)))));
    }

    private static String shortId(Identifier id) {
        return id.getNamespace().equals(DnDClasses.MOD_ID) ? id.getPath() : id.toString();
    }

    // ------------------------------------------------------------ mode

    private static int setDm(CommandContext<ServerCommandSource> context, boolean on) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        DungeonMaster state = DungeonMaster.get(player.getServer());
        state.setDm(player, on);
        DmVeil.apply(player);
        if (on) {
            // A DM isn't frozen by the scene they're running.
            DmFreeze.unfreeze(player);
        }
        context.getSource().sendFeedback(Text.literal(on
                ? "You are the Dungeon Master. Mobs ignore you; you earn no XP or credit."
                : "You are no longer the Dungeon Master.").formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int veil(CommandContext<ServerCommandSource> context, Boolean on) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        DungeonMaster state = DungeonMaster.get(player.getServer());
        boolean veiled = on != null ? on : !state.isVeiled(player.getUuid());
        state.setVeiled(player, veiled);
        if (veiled) {
            DmFreeze.unfreeze(player);
        }
        DmVeil.apply(player);
        context.getSource().sendFeedback(Text.literal(veiled
                ? "You draw the veil: players can't see you, mobs can't target you."
                : "You step out from behind the veil.").formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int grant(CommandContext<ServerCommandSource> context, boolean on) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "player");
        ServerCommandSource source = context.getSource();
        DungeonMaster state = DungeonMaster.get(source.getServer());
        for (ServerPlayerEntity player : players) {
            state.setGranted(player.getUuid(), on);
            if (!on && !player.hasPermissionLevel(2)) {
                state.setDm(player, false);
                DmVeil.apply(player);
            }
            source.getServer().getPlayerManager().sendCommandTree(player);
            source.sendFeedback(Text.literal((on ? "Granted /dm to " : "Revoked /dm from ")
                    + player.getEntityName()), true);
        }
        return players.size();
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        DungeonMaster state = DungeonMaster.get(server);
        context.getSource().sendFeedback(Text.literal("DMs: " + names(server, state.getDms())
                + " | veiled: " + names(server, state.getVeiled())
                + " | granted: " + names(server, state.getGranted())), false);
        return state.getDms().size();
    }

    private static String names(MinecraftServer server, Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>();
        for (UUID uuid : uuids) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            names.add(player != null ? player.getEntityName()
                    : server.getUserCache().getByUuid(uuid).map(p -> p.getName()).orElse(uuid.toString()));
        }
        return String.join(", ", names);
    }

    // ------------------------------------------------------------ encounters

    private static int spawn(CommandContext<ServerCommandSource> context, BlockPos pos, boolean frozen)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        Encounter encounter = Encounters.get(id);
        if (encounter == null) {
            throw UNKNOWN_ENCOUNTER.create(id);
        }
        if (pos == null) {
            pos = lookedAtBlock(source);
        }
        ServerWorld world = source.getWorld();
        DungeonMaster state = DungeonMaster.get(source.getServer());
        int number = state.nextEncounterNumber();
        EncounterSpawner.Result result = EncounterSpawner.spawn(world, encounter, pos,
                List.of(ENCOUNTER_TAG, ENCOUNTER_TAG_PREFIX + number));
        List<UUID> uuids = new ArrayList<>();
        for (Entity entity : result.entities()) {
            uuids.add(entity.getUuid());
            if (frozen) {
                DmFreeze.freeze(entity);
            }
        }
        state.addEncounter(new DungeonMaster.EncounterRecord(number, encounter.id(), encounter.name(),
                world.getRegistryKey().getValue(), pos, uuids));
        int mobs = result.entities().size();
        String chests = result.chests().isEmpty() ? "" : ", " + result.chests().size() + " chests";
        source.sendFeedback(Text.literal("Encounter #" + number + " \"" + encounter.name() + "\": " + mobs
                + (mobs == 1 ? " mob" : " mobs") + chests + (frozen ? " (frozen)" : "") + " at "
                + pos.toShortString()).formatted(Formatting.GOLD), true);
        return number;
    }

    private static BlockPos lookedAtBlock(ServerCommandSource source) throws CommandSyntaxException {
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

    private static int listEncounters(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<String> ids = new ArrayList<>();
        for (Encounter encounter : Encounters.all()) {
            ids.add(shortId(encounter.id()) + " (" + encounter.sizeLabel() + ")");
        }
        source.sendFeedback(Text.literal("Encounters: " + (ids.isEmpty() ? "none" : String.join(", ", ids)))
                .formatted(Formatting.GOLD), false);
        DungeonMaster state = DungeonMaster.get(source.getServer());
        List<DungeonMaster.EncounterRecord> live = state.getEncounters();
        if (live.isEmpty()) {
            source.sendFeedback(Text.literal("No live encounters"), false);
        }
        for (DungeonMaster.EncounterRecord record : live) {
            int alive = tagged(source.getServer(), ENCOUNTER_TAG_PREFIX + record.number()).size();
            source.sendFeedback(Text.literal("#" + record.number() + " \"" + record.name() + "\" at "
                    + record.pos().toShortString() + " in " + record.dimension() + ": " + alive + "/"
                    + record.entities().size() + " left (loaded)"), false);
        }
        return live.size();
    }

    private static int clear(CommandContext<ServerCommandSource> context, int number) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        DungeonMaster state = DungeonMaster.get(source.getServer());
        String tag;
        if (number < 0) {
            tag = ENCOUNTER_TAG;
            state.clearEncounters();
        } else {
            if (state.removeEncounter(number) == null && tagged(source.getServer(), ENCOUNTER_TAG_PREFIX + number).isEmpty()) {
                throw NO_SUCH_LIVE.create(number);
            }
            tag = ENCOUNTER_TAG_PREFIX + number;
        }
        List<Entity> entities = tagged(source.getServer(), tag);
        for (Entity entity : entities) {
            if (entity.world instanceof ServerWorld world) {
                world.spawnParticles(net.minecraft.particle.ParticleTypes.POOF, entity.getX(), entity.getBodyY(0.5D),
                        entity.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
            }
            // discard(): gone without dying, so nothing drops.
            entity.discard();
        }
        source.sendFeedback(Text.literal("Cleared " + (number < 0 ? "all encounters" : "encounter #" + number)
                + ": " + entities.size() + " removed").formatted(Formatting.GOLD), true);
        return entities.size();
    }

    /** Every loaded entity with {@code tag}, in all dimensions. */
    private static List<Entity> tagged(MinecraftServer server, String tag) {
        List<Entity> result = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity.getCommandTags().contains(tag)) {
                    result.add(entity);
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------ freeze

    private static int freezeTargets(CommandContext<ServerCommandSource> context, boolean freeze)
            throws CommandSyntaxException {
        int changed = 0;
        for (Entity entity : EntityArgumentType.getEntities(context, "targets")) {
            if (freeze ? DmFreeze.freeze(entity) : DmFreeze.unfreeze(entity)) {
                changed++;
            }
        }
        return report(context.getSource(), freeze, changed);
    }

    private static int freezeRadius(CommandContext<ServerCommandSource> context, int radius, boolean freeze) {
        ServerCommandSource source = context.getSource();
        Box box = Box.from(source.getPosition()).expand(radius);
        int changed = 0;
        for (Entity entity : source.getWorld().getOtherEntities(null, box,
                e -> DmFreeze.freezable(e) && e.squaredDistanceTo(source.getPosition()) <= radius * radius)) {
            if (freeze ? DmFreeze.freeze(entity) : DmFreeze.unfreeze(entity)) {
                changed++;
            }
        }
        return report(source, freeze, changed);
    }

    private static int unfreezeAll(CommandContext<ServerCommandSource> context) {
        int changed = 0;
        for (Entity entity : tagged(context.getSource().getServer(), DmFreeze.TAG)) {
            if (DmFreeze.unfreeze(entity)) {
                changed++;
            }
        }
        return report(context.getSource(), false, changed);
    }

    private static int report(ServerCommandSource source, boolean freeze, int changed) {
        source.sendFeedback(Text.literal((freeze ? "Froze " : "Unfroze ") + changed
                + (changed == 1 ? " creature" : " creatures")).formatted(Formatting.AQUA), true);
        return changed;
    }
}
