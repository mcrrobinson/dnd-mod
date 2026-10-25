package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.RiftBlocks;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.UnderdarkRifts;
import mattonfire.dnd.world.underdark.rift.RiftChasmPiece;
import mattonfire.dnd.world.underdark.rift.RiftLandingPiece;
import mattonfire.dnd.world.underdark.rift.RiftSinkholePiece;
import mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructures;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * The Underdark Rift parts of {@link DndHomeCommand} (design 9 T2; docs/structures/underdark-rifts.md):
 * <ul>
 * <li>{@code /dndhome rift [top|floor|ledge|pool|mouth|step <k>]}: teleports you to that spot of the nearest rift
 * (in the Overworld): over the sinkhole looking down, the chasm floor looking at the pool, the rubble ledge, in the
 * veil, beside the hole at the top, or on climbing step k. Prints the rift's spots and its step count.</li>
 * <li>{@code /dndhome riftlanding}: (in the Underdark) teleports you onto the nearest rift landing, beside its anchor.</li>
 * <li>{@code /dndhome riftlair}: onto the nearest Beholder lair's floor, beside the eye's pupil (where its death
 * tears a rift).</li>
 * <li>{@code /dndhome riftsurvey [grid] [spacing]}: from a grid of points round you, finds the nearest rift to each
 * and checks it (logged as {@code [RiftSurvey]}): both pieces, a landing starting in the same chunk of the
 * Underdark, 15 veil blocks in the pool, and every climbing step clear (two blocks of air over a solid ledge).</li>
 * <li>{@code /dndhome riftseeds [seed...]}: for each seed (this world's and two others by default) finds the nearest
 * rift to 0, 0 that would generate, without making a world (logged as {@code [RiftSeeds]}).</li>
 * </ul>
 */
final class RiftCommands {
    private RiftCommands() {
    }

    @Nullable
    private static StructureStart nearest(ServerWorld world, RegistryKey<Structure> key, BlockPos from) {
        var entry = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getEntry(key);
        if (entry.isEmpty()) {
            return null;
        }
        Pair<BlockPos, RegistryEntry<Structure>> found = world.getChunkManager().getChunkGenerator().locateStructure(world,
                RegistryEntryList.of(entry.get()), from, HomeCommandUtil.SEARCH_CHUNKS, false);
        if (found == null) {
            return null;
        }
        StructureStart start = world.getChunk(found.getFirst().getX() >> 4, found.getFirst().getZ() >> 4,
                ChunkStatus.STRUCTURE_STARTS).getStructureStart(entry.get().value());
        return start != null && start.hasChildren() ? start : null;
    }

    @Nullable
    private static <T extends StructurePiece> T piece(StructureStart start, Class<T> type) {
        for (StructurePiece piece : start.getChildren()) {
            if (type.isInstance(piece)) {
                return type.cast(piece);
            }
        }
        return null;
    }

    static int teleport(CommandContext<ServerCommandSource> context, String spot, int step) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = context.getSource().getServer().getOverworld();
        StructureStart start = nearest(world, UnderdarkRiftStructures.RIFT_KEY,
                player.getWorld() == world ? player.getBlockPos() : BlockPos.ORIGIN);
        RiftChasmPiece chasm = start == null ? null : piece(start, RiftChasmPiece.class);
        RiftSinkholePiece shaft = start == null ? null : piece(start, RiftSinkholePiece.class);
        if (chasm == null || shaft == null) {
            context.getSource().sendError(Text.literal("[Home] No Underdark Rift within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        BlockPos feet;
        float yaw = chasm.yawToPool();
        float pitch = 15.0F;
        BlockPos mouth = shaft.mouth();
        switch (spot) {
            case "top" -> {
                feet = mouth.up(14);
                pitch = 90.0F;
            }
            case "mouth" -> {
                feet = mouth.add(-10, 5, -10);
                yaw = -45.0F;
                pitch = 28.0F;
            }
            case "floor" -> {
                feet = chasm.at(6, mattonfire.dnd.world.underdark.rift.UnderdarkRiftStructure.FLOOR + 1, 0);
                pitch = 20.0F;
            }
            case "ledge" -> feet = chasm.ledge();
            case "pool" -> feet = chasm.poolCenter();
            case "step" -> {
                if (step < 0 || step >= shaft.steps()) {
                    context.getSource().sendError(Text.literal("[Home] The climb has steps 0 to " + (shaft.steps() - 1)));
                    return 0;
                }
                feet = shaft.stepFeet(step);
            }
            default -> {
                context.getSource().sendError(Text.literal("[Home] Which spot? top, mouth, floor, ledge, pool or step <k>"));
                return 0;
            }
        }
        player.teleport(world, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, yaw, pitch);
        BlockPos pool = chasm.poolCenter();
        BlockPos ledge = chasm.ledge();
        context.getSource().sendFeedback(Text.literal("[Home] Underdark Rift at chunk " + start.getPos() + ": pool "
                + pool.toShortString() + ", ledge " + ledge.toShortString() + ", mouth " + mouth.toShortString()
                + ", steps " + shaft.steps() + "; at " + spot + " " + feet.toShortString()), false);
        return 1;
    }

    /**
     * {@code /dndhome riftlair}: onto the nearest Beholder lair's floor, {@code 4} blocks east of the eye's pupil and
     * looking at it (where its death tears the rift), for scripted checks.
     */
    static int lair(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = context.getSource().getServer().getOverworld();
        StructureStart start = nearest(world, RegistryKey.of(RegistryKeys.STRUCTURE,
                new net.minecraft.util.Identifier(DnDClasses.MOD_ID, "beholder_lair")),
                player.getWorld() == world ? player.getBlockPos() : BlockPos.ORIGIN);
        mattonfire.dnd.world.gen.beholder.BeholderCavernPiece cavern = start == null ? null
                : piece(start, mattonfire.dnd.world.gen.beholder.BeholderCavernPiece.class);
        if (cavern == null) {
            context.getSource().sendError(Text.literal("[Home] No Beholder lair within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        BlockPos floor = cavern.getFloorCentre();
        player.teleport(world, floor.getX() + 4.5D, floor.getY() + 1, floor.getZ() + 0.5D, Direction.WEST.asRotation(), 30.0F);
        context.getSource().sendFeedback(Text.literal("[Home] Beholder lair floor centre " + floor.toShortString()), false);
        return 1;
    }

    static int landing(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal("[Home] This world has no Underdark"));
            return 0;
        }
        StructureStart start = nearest(underdark, UnderdarkRiftStructures.LANDING_KEY, player.getBlockPos());
        RiftLandingPiece landing = start == null ? null : piece(start, RiftLandingPiece.class);
        if (landing == null) {
            context.getSource().sendError(Text.literal("[Home] No rift landing within " + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        BlockPos feet = landing.arrival();
        // Face the anchor, so a script can use it.
        player.teleport(underdark, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, Direction.WEST.asRotation(), 25.0F);
        context.getSource().sendFeedback(Text.literal("[Home] Rift landing at chunk " + start.getPos() + ": anchor "
                + landing.anchor().toShortString() + (landing.carved() ? " (carved)" : " (cavern floor)")), false);
        return 1;
    }

    static int survey(CommandContext<ServerCommandSource> context, int grid, int spacing) {
        ServerWorld world = context.getSource().getServer().getOverworld();
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        Structure landingStructure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(UnderdarkRiftStructures.LANDING_KEY);
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[RiftSurvey]", "rifts",
                from -> nearest(world, UnderdarkRiftStructures.RIFT_KEY, from),
                start -> start.getPos().toLong(), start -> {
                    RiftChasmPiece chasm = piece(start, RiftChasmPiece.class);
                    RiftSinkholePiece shaft = piece(start, RiftSinkholePiece.class);
                    if (chasm == null || shaft == null) {
                        return new HomeCommandUtil.Check(false, "[RiftSurvey] rift at chunk " + start.getPos() + ": missing a piece: FAIL");
                    }
                    ChunkPos chunk = start.getPos();
                    boolean landing = underdark != null && landingStructure != null
                            && underdark.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS).getStructureStart(landingStructure) != null;
                    int veil = 0;
                    BlockPos pool = chasm.poolCenter();
                    for (int t = -2; t <= 2; t++) {
                        for (int s = -1; s <= 1; s++) {
                            if (world.getBlockState(chasm.at(RiftChasmPiece.POOL_T0 + 2 + t, pool.getY(), s)).isOf(RiftBlocks.RIFT_VEIL)) {
                                veil++;
                            }
                        }
                    }
                    int clear = 0;
                    List<Integer> blocked = new ArrayList<>();
                    for (int k = 0; k < shaft.steps(); k++) {
                        BlockPos feet = shaft.stepFeet(k);
                        if (world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                                && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()
                                && world.getBlockState(feet.down()).isSolidBlock(world, feet.down())) {
                            clear++;
                        } else if (blocked.size() < 5) {
                            blocked.add(k);
                        }
                    }
                    boolean ok = landing && veil == 15 && clear == shaft.steps();
                    return new HomeCommandUtil.Check(ok, "[RiftSurvey] rift at chunk " + chunk + " (pool " + pool.toShortString()
                            + ", mouth " + shaft.mouth().toShortString() + "): landing " + (landing ? "yes" : "no")
                            + ", veil " + veil + "/15, steps " + clear + "/" + shaft.steps() + " clear"
                            + (blocked.isEmpty() ? "" : " (blocked " + blocked + ")") + ": " + (ok ? "PASS" : "FAIL"));
                });
    }

    /** How far from 0, 0 the seed check looks (the acceptance's 1000 blocks). */
    private static final int SEED_RADIUS = 1000;

    static int seeds(CommandContext<ServerCommandSource> context, List<Long> seeds) {
        ServerWorld world = context.getSource().getServer().getOverworld();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        var structures = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Structure structure = structures.get(UnderdarkRiftStructures.RIFT_KEY);
        StructureSet set = world.getRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(UnderdarkRiftStructures.RIFT_SET);
        if (!(generator instanceof NoiseChunkGenerator noise) || structure == null || set == null
                || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
            context.getSource().sendError(Text.literal("[RiftSeeds] No noise generator or rift structure set"));
            return 0;
        }
        if (seeds.isEmpty()) {
            seeds = List.of(world.getSeed(), 1L, 20261025L);
        }
        int found = 0;
        for (long seed : seeds) {
            NoiseConfig config = NoiseConfig.create(noise.getSettings().value(),
                    world.getRegistryManager().getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS), seed);
            int slots = SEED_RADIUS / 16 / spread.getSpacing() + 1;
            ChunkPos best = null;
            double bestDistance = Double.MAX_VALUE;
            int tried = 0;
            for (int gx = -slots; gx <= slots; gx++) {
                for (int gz = -slots; gz <= slots; gz++) {
                    ChunkPos chunk = spread.getStartChunk(seed, gx * spread.getSpacing(), gz * spread.getSpacing());
                    double d = Math.sqrt(chunk.getCenterX() * (double) chunk.getCenterX() + chunk.getCenterZ() * (double) chunk.getCenterZ());
                    if (d > SEED_RADIUS || d >= bestDistance) {
                        continue;
                    }
                    tried++;
                    Structure.Context there = new Structure.Context(world.getRegistryManager(), generator, generator.getBiomeSource(),
                            config, world.getStructureTemplateManager(), seed, chunk, world, structure.getValidBiomes()::contains);
                    if (structure.getValidStructurePosition(there).isPresent()) {
                        best = chunk;
                        bestDistance = d;
                    }
                }
            }
            String line = "[RiftSeeds] seed " + seed + ": " + (best == null ? "no rift within " + SEED_RADIUS + " blocks"
                    : "nearest rift at " + best.getCenterX() + " " + best.getCenterZ() + ", " + (int) bestDistance + " blocks from 0 0")
                    + " (" + tried + " slots tried): " + (best == null ? "FAIL" : "PASS");
            DnDClasses.LOGGER.info(line);
            context.getSource().sendFeedback(Text.literal(line), false);
            if (best != null) {
                found++;
            }
        }
        return found;
    }

    /** Parses the {@code riftseeds} argument: seeds separated by spaces. */
    static List<Long> parseSeeds(String text) {
        List<Long> seeds = new ArrayList<>();
        Stream.of(text.trim().split("\\s+")).filter(s -> !s.isEmpty()).forEach(s -> {
            try {
                seeds.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                seeds.add((long) s.hashCode());
            }
        });
        return seeds;
    }
}
