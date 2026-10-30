package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.world.gen.warren.BoltHolePiece;
import mattonfire.dnd.world.gen.warren.KoboldTrapRigs;
import mattonfire.dnd.world.gen.warren.KoboldWarren;
import mattonfire.dnd.world.gen.warren.KoboldWarrenStructure;
import mattonfire.dnd.world.gen.warren.KoboldWarrens;
import mattonfire.dnd.world.gen.warren.WarrenChiefPiece;
import mattonfire.dnd.world.gen.warren.WarrenDenPiece;
import mattonfire.dnd.world.gen.warren.WarrenMouthPiece;
import mattonfire.dnd.world.gen.warren.WarrenNurseryPiece;
import mattonfire.dnd.world.gen.warren.WarrenPiece;
import mattonfire.dnd.world.gen.warren.WarrenShrinePiece;
import mattonfire.dnd.world.gen.warren.WarrenTunnelPiece;
import mattonfire.dnd.world.gen.warren.WarrenWorkshopPiece;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * The kobold warren parts of {@link DndHomeCommand} (docs/structures/kobold-warrens.md):
 * <ul>
 * <li>{@code /dndhome koboldwarren [mouth|spikes|rockfall|den|guard|shrine|nursery|workshop|larder|pit|chief|hole|overview]}:
 * teleports you to that spot of the nearest warren, looking at it, and prints where its traps' triggers are.</li>
 * <li>{@code /dndhome koboldwarrensurvey [grid] [spacing]}: from a grid of points round you, finds the nearest warren to
 * each and checks its mouth is on a slope (the noise ground {@value KoboldWarrenStructure#RISE_DISTANCE} blocks in is
 * {@value KoboldWarrenStructure#RISE}+ higher), dry, above sea level + 4 and the warren has all its piece kinds
 * ({@code [WarrenSurvey]}); then the great share, mean slope and mouth heights.</li>
 * <li>{@code /dndhome koboldwarrenseeds [seeds]}: the nearest warren to 0 0 on each seed, without making a world.</li>
 * <li>{@code /dndhome koboldtrap <rockfall|spikes|pit|dart> [tier]}: builds a kobold test trap 3 blocks in front of you
 * on a 5 x 8 stone strip and prints its trigger.</li>
 * </ul>
 */
final class KoboldWarrenCommands {
    /** Mouth, tunnel, den, shrine, nursery, workshop, larder, chief, bolt-hole. */
    private static final int PIECE_KINDS = 9;

    private KoboldWarrenCommands() {
    }

    private static StructureStart nearest(ServerWorld world, BlockPos from) {
        return HomeCommandUtil.nearestStart(world, KoboldWarrens.TAG, KoboldWarrens.KEY, from);
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        StructureStart start = nearest(world, player.getBlockPos());
        if (start == null) {
            context.getSource().sendError(Text.literal("[Home] No kobold warren within 100 chunks"));
            return 0;
        }
        WarrenMouthPiece mouth = KoboldWarren.first(start, WarrenMouthPiece.class);
        WarrenTunnelPiece tunnel = KoboldWarren.first(start, WarrenTunnelPiece.class);
        WarrenChiefPiece chief = KoboldWarren.first(start, WarrenChiefPiece.class);
        WarrenShrinePiece shrine = KoboldWarren.first(start, WarrenShrinePiece.class);
        WarrenNurseryPiece nursery = KoboldWarren.first(start, WarrenNurseryPiece.class);
        WarrenWorkshopPiece workshop = KoboldWarren.first(start, WarrenWorkshopPiece.class);
        WarrenDenPiece den = null;
        WarrenDenPiece guard = null;
        BoltHolePiece hole = null;
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof WarrenDenPiece d) {
                if (d.guard()) {
                    guard = d;
                } else {
                    den = d;
                }
            } else if (piece instanceof BoltHolePiece h && (hole == null || h.exit() != null)) {
                hole = h;
            }
        }
        if (mouth == null || tunnel == null || chief == null || shrine == null || nursery == null || workshop == null
                || den == null) {
            context.getSource().sendError(Text.literal("[Home] The warren at chunk " + start.getPos() + " is missing pieces"));
            return 0;
        }
        BlockPos stand;
        BlockPos look;
        switch (spot) {
            case "spikes" -> {
                stand = mouth.at(0, 1, 0);
                look = mouth.at(0, 0, WarrenMouthPiece.SPIKE_B);
            }
            case "rockfall" -> {
                BlockPos floor = tunnel.rockfallFloor();
                stand = floor.up().offset(mouth.front(), 3);
                look = floor;
            }
            case "den" -> {
                stand = den.lookout();
                look = den.at(0, 1, 0);
            }
            case "guard" -> {
                WarrenDenPiece hall = guard != null ? guard : den;
                stand = hall.lookout();
                look = hall.at(0, 1, 0);
            }
            case "shrine" -> {
                stand = shrine.at(-12, 1, 0);
                look = shrine.idol();
            }
            case "nursery" -> {
                stand = nursery.at(10, 1, -1);
                look = nursery.at(14, 1, 0);
            }
            case "workshop" -> {
                stand = workshop.at(-7, 1, -7);
                look = workshop.at(-12, 1, -9);
            }
            case "larder" -> {
                stand = den.at(7, 1, -7);
                look = den.at(10, 1, -10);
            }
            case "pit" -> {
                stand = chief.at(0, 1, -5);
                look = chief.at(0, 0, WarrenChiefPiece.PIT_NEAR - 1);
            }
            case "chief" -> {
                stand = chief.at(0, 1, -13);
                look = chief.chest();
            }
            case "hole" -> {
                List<BlockPos> cells = hole == null ? List.of() : hole.cells();
                stand = hole != null && hole.exit() != null ? hole.exit().offset(mouth.front(), 3)
                        : cells.isEmpty() ? mouth.at(0, 1, 5) : cells.get(0);
                look = hole != null && hole.exit() != null ? hole.exit() : mouth.at(0, 1, 0);
            }
            case "overview" -> {
                stand = mouth.at(0, 12, 22);
                look = mouth.at(0, 2, 0);
            }
            default -> {
                stand = mouth.at(0, 1, 6);
                look = mouth.at(0, 2, -3);
            }
        }
        lookAt(player, world, stand, look);
        String line = "[Home] Kobold warren at chunk " + start.getPos() + " (" + (mouth.great() ? "great, " : "") + "tier "
                + mouth.tier() + ", facing " + mouth.front().asString() + "): at the " + spot + "; mouth "
                + mouth.center().toShortString() + ", spikes trigger " + mouth.spikeTrigger().toShortString()
                + ", rockfall trigger " + tunnel.rockfallTrigger().toShortString() + ", pit trigger "
                + chief.pitTrigger().toShortString() + ", dart plate " + shrine.plate().toShortString() + ", hoard chest "
                + chief.chest().toShortString() + ", egg clutch " + nursery.firstClutch().toShortString();
        DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return 1;
    }

    private static void lookAt(ServerPlayerEntity player, ServerWorld world, BlockPos stand, BlockPos look) {
        Vec3d eye = new Vec3d(stand.getX() + 0.5D, stand.getY() + player.getStandingEyeHeight(), stand.getZ() + 0.5D);
        Vec3d to = Vec3d.ofCenter(look).subtract(eye);
        float yaw = (float) (MathHelper.atan2(to.z, to.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0D / Math.PI);
        player.teleport(world, stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, yaw, pitch);
    }

    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld world = context.getSource().getWorld();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noise = world.getChunkManager().getNoiseConfig();
        int seaLevel = generator.getSeaLevel();
        int[] stats = new int[5]; // count, great, rise sum, min y, max y
        stats[3] = Integer.MAX_VALUE;
        stats[4] = Integer.MIN_VALUE;
        int pass = HomeCommandUtil.survey(context.getSource(), grid, spacing, "[WarrenSurvey]", "warrens",
                from -> nearest(world, from), start -> start.getPos().toLong(), start -> {
                    WarrenMouthPiece mouth = KoboldWarren.first(start, WarrenMouthPiece.class);
                    if (mouth == null) {
                        return new HomeCommandUtil.Check(false, "[WarrenSurvey] warren at chunk " + start.getPos() + ": no mouth: FAIL");
                    }
                    BlockPos m = mouth.center();
                    Direction in = mouth.front().getOpposite();
                    int ground = generator.getHeight(m.getX(), m.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noise) - 1;
                    int floor = generator.getHeight(m.getX(), m.getZ(), Heightmap.Type.OCEAN_FLOOR_WG, world, noise) - 1;
                    BlockPos up = m.offset(in, KoboldWarrenStructure.RISE_DISTANCE);
                    int rise = generator.getHeight(up.getX(), up.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noise) - 1 - m.getY();
                    boolean water = floor < ground;
                    Set<Identifier> kinds = new HashSet<>();
                    start.getChildren().forEach(piece -> kinds.add(Registries.STRUCTURE_PIECE.getId(piece.getType())));
                    String biome = world.getBiome(m).getKey().map(k -> k.getValue().toString()).orElse("?");
                    boolean ok = rise >= KoboldWarrenStructure.RISE && !water
                            && m.getY() >= seaLevel + KoboldWarrenStructure.ABOVE_SEA && kinds.size() >= PIECE_KINDS;
                    stats[0]++;
                    stats[1] += mouth.great() ? 1 : 0;
                    stats[2] += rise;
                    stats[3] = Math.min(stats[3], m.getY());
                    stats[4] = Math.max(stats[4], m.getY());
                    return new HomeCommandUtil.Check(ok, "[WarrenSurvey] warren at " + m.getX() + " " + m.getY() + " "
                            + m.getZ() + " facing " + mouth.front().asString() + (mouth.great() ? " (great)" : "") + ": biome "
                            + biome + ", rise " + rise + " in " + KoboldWarrenStructure.RISE_DISTANCE + ", mouth y " + m.getY()
                            + " (sea " + seaLevel + "), " + (water ? "under water" : "dry") + ", " + start.getChildren().size()
                            + " pieces (" + kinds.size() + " kinds): " + (ok ? "PASS" : "FAIL"));
                });
        if (stats[0] > 0) {
            String summary = String.format("[WarrenSurvey] great %d of %d (%.0f%%), mean rise %.1f in %d, mouths at y %d-%d",
                    stats[1], stats[0], 100.0D * stats[1] / stats[0], stats[2] / (double) stats[0],
                    KoboldWarrenStructure.RISE_DISTANCE, stats[3], stats[4]);
            DnDClasses.LOGGER.info(summary);
            context.getSource().sendFeedback(Text.literal(summary), false);
        }
        return pass;
    }

    static int seeds(CommandContext<ServerCommandSource> context, List<Long> seeds) {
        return HomeCommandUtil.seeds(context.getSource(), context.getSource().getWorld(), KoboldWarrens.KEY,
                KoboldWarrens.SET, seeds, 1500, "[WarrenSeeds]");
    }

    static int trap(CommandContext<ServerCommandSource> context, String kind, int tier) {
        ServerCommandSource source = context.getSource();
        TrapKind trapKind = switch (kind) {
            case "rockfall" -> TrapKind.ROCKFALL;
            case "spikes" -> TrapKind.SPIKES;
            case "pit" -> TrapKind.PIT;
            default -> TrapKind.DART;
        };
        BlockPos floor = BlockPos.ofFloored(source.getPosition()).down();
        Direction forward = Direction.fromRotation(source.getRotation().y);
        BlockPos trigger = KoboldTrapRigs.testRig(source.getWorld(), floor, forward, trapKind, tier);
        String line = "[Home] kobold " + trapKind.id() + " trap (tier " + tier + ") 3 blocks " + forward.asString()
                + " of " + floor.up().toShortString() + ", trigger " + trigger.toShortString();
        DnDClasses.LOGGER.info(line);
        source.sendFeedback(Text.literal(line), false);
        return 1;
    }

    static boolean isWarrenPiece(StructurePiece piece) {
        return piece instanceof WarrenPiece || piece instanceof BoltHolePiece;
    }
}
