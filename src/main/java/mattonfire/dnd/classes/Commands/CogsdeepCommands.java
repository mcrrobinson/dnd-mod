package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.ClockworkSentinelEntity;
import mattonfire.dnd.entity.GnomeTinkerEntity;
import mattonfire.dnd.world.gen.burrow.ClocktowerPiece;
import mattonfire.dnd.world.gen.burrow.CogsdeepPiece;
import mattonfire.dnd.world.gen.burrow.CogsdeepStructure;
import mattonfire.dnd.world.gen.burrow.CogsdeepStructures;
import mattonfire.dnd.world.gen.burrow.CommonsPiece;
import mattonfire.dnd.world.gen.burrow.GemRoomPiece;
import mattonfire.dnd.world.gen.burrow.GrottoPiece;
import mattonfire.dnd.world.gen.burrow.ObservatoryPiece;
import mattonfire.dnd.world.gen.burrow.ShaftPiece;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;

/**
 * The Cogsdeep parts of {@link DndHomeCommand}:
 * <ul>
 * <li>{@code /dndhome cogsdeep [door|top|observatory|stair|foot|commons|workshop|westworkshop|clocktower|strongbox|gems|grotto]}:
 * teleports you to that spot of the nearest burrow, looking at it.</li>
 * <li>{@code /dndhome cogsdeepsurvey [grid] [spacing]}: from a grid of points round you, finds the nearest burrow
 * to each and checks it (logged as {@code [CogsdeepSurvey]}): a forest, flower forest or meadow biome at the door,
 * all 7 piece kinds, the burrow's floor in y 11-16 with its tallest room under y 31, the observatory on the
 * ground (at most 4 of 16 points 8 blocks out more than 3 blocks above or below its floor), no water in the rooms (the grotto's pool aside), and no other structure starting within
 * {@link CogsdeepStructure#CLEARANCE} chunks (the forests' settlements and dungeons, vanilla villages).</li>
 * </ul>
 */
final class CogsdeepCommands {
    static final TagKey<Structure> COGSDEEPS = TagKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "cogsdeeps"));
    private static final int PIECE_KINDS = 7;
    /** Structures whose starts may lie near a burrow (they're everywhere, and underground or tiny); id prefixes. */
    private static final Set<String> IGNORED_NEIGHBOURS = Set.of("minecraft:mineshaft", "minecraft:ruined_portal",
            "minecraft:buried_treasure", "minecraft:stronghold", "minecraft:ancient_city", "minecraft:trail_ruins");

    private CogsdeepCommands() {
    }

    /** The start of the nearest burrow to {@code from} (within 100 chunks), or null. */
    private static StructureStart nearest(ServerWorld world, BlockPos from) {
        BlockPos found = world.locateStructure(COGSDEEPS, from, 100, false);
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(CogsdeepStructures.KEY);
        if (found == null || structure == null) {
            return null;
        }
        StructureStart start = world.getChunk(found.getX() >> 4, found.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS)
                .getStructureStart(structure);
        return start != null && start.hasChildren() ? start : null;
    }

    private static <P extends StructurePiece> P piece(StructureStart start, Class<P> type, int nth) {
        int seen = 0;
        for (StructurePiece piece : start.getChildren()) {
            if (type.isInstance(piece) && seen++ == nth) {
                return type.cast(piece);
            }
        }
        return null;
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        StructureStart start = nearest(world, player.getBlockPos());
        if (start == null) {
            context.getSource().sendError(Text.literal("[Home] No Cogsdeep within 100 chunks"));
            return 0;
        }
        ObservatoryPiece observatory = piece(start, ObservatoryPiece.class, 0);
        ShaftPiece shaft = piece(start, ShaftPiece.class, 0);
        CommonsPiece commons = piece(start, CommonsPiece.class, 0);
        ClocktowerPiece clocktower = piece(start, ClocktowerPiece.class, 0);
        GemRoomPiece gems = piece(start, GemRoomPiece.class, 0);
        GrottoPiece grotto = piece(start, GrottoPiece.class, 0);
        if (observatory == null || shaft == null || commons == null || clocktower == null || gems == null || grotto == null) {
            context.getSource().sendError(Text.literal("[Home] The Cogsdeep at chunk " + start.getPos() + " is missing pieces"));
            return 0;
        }
        BlockPos stand;
        BlockPos look;
        switch (spot) {
            case "top" -> {
                stand = observatory.at(0, 22, 14);
                look = observatory.at(0, 3, 0);
            }
            case "observatory" -> {
                stand = observatory.at(3, 0, 4);
                look = observatory.eyepiece();
            }
            case "stair" -> {
                // Halfway down, looking along the turn below.
                int half = shaft.top() / 2;
                stand = shaft.stepAt(half).up();
                look = shaft.stepAt(half + 4);
            }
            case "foot" -> {
                // In the Commons, a few blocks out from the stair's last step, looking up the last turn.
                int[] cell = ShaftPiece.RING[shaft.top() % ShaftPiece.RING.length];
                stand = shaft.at(cell[0] * 3, 0, cell[1] * 3);
                look = shaft.stepAt(shaft.top() - 3);
            }
            case "commons" -> {
                stand = commons.at(-5, 0, 6);
                look = commons.at(4, 1, -4);
            }
            case "workshop", "westworkshop" -> {
                int side = spot.equals("workshop") ? 1 : -1;
                stand = commons.at(side * 10, 0, 0);
                look = commons.at(side * 22, 1, 0);
            }
            case "clocktower" -> {
                stand = commons.at(0, 0, -10);
                look = clocktower.hub();
            }
            case "strongbox" -> {
                stand = commons.at(6, ClocktowerPiece.GALLERY, -24);
                look = clocktower.strongbox();
            }
            case "gems" -> {
                stand = commons.at(4, 0, 10);
                look = gems.gemChest();
            }
            case "grotto" -> {
                stand = commons.at(29, 0, 0);
                look = grotto.pool();
            }
            default -> {
                stand = observatory.outside();
                look = observatory.at(0, 1, 6);
            }
        }
        Vec3d eye = new Vec3d(stand.getX() + 0.5D, stand.getY() + player.getStandingEyeHeight(), stand.getZ() + 0.5D);
        Vec3d to = Vec3d.ofCenter(look).subtract(eye);
        float yaw = (float) (MathHelper.atan2(to.z, to.x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180.0D / Math.PI);
        player.teleport(world, stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, yaw, pitch);
        context.getSource().sendFeedback(Text.literal("[Home] Cogsdeep at chunk " + start.getPos() + " (ground y "
                + observatory.center().getY() + ", burrow floor y " + commons.center().getY() + "): at the " + spot
                + ", counter " + commons.counter().toShortString() + ", strongbox " + clocktower.strongbox().toShortString()
                + ", gem chest " + gems.gemChest().toShortString()), false);
        return 1;
    }

    /** The first air above the ground at x/z (logs, leaves, plants and snow don't count as ground). */
    private static int ground(ServerWorld world, BlockPos column) {
        world.getChunk(column);
        BlockPos.Mutable pos = new BlockPos.Mutable(column.getX(),
                world.getTopY(Heightmap.Type.WORLD_SURFACE, column.getX(), column.getZ()), column.getZ());
        while (pos.getY() > world.getBottomY()) {
            BlockState state = world.getBlockState(pos.move(0, -1, 0));
            if (!state.isAir() && !state.isIn(BlockTags.LOGS) && !state.isIn(BlockTags.LEAVES) && !state.isReplaceable()
                    && state.getFluidState().isEmpty()) {
                return pos.getY() + 1;
            }
        }
        return world.getBottomY();
    }

    /** The acceptance survey (see the class notes). Generates each burrow's chunks to look. Returns the number that pass. */
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
                ObservatoryPiece observatory = piece(start, ObservatoryPiece.class, 0);
                CommonsPiece commons = piece(start, CommonsPiece.class, 0);
                if (observatory == null || commons == null) {
                    continue;
                }
                BlockPos door = observatory.center();
                // The biome the generator placed it by (World.getBiome blurs the borders).
                var biome = world.getBiomeForNoiseGen(door.getX() >> 2, door.getY() >> 2, door.getZ() >> 2);
                String biomeId = biome.getKey().map(k -> k.getValue().toString()).orElse("?");
                boolean biomeOk = biomeId.equals("minecraft:forest") || biomeId.equals("minecraft:flower_forest")
                        || biomeId.equals("minecraft:meadow");
                Set<Identifier> kinds = new HashSet<>();
                start.getChildren().forEach(piece -> kinds.add(Registries.STRUCTURE_PIECE.getId(piece.getType())));
                int floor = commons.center().getY();
                int roof = floor + ClocktowerPiece.TOP;
                // On the ground: the ground just outside the wall (8 blocks out), all round, is within 3 of the floor.
                int offGround = 0;
                for (int i = 0; i < 16; i++) {
                    double angle = i * Math.PI / 8.0D;
                    BlockPos out = door.add((int) Math.round(Math.cos(angle) * 8), 0, (int) Math.round(Math.sin(angle) * 8));
                    if (Math.abs(ground(world, out) - door.getY()) > 3) {
                        offGround++;
                    }
                }
                int water = 0;
                for (StructurePiece piece : start.getChildren()) {
                    if (piece instanceof GrottoPiece || piece instanceof ObservatoryPiece || !(piece instanceof CogsdeepPiece)) {
                        continue;
                    }
                    BlockBox box = piece.getBoundingBox();
                    for (BlockPos pos : BlockPos.iterate(box.getMinX() + 1, box.getMinY() + 1, box.getMinZ() + 1,
                            box.getMaxX() - 1, box.getMaxY() - 1, box.getMaxZ() - 1)) {
                        BlockState state = world.getBlockState(pos);
                        if (!state.getFluidState().isEmpty()) {
                            water++;
                        }
                    }
                }
                Set<String> neighbours = new TreeSet<>();
                ChunkPos chunk = start.getPos();
                int r = CogsdeepStructure.CLEARANCE;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Map<Structure, StructureStart> starts = world.getChunk(chunk.x + dx, chunk.z + dz, ChunkStatus.STRUCTURE_STARTS)
                                .getStructureStarts();
                        starts.forEach((structure, other) -> {
                            if (other.hasChildren() && other != start) {
                                String id = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(structure).toString();
                                if (IGNORED_NEIGHBOURS.stream().noneMatch(id::startsWith)) {
                                    neighbours.add(id);
                                }
                            }
                        });
                    }
                }
                Box area = new Box(commons.at(-40, -2, -40), commons.at(50, 20, 40)).expand(4);
                int tinkers = world.getEntitiesByClass(GnomeTinkerEntity.class, area, e -> true).size();
                int sentinels = world.getEntitiesByClass(ClockworkSentinelEntity.class, area, e -> true).size();
                boolean ok = biomeOk && kinds.size() == PIECE_KINDS && floor >= CogsdeepStructure.MIN_FLOOR
                        && floor <= CogsdeepStructure.MAX_FLOOR && roof <= 30 && offGround <= 4 && water == 0
                        && neighbours.isEmpty();
                if (ok) {
                    pass++;
                } else {
                    fail++;
                }
                String line = "[CogsdeepSurvey] cogsdeep at " + door.getX() + " " + door.getY() + " " + door.getZ() + ": biome "
                        + biomeId + ", " + start.getChildren().size() + " pieces (" + kinds.size() + " kinds), burrow floor y "
                        + floor + " (roof " + roof + "), ground off by >3 at " + offGround + "/16, water in rooms " + water
                        + ", " + tinkers + " tinkers, " + sentinels + " sentinels, neighbours within " + r + " chunks "
                        + neighbours + ": " + (ok ? "PASS" : "FAIL");
                DnDClasses.LOGGER.info(line);
                source.sendFeedback(Text.literal(line), false);
            }
        }
        String summary = "[CogsdeepSurvey] " + (pass + fail) + " burrows, " + pass + " pass, " + fail + " fail";
        DnDClasses.LOGGER.info(summary);
        source.sendFeedback(Text.literal(summary), false);
        return pass;
    }
}
