package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.entity.drow.DrowEntity;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.drow.DrowBridgePiece;
import mattonfire.dnd.world.underdark.drow.DrowSpireCavernPiece;
import mattonfire.dnd.world.underdark.drow.DrowSpirePiece;
import mattonfire.dnd.world.underdark.drow.DrowSpires;
import mattonfire.dnd.world.underdark.drow.DrowTowerPiece;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * The Drow Spire parts of {@link DndHomeCommand} (design 9 T9; docs/structures/drow-spire.md). They work from
 * anywhere: outside the Underdark they search below your x and z.
 * <ul>
 * <li>{@code /dndhome drowspire [sanctum|throne|above|overview|door|hall|cells|library|loftstair|loft|pit|bridge|tower]}:
 * teleports you into the nearest spire (the Sanctum's front by default, looking at the throne) and prints its layout.</li>
 * <li>{@code /dndhome drowspiresurvey [grid] [spacing]}: from a grid of points round you, finds the nearest spire to each
 * and checks it ({@code [DrowSpireSurvey]}): the biome and floor height, every piece, then (generating its chunks)
 * every step of the main and side stairs with two blocks of headroom, the Sanctum's floor and air, every bridge's deck,
 * the Arcane Seal, the lectern's book, the pit's spawner, and no water in the dome.</li>
 * <li>{@code /dndhome drowspirecensus}: who lives in the nearest spire.</li>
 * <li>{@code /dndhome drowspireseeds [seed...]}: for each seed, the nearest spire to 0, 0 that would generate
 * ({@code [DrowSpireSeeds]}).</li>
 * </ul>
 */
final class DrowSpireCommands {
    static final TagKey<Biome> BIOMES = TagKey.of(RegistryKeys.BIOME, new Identifier("dndclasses", "has_structure/drow_spire"));
    /** How far from 0, 0 the seed check looks: the spires are rare. */
    static final int SEED_RADIUS = 4000;

    private DrowSpireCommands() {
    }

    record Layout(StructureStart start, DrowSpireCavernPiece cavern, DrowSpirePiece spire, List<DrowTowerPiece> towers,
                  List<DrowBridgePiece> bridges) {
        static Layout of(StructureStart start) {
            DrowSpireCavernPiece cavern = null;
            DrowSpirePiece spire = null;
            List<DrowTowerPiece> towers = new ArrayList<>();
            List<DrowBridgePiece> bridges = new ArrayList<>();
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof DrowSpireCavernPiece p) {
                    cavern = p;
                } else if (piece instanceof DrowSpirePiece p) {
                    spire = p;
                } else if (piece instanceof DrowTowerPiece p) {
                    towers.add(p);
                } else if (piece instanceof DrowBridgePiece p) {
                    bridges.add(p);
                }
            }
            return new Layout(start, cavern, spire, towers, bridges);
        }

        boolean complete() {
            return this.cavern != null && this.spire != null && this.towers.size() == 3 && this.bridges.size() == 3;
        }

        BlockPos center() {
            return this.spire != null ? this.spire.center() : this.start.getBoundingBox().getCenter();
        }
    }

    @Nullable
    private static Layout nearest(ServerWorld underdark, BlockPos from) {
        StructureStart start = HomeCommandUtil.nearestStart(underdark, DrowSpires.TAG, DrowSpires.KEY, from);
        return start == null ? null : Layout.of(start);
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[Home] This world has no Underdark"));
            return 0;
        }
        Layout layout = nearest(underdark, new BlockPos(player.getBlockX(), -160, player.getBlockZ()));
        if (layout == null || !layout.complete()) {
            context.getSource().sendError(Text.literal("[Home] No Drow Spire within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        DrowSpirePiece spire = layout.spire();
        BlockPos center = layout.center();
        DrowTowerPiece tower = layout.towers().get(0);
        Vec3d feet;
        Vec3d look;
        switch (spot) {
            case "sanctum" -> {
                feet = Vec3d.ofBottomCenter(spire.sanctumSpot());
                look = Vec3d.ofCenter(spire.matronSpot().up());
            }
            case "throne" -> {
                feet = Vec3d.ofBottomCenter(spire.matronSpot().offset(spire.front(), 2));
                look = Vec3d.ofCenter(spire.matronSpot().up());
            }
            case "above" -> {
                BlockPos at = spire.at(0, 0, DrowSpireCavernPiece.RADIUS - 6).up(DrowSpireCavernPiece.height(DrowSpireCavernPiece.RADIUS - 6) - 3);
                feet = Vec3d.ofBottomCenter(at);
                look = Vec3d.ofCenter(center.up(16));
            }
            case "overview" -> {
                BlockPos at = spire.at(0, 0, DrowSpireCavernPiece.RADIUS - 3).up(4);
                feet = Vec3d.ofBottomCenter(at);
                look = Vec3d.ofCenter(center.up(18));
            }
            case "door" -> {
                feet = Vec3d.ofBottomCenter(spire.doorOutside().offset(spire.front(), 4));
                look = Vec3d.ofCenter(spire.doorOutside().up(1));
            }
            case "hall" -> {
                feet = Vec3d.ofBottomCenter(spire.hallSpot());
                look = Vec3d.ofCenter(center.up(2));
            }
            case "cells" -> {
                feet = Vec3d.ofBottomCenter(spire.cellsSpot());
                look = Vec3d.ofCenter(center.up(DrowSpirePiece.CELLS + 2));
            }
            case "library" -> {
                feet = Vec3d.ofBottomCenter(spire.librarySpot());
                look = Vec3d.ofCenter(spire.lectern());
            }
            case "loftstair" -> {
                feet = Vec3d.ofBottomCenter(spire.loftStairFoot());
                look = Vec3d.ofCenter(spire.seal().get(0));
            }
            case "loft" -> {
                feet = Vec3d.ofBottomCenter(spire.loftSpot());
                look = Vec3d.ofCenter(spire.loftChest());
            }
            case "pit" -> {
                feet = Vec3d.ofBottomCenter(layout.cavern().spawner().up(DrowSpireCavernPiece.PIT_DEPTH + 1)
                        .offset(spire.front(), 5));
                look = Vec3d.ofCenter(layout.cavern().spawner());
            }
            case "bridge" -> {
                DrowBridgePiece bridge = layout.bridges().get(0);
                feet = Vec3d.ofBottomCenter(bridge.middle());
                look = Vec3d.ofCenter(center.up(DrowSpirePiece.CELLS + 2));
            }
            case "tower" -> {
                feet = Vec3d.ofBottomCenter(tower.platformSpot());
                look = Vec3d.ofCenter(center.up(DrowSpirePiece.CELLS + 2));
            }
            default -> {
                context.getSource().sendError(Text.literal("[Home] Which spot? sanctum, throne, above, overview, door, hall, "
                        + "cells, library, loftstair, loft, pit, bridge or tower"));
                return 0;
            }
        }
        Vec3d d = look.subtract(feet.add(0.0D, player.getStandingEyeHeight(), 0.0D));
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 57.2957763671875D) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.2957763671875D);
        player.teleport(underdark, feet.x, feet.y, feet.z, yaw, pitch);
        String biome = underdark.getBiome(center).getKey().map(k -> k.getValue().toString()).orElse("?");
        context.getSource().sendFeedback(Text.literal("[Home] Drow Spire at chunk " + layout.start().getPos() + ": centre "
                + center.toShortString() + " in " + biome + ", front " + spire.front().asString() + ", matron "
                + spire.matronSpot().toShortString() + ", seal " + spire.seal().get(0).toShortString() + ", lectern "
                + spire.lectern().toShortString() + ", pit spawner " + layout.cavern().spawner().toShortString()
                + "; at " + spot + " " + BlockPos.ofFloored(feet).toShortString()), false);
        return 1;
    }

    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[DrowSpireSurvey] This world has no Underdark"));
            return 0;
        }
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[DrowSpireSurvey]", "spires",
                from -> nearest(underdark, new BlockPos(from.getX(), -160, from.getZ())),
                layout -> layout.start().getPos().toLong(), layout -> check(underdark, layout));
    }

    private static boolean walkable(ServerWorld world, BlockPos step) {
        return world.getBlockState(step).getBlock() instanceof StairsBlock
                && world.getBlockState(step.up()).getCollisionShape(world, step.up()).isEmpty()
                && world.getBlockState(step.up(2)).getCollisionShape(world, step.up(2)).isEmpty();
    }

    private static HomeCommandUtil.Check check(ServerWorld world, Layout layout) {
        BlockPos center = layout.center();
        RegistryEntry<Biome> biome = world.getBiome(center);
        boolean biomeOk = biome.isIn(BIOMES);
        int floor = center.getY() - 1;
        if (!layout.complete()) {
            return new HomeCommandUtil.Check(false, "[DrowSpireSurvey] spire at chunk " + layout.start().getPos()
                    + ": pieces missing (towers " + layout.towers().size() + ", bridges " + layout.bridges().size() + "): FAIL");
        }
        BlockBox box = layout.start().getBoundingBox();
        for (int cx = box.getMinX() >> 4; cx <= box.getMaxX() >> 4; cx++) {
            for (int cz = box.getMinZ() >> 4; cz <= box.getMaxZ() >> 4; cz++) {
                world.getChunk(cx, cz);
            }
        }
        DrowSpirePiece spire = layout.spire();
        List<String> notes = new ArrayList<>();
        int steps = 0;
        int stepsClear = 0;
        for (BlockPos step : spire.steps()) {
            steps++;
            if (walkable(world, step)) {
                stepsClear++;
            } else if (notes.size() < 4) {
                notes.add("step " + step.toShortString());
            }
        }
        // The side stair: its top three steps run up under the seal, so only the lower ones need headroom now.
        int loftSteps = 0;
        int loftClear = 0;
        List<BlockPos> side = spire.loftSteps();
        for (int i = 0; i < side.size(); i++) {
            BlockPos step = side.get(i);
            loftSteps++;
            boolean ok = i >= 4 ? world.getBlockState(step).getBlock() instanceof StairsBlock : walkable(world, step);
            if (ok) {
                loftClear++;
            } else if (notes.size() < 6) {
                notes.add("loft step " + step.toShortString());
            }
        }
        int seal = 0;
        for (BlockPos pos : spire.seal()) {
            if (world.getBlockState(pos).getBlock() instanceof ObstacleBlock) {
                seal++;
            }
        }
        // The Sanctum: floor under every cell of radius 7 and 6 blocks of air over it (bar furniture and the stair gap).
        int sanctumOpen = 0;
        int sanctumCells = 0;
        BlockPos sanctumFloor = center.up(DrowSpirePiece.SANCTUM);
        for (int a = -7; a <= 7; a++) {
            for (int c = -7; c <= 7; c++) {
                if (a * a + c * c > 49 || Math.max(Math.abs(a), Math.abs(c)) <= 2) {
                    continue;
                }
                sanctumCells++;
                BlockPos at = sanctumFloor.add(a, 0, c);
                boolean ok = !world.getBlockState(at).isAir();
                for (int y = 2; y <= 6 && ok; y++) {
                    ok = !world.getBlockState(at.up(y)).isOpaqueFullCube(world, at.up(y));
                }
                if (ok) {
                    sanctumOpen++;
                }
            }
        }
        int deckMissing = 0;
        int deckTotal = 0;
        for (DrowBridgePiece bridge : layout.bridges()) {
            for (BlockPos pos : bridge.deck()) {
                deckTotal++;
                if (world.getBlockState(pos).isAir() || !world.getFluidState(pos).isEmpty()) {
                    deckMissing++;
                }
            }
        }
        boolean book = world.getBlockState(spire.lectern()).isOf(Blocks.LECTERN)
                && world.getBlockState(spire.lectern()).get(LecternBlock.HAS_BOOK)
                && world.getBlockEntity(spire.lectern()) instanceof LecternBlockEntity lectern && lectern.hasBook();
        boolean spawner = world.getBlockState(layout.cavern().spawner()).isOf(Blocks.SPAWNER);
        int wet = 0;
        for (int a = -24; a <= 24; a++) {
            for (int c = -24; c <= 24; c++) {
                if (a * a + c * c > 576) {
                    continue;
                }
                for (int y = 0; y <= 6; y += 2) {
                    if (!world.getFluidState(center.add(a, y, c)).isEmpty()) {
                        wet++;
                    }
                }
            }
        }
        boolean ok = biomeOk && floor >= -220 && floor <= -110 && stepsClear == steps && loftClear == loftSteps && seal == 3
                && sanctumOpen >= sanctumCells * 9 / 10 && deckMissing == 0 && book && spawner && wet == 0;
        String name = biome.getKey().map(k -> k.getValue().getPath()).orElse("?");
        return new HomeCommandUtil.Check(ok, "[DrowSpireSurvey] spire at chunk " + layout.start().getPos() + " (centre "
                + center.toShortString() + ", " + name + (biomeOk ? "" : " WRONG BIOME") + ", floor y " + floor + "): stairs "
                + stepsClear + "/" + steps + " clear, loft stair " + loftClear + "/" + loftSteps + ", seal " + seal + "/3, sanctum "
                + sanctumOpen + "/" + sanctumCells + " open, deck " + (deckTotal - deckMissing) + "/" + deckTotal + ", book "
                + book + ", spawner " + spawner + ", wet blocks " + wet
                + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")") + ": " + (ok ? "PASS" : "FAIL"));
    }

    /** {@code /dndhome drowspirecensus}: the nearest spire's drow by kind (and how many are persistent) and its spiders. */
    static int census(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        Layout layout = underdark == null ? null : nearest(underdark, new BlockPos(player.getBlockX(), -160, player.getBlockZ()));
        if (layout == null) {
            context.getSource().sendError(Text.literal("[DrowSpireCensus] No Drow Spire within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        Box box = Box.from(layout.start().getBoundingBox()).expand(4.0D);
        int[] kinds = new int[5];
        int persistent = 0;
        int drow = 0;
        int spiders = 0;
        for (Entity entity : underdark.getEntitiesByClass(Entity.class, box, Entity::isAlive)) {
            if (entity instanceof DrowEntity d) {
                drow++;
                if (d.isPersistent()) {
                    persistent++;
                }
                EntityType<?> type = d.getType();
                kinds[type == Drow.DROW_WARRIOR ? 0 : type == Drow.DROW_MAGE ? 1 : type == Drow.DROW_PRIESTESS ? 2
                        : type == Drow.DROW_ELITE ? 3 : 4]++;
            } else if (entity.getType() == EntityType.CAVE_SPIDER) {
                spiders++;
            }
        }
        String line = "[DrowSpireCensus] spire at chunk " + layout.start().getPos() + ": drow " + drow + " (warriors "
                + kinds[0] + ", mages " + kinds[1] + ", priestesses " + kinds[2] + ", elites " + kinds[3] + ", matron "
                + kinds[4] + "; persistent " + persistent + "), cave spiders " + spiders;
        mattonfire.dnd.classes.DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return drow;
    }

    static int seeds(CommandContext<ServerCommandSource> context, List<Long> seeds) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[DrowSpireSeeds] This world has no Underdark"));
            return 0;
        }
        return HomeCommandUtil.seeds(context.getSource(), underdark, DrowSpires.KEY, DrowSpires.SET, seeds, SEED_RADIUS,
                "[DrowSpireSeeds]");
    }
}
