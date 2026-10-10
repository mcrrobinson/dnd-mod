package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.world.gen.temple.DragonTempleStructures;
import mattonfire.dnd.world.gen.temple.SanctumPiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;
import mattonfire.dnd.entity.DragonbornTrades;
import mattonfire.dnd.world.gen.temple.DragonTempleStructure;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructurePiece;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * /dndhome [player]: which racial home the player stands in, the piece under their feet, and whether
 * it's their own home and hearth.
 * /dndhome pieces: every piece of that settlement with its box, to find rooms (e.g. a forge) by hand.
 * /dndhome templesurvey [grid] [spacing]: from a grid of points round you, finds the nearest Dragon Temple to
 * each and checks a dragon lair or frost lair starts within 10 chunks of it (logged as {@code [TempleSurvey]}).
 * /dndhome temple [ember|frost|storm]: teleports you to the nearest Dragon Temple's sanctum, two blocks in front
 * of that breath altar (Ember by default) and looking at it, so a script can {@code use} it.
 * /dndhome stronghold [pit|seat|hoard|smithy|gate]: teleports you to that spot of the nearest Orc Stronghold
 * (the pit floor looking at the war drum, by default).
 * /dndhome strongholdsurvey [grid] [spacing]: from a grid of points round you, finds the nearest Orc Stronghold
 * to each and checks it stands in the badlands with all its pieces (logged as {@code [StrongholdSurvey]}).
 * /dndhome bloodsworn <players> <true|false>: sets or clears Blood-Sworn at the stronghold you stand in.
 * /dndhome trialreset <players>: forgets today's lost Trial at the stronghold you stand in.
 */
public final class DndHomeCommand {
    private DndHomeCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndhome")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> info(context, context.getSource().getPlayerOrThrow()))
                .then(CommandManager.literal("pieces").executes(DndHomeCommand::pieces))
                .then(CommandManager.literal("temple")
                        .executes(context -> temple(context, DragonAncestry.EMBER))
                        .then(CommandManager.argument("altar", StringArgumentType.word())
                                .executes(context -> temple(context, DragonAncestry.byId(StringArgumentType.getString(context, "altar"))))))
                .then(CommandManager.literal("templesurvey")
                        .executes(context -> templeSurvey(context, 3, 2000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> templeSurvey(context, IntegerArgumentType.getInteger(context, "grid"), 2000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> templeSurvey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("stronghold")
                        .executes(context -> StrongholdCommands.teleport(context, "pit"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> StrongholdCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("strongholdsurvey")
                        .executes(context -> StrongholdCommands.survey(context, 3, 2000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> StrongholdCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 2000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> StrongholdCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("bloodsworn")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .then(CommandManager.argument("sworn", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(context -> StrongholdCommands.bloodSworn(context, EntityArgumentType.getPlayers(context, "players"),
                                                com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "sworn"))))))
                .then(CommandManager.literal("trialreset")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .executes(context -> StrongholdCommands.trialReset(context, EntityArgumentType.getPlayers(context, "players")))))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> info(context, EntityArgumentType.getPlayer(context, "player")))));
    }

    private static int info(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        RacialHomes.Visit visit = RacialHomes.homeAt(player.getWorld(), pos);
        if (visit == null) {
            context.getSource().sendFeedback(Text.literal("[Home] " + player.getEntityName() + " is in no racial home"), false);
            return 0;
        }
        StructurePiece piece = visit.pieceAt(pos);
        String pieceName = piece == null ? "none" : String.valueOf(Registries.STRUCTURE_PIECE.getId(piece.getType()));
        boolean own = visit.home().isHomeOf(RaceLifecycle.activeRaceOf(player));
        context.getSource().sendFeedback(Text.literal("[Home] " + player.getEntityName() + " is in "
                + visit.home().structure() + " (home of " + visit.home().race().id() + ", start chunk "
                + visit.start().getPos() + "), piece " + pieceName + ", own home " + own
                + ", hearth " + (own && visit.inHearth(pos))), false);
        return own ? 2 : 1;
    }

    private static int pieces(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        RacialHomes.Visit visit = RacialHomes.homeAt(player.getWorld(), player.getBlockPos());
        if (visit == null) {
            context.getSource().sendFeedback(Text.literal("[Home] Not in a racial home"), false);
            return 0;
        }
        for (StructurePiece piece : visit.start().getChildren()) {
            BlockBox box = piece.getBoundingBox();
            context.getSource().sendFeedback(Text.literal("[Home] piece " + Registries.STRUCTURE_PIECE.getId(piece.getType())
                    + " " + box.getMinX() + " " + box.getMinY() + " " + box.getMinZ()
                    + " to " + box.getMaxX() + " " + box.getMaxY() + " " + box.getMaxZ()), false);
        }
        return visit.start().getChildren().size();
    }

    private static final TagKey<Structure> TEMPLES = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(mattonfire.dnd.classes.DnDClasses.MOD_ID, "dragon_temples"));

    /**
     * The acceptance survey for the Dragon Temple's lair rule: every temple found from a grid x grid of
     * points {@code spacing} blocks apart round the source must have a dragon lair or frost lair starting
     * within {@link DragonTempleStructure#LAIR_RADIUS} chunks (square distance, as the placement check).
     * Returns the number of temples that pass; fails are logged too.
     */
    private static int templeSurvey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos center = BlockPos.ofFloored(source.getPosition());
        Set<Long> seen = new HashSet<>();
        int pass = 0;
        int fail = 0;
        int half = (grid - 1) / 2;
        for (int gx = -half; gx <= grid - 1 - half; gx++) {
            for (int gz = -half; gz <= grid - 1 - half; gz++) {
                BlockPos from = center.add(gx * spacing, 0, gz * spacing);
                BlockPos temple = world.locateStructure(TEMPLES, from, 100, false);
                if (temple == null || !seen.add(ChunkPos.toLong(temple.getX() >> 4, temple.getZ() >> 4))) {
                    continue;
                }
                int dragon = chunkDistance(temple, world.locateStructure(DragonbornTrades.DRAGON_LAIR_MAPS, temple, 16, false));
                int frost = chunkDistance(temple, world.locateStructure(DragonbornTrades.FROST_LAIR_MAPS, temple, 16, false));
                boolean ok = Math.min(dragon, frost) <= DragonTempleStructure.LAIR_RADIUS;
                if (ok) {
                    pass++;
                } else {
                    fail++;
                }
                String line = "[TempleSurvey] temple at " + temple.getX() + " " + temple.getZ() + ": dragon lair "
                        + (dragon == Integer.MAX_VALUE ? "none" : dragon + " chunks") + ", frost lair "
                        + (frost == Integer.MAX_VALUE ? "none" : frost + " chunks") + ": " + (ok ? "PASS" : "FAIL");
                mattonfire.dnd.classes.DnDClasses.LOGGER.info(line);
                source.sendFeedback(Text.literal(line), false);
            }
        }
        String summary = "[TempleSurvey] " + (pass + fail) + " temples, " + pass + " pass, " + fail + " fail";
        mattonfire.dnd.classes.DnDClasses.LOGGER.info(summary);
        source.sendFeedback(Text.literal(summary), false);
        return pass;
    }

    /** Square distance in chunks between two structure positions, or MAX_VALUE if there's no second one. */
    private static int chunkDistance(BlockPos a, BlockPos b) {
        if (b == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs((a.getX() >> 4) - (b.getX() >> 4)), Math.abs((a.getZ() >> 4) - (b.getZ() >> 4)));
    }

    private static int temple(CommandContext<ServerCommandSource> context, DragonAncestry altar) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        if (altar == null || altar == DragonAncestry.NONE) {
            context.getSource().sendError(Text.literal("[Home] Which altar? ember, frost or storm"));
            return 0;
        }
        BlockPos found = world.locateStructure(TEMPLES, player.getBlockPos(), 100, false);
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(DragonTempleStructures.KEY);
        if (found == null || structure == null) {
            context.getSource().sendError(Text.literal("[Home] No Dragon Temple within 100 chunks"));
            return 0;
        }
        StructureStart start = world.getChunk(found.getX() >> 4, found.getZ() >> 4).getStructureStart(structure);
        SanctumPiece sanctum = null;
        if (start != null) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof SanctumPiece s) {
                    sanctum = s;
                }
            }
        }
        if (sanctum == null) {
            context.getSource().sendError(Text.literal("[Home] The temple at " + found.toShortString() + " has no sanctum"));
            return 0;
        }
        BlockPos stand = sanctum.inFrontOf(altar);
        Vec3d eye = new Vec3d(stand.getX() + 0.5D, stand.getY() + player.getStandingEyeHeight(), stand.getZ() + 0.5D);
        Vec3d to = Vec3d.of(sanctum.altarTop(altar)).add(0.5D, 0.3D, 0.5D).subtract(eye);
        float yaw = (float) (MathHelper.atan2(to.z, to.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0D / Math.PI);
        player.teleport(world, stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, yaw, pitch);
        BlockPos reliquary = sanctum.reliquary();
        context.getSource().sendFeedback(Text.literal("[Home] Dragon Temple at chunk " + start.getPos() + ": at the "
                + altar.id() + " altar (" + sanctum.altarTop(altar).toShortString() + "), reliquary at "
                + reliquary.toShortString()), false);
        return 1;
    }
}
