package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.entity.DuergarEntity;
import mattonfire.dnd.world.underdark.Captives;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.duergar.DuergarBarracksPiece;
import mattonfire.dnd.world.underdark.duergar.DuergarForgePiece;
import mattonfire.dnd.world.underdark.duergar.DuergarGroundsPiece;
import mattonfire.dnd.world.underdark.duergar.DuergarHoldPlanner;
import mattonfire.dnd.world.underdark.duergar.DuergarHolds;
import mattonfire.dnd.world.underdark.duergar.DuergarMinePiece;
import mattonfire.dnd.world.underdark.duergar.DuergarTowerPiece;
import mattonfire.dnd.world.underdark.duergar.DuergarTradePiece;
import mattonfire.dnd.world.underdark.duergar.DuergarWallPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
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
 * The Duergar Hold parts of {@link DndHomeCommand} (design 9 T8; docs/structures/duergar-holds.md). They work from
 * anywhere: outside the Underdark they search below your x and z.
 * <ul>
 * <li>{@code /dndhome duergarhold [spot]}: teleports you into the nearest hold and prints its layout. Spots:
 * {@code gate} (outside, looking in), {@code inside}, {@code above}, {@code overview}, {@code forge} (at the forge
 * chest), {@code trade} (at the factor's counter), {@code mine} (in the gallery), {@code pen} (at the first pen gate),
 * {@code tower}, {@code vault} (at the Locked Door), {@code top} (the Taskmaster's floor), {@code lever} (the
 * portcullis lever) and {@code lake} (the lava's shore).</li>
 * <li>{@code /dndhome duergarholdsurvey [grid] [spacing]}: from a grid of points round you, finds the nearest hold to
 * each and checks it ({@code [DuergarHoldSurvey]}): the biome, the floor's height, every piece, then (generating its
 * chunks) the lava (at least 90% of the 480 basin blocks there, none anywhere else), no water inside the wall, the open portcullis, the locked pen
 * gates, the vault's Locked Door shut, the chests, and every step of the mine stair with two blocks of headroom.</li>
 * <li>{@code /dndhome duergarholdcensus}: who lives in the nearest hold.</li>
 * <li>{@code /dndhome duergarholdseeds [seed...]}: for each seed (this world's and two others by default) the nearest
 * hold to 0, 0 that would generate, and its biome, without making a world ({@code [DuergarHoldSeeds]}).</li>
 * </ul>
 */
final class DuergarHoldCommands {
    static final TagKey<Biome> BIOMES = TagKey.of(RegistryKeys.BIOME, new Identifier("dndclasses", "has_structure/duergar_hold"));
    static final int SEED_RADIUS = 2500;
    private static final String SPOTS = "gate, inside, above, overview, forge, trade, mine, pen, tower, vault, top, lever or lake";

    private DuergarHoldCommands() {
    }

    /** The pieces of one hold. */
    record Layout(StructureStart start, DuergarGroundsPiece grounds, DuergarWallPiece wall, DuergarForgePiece forge,
                  DuergarTradePiece trade, DuergarMinePiece mine, DuergarTowerPiece tower,
                  List<DuergarBarracksPiece> barracks) {
        static Layout of(StructureStart start) {
            DuergarGroundsPiece grounds = null;
            DuergarWallPiece wall = null;
            DuergarForgePiece forge = null;
            DuergarTradePiece trade = null;
            DuergarMinePiece mine = null;
            DuergarTowerPiece tower = null;
            List<DuergarBarracksPiece> barracks = new ArrayList<>();
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof DuergarGroundsPiece p) {
                    grounds = p;
                } else if (piece instanceof DuergarWallPiece p) {
                    wall = p;
                } else if (piece instanceof DuergarForgePiece p) {
                    forge = p;
                } else if (piece instanceof DuergarTradePiece p) {
                    trade = p;
                } else if (piece instanceof DuergarMinePiece p) {
                    mine = p;
                } else if (piece instanceof DuergarTowerPiece p) {
                    tower = p;
                } else if (piece instanceof DuergarBarracksPiece p) {
                    barracks.add(p);
                }
            }
            return new Layout(start, grounds, wall, forge, trade, mine, tower, barracks);
        }

        boolean complete() {
            return this.grounds != null && this.wall != null && this.forge != null && this.trade != null && this.mine != null
                    && this.tower != null && !this.barracks.isEmpty() && this.barracks.size() <= 2;
        }

        BlockPos center() {
            return this.grounds != null ? this.grounds.center() : this.start.getBoundingBox().getCenter();
        }
    }

    @Nullable
    private static Layout nearest(ServerWorld underdark, BlockPos from) {
        StructureStart start = HomeCommandUtil.nearestStart(underdark, DuergarHolds.TAG, DuergarHolds.KEY, from);
        return start == null ? null : Layout.of(start);
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[Home] This world has no Underdark"));
            return 0;
        }
        Layout layout = nearest(underdark, new BlockPos(player.getBlockX(), -150, player.getBlockZ()));
        if (layout == null || !layout.complete()) {
            context.getSource().sendError(Text.literal("[Home] No Duergar Hold within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        BlockPos center = layout.center();
        DuergarGroundsPiece g = layout.grounds();
        Vec3d feet;
        Vec3d look;
        switch (spot) {
            case "gate" -> {
                feet = Vec3d.ofBottomCenter(layout.wall().outside());
                look = Vec3d.ofCenter(layout.wall().portcullis(1));
            }
            case "inside" -> {
                feet = Vec3d.ofBottomCenter(layout.wall().inside());
                look = Vec3d.ofCenter(g.at(0, 2, DuergarHoldPlanner.BACK));
            }
            case "above" -> {
                BlockPos top = center.up(2);
                for (int i = 0; i < 40 && underdark.getBlockState(top.up()).isAir(); i++) {
                    top = top.up();
                }
                feet = Vec3d.ofBottomCenter(top.down());
                look = feet.add(0.001D, -10.0D, 0.0D);
            }
            case "overview" -> {
                // High over the yard inside the gate, looking back across the hold to the lava.
                feet = Vec3d.ofBottomCenter(g.at(0, 7, DuergarWallPiece.GATE_IN - 1));
                look = Vec3d.ofCenter(g.at(0, 0, -12));
            }
            case "forge" -> {
                feet = Vec3d.ofBottomCenter(layout.forge().atChest());
                look = Vec3d.ofCenter(layout.forge().chest());
            }
            case "trade" -> {
                // Looking at the factor, wherever behind the counter it stands.
                feet = Vec3d.ofBottomCenter(layout.trade().atCounter());
                look = underdark.getEntitiesByClass(mattonfire.dnd.entity.DuergarFactorEntity.class,
                                new Box(layout.trade().factorHome()).expand(6.0D), Entity::isAlive).stream()
                        .findFirst().map(Entity::getEyePos).orElse(Vec3d.ofCenter(layout.trade().factorHome().up()));
            }
            case "mine" -> {
                feet = Vec3d.ofBottomCenter(layout.mine().gallery());
                look = Vec3d.ofCenter(layout.mine().oreChest());
            }
            case "pen" -> {
                feet = Vec3d.ofBottomCenter(layout.mine().atGate(0));
                look = Vec3d.ofCenter(layout.mine().gate(0));
            }
            case "tower" -> {
                feet = Vec3d.ofBottomCenter(layout.tower().inside());
                look = Vec3d.ofCenter(layout.tower().center().up(2));
            }
            case "vault" -> {
                feet = Vec3d.ofBottomCenter(layout.tower().atVault());
                look = Vec3d.ofCenter(layout.tower().vaultDoor());
            }
            case "top" -> {
                feet = Vec3d.ofBottomCenter(layout.tower().quarters());
                look = Vec3d.ofCenter(center);
            }
            case "lever" -> {
                feet = Vec3d.ofBottomCenter(layout.wall().leverRoom());
                look = Vec3d.ofCenter(layout.wall().lever());
            }
            case "lake" -> {
                feet = Vec3d.ofBottomCenter(g.at(0, 0, DuergarHoldPlanner.BACK));
                look = Vec3d.ofCenter(g.lava(0, -22));
            }
            default -> {
                context.getSource().sendError(Text.literal("[Home] Which spot? " + SPOTS));
                return 0;
            }
        }
        Vec3d d = look.subtract(feet.add(0.0D, player.getStandingEyeHeight(), 0.0D));
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 57.2957763671875D) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.2957763671875D);
        player.teleport(underdark, feet.x, feet.y, feet.z, yaw, pitch);
        String biome = underdark.getBiome(center).getKey().map(k -> k.getValue().toString()).orElse("?");
        context.getSource().sendFeedback(Text.literal("[Home] Duergar Hold at chunk " + layout.start().getPos() + ": centre "
                + center.toShortString() + " in " + biome + ", gate facing " + g.front() + ", " + layout.barracks().size()
                + " barracks, forge chest " + layout.forge().chest().toShortString() + ", lever "
                + layout.wall().lever().toShortString() + ", portcullis " + layout.wall().portcullis(1).toShortString()
                + ", pen gate " + layout.mine().gate(0).toShortString() + ", vault door "
                + layout.tower().vaultDoor().toShortString() + ", strongbox " + layout.tower().strongbox().toShortString()
                + ", lava " + g.lava(0, -22).toShortString() + "; at " + spot + " "
                + BlockPos.ofFloored(feet).toShortString()), false);
        return 1;
    }

    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[DuergarHoldSurvey] This world has no Underdark"));
            return 0;
        }
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[DuergarHoldSurvey]", "holds",
                from -> nearest(underdark, new BlockPos(from.getX(), -150, from.getZ())),
                layout -> layout.start().getPos().toLong(), layout -> check(underdark, layout));
    }

    private static HomeCommandUtil.Check check(ServerWorld world, Layout layout) {
        String at = "[DuergarHoldSurvey] hold at chunk " + layout.start().getPos();
        if (!layout.complete()) {
            return new HomeCommandUtil.Check(false, at + ": pieces missing (barracks " + layout.barracks().size() + "): FAIL");
        }
        BlockPos center = layout.center();
        RegistryEntry<Biome> biome = world.getBiome(center);
        boolean biomeOk = biome.isIn(BIOMES);
        int floor = center.getY() - 1;
        boolean floorOk = floor >= -220 && floor <= -110;
        BlockBox box = layout.start().getBoundingBox();
        for (int cx = box.getMinX() >> 4; cx <= box.getMaxX() >> 4; cx++) {
            for (int cz = box.getMinZ() >> 4; cz <= box.getMaxZ() >> 4; cz++) {
                world.getChunk(cx, cz);
            }
        }
        DuergarGroundsPiece g = layout.grounds();
        BlockBox forgeBox = layout.forge().getBoundingBox();
        int basin = 0;
        int stray = 0;
        int wet = 0;
        int wetOutside = 0;
        List<String> notes = new ArrayList<>();
        int half = DuergarHoldPlanner.HALF + 1;
        for (int a = -half; a <= half; a++) {
            for (int c = -half; c <= half; c++) {
                for (int y = -3; y <= 8; y++) {
                    BlockPos pos = g.at(a, y, c);
                    FluidState fluid = world.getFluidState(pos);
                    if (fluid.isEmpty()) {
                        continue;
                    }
                    if (fluid.isIn(FluidTags.WATER)) {
                        if (Math.abs(a) > DuergarHoldPlanner.INNER || c < DuergarHoldPlanner.BACK || c > DuergarHoldPlanner.FRONT) {
                            wetOutside++;
                            continue;
                        }
                        wet++;
                        if (notes.size() < 4) {
                            notes.add("water at a " + a + " b " + c + " y " + y + (fluid.isStill() ? " (source)" : ""));
                        }
                    } else if (DuergarGroundsPiece.inLake(a, c) && y <= DuergarGroundsPiece.LAVA_TOP) {
                        if (fluid.isStill()) {
                            basin++;
                        }
                    } else if (!(y <= -2 && forgeBox.contains(pos))) {
                        stray++;
                        if (notes.size() < 4) {
                            notes.add("lava " + pos.toShortString());
                        }
                    }
                }
            }
        }
        for (int a = DuergarHoldPlanner.LAKE_A1; a <= DuergarHoldPlanner.LAKE_A2 && notes.size() < 4; a++) {
            for (int c = DuergarHoldPlanner.LAKE_B1; c <= DuergarHoldPlanner.LAKE_B2 && notes.size() < 4; c++) {
                for (int y = DuergarGroundsPiece.LAVA_TOP - 1; y <= DuergarGroundsPiece.LAVA_TOP; y++) {
                    FluidState fluid = world.getFluidState(g.at(a, y, c));
                    if (!(fluid.isIn(FluidTags.LAVA) && fluid.isStill()) && notes.size() < 4) {
                        notes.add("basin " + g.at(a, y, c).toShortString() + " " + world.getBlockState(g.at(a, y, c)));
                    }
                }
            }
        }
        int open = 0;
        for (int i = 0; i < 3; i++) {
            BlockState trapdoor = world.getBlockState(layout.wall().portcullis(i));
            if (trapdoor.isOf(ModBlockFamilies.GREYSTEEL_PLATE.trapdoor()) && trapdoor.get(TrapdoorBlock.OPEN)) {
                open++;
            } else if (notes.size() < 8) {
                notes.add("portcullis " + trapdoor);
            }
        }
        int gates = 0;
        for (int i = 0; i < 2; i++) {
            for (int y = 0; y <= 1; y++) {
                BlockState gate = world.getBlockState(layout.mine().gate(i).up(y));
                if (gate.isOf(UnderdarkBlocks.CHAIN_CAGE) && gate.get(ChainCageBlock.LOCKED)) {
                    gates++;
                } else if (notes.size() < 8) {
                    notes.add("pen gate " + layout.mine().gate(i).up(y).toShortString() + " " + gate);
                }
            }
        }
        int door = 0;
        for (int y = 0; y <= 1; y++) {
            BlockState part = world.getBlockState(layout.tower().vaultDoor().up(y));
            if (part.isOf(ObstacleTypes.LOCKED_DOOR_BLOCK) && part.get(mattonfire.dnd.classes.Obstacles.ObstacleBlock.STATE)
                    != mattonfire.dnd.classes.Obstacles.ObstacleState.OPEN) {
                door++;
            }
        }
        boolean chests = world.getBlockState(layout.forge().chest()).isOf(Blocks.CHEST)
                && world.getBlockState(layout.tower().strongbox()).isOf(Blocks.CHEST)
                && world.getBlockState(layout.mine().oreChest()).isOf(Blocks.CHEST);
        int steps = 0;
        List<BlockPos> stair = layout.mine().steps();
        for (BlockPos step : stair) {
            if (world.getBlockState(step).getBlock() instanceof StairsBlock
                    && world.getBlockState(step.up()).getCollisionShape(world, step.up()).isEmpty()
                    && world.getBlockState(step.up(2)).getCollisionShape(world, step.up(2)).isEmpty()) {
                steps++;
            } else if (notes.size() < 8) {
                notes.add("step " + step.toShortString());
            }
        }
        int lakeSize = (DuergarHoldPlanner.LAKE_A2 - DuergarHoldPlanner.LAKE_A1 + 1)
                * (DuergarHoldPlanner.LAKE_B2 - DuergarHoldPlanner.LAKE_B1 + 1) * 2;
        // Features from neighbouring chunks (dripstone clusters) can drop a few blocks into the basin after it's built.
        boolean ok = biomeOk && floorOk && basin * 10 >= lakeSize * 9 && stray == 0 && wet == 0 && open == 3 && gates == 4
                && door == 2 && chests && steps == stair.size();
        String name = biome.getKey().map(k -> k.getValue().getPath()).orElse("?");
        return new HomeCommandUtil.Check(ok, at + " (centre " + center.toShortString() + ", " + name
                + (biomeOk ? "" : " WRONG BIOME") + ", floor y " + floor + ", gate " + g.front() + "): "
                + layout.barracks().size() + " barracks, lava " + basin + "/" + lakeSize + " in the basin, " + stray
                + " stray, wet blocks " + wet + " inside the wall (" + wetOutside + " outside), portcullis open " + open + "/3, locked pen gates " + gates
                + "/4, vault door shut " + door + "/2, chests " + (chests ? "ok" : "MISSING") + ", stair " + steps + "/"
                + stair.size() + " clear" + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")") + ": "
                + (ok ? "PASS" : "FAIL"));
    }

    /** {@code /dndhome duergarholdcensus}: who lives in the nearest hold ({@code [DuergarHoldCensus]}). */
    static int census(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        Layout layout = underdark == null ? null : nearest(underdark, new BlockPos(player.getBlockX(), -150, player.getBlockZ()));
        if (layout == null) {
            context.getSource().sendError(Text.literal("[DuergarHoldCensus] No Duergar Hold within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        Box box = Box.from(layout.start().getBoundingBox()).expand(8.0D);
        int soldiers = 0;
        int arbalesters = 0;
        int factors = 0;
        int taskmasters = 0;
        int persistent = 0;
        int duergar = 0;
        int captives = 0;
        for (Entity entity : underdark.getEntitiesByClass(Entity.class, box, Entity::isAlive)) {
            if (entity instanceof DuergarEntity d) {
                duergar++;
                if (d.isPersistent()) {
                    persistent++;
                }
                if (d.getType() == DuergarEntities.DUERGAR_SOLDIER) {
                    soldiers++;
                } else if (d.getType() == DuergarEntities.DUERGAR_ARBALESTER) {
                    arbalesters++;
                } else if (d.getType() == DuergarEntities.DUERGAR_FACTOR) {
                    factors++;
                } else if (d.getType() == DuergarEntities.DUERGAR_TASKMASTER) {
                    taskmasters++;
                }
            }
            if (Captives.isCaptive(entity)) {
                captives++;
            }
        }
        String line = "[DuergarHoldCensus] hold at chunk " + layout.start().getPos() + ": duergar " + duergar + " (soldiers "
                + soldiers + ", arbalesters " + arbalesters + ", factors " + factors + ", taskmasters " + taskmasters
                + "; persistent " + persistent + "), captives " + captives;
        mattonfire.dnd.classes.DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return duergar;
    }

    static int seeds(CommandContext<ServerCommandSource> context, List<Long> seeds) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[DuergarHoldSeeds] This world has no Underdark"));
            return 0;
        }
        return HomeCommandUtil.seeds(context.getSource(), underdark, DuergarHolds.KEY, DuergarHolds.SET, seeds,
                SEED_RADIUS, "[DuergarHoldSeeds]");
    }
}
