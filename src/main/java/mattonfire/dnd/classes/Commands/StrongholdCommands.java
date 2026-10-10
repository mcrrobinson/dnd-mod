package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.stronghold.BloodSworn;
import mattonfire.dnd.world.gen.stronghold.FightingPitPiece;
import mattonfire.dnd.world.gen.stronghold.LonghousePiece;
import mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructure;
import mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructures;
import mattonfire.dnd.world.gen.stronghold.StrongholdPiece;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;

/** The Orc Stronghold parts of {@link DndHomeCommand}: teleports, the badlands survey and the Trial records. */
final class StrongholdCommands {
    static final TagKey<Structure> STRONGHOLDS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "orc_strongholds"));
    /** Piece types every stronghold must have. */
    private static final int PIECE_KINDS = 6;

    private StrongholdCommands() {
    }

    /** The start of the nearest stronghold to {@code from} (within 100 chunks), or null. */
    private static StructureStart nearest(ServerWorld world, BlockPos from) {
        BlockPos found = world.locateStructure(STRONGHOLDS, from, 100, false);
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(OrcStrongholdStructures.KEY);
        if (found == null || structure == null) {
            return null;
        }
        StructureStart start = world.getChunk(found.getX() >> 4, found.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS)
                .getStructureStart(structure);
        return start != null && start.hasChildren() ? start : null;
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        StructureStart start = nearest(world, player.getBlockPos());
        if (start == null) {
            context.getSource().sendError(Text.literal("[Home] No Orc Stronghold within 100 chunks"));
            return 0;
        }
        FightingPitPiece pit = null;
        LonghousePiece longhouse = null;
        StrongholdPiece any = null;
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof FightingPitPiece p) {
                pit = p;
            } else if (piece instanceof LonghousePiece l) {
                longhouse = l;
            }
            if (piece instanceof StrongholdPiece s) {
                any = s;
            }
        }
        if (pit == null || longhouse == null || any == null) {
            context.getSource().sendError(Text.literal("[Home] The stronghold at chunk " + start.getPos() + " is missing pieces"));
            return 0;
        }
        BlockPos stand;
        BlockPos look;
        switch (spot) {
            case "seat" -> {
                stand = longhouse.seat();
                look = longhouse.at(0, 1, -16);
            }
            case "hoard" -> {
                stand = longhouse.at(3, 1, -15);
                look = longhouse.hoard();
            }
            case "smithy" -> {
                stand = any.at(13, 1, 0);
                look = any.at(15, 1, 0);
            }
            case "gate" -> {
                stand = any.at(0, 1, OrcStrongholdStructure.PALISADE + 4);
                look = any.at(0, 1, 0);
            }
            default -> {
                stand = pit.floor(-6);
                look = pit.drum();
            }
        }
        Vec3d eye = new Vec3d(stand.getX() + 0.5D, stand.getY() + player.getStandingEyeHeight(), stand.getZ() + 0.5D);
        Vec3d to = Vec3d.ofCenter(look).subtract(eye);
        float yaw = (float) (MathHelper.atan2(to.z, to.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0D / Math.PI);
        player.teleport(world, stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, yaw, pitch);
        context.getSource().sendFeedback(Text.literal("[Home] Orc Stronghold at chunk " + start.getPos() + " (hold "
                + start.getPos().toLong() + "): at the " + spot + ", drum " + pit.drum().toShortString() + ", hoard "
                + longhouse.hoard().toShortString()), false);
        return 1;
    }

    /**
     * The acceptance survey: every stronghold found from a grid x grid of points {@code spacing} blocks apart
     * round the source must stand in the badlands (its middle's biome is in {@code #minecraft:is_badlands}) and
     * have all its pieces. Returns the number that pass.
     */
    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos center = BlockPos.ofFloored(source.getPosition());
        Set<Long> seen = new HashSet<>();
        int pass = 0;
        int fail = 0;
        int half = (grid - 1) / 2;
        for (int gx = -half; gx <= grid - 1 - half; gx++) {
            for (int gz = -half; gz <= grid - 1 - half; gz++) {
                StructureStart start = nearest(world, center.add(gx * spacing, 0, gz * spacing));
                if (start == null || !seen.add(start.getPos().toLong())) {
                    continue;
                }
                BlockPos middle = start.getBoundingBox().getCenter();
                boolean badlands = world.getBiome(middle).isIn(BiomeTags.IS_BADLANDS);
                Set<Identifier> kinds = new HashSet<>();
                start.getChildren().forEach(piece -> kinds.add(Registries.STRUCTURE_PIECE.getId(piece.getType())));
                boolean ok = badlands && kinds.size() == PIECE_KINDS;
                if (ok) {
                    pass++;
                } else {
                    fail++;
                }
                String line = "[StrongholdSurvey] stronghold at " + middle.getX() + " " + middle.getY() + " " + middle.getZ()
                        + ": biome " + world.getBiome(middle).getKey().map(k -> k.getValue().toString()).orElse("?")
                        + ", " + start.getChildren().size() + " pieces (" + kinds.size() + " kinds): " + (ok ? "PASS" : "FAIL");
                DnDClasses.LOGGER.info(line);
                source.sendFeedback(Text.literal(line), false);
            }
        }
        String summary = "[StrongholdSurvey] " + (pass + fail) + " strongholds, " + pass + " pass, " + fail + " fail";
        DnDClasses.LOGGER.info(summary);
        source.sendFeedback(Text.literal(summary), false);
        return pass;
    }

    /** The hold the command's source stands in, or null (with an error). */
    private static Long holdHere(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        RacialHomes.Visit visit = RacialHomes.homeAt(source.getWorld(), BlockPos.ofFloored(source.getPosition()));
        if (visit == null || visit.home() != RacialHomes.ORC_STRONGHOLD) {
            source.sendError(Text.literal("[Home] Not in an Orc Stronghold"));
            return null;
        }
        return visit.id();
    }

    static int bloodSworn(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, boolean sworn) {
        Long hold = holdHere(context);
        if (hold == null) {
            return 0;
        }
        BloodSworn records = BloodSworn.get(context.getSource().getWorld());
        for (ServerPlayerEntity player : players) {
            records.setSworn(hold, player.getUuid(), sworn);
            context.getSource().sendFeedback(Text.literal("[Home] " + player.getEntityName() + " Blood-Sworn at hold " + hold
                    + ": " + sworn), true);
        }
        return players.size();
    }

    static int trialReset(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) {
        Long hold = holdHere(context);
        if (hold == null) {
            return 0;
        }
        BloodSworn records = BloodSworn.get(context.getSource().getWorld());
        for (ServerPlayerEntity player : players) {
            records.clearLost(hold, player.getUuid());
            context.getSource().sendFeedback(Text.literal("[Home] " + player.getEntityName() + " may take the Trial again at hold "
                    + hold), true);
        }
        return players.size();
    }
}
