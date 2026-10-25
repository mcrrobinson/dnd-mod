package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.drow.DrowBarracksPiece;
import mattonfire.dnd.world.underdark.drow.DrowBridgePiece;
import mattonfire.dnd.world.underdark.drow.DrowGroundsPiece;
import mattonfire.dnd.world.underdark.drow.DrowOutposts;
import mattonfire.dnd.world.underdark.drow.DrowPenPiece;
import mattonfire.dnd.world.underdark.drow.DrowShrinePiece;
import mattonfire.dnd.world.underdark.drow.DrowTowerPiece;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * The Drow Outpost parts of {@link DndHomeCommand} (design 9 T7; docs/structures/drow-outposts.md). They work from
 * anywhere: outside the Underdark they search below your x and z.
 * <ul>
 * <li>{@code /dndhome drowoutpost [shrine|above|overview|pen|barracks|tower|inside|top|bridge|reliquary|trap]}: teleports you
 * into the nearest outpost: in front of the altar, high over the middle looking down, at the cavern's edge looking
 * across, outside the first pen gate, in the barracks, on a tower's ground floor (or looking up its stair), on a platform (a lookout's), on the
 * first bridge, behind the idol facing the reliquary, or just past the dart trap. Prints its layout.</li>
 * <li>{@code /dndhome drowoutpostsurvey [grid] [spacing]}: from a grid of points round you, finds the nearest outpost
 * to each and checks it (logged as {@code [DrowOutpostSurvey]}): the biome, the floor's height, every piece, then
 * (generating its chunks) every tower's inner shell and every bridge's deck whole across chunk borders, every step of every stair with
 * two blocks of headroom, the pen's
 * locked gates and no water on the levelled floor.</li>
 * <li>{@code /dndhome drowoutpostcensus}: who lives in the nearest outpost ({@link #census}).</li>
 * <li>{@code /dndhome drowoutpostseeds [seed...]}: for each seed (this world's and two others by default) finds the
 * nearest outpost to 0, 0 that would generate, and its biome, without making a world ({@code [DrowOutpostSeeds]}).</li>
 * </ul>
 */
final class DrowOutpostCommands {
    static final TagKey<Biome> BIOMES = TagKey.of(RegistryKeys.BIOME, new Identifier("dndclasses", "has_structure/drow_outpost"));
    /** How far from 0, 0 the seed check looks. */
    static final int SEED_RADIUS = 2000;

    private DrowOutpostCommands() {
    }

    /** The pieces of one outpost. */
    record Layout(StructureStart start, DrowGroundsPiece grounds, DrowShrinePiece shrine, DrowPenPiece pen,
                  DrowBarracksPiece barracks, List<DrowTowerPiece> towers, List<DrowBridgePiece> bridges) {
        static Layout of(StructureStart start) {
            DrowGroundsPiece grounds = null;
            DrowShrinePiece shrine = null;
            DrowPenPiece pen = null;
            DrowBarracksPiece barracks = null;
            List<DrowTowerPiece> towers = new ArrayList<>();
            List<DrowBridgePiece> bridges = new ArrayList<>();
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof DrowGroundsPiece p) {
                    grounds = p;
                } else if (piece instanceof DrowShrinePiece p) {
                    shrine = p;
                } else if (piece instanceof DrowPenPiece p) {
                    pen = p;
                } else if (piece instanceof DrowBarracksPiece p) {
                    barracks = p;
                } else if (piece instanceof DrowTowerPiece p) {
                    towers.add(p);
                } else if (piece instanceof DrowBridgePiece p) {
                    bridges.add(p);
                }
            }
            return new Layout(start, grounds, shrine, pen, barracks, towers, bridges);
        }

        boolean complete() {
            return this.grounds != null && this.shrine != null && this.pen != null && this.barracks != null
                    && this.towers.size() >= 3 && this.towers.size() <= 5 && this.bridges.size() == this.towers.size();
        }

        BlockPos center() {
            return this.shrine != null ? this.shrine.center() : this.start.getBoundingBox().getCenter();
        }
    }

    @Nullable
    private static Layout nearest(ServerWorld underdark, BlockPos from) {
        StructureStart start = HomeCommandUtil.nearestStart(underdark, DrowOutposts.TAG, DrowOutposts.KEY, from);
        return start == null ? null : Layout.of(start);
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[Home] This world has no Underdark"));
            return 0;
        }
        BlockPos from = new BlockPos(player.getBlockX(), -150, player.getBlockZ());
        Layout layout = nearest(underdark, from);
        if (layout == null || !layout.complete()) {
            context.getSource().sendError(Text.literal("[Home] No Drow Outpost within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        BlockPos center = layout.center();
        DrowTowerPiece tower = layout.towers().get(0);
        DrowTowerPiece lookout = layout.towers().stream().filter(DrowTowerPiece::lookout).findFirst().orElse(tower);
        Vec3d feet;
        Vec3d look;
        switch (spot) {
            case "shrine" -> {
                feet = Vec3d.ofBottomCenter(layout.shrine().altarSpot());
                look = Vec3d.ofCenter(center.up(2));
            }
            case "above" -> {
                BlockPos top = highestAir(underdark, center.up(2), 40);
                feet = Vec3d.ofBottomCenter(top.down());
                look = feet.add(0.001D, -10.0D, 0.0D);
            }
            case "overview" -> {
                // The highest open spot 14-22 blocks out round the middle, looking back at the shrine.
                BlockPos best = center.up(2);
                for (int deg = 0; deg < 360; deg += 30) {
                    for (int r = 14; r <= 22; r += 4) {
                        BlockPos edge = center.add((int) Math.round(Math.cos(Math.toRadians(deg)) * r), 2,
                                (int) Math.round(Math.sin(Math.toRadians(deg)) * r));
                        if (!underdark.getBlockState(edge).isAir()) {
                            continue;
                        }
                        BlockPos top = highestAir(underdark, edge, 40);
                        if (top.getY() > best.getY() || top.getY() == best.getY() && top.getSquaredDistance(center) > best.getSquaredDistance(center)) {
                            best = top;
                        }
                    }
                }
                feet = Vec3d.ofBottomCenter(best.down(2));
                look = Vec3d.ofCenter(center);
            }
            case "inside" -> {
                feet = Vec3d.ofBottomCenter(tower.at(1, 0, 1));
                look = Vec3d.ofCenter(tower.at(-1, tower.platform(), -1));
            }
            case "pen" -> {
                feet = Vec3d.ofBottomCenter(layout.pen().outside(0));
                look = Vec3d.ofCenter(layout.pen().gate(0));
            }
            case "barracks" -> {
                feet = Vec3d.ofBottomCenter(layout.barracks().inside());
                look = Vec3d.ofCenter(layout.barracks().center());
            }
            case "tower" -> {
                feet = Vec3d.ofBottomCenter(tower.groundSpot());
                look = Vec3d.ofCenter(tower.at(0, 1, -2));
            }
            case "top" -> {
                feet = Vec3d.ofBottomCenter(lookout.platformSpot());
                look = Vec3d.ofCenter(center);
            }
            case "bridge" -> {
                DrowBridgePiece bridge = layout.bridges().get(0);
                feet = Vec3d.ofBottomCenter(bridge.middle());
                look = Vec3d.ofCenter(bridge.center().up(lookout.platform()));
            }
            case "reliquary" -> {
                feet = Vec3d.ofBottomCenter(layout.shrine().at(0, 0, -4));
                look = Vec3d.ofCenter(layout.shrine().reliquary());
            }
            case "trap" -> {
                feet = Vec3d.ofBottomCenter(layout.shrine().at(0, 0, DrowShrinePiece.TRAP_B + 3));
                look = Vec3d.ofCenter(layout.shrine().trapPlate());
            }
            default -> {
                context.getSource().sendError(Text.literal("[Home] Which spot? shrine, above, overview, pen, barracks, tower, "
                        + "inside, top, bridge, reliquary or trap"));
                return 0;
            }
        }
        Vec3d d = look.subtract(feet.add(0.0D, player.getStandingEyeHeight(), 0.0D));
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 57.2957763671875D) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.2957763671875D);
        player.teleport(underdark, feet.x, feet.y, feet.z, yaw, pitch);
        String biome = underdark.getBiome(center).getKey().map(k -> k.getValue().toString()).orElse("?");
        context.getSource().sendFeedback(Text.literal("[Home] Drow Outpost at chunk " + layout.start().getPos() + ": centre "
                + center.toShortString() + " in " + biome + ", " + layout.towers().size() + " towers (platform "
                + tower.platform() + "), pen gate " + layout.pen().gate(0).toShortString() + ", reliquary "
                + layout.shrine().reliquary().toShortString() + ", trap " + layout.shrine().trapPlate().toShortString()
                + "; at " + spot + " " + BlockPos.ofFloored(feet).toShortString()), false);
        return 1;
    }

    /** The highest air block over {@code from} before rock, at most {@code up} blocks. */
    private static BlockPos highestAir(ServerWorld world, BlockPos from, int up) {
        BlockPos pos = from;
        for (int i = 0; i < up && world.getBlockState(pos.up()).isAir(); i++) {
            pos = pos.up();
        }
        return pos;
    }

    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[DrowOutpostSurvey] This world has no Underdark"));
            return 0;
        }
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[DrowOutpostSurvey]", "outposts",
                from -> nearest(underdark, new BlockPos(from.getX(), -150, from.getZ())),
                layout -> layout.start().getPos().toLong(), layout -> check(underdark, layout));
    }

    private static HomeCommandUtil.Check check(ServerWorld world, Layout layout) {
        BlockPos center = layout.center();
        RegistryEntry<Biome> biome = world.getBiome(center);
        boolean biomeOk = biome.isIn(BIOMES);
        int floor = center.getY() - 1;
        boolean floorOk = floor >= -220 && floor <= -110;
        if (!layout.complete()) {
            return new HomeCommandUtil.Check(false, "[DrowOutpostSurvey] outpost at chunk " + layout.start().getPos()
                    + ": pieces missing (towers " + layout.towers().size() + ", bridges " + layout.bridges().size() + "): FAIL");
        }
        // Generate every chunk it covers, then check the blocks that cross chunk borders.
        BlockBox box = layout.start().getBoundingBox();
        for (int cx = box.getMinX() >> 4; cx <= box.getMaxX() >> 4; cx++) {
            for (int cz = box.getMinZ() >> 4; cz <= box.getMaxZ() >> 4; cz++) {
                world.getChunk(cx, cz);
            }
        }
        int shellMissing = 0;
        int shellTotal = 0;
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        for (DrowTowerPiece tower : layout.towers()) {
            for (BlockPos pos : tower.shell()) {
                shellTotal++;
                if (!world.getBlockState(pos).equals(bricks)) {
                    shellMissing++;
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
        int stepsClear = 0;
        int stepsTotal = 0;
        List<String> blocked = new ArrayList<>();
        for (DrowTowerPiece tower : layout.towers()) {
            for (BlockPos step : tower.steps()) {
                stepsTotal++;
                if (world.getBlockState(step).getBlock() instanceof net.minecraft.block.StairsBlock
                        && world.getBlockState(step.up()).getCollisionShape(world, step.up()).isEmpty()
                        && world.getBlockState(step.up(2)).getCollisionShape(world, step.up(2)).isEmpty()) {
                    stepsClear++;
                } else if (blocked.size() < 4) {
                    blocked.add(step.toShortString());
                }
            }
        }
        int gates = 0;
        for (int i = 0; i < 3; i++) {
            for (int y = 0; y <= 1; y++) {
                BlockState gate = world.getBlockState(layout.pen().gate(i).up(y));
                if (gate.isOf(UnderdarkBlocks.CHAIN_CAGE) && gate.get(ChainCageBlock.LOCKED)) {
                    gates++;
                } else if (blocked.size() < 8) {
                    blocked.add("gate " + layout.pen().gate(i).up(y).toShortString() + " " + gate.getBlock().getTranslationKey()
                            + (gate.isOf(UnderdarkBlocks.CHAIN_CAGE) ? " " + gate : ""));
                }
            }
        }
        int wet = 0;
        for (int a = -20; a <= 20; a++) {
            for (int c = -20; c <= 20; c++) {
                if (a * a + c * c > 400) {
                    continue;
                }
                for (int y = 0; y <= 3; y++) {
                    if (!world.getFluidState(center.add(a, y, c)).isEmpty()) {
                        wet++;
                    }
                }
            }
        }
        boolean ok = biomeOk && floorOk && shellMissing == 0 && deckMissing == 0 && stepsClear == stepsTotal && gates == 6
                && wet == 0;
        String name = biome.getKey().map(k -> k.getValue().getPath()).orElse("?");
        return new HomeCommandUtil.Check(ok, "[DrowOutpostSurvey] outpost at chunk " + layout.start().getPos() + " (centre "
                + center.toShortString() + ", " + name + (biomeOk ? "" : " WRONG BIOME") + ", floor y " + floor + "): "
                + layout.towers().size() + " towers, " + layout.bridges().size() + " bridges, shell "
                + (shellTotal - shellMissing) + "/" + shellTotal + ", deck " + (deckTotal - deckMissing) + "/" + deckTotal
                + ", stairs " + stepsClear + "/" + stepsTotal + " clear, locked gates " + gates + "/6, wet blocks " + wet
                + (blocked.isEmpty() ? "" : " (" + String.join("; ", blocked) + ")") + ": " + (ok ? "PASS" : "FAIL"));
    }

    /**
     * {@code /dndhome drowoutpostcensus}: counts who lives in the nearest outpost (its whole box, 8 blocks round it):
     * drow by kind and how many of them are persistent, cave spiders, captives. Logged as {@code [DrowOutpostCensus]}.
     */
    static int census(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        Layout layout = underdark == null ? null : nearest(underdark, new BlockPos(player.getBlockX(), -150, player.getBlockZ()));
        if (layout == null) {
            context.getSource().sendError(Text.literal("[DrowOutpostCensus] No Drow Outpost within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        net.minecraft.util.math.Box box = net.minecraft.util.math.Box.from(layout.start().getBoundingBox()).expand(8.0D);
        List<net.minecraft.entity.Entity> all = underdark.getEntitiesByClass(net.minecraft.entity.Entity.class, box,
                net.minecraft.entity.Entity::isAlive);
        int warriors = 0;
        int mages = 0;
        int priestesses = 0;
        int persistent = 0;
        int drow = 0;
        int spiders = 0;
        int captives = 0;
        for (net.minecraft.entity.Entity entity : all) {
            if (entity instanceof mattonfire.dnd.entity.drow.DrowEntity d) {
                drow++;
                if (d.isPersistent()) {
                    persistent++;
                }
                if (d.getType() == mattonfire.dnd.entity.drow.Drow.DROW_WARRIOR) {
                    warriors++;
                } else if (d.getType() == mattonfire.dnd.entity.drow.Drow.DROW_MAGE) {
                    mages++;
                } else if (d.getType() == mattonfire.dnd.entity.drow.Drow.DROW_PRIESTESS) {
                    priestesses++;
                }
            } else if (entity.getType() == net.minecraft.entity.EntityType.CAVE_SPIDER) {
                spiders++;
            }
            if (mattonfire.dnd.world.underdark.Captives.isCaptive(entity)) {
                captives++;
            }
        }
        String line = "[DrowOutpostCensus] outpost at chunk " + layout.start().getPos() + ": drow " + drow + " (warriors "
                + warriors + ", mages " + mages + ", priestesses " + priestesses + "; persistent " + persistent
                + "), cave spiders " + spiders + ", captives " + captives;
        mattonfire.dnd.classes.DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return drow;
    }

    static int seeds(CommandContext<ServerCommandSource> context, List<Long> seeds) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[DrowOutpostSeeds] This world has no Underdark"));
            return 0;
        }
        return HomeCommandUtil.seeds(context.getSource(), underdark, DrowOutposts.KEY, DrowOutposts.SET, seeds,
                SEED_RADIUS, "[DrowOutpostSeeds]");
    }
}
