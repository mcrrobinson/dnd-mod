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
import mattonfire.dnd.world.gen.Desolation;
import mattonfire.dnd.world.gen.DesolationLevel;
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
 * /dndhome templeplace: builds a Dragon Temple on the best site within 6 chunks of you, skipping the rule that a
 * lair must be near (for trying temples on varied ground); /dndhome temple finds it afterwards.
 * /dndhome temple [ember|frost|storm]: teleports you to the nearest Dragon Temple's sanctum, two blocks in front
 * of that breath altar (Ember by default) and looking at it, so a script can {@code use} it.
 * /dndhome stronghold [pit|seat|hoard|smithy|gate]: teleports you to that spot of the nearest Orc Stronghold
 * (the pit floor looking at the war drum, by default).
 * /dndhome strongholdsurvey [grid] [spacing]: from a grid of points round you, finds the nearest Orc Stronghold
 * to each and checks it stands in the badlands with all its pieces (logged as {@code [StrongholdSurvey]}).
 * /dndhome bloodsworn <players> <true|false>: sets or clears Blood-Sworn at the stronghold you stand in.
 * /dndhome trialreset <players>: forgets today's lost Trial at the stronghold you stand in.
 * /dndhome sanctum [hall|offering|anchor|market|den|gate|top|side]: teleports you to that spot of the nearest
 * Brimstone Sanctum (in the Nether), looking at it.
 * /dndhome cogsdeep [door|top|observatory|ladder|telescope|slit|stair|pillar|pillarmid|foot|commons|workshop|westworkshop|clocktower|strongbox|gems|grotto]:
 * teleports you to that spot of the nearest Cogsdeep, looking at it.
 * /dndhome cogsdeepsurvey [grid] [spacing]: finds the nearest Cogsdeep from a grid of points round you and checks
 * its biome, pieces, depth, ground, rooms and neighbours (logged as {@code [CogsdeepSurvey]}).
 * /dndhome sanctumsurvey [grid] [spacing]: from a grid of points round you in the Nether, finds the nearest
 * sanctum to each and checks its biome, pieces, that no lava lies under it and that it doesn't clip the ceiling
 * (logged as {@code [SanctumSurvey]}).
 * /dndhome rift [top|mouth|floor|ledge|pool|step &lt;k&gt;], riftlanding, riftlair, riftsurvey [grid] [spacing], riftseeds
 * [seeds]:
 * the Underdark Rifts' teleports and checks ({@link RiftCommands}).
 * /dndhome drowoutpost [spot], drowoutpostsurvey [grid] [spacing], drowoutpostcensus, drowoutpostseeds [seeds]: the Drow Outposts'
 * teleports and checks ({@link DrowOutpostCommands}).
 * /dndhome duergarhold [spot], duergarholdsurvey [grid] [spacing], duergarholdcensus, duergarholdseeds [seeds]: the Duergar
 * Holds' teleports and checks ({@link DuergarHoldCommands}).
 * /dndhome drowspire [spot], drowspiresurvey [grid] [spacing], drowspirecensus, drowspireseeds [seeds]: the Drow Spire's
 * teleports and checks ({@link DrowSpireCommands}).
 * /dndhome underdarklair &lt;lair&gt; [spot], underdarklairsurvey &lt;lair&gt; [grid] [spacing], underdarklaircensus &lt;lair&gt;,
 * underdarklairseeds &lt;lair&gt; [seeds]: the Underdark monster lairs' teleports and checks ({@link LairCommands}).
 * /dndhome koboldwarren [spot], koboldwarrensurvey [grid] [spacing], koboldwarrenseeds [seeds], koboldtrap &lt;kind&gt; [tier]:
 * the kobold warrens' teleports, checks and test traps ({@link KoboldWarrenCommands}).
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
                .then(CommandManager.literal("templeplace").executes(DndHomeCommand::templePlace))
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
                .then(CommandManager.literal("sanctum")
                        .executes(context -> SanctumCommands.teleport(context, "hall"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> SanctumCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("sanctumsurvey")
                        .executes(context -> SanctumCommands.survey(context, 3, 1500))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> SanctumCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 1500))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> SanctumCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("cogsdeep")
                        .executes(context -> CogsdeepCommands.teleport(context, "door"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> CogsdeepCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("cogsdeepsurvey")
                        .executes(context -> CogsdeepCommands.survey(context, 3, 2000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> CogsdeepCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 2000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> CogsdeepCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("koboldwarren")
                        .executes(context -> KoboldWarrenCommands.teleport(context, "mouth"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> KoboldWarrenCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("koboldwarrensurvey")
                        .executes(context -> KoboldWarrenCommands.survey(context, 3, 1500))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> KoboldWarrenCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 1500))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> KoboldWarrenCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("koboldwarrenseeds")
                        .executes(context -> KoboldWarrenCommands.seeds(context, java.util.List.of()))
                        .then(CommandManager.argument("seeds", StringArgumentType.greedyString())
                                .executes(context -> KoboldWarrenCommands.seeds(context,
                                        RiftCommands.parseSeeds(StringArgumentType.getString(context, "seeds"))))))
                .then(CommandManager.literal("koboldtrap")
                        .then(CommandManager.argument("kind", StringArgumentType.word())
                                .executes(context -> KoboldWarrenCommands.trap(context, StringArgumentType.getString(context, "kind"), 1))
                                .then(CommandManager.argument("tier", IntegerArgumentType.integer(1, 4))
                                        .executes(context -> KoboldWarrenCommands.trap(context, StringArgumentType.getString(context, "kind"),
                                                IntegerArgumentType.getInteger(context, "tier"))))))
                .then(CommandManager.literal("rift")
                        .executes(context -> RiftCommands.teleport(context, "ledge", 0))
                        .then(CommandManager.literal("step")
                                .then(CommandManager.argument("k", IntegerArgumentType.integer(0))
                                        .executes(context -> RiftCommands.teleport(context, "step",
                                                IntegerArgumentType.getInteger(context, "k")))))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> RiftCommands.teleport(context, StringArgumentType.getString(context, "spot"), 0))))
                .then(CommandManager.literal("riftlanding").executes(RiftCommands::landing))
                .then(CommandManager.literal("riftlair").executes(RiftCommands::lair))
                .then(CommandManager.literal("riftsurvey")
                        .executes(context -> RiftCommands.survey(context, 3, 1000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> RiftCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 1000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> RiftCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("riftseeds")
                        .executes(context -> RiftCommands.seeds(context, java.util.List.of()))
                        .then(CommandManager.argument("seeds", StringArgumentType.greedyString())
                                .executes(context -> RiftCommands.seeds(context,
                                        RiftCommands.parseSeeds(StringArgumentType.getString(context, "seeds"))))))
                .then(CommandManager.literal("drowoutpost")
                        .executes(context -> DrowOutpostCommands.teleport(context, "shrine"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> DrowOutpostCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("drowoutpostsurvey")
                        .executes(context -> DrowOutpostCommands.survey(context, 3, 1000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> DrowOutpostCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 1000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> DrowOutpostCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("drowoutpostcensus").executes(DrowOutpostCommands::census))
                .then(CommandManager.literal("drowoutpostseeds")
                        .executes(context -> DrowOutpostCommands.seeds(context, java.util.List.of()))
                        .then(CommandManager.argument("seeds", StringArgumentType.greedyString())
                                .executes(context -> DrowOutpostCommands.seeds(context,
                                        RiftCommands.parseSeeds(StringArgumentType.getString(context, "seeds"))))))
                .then(CommandManager.literal("duergarhold")
                        .executes(context -> DuergarHoldCommands.teleport(context, "inside"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> DuergarHoldCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("duergarholdsurvey")
                        .executes(context -> DuergarHoldCommands.survey(context, 3, 1000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> DuergarHoldCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 1000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> DuergarHoldCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("duergarholdcensus").executes(DuergarHoldCommands::census))
                .then(CommandManager.literal("duergarholdseeds")
                        .executes(context -> DuergarHoldCommands.seeds(context, java.util.List.of()))
                        .then(CommandManager.argument("seeds", StringArgumentType.greedyString())
                                .executes(context -> DuergarHoldCommands.seeds(context,
                                        RiftCommands.parseSeeds(StringArgumentType.getString(context, "seeds"))))))
                .then(CommandManager.literal("drowspire")
                        .executes(context -> DrowSpireCommands.teleport(context, "sanctum"))
                        .then(CommandManager.argument("spot", StringArgumentType.word())
                                .executes(context -> DrowSpireCommands.teleport(context, StringArgumentType.getString(context, "spot")))))
                .then(CommandManager.literal("drowspiresurvey")
                        .executes(context -> DrowSpireCommands.survey(context, 3, 2000))
                        .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                .executes(context -> DrowSpireCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"), 2000))
                                .then(CommandManager.argument("spacing", IntegerArgumentType.integer(256, 20000))
                                        .executes(context -> DrowSpireCommands.survey(context, IntegerArgumentType.getInteger(context, "grid"),
                                                IntegerArgumentType.getInteger(context, "spacing"))))))
                .then(CommandManager.literal("drowspirecensus").executes(DrowSpireCommands::census))
                .then(CommandManager.literal("drowspireseeds")
                        .executes(context -> DrowSpireCommands.seeds(context, java.util.List.of()))
                        .then(CommandManager.argument("seeds", StringArgumentType.greedyString())
                                .executes(context -> DrowSpireCommands.seeds(context,
                                        RiftCommands.parseSeeds(StringArgumentType.getString(context, "seeds"))))))
                .then(CommandManager.literal("underdarklair")
                        .then(CommandManager.argument("lair", StringArgumentType.word())
                                .executes(context -> LairCommands.teleport(context, StringArgumentType.getString(context, "lair"), "inside"))
                                .then(CommandManager.argument("spot", StringArgumentType.word())
                                        .executes(context -> LairCommands.teleport(context, StringArgumentType.getString(context, "lair"),
                                                StringArgumentType.getString(context, "spot"))))))
                .then(CommandManager.literal("underdarklairsurvey")
                        .then(CommandManager.argument("lair", StringArgumentType.word())
                                .executes(context -> LairCommands.survey(context, StringArgumentType.getString(context, "lair"), 3, 1000))
                                .then(CommandManager.argument("grid", IntegerArgumentType.integer(1, 7))
                                        .executes(context -> LairCommands.survey(context, StringArgumentType.getString(context, "lair"),
                                                IntegerArgumentType.getInteger(context, "grid"), 1000))
                                        .then(CommandManager.argument("spacing", IntegerArgumentType.integer(64, 20000))
                                                .executes(context -> LairCommands.survey(context, StringArgumentType.getString(context, "lair"),
                                                        IntegerArgumentType.getInteger(context, "grid"),
                                                        IntegerArgumentType.getInteger(context, "spacing")))))))
                .then(CommandManager.literal("underdarklaircensus")
                        .then(CommandManager.argument("lair", StringArgumentType.word())
                                .executes(context -> LairCommands.census(context, StringArgumentType.getString(context, "lair")))))
                .then(CommandManager.literal("underdarklairseeds")
                        .then(CommandManager.argument("lair", StringArgumentType.word())
                                .executes(context -> LairCommands.seeds(context, StringArgumentType.getString(context, "lair"), java.util.List.of()))
                                .then(CommandManager.argument("seeds", StringArgumentType.greedyString())
                                        .executes(context -> LairCommands.seeds(context, StringArgumentType.getString(context, "lair"),
                                                RiftCommands.parseSeeds(StringArgumentType.getString(context, "seeds")))))))
                .then(CommandManager.literal("bloodsworn")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .then(CommandManager.argument("sworn", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(context -> StrongholdCommands.bloodSworn(context, EntityArgumentType.getPlayers(context, "players"),
                                                com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "sworn"))))))
                .then(CommandManager.literal("trialreset")
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                .executes(context -> StrongholdCommands.trialReset(context, EntityArgumentType.getPlayers(context, "players")))))
                .then(CommandManager.literal("visit")
                        .then(CommandManager.argument("structure", net.minecraft.command.argument.IdentifierArgumentType.identifier())
                                .executes(context -> visit(context, net.minecraft.command.argument.IdentifierArgumentType
                                        .getIdentifier(context, "structure"), null))
                                .then(CommandManager.argument("piece", net.minecraft.command.argument.IdentifierArgumentType.identifier())
                                        .executes(context -> visit(context, net.minecraft.command.argument.IdentifierArgumentType
                                                .getIdentifier(context, "structure"), net.minecraft.command.argument.IdentifierArgumentType
                                                .getIdentifier(context, "piece"))))))
                .then(CommandManager.literal("desolation")
                        .then(CommandManager.argument("level", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("random");
                                    for (DesolationLevel level : DesolationLevel.values()) {
                                        builder.suggest(level.name().toLowerCase(java.util.Locale.ROOT));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> desolation(context, StringArgumentType.getString(context, "level")))))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> info(context, EntityArgumentType.getPlayer(context, "player")))));
    }

    /**
     * {@code /dndhome visit <structure>}: teleports you 200 high over the nearest start of that structure (within 100
     * chunks), for scripted checks that can't read {@code /locate}'s answer. {@code /dndhome visit <structure> <piece>}
     * instead puts you out in front of the first piece of that type (its open side, by its facing), looking at it.
     */
    private static int visit(CommandContext<ServerCommandSource> context, Identifier id, Identifier pieceId) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = context.getSource().getWorld();
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        var entry = registry.getEntry(net.minecraft.registry.RegistryKey.of(RegistryKeys.STRUCTURE, id));
        if (entry.isEmpty()) {
            context.getSource().sendError(Text.literal("[Home] No structure " + id));
            return 0;
        }
        var found = world.getChunkManager().getChunkGenerator().locateStructure(world,
                net.minecraft.registry.entry.RegistryEntryList.of(entry.get()), player.getBlockPos(), 100, false);
        if (found == null) {
            context.getSource().sendError(Text.literal("[Home] No " + id + " within 100 chunks"));
            return 0;
        }
        BlockPos pos = found.getFirst();
        if (pieceId != null) {
            StructureStart start = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS)
                    .getStructureStart(entry.get().value());
            StructurePiece piece = start == null ? null : start.getChildren().stream()
                    .filter(p -> pieceId.equals(Registries.STRUCTURE_PIECE.getId(p.getType()))).findFirst().orElse(null);
            if (piece == null || piece.getFacing() == null) {
                context.getSource().sendError(Text.literal("[Home] No " + pieceId + " piece with a facing in " + id + " at " + pos.getX() + " " + pos.getZ()));
                return 0;
            }
            BlockBox box = piece.getBoundingBox();
            net.minecraft.util.math.Direction facing = piece.getFacing();
            Vec3d center = Vec3d.ofCenter(box.getCenter());
            double back = Math.max(box.getBlockCountX(), box.getBlockCountZ()) / 2.0D + 20.0D;
            player.teleport(world, center.x - facing.getOffsetX() * back, box.getMinY() + 10.0D, center.z - facing.getOffsetZ() * back,
                    facing.asRotation(), 15.0F);
            context.getSource().sendFeedback(Text.literal("[Home] " + pieceId + " of " + id + " at " + box.getCenter().toShortString()
                    + " facing " + facing.asString()), false);
            return 1;
        }
        player.teleport(world, pos.getX() + 0.5D, 200.0D, pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
        context.getSource().sendFeedback(Text.literal("[Home] " + id + " at " + pos.getX() + " " + pos.getZ()), false);
        return 1;
    }

    /** {@code /dndhome desolation <level|random>}: the level every desolate settlement generated from now on gets. */
    private static int desolation(CommandContext<ServerCommandSource> context, String name) {
        if (name.equals("random")) {
            Desolation.force(null);
            context.getSource().sendFeedback(Text.literal("[Home] Desolate settlements roll their level"), true);
            return 1;
        }
        for (DesolationLevel level : DesolationLevel.values()) {
            if (level.name().equalsIgnoreCase(name)) {
                Desolation.force(level);
                context.getSource().sendFeedback(Text.literal("[Home] New desolate settlements are " + name), true);
                return 1;
            }
        }
        context.getSource().sendError(Text.literal("[Home] No desolation level " + name));
        return 0;
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
                    + " to " + box.getMaxX() + " " + box.getMaxY() + " " + box.getMaxZ() + " facing " + piece.getFacing()), false);
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
        ServerWorld world = context.getSource().getWorld();
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[TempleSurvey]", "temples",
                from -> world.locateStructure(TEMPLES, from, HomeCommandUtil.SEARCH_CHUNKS, false),
                temple -> ChunkPos.toLong(temple.getX() >> 4, temple.getZ() >> 4), temple -> {
                    int dragon = chunkDistance(temple, world.locateStructure(DragonbornTrades.DRAGON_LAIR_MAPS, temple, 16, false));
                    int frost = chunkDistance(temple, world.locateStructure(DragonbornTrades.FROST_LAIR_MAPS, temple, 16, false));
                    boolean ok = Math.min(dragon, frost) <= DragonTempleStructure.LAIR_RADIUS;
                    return new HomeCommandUtil.Check(ok, "[TempleSurvey] temple at " + temple.getX() + " " + temple.getZ()
                            + ": dragon lair " + (dragon == Integer.MAX_VALUE ? "none" : dragon + " chunks") + ", frost lair "
                            + (frost == Integer.MAX_VALUE ? "none" : frost + " chunks") + ": " + (ok ? "PASS" : "FAIL"));
                });
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
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(DragonTempleStructures.KEY);
        StructureStart start;
        if (lastPlaced != null && lastPlaced.getBoundingBox().getCenter().isWithinDistance(player.getBlockPos(), 160.0D)) {
            // /locate doesn't know about temples placed by command.
            start = lastPlaced;
        } else {
            BlockPos found = world.locateStructure(TEMPLES, player.getBlockPos(), 100, false);
            if (found == null || structure == null) {
                context.getSource().sendError(Text.literal("[Home] No Dragon Temple within 100 chunks"));
                return 0;
            }
            start = world.getChunk(found.getX() >> 4, found.getZ() >> 4).getStructureStart(structure);
        }
        SanctumPiece sanctum = null;
        if (start != null) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof SanctumPiece s) {
                    sanctum = s;
                }
            }
        }
        if (sanctum == null) {
            context.getSource().sendError(Text.literal("[Home] That temple has no sanctum"));
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

    /**
     * Builds a Dragon Temple on the nearest chunk round the player (out to 6 chunks) whose terrain and biome give
     * a site, ignoring the lair and fortress rules, and records it in the chunks like a generated one, so
     * {@code /dndhome temple}, homes and temple law find it.
     */
    /** The last temple {@code templeplace} built, which {@code /dndhome temple} prefers when it's near. */
    private static StructureStart lastPlaced;

    private static int templePlace(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(DragonTempleStructures.KEY);
        var generator = world.getChunkManager().getChunkGenerator();
        ChunkPos center = new ChunkPos(player.getBlockPos());
        for (int r = 0; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                    StructureStart start;
                    DragonTempleStructure.ignoreNeighbours = true;
                    try {
                        start = structure.createStructureStart(world.getRegistryManager(), generator, generator.getBiomeSource(),
                                world.getChunkManager().getNoiseConfig(), world.getStructureTemplateManager(), world.getSeed(),
                                chunk, 0, world, biome -> true);
                    } finally {
                        DragonTempleStructure.ignoreNeighbours = false;
                    }
                    if (!start.hasChildren()) {
                        continue;
                    }
                    net.minecraft.util.math.BlockBox box = start.getBoundingBox();
                    ChunkPos min = new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4);
                    ChunkPos max = new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4);
                    world.getChunk(chunk.x, chunk.z).setStructureStart(structure, start);
                    ChunkPos.stream(min, max).forEach(c -> {
                        world.getChunk(c.x, c.z).addStructureReference(structure, chunk.toLong());
                        start.place(world, world.getStructureAccessor(), generator, world.getRandom(),
                                new net.minecraft.util.math.BlockBox(c.getStartX(), world.getBottomY(), c.getStartZ(),
                                        c.getEndX(), world.getTopY(), c.getEndZ()), c);
                    });
                    lastPlaced = start;
                    context.getSource().sendFeedback(Text.literal("[Home] Placed a Dragon Temple at chunk " + chunk
                            + ", box " + box.getMinX() + " " + box.getMinY() + " " + box.getMinZ() + " to " + box.getMaxX()
                            + " " + box.getMaxY() + " " + box.getMaxZ()), true);
                    return 1;
                }
            }
        }
        context.getSource().sendError(Text.literal("[Home] No Dragon Temple site within 6 chunks"));
        return 0;
    }
}
