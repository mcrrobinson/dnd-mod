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
                .then(CommandManager.literal("survey")
                        .executes(ctx -> survey(ctx, 128))
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(8, 1024))
                                .executes(ctx -> survey(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))));
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
