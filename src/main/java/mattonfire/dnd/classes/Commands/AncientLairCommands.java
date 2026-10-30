package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.boss.StructureBosses;
import mattonfire.dnd.entity.dragon.AncientDragonEntity;
import mattonfire.dnd.world.gen.ancient.AncientLairGuards;
import mattonfire.dnd.world.gen.ancient.AncientLairLayout;
import mattonfire.dnd.world.gen.ancient.AncientLairStructure;
import mattonfire.dnd.world.gen.ancient.AncientLairStructures;
import mattonfire.dnd.world.gen.ancient.AncientLairs;
import mattonfire.dnd.world.gen.ancient.CalderaPiece;
import mattonfire.dnd.world.gen.ancient.HoardHallPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.registry.RegistryKey;
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
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * The Ancient lairs' part of {@link DndHomeCommand} (design 10, 6.7; docs/structures/ancient-lairs.md).
 * <ul>
 * <li>{@code /dndhome ancientlair [spot]}: teleports you to that spot of the nearest Ancient lair, looking at it, and
 * prints its layout. Spots: {@code mouth} (default: on the mouth's stair, looking in), {@code air} (high over the
 * mouth, looking down on the perch), {@code crater}, {@code perch}, {@code door}, {@code dart}, {@code pit},
 * {@code barracks}, {@code workshop}, {@code hall} (at the stair's foot), {@code chest} (in front of the first hoard
 * chest), {@code coffer}.</li>
 * <li>{@code /dndhome ancientlairsurvey [grid] [spacing]}: finds the nearest lair to each point of a grid round you and
 * checks it ({@code [AncientLairSurvey]}): site heights, slain flag, the caldera's fluids, the stair, the chests, its
 * dragon and guard posts.</li>
 * <li>{@code /dndhome ancientlairseeds [seed...]}: for each seed, the nearest lair to 0, 0 within
 * {@value #SEED_RADIUS} blocks ({@code [AncientLairSeeds]}).</li>
 * <li>{@code /dndhome ancientlairreset}: forgets that the nearest lair's dragon was slain and puts it back on its
 * perch (tests).</li>
 * </ul>
 */
final class AncientLairCommands {
    static final int SEED_RADIUS = 3000;
    static final RegistryKey<Structure> RED = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "ancient_red_lair"));
    static final TagKey<Biome> BIOMES = TagKey.of(RegistryKeys.BIOME,
            new Identifier(DnDClasses.MOD_ID, "has_structure/ancient_red_lair"));

    private AncientLairCommands() {
    }

    record Layout(StructureStart start, CalderaPiece caldera, HoardHallPiece hall) {
        @Nullable
        static Layout of(StructureStart start) {
            CalderaPiece caldera = null;
            HoardHallPiece hall = null;
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof CalderaPiece c) {
                    caldera = c;
                } else if (piece instanceof HoardHallPiece h) {
                    hall = h;
                }
            }
            return caldera == null || hall == null ? null : new Layout(start, caldera, hall);
        }

        AncientLairLayout layout() {
            return this.caldera.layout();
        }
    }

    @Nullable
    static Layout nearest(ServerWorld world, BlockPos from) {
        StructureStart start = HomeCommandUtil.nearestStart(world, AncientLairStructures.TAG, RED, from);
        return start == null ? null : Layout.of(start);
    }

    private static void generate(ServerWorld world, BlockBox box) {
        for (int cx = box.getMinX() >> 4; cx <= box.getMaxX() >> 4; cx++) {
            for (int cz = box.getMinZ() >> 4; cz <= box.getMaxZ() >> 4; cz++) {
                world.getChunk(cx, cz);
            }
        }
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        Layout found = nearest(world, player.getBlockPos());
        if (found == null) {
            context.getSource().sendError(Text.literal("[Home] No Ancient lair within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        AncientLairLayout l = found.layout();
        HoardHallPiece hall = found.hall();
        Vec3d feet;
        Vec3d look;
        BlockPos perch = l.perchTop();
        switch (spot) {
            case "mouth" -> {
                feet = Vec3d.ofBottomCenter(l.at(AncientLairLayout.RIM_RADIUS + 2, 0, l.floorY() - 1));
                look = Vec3d.ofCenter(perch.up(4));
            }
            case "air" -> {
                feet = Vec3d.ofBottomCenter(l.at(AncientLairLayout.BASE_RADIUS + 16, 0, l.floorY() + 46));
                look = Vec3d.ofCenter(l.at(-6, 0, l.floorY()));
            }
            case "crater" -> {
                feet = Vec3d.ofBottomCenter(l.at(10, 4, l.floorY() + 1));
                look = Vec3d.ofCenter(perch.up(3));
            }
            case "perch" -> {
                feet = Vec3d.ofBottomCenter(l.stair(-AncientLairLayout.PERCH_HALF, AncientLairLayout.PERCH_HALF,
                        AncientLairLayout.PERCH_HEIGHT + 1));
                look = Vec3d.ofCenter(l.center().up(2));
            }
            case "door" -> {
                feet = Vec3d.ofBottomCenter(hall.outsideDoor().offset(l.front(), 2));
                look = Vec3d.ofCenter(hall.door());
            }
            case "dart" -> {
                feet = Vec3d.ofBottomCenter(hall.door());
                look = Vec3d.ofCenter(hall.dartTrigger());
            }
            case "pit" -> {
                BlockPos trigger = hall.pitTrigger();
                feet = Vec3d.ofBottomCenter(l.stair(3, -3, -8));
                look = Vec3d.ofCenter(trigger);
            }
            case "barracks" -> {
                feet = Vec3d.ofBottomCenter(l.stair(3, AncientLairLayout.PLINTH_HALF + 1, -4));
                look = Vec3d.ofCenter(hall.barracks().up());
            }
            case "workshop" -> {
                feet = Vec3d.ofBottomCenter(l.stair(3, -AncientLairLayout.PLINTH_HALF - 1, -8));
                look = Vec3d.ofCenter(hall.workshopChest());
            }
            case "hall" -> {
                feet = Vec3d.ofBottomCenter(hall.stairFoot());
                look = Vec3d.ofCenter(hall.coffer());
            }
            case "chest" -> {
                BlockPos chest = hall.hoardChests().get(0);
                BlockState state = world.getBlockState(chest);
                net.minecraft.util.math.Direction facing = state.contains(ChestBlock.FACING) ? state.get(ChestBlock.FACING)
                        : l.front();
                feet = Vec3d.ofBottomCenter(chest.offset(facing, 2));
                look = Vec3d.ofCenter(chest);
            }
            case "coffer" -> {
                feet = Vec3d.ofBottomCenter(hall.coffer().offset(l.back(), 2).down());
                look = Vec3d.ofCenter(hall.coffer());
            }
            default -> {
                context.getSource().sendError(Text.literal("[Home] Which spot? mouth, air, crater, perch, door, dart, pit, "
                        + "barracks, workshop, hall, chest or coffer"));
                return 0;
            }
        }
        Vec3d d = look.subtract(feet.add(0.0D, player.getStandingEyeHeight(), 0.0D));
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 57.2957763671875D) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.2957763671875D);
        player.teleport(world, feet.x, feet.y, feet.z, yaw, pitch);
        List<String> pools = new ArrayList<>();
        for (CalderaPiece.Pool pool : found.caldera().pools()) {
            pools.add(l.at(pool.along(), pool.side(), l.floorY()).toShortString() + " r" + pool.radius());
        }
        String line = "[Home] Ancient " + l.color().id() + " lair at chunk " + found.start().getPos() + ": centre "
                + l.center().toShortString() + ", rim y " + l.rimY() + ", ground y " + l.base() + ", mouth "
                + l.front().asString() + ", perch " + perch.toShortString() + ", pools " + pools + ", hall "
                + l.hallCenter().toShortString() + ", coffer " + hall.coffer().toShortString() + ", chest "
                + hall.hoardChests().get(0).toShortString() + ", dart " + hall.dartTrigger().toShortString() + ", pit "
                + hall.pitTrigger().toShortString() + ", slain " + AncientLairs.get(world).isSlain(l.lairCentre())
                + "; at " + spot + " " + BlockPos.ofFloored(feet).toShortString();
        DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return 1;
    }

    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld world = context.getSource().getWorld();
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[AncientLairSurvey]", "lairs",
                from -> nearest(world, from), layout -> layout.start().getPos().toLong(), layout -> check(world, layout));
    }

    private static boolean passable(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    private static HomeCommandUtil.Check check(ServerWorld world, Layout found) {
        AncientLairLayout l = found.layout();
        generate(world, found.start().getBoundingBox());
        RegistryEntry<Biome> biome = world.getBiome(l.center());
        boolean slain = AncientLairs.get(world).isSlain(l.lairCentre());
        // Fluids: lava only inside the pools, none above the crater floor, no water anywhere in the crater or hall.
        int strayLava = 0;
        int poolLava = 0;
        int water = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int reach = AncientLairLayout.RIM_RADIUS + 2;
        for (int along = -reach; along <= reach; along++) {
            for (int side = -reach; side <= reach; side++) {
                if (along * along + side * side > reach * reach) {
                    continue;
                }
                boolean inPool = false;
                for (CalderaPiece.Pool pool : found.caldera().pools()) {
                    double dx = along - pool.along();
                    double ds = side - pool.side();
                    inPool |= Math.sqrt(dx * dx + ds * ds) <= pool.radius() + 0.4D;
                }
                BlockPos column = l.at(along, side, 0);
                for (int y = l.floorY() - 2; y <= l.floorY() + 3; y++) {
                    pos.set(column.getX(), y, column.getZ());
                    var fluid = world.getFluidState(pos);
                    if (fluid.isEmpty()) {
                        continue;
                    }
                    if (fluid.isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                        water++;
                    } else if (inPool && y <= l.floorY()) {
                        poolLava++;
                    } else {
                        strayLava++;
                    }
                }
            }
        }
        BlockPos hc = l.hallCenter();
        for (int dx = -AncientLairLayout.HALL_RADIUS; dx <= AncientLairLayout.HALL_RADIUS; dx += 2) {
            for (int dz = -AncientLairLayout.HALL_RADIUS; dz <= AncientLairLayout.HALL_RADIUS; dz += 2) {
                for (int dy = 1; dy <= 6; dy++) {
                    if (!world.getFluidState(hc.add(dx, dy, dz)).isEmpty()) {
                        water++;
                    }
                }
            }
        }
        boolean fluidsOk = strayLava == 0 && water == 0 && poolLava > 0;
        String fluidLine = "[Lair] caldera fluids " + (fluidsOk ? "ok" : "BAD") + " (pool lava " + poolLava + ", stray lava "
                + strayLava + ", water " + water + ")";
        DnDClasses.LOGGER.info(fluidLine);
        // The stair: every step of the centre line has room to stand, from the door to the hall.
        int steps = 0;
        int clear = 0;
        List<String> notes = new ArrayList<>();
        for (BlockPos step : found.hall().walk()) {
            steps++;
            boolean ok = passable(world, step) && passable(world, step.up()) && !passable(world, step.down());
            if (ok) {
                clear++;
            } else if (notes.size() < 4) {
                notes.add("step " + step.toShortString());
            }
        }
        int chests = 0;
        for (BlockPos chest : found.hall().hoardChests()) {
            chests += world.getBlockState(chest).isOf(Blocks.CHEST) ? 1 : 0;
        }
        boolean coffer = world.getBlockState(found.hall().coffer()).isOf(Blocks.CHEST);
        boolean workshop = world.getBlockState(found.hall().workshopChest()).isOf(Blocks.CHEST);
        Box box = Box.from(found.start().getBoundingBox());
        int dragons = world.getEntitiesByClass(AncientDragonEntity.class, box, e -> e.isAlive()).size();
        int posts = world.getEntitiesByClass(MarkerEntity.class, box,
                e -> e.getCommandTags().contains(AncientLairGuards.POST_TAG)).size();
        int dragonMarkers = world.getEntitiesByClass(MarkerEntity.class, box,
                e -> e.getCommandTags().stream().anyMatch(t -> t.startsWith("dndclasses.pending_boss.ancient"))).size();
        int perchTop = l.perchTop().getY() - 1;
        boolean perchOk = world.getBlockState(l.perchTop().down()).isOpaqueFullCube(world, l.perchTop().down())
                && passable(world, l.perchTop()) && passable(world, l.perchTop().up(4));
        boolean biomeOk = biome.isIn(BIOMES);
        boolean ok = biomeOk && fluidsOk && clear == steps && chests == 4 && coffer && workshop && perchOk;
        String name = biome.getKey().map(k -> k.getValue().getPath()).orElse("?");
        return new HomeCommandUtil.Check(ok, "[AncientLairSurvey] lair at chunk " + found.start().getPos() + " (centre "
                + l.center().toShortString() + ", " + name + (biomeOk ? "" : " WRONG BIOME") + ", ground y " + l.base()
                + ", floor y " + l.floorY() + ", rim y " + l.rimY() + ", perch top y " + perchTop + ", mouth "
                + l.front().asString() + "): slain " + slain + ", " + fluidLine.substring(7) + ", stair " + clear + "/"
                + steps + " clear, hoard chests " + chests + "/4, coffer " + coffer + ", workshop chest " + workshop
                + ", perch " + (perchOk ? "clear" : "BLOCKED") + ", dragons " + dragons + " (markers " + dragonMarkers
                + "), guard posts " + posts + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")") + ": "
                + (ok ? "PASS" : "FAIL"));
    }

    static int seeds(CommandContext<ServerCommandSource> context, List<Long> seeds) {
        AncientLairStructure.REJECTS.clear();
        int found = HomeCommandUtil.seeds(context.getSource(), context.getSource().getWorld(), RED, AncientLairStructures.SET,
                seeds, SEED_RADIUS, "[AncientLairSeeds]");
        String line = "[AncientLairSeeds] sites checked: " + new TreeMap<>(
                AncientLairStructure.REJECTS);
        DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return found;
    }

    /** Clears the nearest lair's slain flag and puts its dragon back on the perch (a marker, raised within a second). */
    static int reset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        Layout found = nearest(world, player.getBlockPos());
        if (found == null) {
            context.getSource().sendError(Text.literal("[Home] No Ancient lair within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        AncientLairLayout l = found.layout();
        AncientLairs.get(world).clear(l.lairCentre());
        Box box = Box.from(found.start().getBoundingBox()).expand(64.0D);
        int alive = world.getEntitiesByClass(AncientDragonEntity.class, box, e -> e.isAlive()).size();
        if (alive == 0) {
            StructureBosses.placeAncient(world, l.perchTop(), AncientLairLayout.yaw(l.front()), l.color(), l.lairCentre());
        }
        context.getSource().sendFeedback(Text.literal("[Home] Ancient lair at " + l.center().toShortString()
                + " is no longer slain" + (alive == 0 ? "; its dragon returns to the perch" : "")), true);
        return 1;
    }
}
