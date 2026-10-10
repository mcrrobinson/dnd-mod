package mattonfire.dnd.dungeon;

import java.util.List;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.classes.Blocks.FlameVentBlock;
import mattonfire.dnd.classes.Blocks.RuneBlock;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Party.PartyEvents;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * The rune-pillar puzzle at run time (design 2.7). The room's ward holds a {@link PuzzleState}; the rune
 * blocks call in here when they're turned or studied, and the ward ticks {@link #tick} every 10 ticks.
 * <ul>
 * <li>Every pillar right: the exit seal opens at once, the room counts as cleared and everyone in it gets
 * the cleared-room class XP.</li>
 * <li>All four pillars turned within {@link #ATTEMPT_WINDOW} ticks of each other, then left alone for
 * {@link #SETTLE_TICKS}, and still wrong: the flame vents burn everyone in the room (4 + 2 per tier
 * over I, Dex save for half) and the pillars shuffle. The answer stays the same.</li>
 * <li>Sneak-use a rune with an empty hand: one Investigation or Arcana check (whichever is better) per
 * player per reset, against the trap DC, to read the defaced mural.</li>
 * <li>When the dungeon resets ({@link DungeonState#resets()} moves on) the puzzle is unsolved again: the
 * exit reseals, the pillars shuffle and the lectern gets its book back.</li>
 * </ul>
 */
public final class PuzzleRooms {
    /** All four pillars turned within 5 s of each other counts as an answer. */
    public static final long ATTEMPT_WINDOW = 100L;
    /** ...once nobody has turned one for 2 s. */
    public static final long SETTLE_TICKS = 40L;
    /** Turns older than this are forgotten. */
    private static final long FORGET_TICKS = 200L;
    public static final int BURN_TICKS = 30;
    public static final int FIRE_SECONDS = 3;

    private PuzzleRooms() {
    }

    // ---------------------------------------------------------------- rune use

    /** The ward of the puzzle room a rune at {@code pos} belongs to, or null (a rune placed by hand). */
    @Nullable
    private static DungeonWardBlockEntity wardFor(ServerWorld world, BlockPos pos) {
        DungeonRegistry registry = DungeonRegistry.get(world);
        DungeonState dungeon = registry.containing(pos).orElse(null);
        if (dungeon == null) {
            return null;
        }
        DungeonState.Room room = dungeon.roomAt(pos);
        if (room == null || room.role() != RoomRole.PUZZLE) {
            return null;
        }
        DungeonWardBlockEntity ward = DungeonCombat.ward(world, dungeon, room);
        return ward != null && ward.getPuzzle() != null && ward.getPuzzle().pillarAt(pos) >= 0 ? ward : null;
    }

    /** A solved puzzle's runes no longer turn. */
    public static boolean isLocked(ServerWorld world, BlockPos pos) {
        DungeonWardBlockEntity ward = wardFor(world, pos);
        return ward != null && ward.getPuzzle().solved();
    }

    /** A rune has just been turned to its next glyph. */
    public static void onRuneTurned(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        DungeonWardBlockEntity ward = wardFor(world, pos);
        if (ward == null) {
            return;
        }
        PuzzleState puzzle = ward.getPuzzle();
        puzzle.touch(puzzle.pillarAt(pos), world.getTime());
        int[] glyphs = read(world, puzzle);
        if (glyphs != null && puzzle.matches(glyphs)) {
            DungeonRegistry registry = DungeonRegistry.get(world);
            DungeonState dungeon = registry.get(ward.getStartKey());
            DungeonState.Room room = dungeon == null ? null : dungeon.room(ward.getRoomId());
            solve(world, ward, puzzle, dungeon, room, true);
        }
    }

    /** Sneak-use: study the murals for the defaced glyph. */
    public static void study(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        DungeonWardBlockEntity ward = wardFor(world, pos);
        if (ward == null) {
            return;
        }
        PuzzleState puzzle = ward.getPuzzle();
        if (puzzle.solved()) {
            player.sendMessage(Text.translatable("puzzle.dndclasses.locked").formatted(Formatting.GOLD), true);
            return;
        }
        if (!puzzle.study(player.getUuid())) {
            player.sendMessage(Text.translatable("puzzle.dndclasses.study.again").formatted(Formatting.GRAY), true);
            return;
        }
        ward.markDirty();
        DungeonState dungeon = DungeonRegistry.get(world).get(ward.getStartKey());
        int dc = TrapKind.dc(dungeon != null ? dungeon.tier() : 1);
        Skill skill = AbilityScores.checkBonus(player, Skill.ARCANA) > AbilityScores.checkBonus(player, Skill.INVESTIGATION)
                ? Skill.ARCANA : Skill.INVESTIGATION;
        D20.Roll roll = SkillCheck.builder(player, skill, dc, "puzzle").roll();
        D20.log(player, roll);
        Text detail = roll.outcome().succeeded()
                ? Text.translatable("puzzle.dndclasses.study.success", puzzle.defacedGlyph().text())
                : Text.translatable("puzzle.dndclasses.study.failure");
        D20.show(player, roll, detail);
        player.sendMessage(detail.copy().formatted(roll.outcome().succeeded() ? Formatting.AQUA : Formatting.GRAY), true);
        DnDClasses.LOGGER.info("[Puzzle] {} studies the murals with {}: {}", player.getEntityName(), skill,
                roll.outcome().succeeded() ? "reads " + puzzle.defacedGlyph().id() : "fails");
    }

    // ---------------------------------------------------------------- ticking

    /** Every {@link DungeonWardBlockEntity#CHECK_INTERVAL} ticks, whether or not anyone is inside. */
    public static void tick(ServerWorld world, DungeonWardBlockEntity ward, PuzzleState puzzle, DungeonState dungeon,
                            List<ServerPlayerEntity> inside) {
        if (!loaded(world, puzzle)) {
            return;
        }
        long now = world.getTime();
        DungeonState.Room room = dungeon.room(ward.getRoomId());
        if (puzzle.resetsSeen() != dungeon.resets()) {
            reset(world, ward, puzzle, dungeon);
        }
        if (!puzzle.solved() && room != null && room.state() == RoomState.CLEARED) {
            // Cleared from outside (/dungeon clear, the boss's death): every ward opens.
            solve(world, ward, puzzle, dungeon, room, false);
        }
        setSeal(world, puzzle, !puzzle.solved());
        lightVents(world, puzzle, false);
        if (puzzle.solved() || puzzle.lastTouch() == 0L) {
            return;
        }
        long quiet = now - puzzle.lastTouch();
        if (quiet >= SETTLE_TICKS && puzzle.allTouchedWithin(ATTEMPT_WINDOW)) {
            int[] glyphs = read(world, puzzle);
            if (glyphs != null && puzzle.matches(glyphs)) {
                solve(world, ward, puzzle, dungeon, room, true);
            } else {
                wrong(world, ward, puzzle, dungeon, room);
            }
        } else if (quiet > FORGET_TICKS) {
            puzzle.clearTouches();
        }
    }

    private static boolean loaded(ServerWorld world, PuzzleState puzzle) {
        for (BlockPos pos : puzzle.pillars()) {
            if (!world.isChunkLoaded(pos)) {
                return false;
            }
        }
        for (BlockPos pos : puzzle.seal()) {
            if (!world.isChunkLoaded(pos)) {
                return false;
            }
        }
        return true;
    }

    /** The pillars' glyphs, or null if one of them is missing. */
    @Nullable
    private static int[] read(ServerWorld world, PuzzleState puzzle) {
        int[] glyphs = new int[PuzzleState.PILLARS];
        for (int i = 0; i < PuzzleState.PILLARS && i < puzzle.pillars().size(); i++) {
            BlockState state = world.getBlockState(puzzle.pillars().get(i));
            if (!(state.getBlock() instanceof RuneBlock)) {
                return null;
            }
            glyphs[i] = state.get(RuneBlock.GLYPH);
        }
        return glyphs;
    }

    private static void setGlyphs(ServerWorld world, PuzzleState puzzle, int[] glyphs) {
        for (int i = 0; i < PuzzleState.PILLARS && i < puzzle.pillars().size(); i++) {
            BlockPos pos = puzzle.pillars().get(i);
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof RuneBlock) {
                world.setBlockState(pos, state.with(RuneBlock.GLYPH, glyphs[i]), Block.NOTIFY_ALL);
            }
        }
    }

    private static void solve(ServerWorld world, DungeonWardBlockEntity ward, PuzzleState puzzle, @Nullable DungeonState dungeon,
                              @Nullable DungeonState.Room room, boolean byPlayers) {
        puzzle.setSolved(true);
        puzzle.clearTouches();
        ward.markDirty();
        setSeal(world, puzzle, false);
        lightVents(world, puzzle, false);
        if (!byPlayers || dungeon == null || room == null) {
            DnDClasses.LOGGER.info("[Puzzle] room #{} opened from outside", ward.getRoomId());
            return;
        }
        List<ServerPlayerEntity> players = DungeonCombat.playersIn(world, room);
        DnDClasses.LOGGER.info("[Puzzle] room #{} solved ({}), {} player(s) inside", ward.getRoomId(),
                PuzzleState.describe(puzzle.solution()), players.size());
        BlockPos centre = room.box().getCenter();
        world.playSound(null, centre, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.5F, 0.8F);
        world.playSound(null, centre, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.5F, 1.2F);
        for (BlockPos pillar : puzzle.pillars()) {
            world.spawnParticles(ParticleTypes.END_ROD, pillar.getX() + 0.5, pillar.getY() + 0.5, pillar.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.05);
        }
        if (room.state() != RoomState.CLEARED) {
            dungeon.setRoomState(room, RoomState.CLEARED);
            int xp = DungeonWardBlockEntity.ROOM_XP_PER_TIER * dungeon.tier();
            for (ServerPlayerEntity player : players) {
                PartyEvents.shareXp(player, xp, DungeonWardBlockEntity.XP_CHANNEL, Progression::addXp);
            }
            DungeonEvents.ROOM_CLEARED.invoker().onRoomCleared(world, dungeon, room, players);
        }
        for (ServerPlayerEntity player : players) {
            player.sendMessage(Text.translatable("puzzle.dndclasses.solved").formatted(Formatting.GREEN), true);
        }
    }

    /** A wrong answer: the vents burn everyone in the room and the pillars shuffle. */
    private static void wrong(ServerWorld world, DungeonWardBlockEntity ward, PuzzleState puzzle, DungeonState dungeon,
                              @Nullable DungeonState.Room room) {
        puzzle.addWrongAnswer();
        puzzle.clearTouches();
        long now = world.getTime();
        puzzle.burnUntil = now + BURN_TICKS;
        lightVents(world, puzzle, true);
        int tier = dungeon.tier();
        float base = 4.0F + 2.0F * (tier - 1);
        int dc = TrapKind.dc(tier);
        world.playSound(null, room != null ? room.box().getCenter() : ward.getPos(), SoundEvents.ITEM_FIRECHARGE_USE,
                SoundCategory.BLOCKS, 1.5F, 0.7F);
        for (BlockPos vent : puzzle.vents()) {
            world.spawnParticles(ParticleTypes.FLAME, vent.getX() + 0.5, vent.getY() + 1.2, vent.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.03);
        }
        if (room != null) {
            Box box = Box.from(room.box()).contract(2.0, 0.0, 2.0);
            for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, box,
                    e -> e.isAlive() && !(e instanceof ServerPlayerEntity p && (p.isCreative() || p.isSpectator())))) {
                SaveResult save = SavingThrow.of(victim, Ability.DEX, dc).label("save.dndclasses.trap.flame")
                        .exposure("puzzle:" + ward.getPos().asLong(), "flame", 40).tags("trap").halvesDamage().roll();
                float damage = save.damage(base);
                victim.damage(ModDamageTypes.of(world, ModDamageTypes.TRAP), damage);
                if (save.failed()) {
                    victim.setOnFireFor(FIRE_SECONDS);
                }
                DnDClasses.LOGGER.info("[Puzzle] wrong answer burns {} for {} ({})", victim.getName().getString(), damage,
                        save.succeeded() ? "saved, half" : "failed save, full");
            }
            for (ServerPlayerEntity player : DungeonCombat.playersIn(world, room)) {
                player.sendMessage(Text.translatable("puzzle.dndclasses.wrong").formatted(Formatting.RED), true);
            }
        }
        setGlyphs(world, puzzle, puzzle.shuffled(world.getRandom()));
        ward.markDirty();
        DnDClasses.LOGGER.info("[Puzzle] room #{} wrong answer #{}: pillars shuffled", ward.getRoomId(), puzzle.wrongAnswers());
    }

    /** The dungeon was reset: unsolved, resealed, shuffled, with the riddle back on its lectern. */
    private static void reset(ServerWorld world, DungeonWardBlockEntity ward, PuzzleState puzzle, DungeonState dungeon) {
        puzzle.setSolved(false);
        puzzle.clearTouches();
        puzzle.clearStudied();
        puzzle.setResetsSeen(dungeon.resets());
        setGlyphs(world, puzzle, puzzle.shuffled(world.getRandom()));
        setSeal(world, puzzle, true);
        BlockPos lectern = puzzle.lectern();
        if (lectern != null && world.isChunkLoaded(lectern) && world.getBlockEntity(lectern) instanceof LecternBlockEntity be
                && !be.hasBook()) {
            ItemStack book = riddleBook(dungeon.theme(), puzzle);
            be.setBook(book);
            LecternBlock.setHasBook(null, world, lectern, world.getBlockState(lectern), true);
        }
        ward.markDirty();
        DnDClasses.LOGGER.info("[Puzzle] room #{} reset with the dungeon (reset {})", ward.getRoomId(), dungeon.resets());
    }

    /** Fills (or opens) the exit doorway with ward seals, skipping blocks someone is standing in. */
    private static void setSeal(ServerWorld world, PuzzleState puzzle, boolean sealed) {
        for (BlockPos pos : puzzle.seal()) {
            BlockState state = world.getBlockState(pos);
            if (sealed && state.isAir() && world.getEntitiesByClass(LivingEntity.class, new Box(pos), e -> true).isEmpty()) {
                world.setBlockState(pos, ModBlocks.ARCANE_SEAL.getDefaultState(), Block.NOTIFY_ALL);
            } else if (!sealed && state.isOf(ModBlocks.ARCANE_SEAL)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    private static void lightVents(ServerWorld world, PuzzleState puzzle, boolean lit) {
        if (!lit && world.getTime() < puzzle.burnUntil) {
            return;
        }
        for (BlockPos pos : puzzle.vents()) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof FlameVentBlock && state.get(Properties.LIT) != lit) {
                world.setBlockState(pos, state.with(Properties.LIT, lit), Block.NOTIFY_ALL);
            }
        }
    }

    // ---------------------------------------------------------------- testing

    /** Where pillar {@code i}'s rune is (clockwise from the north-west), from the room's box alone. */
    public static BlockPos pillarSpot(DungeonState dungeon, DungeonState.Room room, int i) {
        int[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        BlockPos centre = room.box().getCenter();
        return new BlockPos(centre.getX() + corners[i][0] * 3, dungeon.floorY() + 2, centre.getZ() + corners[i][1] * 3);
    }

    /**
     * Turns every rune to one glyph short of a target, so one use on each pillar reaches it: the answer
     * (free pillars aim at glyph 0), or, for {@code right} false, the answer with one set pillar off by one.
     * Returns the targets.
     */
    public static int[] prime(ServerWorld world, PuzzleState puzzle, boolean right) {
        int[] targets = puzzle.solution();
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == PuzzleState.FREE) {
                targets[i] = 0;
            }
        }
        if (!right) {
            int k = puzzle.solution()[0] == PuzzleState.FREE ? 1 : 0;
            targets[k] = (targets[k] + 1) % 6;
        }
        int[] glyphs = new int[targets.length];
        for (int i = 0; i < targets.length; i++) {
            glyphs[i] = Math.floorMod(targets[i] - 1, 6);
        }
        puzzle.clearTouches();
        setGlyphs(world, puzzle, glyphs);
        return targets;
    }

    // ---------------------------------------------------------------- the riddle

    /** The written book on the lectern: the theme's inscription, and a riddle naming the defaced glyph. */
    public static ItemStack riddleBook(DungeonTheme theme, PuzzleState puzzle) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "The Warden's Riddle");
        nbt.putString("author", "a nameless warden");
        nbt.putBoolean("resolved", true);
        NbtList pages = new NbtList();
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("puzzle.dndclasses.book.intro." + theme.id()))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("puzzle.dndclasses.book.rules"))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.translatable("puzzle.dndclasses.riddle." + puzzle.defacedGlyph().id()))));
        nbt.put("pages", pages);
        return book;
    }
}
