package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.UnderdarkLanding;
import mattonfire.dnd.world.underdark.UnderdarkMaps;
import mattonfire.dnd.world.underdark.UnderdarkSurvey;
import mattonfire.dnd.world.underdark.UnderdarkTravel;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * {@code /underdark} (permission 2), the Underdark admin and test command (design 9, 2.7):
 * <ul>
 * <li>{@code go [player]}: down into the Underdark at the same x and z, remembering where you were.</li>
 * <li>{@code back [player]}: back to the remembered spot (or the world spawn).</li>
 * <li>{@code biome <biome>}: to the nearest Underdark biome of that kind, on a safe floor.</li>
 * <li>{@code survey <radius>}: biome shares, floors, water, bedrock and cost round you (logged as
 * {@code [Underdark] survey ...}).</li>
 * <li>{@code lake [radius]}: to the nearest dry shore of a Darklake lake (open water at the Underdark's waterline), looking out over it. For the mist and lake-life screenshots and tests.</li>
 * <li>{@code flora [radius]}: counts the T3 flora (giant fungi, lake life, plants) in the loaded blocks round you
 * (logged as {@code [Underdark] flora ...}).</li>
 * <li>{@code map <drow_outpost|duergar_hold|rift>}: an Underdark explorer map to the nearest such place
 * ({@link UnderdarkMaps}).</li>
 * </ul>
 */
public final class UnderdarkCommand {
    /** How far past a located biome's edge {@code /underdark biome} tries to land. */
    private static final int INSIDE = 48;
    private UnderdarkCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("underdark").requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("go")
                        .executes(ctx -> go(ctx, ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> go(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                .then(CommandManager.literal("back")
                        .executes(ctx -> back(ctx, ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> back(ctx, EntityArgumentType.getPlayer(ctx, "player")))))
                .then(CommandManager.literal("biome")
                        .then(CommandManager.argument("biome", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(Underdark.BIOMES.stream()
                                        .map(key -> key.getValue().getPath()), builder))
                                .executes(ctx -> biome(ctx, StringArgumentType.getString(ctx, "biome")))))
                .then(CommandManager.literal("lake")
                        .executes(ctx -> lake(ctx, 96))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(8, 256))
                                .executes(ctx -> lake(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(CommandManager.literal("flora")
                        .executes(ctx -> flora(ctx, 48))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(4, 96))
                                .executes(ctx -> flora(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(CommandManager.literal("map")
                        .then(CommandManager.argument("kind", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(java.util.Arrays.stream(
                                        UnderdarkMaps.Kind.values()).map(UnderdarkMaps.Kind::id), builder))
                                .executes(ctx -> map(ctx, StringArgumentType.getString(ctx, "kind")))))
                .then(CommandManager.literal("survey")
                        .executes(ctx -> survey(ctx, 128))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(8, 1024))
                                .executes(ctx -> survey(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static int lake(CommandContext<ServerCommandSource> ctx, int radius) throws CommandSyntaxException {
        if (missing(ctx)) {
            return 0;
        }
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        if (!Underdark.isUnderdark(player.getWorld())) {
            ctx.getSource().sendError(Text.literal("Go down first (/underdark biome darklake)"));
            return 0;
        }
        ServerWorld world = player.getWorld();
        int surface = world.getChunkManager().getChunkGenerator().getSeaLevel() - 1;
        int x0 = player.getBlockX();
        int z0 = player.getBlockZ();
        for (int r = 0; r <= radius; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos water = new BlockPos(x0 + dx, surface, z0 + dz);
                    if (!world.getFluidState(water).isIn(net.minecraft.registry.tag.FluidTags.WATER)
                            || !world.getFluidState(water).isStill() || !world.getBlockState(water.up()).isAir()
                            || !world.getBiome(water).matchesKey(Underdark.DARKLAKE)) {
                        continue;
                    }
                    for (net.minecraft.util.math.Direction side : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                        for (int k = 1; k <= 3; k++) {
                            BlockPos feet = water.up().offset(side, k);
                            if (UnderdarkLanding.standable(world, feet)) {
                                float yaw = side.getOpposite().asRotation();
                                UnderdarkTravel.sendTo(player, feet, yaw, 20.0F);
                                DnDClasses.LOGGER.info("[Underdark] lake shore at {} {} {} in {}", feet.getX(),
                                        feet.getY(), feet.getZ(), world.getBiome(feet).getKey()
                                                .map(key -> key.getValue().getPath()).orElse("?"));
                                ctx.getSource().sendFeedback(Text.literal(String.format("Lake shore: %d %d %d",
                                        feet.getX(), feet.getY(), feet.getZ())), true);
                                return 1;
                            }
                        }
                    }
                }
            }
        }
        ctx.getSource().sendError(Text.literal("No Darklake shore within " + radius + " blocks"));
        return 0;
    }

    private static int flora(CommandContext<ServerCommandSource> ctx, int radius) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerWorld world = player.getWorld();
        java.util.Map<net.minecraft.block.Block, String> watched = new java.util.LinkedHashMap<>();
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkBlocks.ZURKHWOOD_STEM, "zurkhwood_stem");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkBlocks.ZURKHWOOD_CAP, "zurkhwood_cap");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.ZURKHWOOD_GILLS, "zurkhwood_gills");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkBlocks.NIGHTLIGHT_CAP, "nightlight_cap");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.SPORE_DRIP, "spore_drip");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.RIPPLEBARK, "ripplebark");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.BARRELSTALK, "barrelstalk");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.WATERORB, "waterorb");
        watched.put(net.minecraft.block.Blocks.SEAGRASS, "seagrass");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.FAERIE_LICHEN, "faerie_lichen");
        watched.put(mattonfire.dnd.classes.Registry.UnderdarkFlora.PEARL_CLAM, "pearl_clam");
        watched.put(net.minecraft.block.Blocks.CLAY, "clay");
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        watched.values().forEach(name -> counts.put(name, 0));
        BlockPos centre = player.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = centre.getX() - radius; x <= centre.getX() + radius; x++) {
            for (int z = centre.getZ() - radius; z <= centre.getZ() + radius; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = world.getBottomY(); y < world.getTopY(); y++) {
                    pos.set(x, y, z);
                    String name = watched.get(world.getBlockState(pos).getBlock());
                    if (name != null) {
                        counts.merge(name, 1, Integer::sum);
                    }
                }
            }
        }
        StringBuilder line = new StringBuilder(String.format("flora at %d %d radius %d:", centre.getX(),
                centre.getZ(), radius));
        counts.forEach((name, n) -> line.append(' ').append(name).append('=').append(n));
        DnDClasses.LOGGER.info("[Underdark] {}", line);
        ctx.getSource().sendFeedback(Text.literal(line.toString()), false);
        return 1;
    }

    private static int map(CommandContext<ServerCommandSource> ctx, String id) throws CommandSyntaxException {
        UnderdarkMaps.Kind kind = UnderdarkMaps.Kind.byId(id);
        if (kind == null) {
            ctx.getSource().sendError(Text.literal("Not an Underdark map: " + id));
            return 0;
        }
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        net.minecraft.item.ItemStack map = UnderdarkMaps.create(player.getWorld(), player.getBlockPos(), kind);
        if (map == null) {
            ctx.getSource().sendError(Text.literal("No " + kind.id() + " to map near here"));
            return 0;
        }
        player.getInventory().offerOrDrop(map);
        ctx.getSource().sendFeedback(Text.literal("Gave " + player.getName().getString() + " a map to the "
                + kind.id()), true);
        return 1;
    }

    private static boolean missing(CommandContext<ServerCommandSource> ctx) {
        if (Underdark.world(ctx.getSource().getServer()) != null) {
            return false;
        }
        ctx.getSource().sendError(Text.literal("This world has no Underdark (is the mod's data pack disabled?)"));
        return true;
    }

    private static int go(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        if (missing(ctx)) {
            return 0;
        }
        UnderdarkLanding.Spot spot = UnderdarkTravel.descend(player);
        BlockPos feet = spot.feet();
        ctx.getSource().sendFeedback(Text.literal(String.format("%s descends into the Underdark at %d %d %d (%s)",
                player.getName().getString(), feet.getX(), feet.getY(), feet.getZ(), spot.kind().id())), true);
        return 1;
    }

    private static int back(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        BlockPos pos = UnderdarkTravel.back(player);
        ctx.getSource().sendFeedback(Text.literal(String.format("%s returns to %d %d %d in %s",
                player.getName().getString(), pos.getX(), pos.getY(), pos.getZ(),
                player.getWorld().getRegistryKey().getValue())), true);
        return 1;
    }

    private static int biome(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        if (missing(ctx)) {
            return 0;
        }
        String path = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
        RegistryKey<Biome> key = Underdark.BIOMES.stream().filter(k -> k.getValue().getPath().equals(path)).findFirst()
                .orElse(null);
        if (key == null) {
            ctx.getSource().sendError(Text.literal("Not an Underdark biome: " + name));
            return 0;
        }
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(ctx.getSource().getServer());
        BlockPos from = new BlockPos(player.getBlockX(), -160, player.getBlockZ());
        Pair<BlockPos, RegistryEntry<Biome>> found = underdark.locateBiome(entry -> entry.matchesKey(key), from, 6400,
                32, 64);
        if (found == null) {
            ctx.getSource().sendError(Text.literal("No " + path + " within 6400 blocks"));
            return 0;
        }
        BlockPos at = inside(underdark, key, from, found.getFirst());
        UnderdarkLanding.Spot spot = UnderdarkLanding.findOrMake(underdark, at.getX(), at.getZ());
        UnderdarkTravel.sendTo(player, spot.feet(), UnderdarkLanding.openYaw(underdark, spot.feet()), 10.0F);
        BlockPos feet = spot.feet();
        int distance = (int) Math.round(Math.hypot(feet.getX() - from.getX(), feet.getZ() - from.getZ()));
        DnDClasses.LOGGER.info("[Underdark] biome {} at {} {} {} ({}, {} blocks away)", path, feet.getX(), feet.getY(),
                feet.getZ(), spot.kind().id(), distance);
        ctx.getSource().sendFeedback(Text.literal(String.format("Nearest %s: %d %d %d (%s, %d blocks away)", path,
                feet.getX(), feet.getY(), feet.getZ(), spot.kind().id(), distance)), true);
        return 1;
    }

    /**
     * {@code locateBiome} finds a biome's near edge; this steps up to {@link #INSIDE} blocks further on, away from
     * {@code from}, while still in the biome, so you land well inside it.
     */
    private static BlockPos inside(ServerWorld world, RegistryKey<Biome> key, BlockPos from, BlockPos edge) {
        double dx = edge.getX() - from.getX();
        double dz = edge.getZ() - from.getZ();
        double length = Math.hypot(dx, dz);
        if (length < 1) {
            return edge;
        }
        for (int step = INSIDE; step > 0; step -= 16) {
            BlockPos deeper = new BlockPos((int) (edge.getX() + dx / length * step), edge.getY(),
                    (int) (edge.getZ() + dz / length * step));
            if (world.getBiome(deeper).matchesKey(key)) {
                return deeper;
            }
        }
        return edge;
    }

    private static int survey(CommandContext<ServerCommandSource> ctx, int radius) {
        if (missing(ctx)) {
            return 0;
        }
        ServerWorld underdark = Underdark.world(ctx.getSource().getServer());
        BlockPos centre = BlockPos.ofFloored(ctx.getSource().getPosition());
        UnderdarkSurvey.Result result = UnderdarkSurvey.run(underdark, centre.getX(), centre.getZ(), radius);
        String line = String.format("survey at %d %d radius %d: %s", centre.getX(), centre.getZ(), radius,
                result.describe());
        DnDClasses.LOGGER.info("[Underdark] {}", line);
        ctx.getSource().sendFeedback(Text.literal(line), false);
        return 1;
    }
}
