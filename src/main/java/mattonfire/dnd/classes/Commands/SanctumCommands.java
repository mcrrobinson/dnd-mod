package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashSet;
import java.util.Set;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.sanctum.BrimstonePiece;
import mattonfire.dnd.world.gen.sanctum.BrimstoneSanctumStructure;
import mattonfire.dnd.world.gen.sanctum.BrimstoneSanctumStructures;
import mattonfire.dnd.world.gen.sanctum.PactHallPiece;
import mattonfire.dnd.world.gen.sanctum.RefugePiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
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
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.structure.Structure;

/**
 * The Brimstone Sanctum parts of {@link DndHomeCommand} (run them in the Nether, e.g. with
 * {@code /execute in minecraft:the_nether run ...}):
 * <ul>
 * <li>{@code /dndhome sanctum [hall|offering|anchor|market|den|gate|top|side]}: teleports you to that spot of the
 * nearest sanctum, looking at it.</li>
 * <li>{@code /dndhome sanctumsurvey [grid] [spacing]}: from a grid of points round you, finds the nearest sanctum
 * to each and checks it stands in a sanctum biome with all its pieces, never over lava, with no cut ceiling over
 * more than {@link #MAX_CUT} of the platform (logged as {@code [SanctumSurvey]}).</li>
 * </ul>
 */
final class SanctumCommands {
    static final TagKey<Structure> SANCTUMS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "brimstone_sanctums"));
    /** Piece types every sanctum must have. */
    private static final int PIECE_KINDS = 5;
    /** The share of platform columns that may have rock right above the cleared space. */
    static final double MAX_CUT = 0.15D;
    /** How far below the platform the survey looks for lava. */
    private static final int LAVA_SCAN = 24;

    private SanctumCommands() {
    }

    /** The start of the nearest sanctum to {@code from} (within 100 chunks), or null. */
    private static StructureStart nearest(ServerWorld world, BlockPos from) {
        return HomeCommandUtil.nearestStart(world, SANCTUMS, BrimstoneSanctumStructures.KEY, from);
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        StructureStart start = nearest(world, player.getBlockPos());
        if (start == null) {
            context.getSource().sendError(Text.literal("[Home] No Brimstone Sanctum within 100 chunks (run this in the Nether)"));
            return 0;
        }
        PactHallPiece hall = null;
        RefugePiece refuge = null;
        BrimstonePiece any = null;
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof PactHallPiece h) {
                hall = h;
            } else if (piece instanceof RefugePiece r) {
                refuge = r;
            }
            if (piece instanceof BrimstonePiece b) {
                any = b;
            }
        }
        if (hall == null || refuge == null || any == null) {
            context.getSource().sendError(Text.literal("[Home] The sanctum at chunk " + start.getPos() + " is missing pieces"));
            return 0;
        }
        BlockPos stand;
        BlockPos look;
        switch (spot) {
            case "offering" -> {
                stand = any.at(0, 1, -11);
                look = hall.offering();
            }
            case "anchor" -> {
                stand = refuge.beforeAnchor();
                look = refuge.anchors().get(0);
            }
            case "market" -> {
                stand = any.at(4, 1, 0);
                look = any.at(10, 1, 0);
            }
            case "den" -> {
                stand = any.at(-9, 1, -4);
                look = any.at(-12, 1, -8);
            }
            case "gate" -> {
                stand = any.at(0, 3, BrimstoneSanctumStructure.PLATFORM + 8);
                look = any.at(0, 3, 0);
            }
            case "top" -> {
                // As high as the cavern allows (at most 22 up: the Nether's fog hides anything further), in front
                // of the middle: the Nether has a roof.
                int y = 16;
                while (y < 23 && world.getBlockState(any.at(0, y + 2, 12)).isAir()) {
                    y++;
                }
                stand = any.at(0, y, 12);
                look = any.at(0, 0, -4);
            }
            case "side" -> {
                stand = any.at(BrimstoneSanctumStructure.PLATFORM + 22, 4, 0);
                look = any.at(0, -4, 0);
            }
            default -> {
                stand = any.at(-2, 1, -6);
                look = hall.altar();
            }
        }
        Vec3d eye = new Vec3d(stand.getX() + 0.5D, stand.getY() + player.getStandingEyeHeight(), stand.getZ() + 0.5D);
        Vec3d to = Vec3d.ofCenter(look).subtract(eye);
        float yaw = (float) (MathHelper.atan2(to.z, to.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0D / Math.PI);
        player.teleport(world, stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, yaw, pitch);
        context.getSource().sendFeedback(Text.literal("[Home] Brimstone Sanctum at chunk " + start.getPos() + " (platform y "
                + any.center().getY() + "): at the " + spot + ", altar " + hall.altar().toShortString() + ", offering "
                + hall.offering().toShortString() + ", anchor " + refuge.anchors().get(0).toShortString()), false);
        return 1;
    }

    /**
     * The acceptance survey: every sanctum found from a grid x grid of points {@code spacing} blocks apart round
     * the source must stand in a crimson forest or soul sand valley, have all its pieces, have no lava under any
     * platform column within {@link #LAVA_SCAN} blocks (it never floats over the lava sea), and have rock right
     * above the cleared space over at most {@link #MAX_CUT} of the platform (it doesn't clip the ceiling).
     * Generates the sanctum's chunks to look. Returns the number that pass.
     */
    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld world = context.getSource().getWorld();
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[SanctumSurvey]", "sanctums",
                from -> nearest(world, from), start -> start.getPos().toLong(), start -> {
                    if (!(start.getChildren().get(0) instanceof BrimstonePiece first)) {
                        return null;
                    }
                    BlockPos middle = first.center();
                    var biome = world.getBiome(middle.up());
                    boolean biomeOk = biome.matchesKey(BiomeKeys.CRIMSON_FOREST) || biome.matchesKey(BiomeKeys.SOUL_SAND_VALLEY);
                    Set<Identifier> kinds = new HashSet<>();
                    start.getChildren().forEach(piece -> kinds.add(Registries.STRUCTURE_PIECE.getId(piece.getType())));
                    int r = BrimstoneSanctumStructure.PLATFORM;
                    int columns = 0;
                    int lava = 0;
                    int cut = 0;
                    int roof = middle.getY() + 16;
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dz * dz > (r + 0.5D) * (r + 0.5D)) {
                                continue;
                            }
                            columns++;
                            BlockPos top = middle.add(dx, 0, dz);
                            if (lavaBelow(world, top)) {
                                lava++;
                            }
                            BlockState above = world.getBlockState(new BlockPos(top.getX(), roof, top.getZ()));
                            if (!above.isAir() && above.getFluidState().isEmpty()) {
                                cut++;
                            }
                        }
                    }
                    boolean ok = biomeOk && kinds.size() == PIECE_KINDS && lava == 0 && cut <= columns * MAX_CUT;
                    return new HomeCommandUtil.Check(ok, "[SanctumSurvey] sanctum at " + middle.getX() + " " + middle.getY() + " " + middle.getZ()
                            + ": biome " + biome.getKey().map(k -> k.getValue().toString()).orElse("?")
                            + ", " + start.getChildren().size() + " pieces (" + kinds.size() + " kinds), lava under " + lava
                            + " of " + columns + " columns, cut ceiling over " + cut + ": " + (ok ? "PASS" : "FAIL"));
                });
    }

    /** Whether lava lies under the platform block at {@code top}, through its pillars and the air below. */
    private static boolean lavaBelow(ServerWorld world, BlockPos top) {
        BlockPos.Mutable pos = top.mutableCopy().move(0, -2, 0);
        for (int i = 0; i < LAVA_SCAN; i++, pos.move(0, -1, 0)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.LAVA) || !state.getFluidState().isEmpty()) {
                return true;
            }
            boolean fill = state.isOf(Blocks.BASALT) || state.isOf(Blocks.BLACKSTONE);
            if (!state.isAir() && !fill) {
                return false;
            }
        }
        return false;
    }
}
