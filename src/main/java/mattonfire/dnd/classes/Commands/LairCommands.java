package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.RuinBlocks;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.BasiliskEntity;
import mattonfire.dnd.entity.MindFlayerEntity;
import mattonfire.dnd.entity.TrollEntity;
import mattonfire.dnd.world.underdark.Captives;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.lair.BasiliskDenPiece;
import mattonfire.dnd.world.underdark.lair.IllithidColonyPiece;
import mattonfire.dnd.world.underdark.lair.TrollBridgePiece;
import mattonfire.dnd.world.underdark.lair.UnderdarkLairs.Lair;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
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
 * The Underdark monster lairs' parts of {@link DndHomeCommand} (design 9 T11; docs/structures/underdark-lairs.md).
 * {@code <lair>} is {@code basilisk}, {@code troll} or {@code illithid}. They work from anywhere: outside the
 * Underdark they search below your x and z.
 * <ul>
 * <li>{@code /dndhome underdarklair <lair> [spot]}: teleports you to the nearest lair of that kind and prints where it
 * is. Spots: {@code inside} (the default: the den's mouth, the bridge's deck, the colony's entrance looking in),
 * {@code above}, {@code chest}; the bridge also {@code side} and {@code nook}, the colony {@code core} and
 * {@code larder}.</li>
 * <li>{@code /dndhome underdarklairsurvey <lair> [grid] [spacing]}: from a grid of points round you, finds the
 * nearest lair to each and checks it ({@code [LairSurvey]}): the biome, then (generating its chunks) its blocks.</li>
 * <li>{@code /dndhome underdarklaircensus <lair>}: who lives in the nearest lair ({@code [LairCensus]}).</li>
 * <li>{@code /dndhome underdarklairseeds <lair> [seed...]}: the nearest lair to 0, 0 for each seed
 * ({@code [LairSeeds]}).</li>
 * </ul>
 */
final class LairCommands {
    static final String LAIRS = "basilisk, troll or illithid";
    static final int SEED_RADIUS = 2000;

    private LairCommands() {
    }

    @Nullable
    private static StructurePiece nearest(ServerWorld underdark, Lair lair, BlockPos from) {
        StructureStart start = HomeCommandUtil.nearestStart(underdark, lair.tag, lair.key,
                new BlockPos(from.getX(), -150, from.getZ()));
        return start == null || start.getChildren().isEmpty() ? null : start.getChildren().get(0);
    }

    @Nullable
    private static Lair lair(CommandContext<ServerCommandSource> context, String name, String tag) {
        Lair lair = Lair.byName(name);
        if (lair == null) {
            context.getSource().sendError(Text.literal(tag + " Which lair? " + LAIRS));
        }
        return lair;
    }

    @Nullable
    private static ServerWorld underdark(CommandContext<ServerCommandSource> context, String tag) {
        ServerWorld underdark = Underdark.world(context.getSource().getServer());
        if (underdark == null) {
            context.getSource().sendError(Text.literal(tag + " This world has no Underdark"));
        }
        return underdark;
    }

    static int teleport(CommandContext<ServerCommandSource> context, String name, String spot) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Lair lair = lair(context, name, "[Home]");
        ServerWorld underdark = underdark(context, "[Home]");
        if (lair == null || underdark == null) {
            return 0;
        }
        StructurePiece piece = nearest(underdark, lair, player.getBlockPos());
        if (piece == null) {
            context.getSource().sendError(Text.literal("[Home] No " + lair.id.getPath() + " within "
                    + HomeCommandUtil.SEARCH_CHUNKS + " chunks"));
            return 0;
        }
        // Make sure its chunks exist before reading heights off them.
        BlockBox box = piece.getBoundingBox();
        for (int cx = box.getMinX() >> 4; cx <= box.getMaxX() >> 4; cx++) {
            for (int cz = box.getMinZ() >> 4; cz <= box.getMaxZ() >> 4; cz++) {
                underdark.getChunk(cx, cz);
            }
        }
        Vec3d feet;
        Vec3d look;
        BlockPos center;
        String extra;
        if (piece instanceof BasiliskDenPiece den) {
            center = den.center();
            extra = "chest " + den.chest().toShortString();
            switch (spot) {
                case "inside" -> {
                    feet = Vec3d.ofBottomCenter(den.mouth());
                    look = Vec3d.ofCenter(center);
                }
                case "above" -> {
                    feet = Vec3d.ofBottomCenter(center.up(BasiliskDenPiece.HEIGHT - 2));
                    look = Vec3d.ofCenter(center).add(0.001D, -5.0D, 0.0D);
                }
                case "chest" -> {
                    feet = Vec3d.ofBottomCenter(den.at(0, 0, BasiliskDenPiece.CHEST_B + 2));
                    look = Vec3d.ofCenter(den.chest());
                }
                default -> {
                    return badSpot(context, "inside, above or chest");
                }
            }
        } else if (piece instanceof TrollBridgePiece bridge) {
            center = bridge.middle();
            extra = "length " + bridge.length() + ", nook " + bridge.nook().toShortString() + ", chest "
                    + bridge.chest().toShortString();
            switch (spot) {
                case "inside", "deck" -> {
                    feet = Vec3d.ofBottomCenter(center);
                    look = Vec3d.ofCenter(bridge.nook());
                }
                case "side" -> {
                    feet = Vec3d.ofBottomCenter(bridge.side());
                    look = Vec3d.ofCenter(bridge.nook().up(2));
                }
                case "nook" -> {
                    feet = Vec3d.ofBottomCenter(bridge.nookView());
                    look = Vec3d.ofCenter(bridge.chest());
                }
                case "above" -> {
                    feet = Vec3d.ofBottomCenter(center.up(6));
                    look = Vec3d.ofCenter(center).add(0.001D, -6.0D, 0.0D);
                }
                case "chest" -> {
                    feet = Vec3d.ofBottomCenter(bridge.nook());
                    look = Vec3d.ofCenter(bridge.chest());
                }
                default -> {
                    return badSpot(context, "inside, side, nook, above or chest");
                }
            }
        } else if (piece instanceof IllithidColonyPiece colony) {
            center = colony.center();
            extra = "pool " + colony.pool().toShortString() + ", larder gate " + colony.gate(0).toShortString()
                    + ", chest " + colony.chest().toShortString();
            switch (spot) {
                case "inside", "entrance" -> {
                    feet = Vec3d.ofBottomCenter(colony.entrance());
                    look = Vec3d.ofCenter(colony.pool());
                }
                case "core" -> {
                    feet = Vec3d.ofBottomCenter(colony.at(0, 0, 7));
                    look = Vec3d.ofCenter(colony.pool());
                }
                case "above" -> {
                    feet = Vec3d.ofBottomCenter(colony.at(0, 6, 5));
                    look = Vec3d.ofCenter(colony.pool());
                }
                case "larder" -> {
                    feet = Vec3d.ofBottomCenter(colony.at(0, 0, IllithidColonyPiece.CELL_B + 4));
                    look = Vec3d.ofCenter(colony.gate(1));
                }
                case "chest" -> {
                    feet = Vec3d.ofBottomCenter(colony.at(IllithidColonyPiece.CHEST[0] - 3, 0, IllithidColonyPiece.CHEST[1]));
                    look = Vec3d.ofCenter(colony.chest());
                }
                default -> {
                    return badSpot(context, "inside, core, above, larder or chest");
                }
            }
        } else {
            return 0;
        }
        Vec3d d = look.subtract(feet.add(0.0D, player.getStandingEyeHeight(), 0.0D));
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * 57.2957763671875D) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.2957763671875D);
        player.teleport(underdark, feet.x, feet.y, feet.z, yaw, pitch);
        String biome = underdark.getBiome(center).getKey().map(k -> k.getValue().toString()).orElse("?");
        String line = "[Home] " + lair.id.getPath() + " at chunk " + new net.minecraft.util.math.ChunkPos(center)
                + ": centre " + center.toShortString() + " in " + biome + ", " + extra + "; at " + spot + " "
                + BlockPos.ofFloored(feet).toShortString();
        DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return 1;
    }

    private static int badSpot(CommandContext<ServerCommandSource> context, String spots) {
        context.getSource().sendError(Text.literal("[Home] Which spot? " + spots));
        return 0;
    }

    static int survey(CommandContext<ServerCommandSource> context, String name, int grid, int spacing) {
        Lair lair = lair(context, name, "[LairSurvey]");
        ServerWorld underdark = underdark(context, "[LairSurvey]");
        if (lair == null || underdark == null) {
            return 0;
        }
        return HomeCommandUtil.survey(context.getSource(), grid, spacing, "[LairSurvey] " + lair.id.getPath(), "lairs",
                from -> nearest(underdark, lair, from), piece -> piece.getBoundingBox().getCenter().asLong(),
                piece -> check(underdark, lair, piece));
    }

    private static HomeCommandUtil.Check check(ServerWorld world, Lair lair, StructurePiece piece) {
        BlockBox box = piece.getBoundingBox();
        for (int cx = box.getMinX() >> 4; cx <= box.getMaxX() >> 4; cx++) {
            for (int cz = box.getMinZ() >> 4; cz <= box.getMaxZ() >> 4; cz++) {
                world.getChunk(cx, cz);
            }
        }
        TagKey<Biome> biomes = TagKey.of(RegistryKeys.BIOME, new Identifier(DnDClasses.MOD_ID,
                "has_structure/" + lair.id.getPath()));
        BlockPos center;
        String detail;
        boolean ok;
        if (piece instanceof BasiliskDenPiece den) {
            center = den.center();
            int remains = 0;
            int floor = 0;
            int floorTotal = 0;
            for (int a = -BasiliskDenPiece.RADIUS; a <= BasiliskDenPiece.RADIUS; a++) {
                for (int c = -BasiliskDenPiece.RADIUS; c <= BasiliskDenPiece.RADIUS; c++) {
                    if (a * a + c * c > (BasiliskDenPiece.RADIUS - 1) * (BasiliskDenPiece.RADIUS - 1)) {
                        continue;
                    }
                    floorTotal++;
                    BlockPos f = den.at(a, -1, c);
                    if (world.getBlockState(f).isSolidBlock(world, f)) {
                        floor++;
                    }
                    if (world.getBlockState(den.at(a, 0, c)).isOf(RuinBlocks.PETRIFIED_REMAINS)) {
                        remains++;
                    }
                }
            }
            boolean chest = world.getBlockState(den.chest()).isOf(Blocks.CHEST);
            ok = floor == floorTotal && remains >= 1 && chest;
            detail = "floor " + floor + "/" + floorTotal + ", remains " + remains + ", chest " + chest;
        } else if (piece instanceof TrollBridgePiece bridge) {
            center = bridge.middle();
            BlockState planks = ModBlockFamilies.ZURKHWOOD_PLANKS.base().getDefaultState();
            List<BlockPos> deck = bridge.deck();
            int whole = 0;
            for (BlockPos pos : deck) {
                if (world.getBlockState(pos).equals(planks)) {
                    whole++;
                }
            }
            int water = 0;
            for (int y = 2; y <= 8; y++) {
                if (!world.getFluidState(center.down(y)).isEmpty()) {
                    water++;
                }
            }
            boolean chest = world.getBlockState(bridge.chest()).isOf(Blocks.CHEST);
            boolean nook = world.getBlockState(bridge.nook()).isAir() && world.getBlockState(bridge.nook().up(2)).isAir();
            int length = bridge.length();
            ok = whole == deck.size() && water > 0 && chest && nook && length >= 14 && length <= 27;
            detail = "deck " + whole + "/" + deck.size() + " (length " + length + "), water under the middle " + water
                    + ", nook " + (nook ? "open" : "BLOCKED") + ", chest " + chest;
        } else if (piece instanceof IllithidColonyPiece colony) {
            center = colony.center();
            int water = 0;
            int magma = 0;
            for (int a = -4; a <= 4; a++) {
                for (int c = -4; c <= 4; c++) {
                    for (int y = -1 - IllithidColonyPiece.POOL_DEPTH; y <= -1; y++) {
                        BlockPos pos = colony.at(a, y, c);
                        if (world.getFluidState(pos).isStill() || world.getBlockState(pos).isOf(Blocks.BUBBLE_COLUMN)) {
                            water++;
                        } else if (world.getBlockState(pos).isOf(Blocks.MAGMA_BLOCK)) {
                            magma++;
                        }
                    }
                }
            }
            int gates = 0;
            for (int i = 0; i < IllithidColonyPiece.CELLS.length; i++) {
                for (int y = 0; y <= 1; y++) {
                    BlockState gate = world.getBlockState(colony.gate(i).up(y));
                    if (gate.isOf(UnderdarkBlocks.CHAIN_CAGE) && gate.get(ChainCageBlock.LOCKED)) {
                        gates++;
                    }
                }
            }
            boolean way = world.getBlockState(colony.entrance()).isAir() && world.getBlockState(colony.entrance().up()).isAir();
            boolean chest = world.getBlockState(colony.chest()).isOf(Blocks.CHEST);
            ok = water >= 30 && magma >= 9 && gates == 6 && way && chest;
            detail = "pool water " + water + ", magma " + magma + ", locked gates " + gates + "/6, entrance "
                    + (way ? "open" : "BLOCKED") + ", chest " + chest;
        } else {
            return null;
        }
        RegistryEntry<Biome> biome = world.getBiome(center);
        boolean biomeOk = biome.isIn(biomes);
        String biomeName = biome.getKey().map(k -> k.getValue().getPath()).orElse("?");
        ok &= biomeOk;
        return new HomeCommandUtil.Check(ok, "[LairSurvey] " + lair.id.getPath() + " at " + center.toShortString()
                + " (" + biomeName + (biomeOk ? "" : " WRONG BIOME") + "): " + detail + ": " + (ok ? "PASS" : "FAIL"));
    }

    /**
     * Counts who lives in the nearest lair (its box, 8 blocks round it): basilisks, trolls, mind flayers (and how
     * many are persistent), colony thralls (and how many a flayer holds), captives, statues.
     */
    static int census(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Lair lair = lair(context, name, "[LairCensus]");
        ServerWorld underdark = underdark(context, "[LairCensus]");
        if (lair == null || underdark == null) {
            return 0;
        }
        StructurePiece piece = nearest(underdark, lair, player.getBlockPos());
        if (piece == null) {
            context.getSource().sendError(Text.literal("[LairCensus] No " + lair.id.getPath()));
            return 0;
        }
        Box box = Box.from(piece.getBoundingBox()).expand(8.0D);
        List<Entity> all = underdark.getEntitiesByClass(Entity.class, box, Entity::isAlive);
        List<MindFlayerEntity> flayers = underdark.getEntitiesByClass(MindFlayerEntity.class, box, Entity::isAlive);
        int basilisks = 0;
        int trolls = 0;
        int persistent = 0;
        int thralls = 0;
        int held = 0;
        int captives = 0;
        int statues = 0;
        for (Entity entity : all) {
            if (entity instanceof BasiliskEntity || entity instanceof TrollEntity || entity instanceof MindFlayerEntity) {
                if (entity instanceof BasiliskEntity) {
                    basilisks++;
                } else if (entity instanceof TrollEntity) {
                    trolls++;
                }
                if (((MobEntity) entity).isPersistent()) {
                    persistent++;
                }
            }
            if (entity instanceof MobEntity mob && mob.getCommandTags().contains(MindFlayerEntity.COLONY_THRALL_TAG)) {
                thralls++;
                if (flayers.stream().anyMatch(f -> f.isThrall(mob))) {
                    held++;
                }
            }
            if (Captives.isCaptive(entity)) {
                captives++;
            }
            if (entity instanceof ArmorStandEntity) {
                statues++;
            }
        }
        String line = "[LairCensus] " + lair.id.getPath() + " at " + piece.getBoundingBox().getCenter().toShortString()
                + ": basilisks " + basilisks + ", trolls " + trolls + ", mind flayers " + flayers.size()
                + " (persistent monsters " + persistent + "), thralls " + thralls + " (held " + held + "), captives "
                + captives + ", statues " + statues;
        DnDClasses.LOGGER.info(line);
        context.getSource().sendFeedback(Text.literal(line), false);
        return 1;
    }

    static int seeds(CommandContext<ServerCommandSource> context, String name, List<Long> seeds) {
        Lair lair = lair(context, name, "[LairSeeds]");
        ServerWorld underdark = underdark(context, "[LairSeeds]");
        if (lair == null || underdark == null) {
            return 0;
        }
        return HomeCommandUtil.seeds(context.getSource(), underdark, lair.key, lair.set, seeds, SEED_RADIUS,
                "[LairSeeds] " + lair.id.getPath());
    }
}
