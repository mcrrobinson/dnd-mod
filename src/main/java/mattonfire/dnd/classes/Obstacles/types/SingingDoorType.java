package mattonfire.dnd.classes.Obstacles.types;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Items.InstrumentItem;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.magic.Attunement;
import mattonfire.dnd.magic.items.DossLuteItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A stone face door with note blocks (its "keys") beside it, within {@value #KEY_RANGE} blocks.
 * <ul>
 * <li><b>Bard:</b> right-click it holding an instrument for a Performance check; a success opens it at once.
 * An attuned Doss Lute (a bound instrument) adds +{@value #BOUND_BONUS}.</li>
 * <li><b>Tune puzzle (anyone):</b> right-click it with an empty hand and it sings a phrase of {@value #TUNE_LENGTH}
 * notes, each one played on one of its keys. Play the phrase back on the keys, in order, within
 * {@value #NOTE_TIMEOUT_TICKS} ticks of each other, and it opens (no class XP). A wrong key starts over.</li>
 * </ul>
 * The phrase comes from the group's {@code GroupSeed} and the door's position, and is kept in
 * {@code Data.Tune} (indexes into the keys, sorted by x, then z, then y) once first played, so a DM can set it
 * with {@code /data}. While a door is sealed, its keys play without changing pitch.
 */
public final class SingingDoorType extends ObstacleType {
    public static final int KEY_RANGE = 4;
    public static final int MAX_KEYS = 8;
    public static final int TUNE_LENGTH = 4;
    public static final int NOTE_GAP_TICKS = 12;
    public static final int NOTE_TIMEOUT_TICKS = 200;
    public static final int BOUND_BONUS = 2;
    public static final String TUNE_KEY = "Tune";

    private record Note(ServerWorld world, BlockPos key, long at) {
    }

    private record Progress(int index, long last) {
    }

    /** Notes the doors are singing, in time order. */
    private final List<Note> playing = new ArrayList<>();
    /** Door (its first block) to the player's progress through its phrase. */
    private final Map<Long, Progress> progress = new HashMap<>();

    public SingingDoorType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.PERFORMANCE;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.BARD ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public boolean accepts(PlayerEntity player, ItemStack held) {
        return held.isEmpty() || held.getItem() instanceof InstrumentItem;
    }

    /** An empty hand hears the tune (Bards too); only an instrument rolls. */
    @Override
    public boolean intercept(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        if (!player.getMainHandStack().isEmpty()) {
            return false;
        }
        sing(world, pos, player);
        return true;
    }

    @Override
    public D20.Builder rollExtras(D20.Builder builder, ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        if (held.getItem() instanceof DossLuteItem && Attunement.isActive(player, held)) {
            builder.bonus("obstacle.dndclasses.bound_instrument", BOUND_BONUS);
        }
        return builder;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    /** A natural 1: the face shrieks (and raises the alarm). */
    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        world.playSound(null, pos, SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.BLOCKS, 2.0f, 0.7f);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.4, 0.3,
                0.4, 1.0);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 1.5f, 1.0f);
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.5f, 1.26f);
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.8f, 0.6f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.8f, 0.6f);
        }
    }

    // ---- The tune ----

    private static final Comparator<BlockPos> ORDER = Comparator.comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getY);

    /** The door's first block (lowest x, z, y): where its tune lives, and its key in {@link #progress}. */
    private static BlockPos first(List<BlockPos> group) {
        return group.stream().min(ORDER).orElseThrow();
    }

    /** The note blocks within {@value #KEY_RANGE} blocks of the door, in key order. */
    public static List<BlockPos> keys(World world, List<BlockPos> group) {
        List<BlockPos> keys = new ArrayList<>();
        if (group.isEmpty()) {
            return keys;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : group) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        for (BlockPos pos : BlockPos.iterate(minX - KEY_RANGE, minY - KEY_RANGE, minZ - KEY_RANGE,
                maxX + KEY_RANGE, maxY + KEY_RANGE, maxZ + KEY_RANGE)) {
            if (world.getBlockState(pos).isOf(Blocks.NOTE_BLOCK)) {
                keys.add(pos.toImmutable());
            }
        }
        keys.sort(ORDER);
        return keys.size() > MAX_KEYS ? keys.subList(0, MAX_KEYS) : keys;
    }

    /** The door's phrase as key indexes, made (and saved on every block) the first time it's asked for. */
    private int[] tune(ServerWorld world, List<BlockPos> group, int keyCount) {
        BlockPos first = first(group);
        if (world.getBlockEntity(first) instanceof ObstacleBlockEntity be) {
            int[] saved = be.data().getIntArray(TUNE_KEY);
            if (saved.length > 0 && java.util.Arrays.stream(saved).allMatch(i -> i >= 0 && i < keyCount)) {
                return saved;
            }
            Random random = Random.create(be.groupSeed() ^ first.asLong());
            int[] tune = new int[TUNE_LENGTH];
            for (int i = 0; i < TUNE_LENGTH; i++) {
                tune[i] = random.nextInt(keyCount);
            }
            if (java.util.Arrays.stream(tune).allMatch(i -> i == tune[0])) {
                tune[TUNE_LENGTH - 1] = (tune[0] + 1) % keyCount;
            }
            for (BlockPos pos : group) {
                if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity block) {
                    block.data().putIntArray(TUNE_KEY, tune);
                    block.markDirty();
                }
            }
            return tune;
        }
        return new int[0];
    }

    /** The door sings its phrase on its keys. */
    private void sing(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        List<BlockPos> group = ObstacleGroups.group(world, pos);
        List<BlockPos> keys = keys(world, group);
        if (keys.size() < 2) {
            player.sendMessage(Text.translatable(translationKey() + ".no_keys"), true);
            return;
        }
        int[] tune = tune(world, group, keys.size());
        long now = world.getTime();
        if (playing.stream().anyMatch(note -> note.world() == world && keys.contains(note.key()))) {
            return; // already singing
        }
        progress.remove(first(group).asLong());
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.BLOCKS, 0.6f, 0.5f);
        world.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.4, 0.2,
                0.4, 1.0);
        for (int i = 0; i < tune.length; i++) {
            playing.add(new Note(world, keys.get(tune[i]), now + (long) (i + 1) * NOTE_GAP_TICKS));
        }
        player.sendMessage(Text.translatable(translationKey() + ".sings", tune.length), true);
        StringBuilder phrase = new StringBuilder();
        for (int index : tune) {
            phrase.append(' ').append(keys.get(index).toShortString().replace(" ", ""));
        }
        DnDClasses.LOGGER.info("[Obstacle] singing door at {} sings {} notes:{}", first(group).toShortString(),
                tune.length, phrase);
    }

    /** Plays a key as a note block would, without its pitch changing. */
    private static void playKey(ServerWorld world, BlockPos key) {
        world.addSyncedBlockEvent(key, Blocks.NOTE_BLOCK, 0, 0);
        world.spawnParticles(ParticleTypes.NOTE, key.getX() + 0.5, key.getY() + 1.2, key.getZ() + 0.5, 1, 0, 0, 0,
                world.getBlockState(key).get(NoteBlock.NOTE) / 24.0);
    }

    /** The sealed singing door a key belongs to (its group's first block), or null. */
    @Nullable
    private List<BlockPos> doorOf(World world, BlockPos key) {
        for (BlockPos pos : BlockPos.iterate(key.add(-KEY_RANGE, -KEY_RANGE, -KEY_RANGE),
                key.add(KEY_RANGE, KEY_RANGE, KEY_RANGE))) {
            BlockState state = world.getBlockState(pos);
            if (isSealedMine(state)) {
                List<BlockPos> group = ObstacleGroups.group(world, pos.toImmutable());
                if (keys(world, group).contains(key)) {
                    return group;
                }
            }
        }
        return null;
    }

    /** A player played a key of a sealed door. */
    private void pressed(ServerWorld world, List<BlockPos> group, BlockPos key, ServerPlayerEntity player) {
        List<BlockPos> keys = keys(world, group);
        int pressed = keys.indexOf(key);
        if (pressed < 0) {
            return;
        }
        int[] tune = tune(world, group, keys.size());
        BlockPos door = first(group);
        long now = world.getTime();
        Progress was = progress.get(door.asLong());
        int index = was == null || now - was.last() > NOTE_TIMEOUT_TICKS ? 0 : was.index();
        if (tune[index] == pressed) {
            index++;
        } else {
            index = tune[0] == pressed ? 1 : 0;
            world.playSound(null, door, SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 0.8f,
                    0.5f);
            player.sendMessage(Text.translatable(translationKey() + ".off_key"), true);
        }
        DnDClasses.LOGGER.info("[Obstacle] {} played key {} of the singing door at {}: {}/{}",
                player.getEntityName(), pressed, door.toShortString(), index, tune.length);
        if (index >= tune.length) {
            progress.remove(door.asLong());
            // The puzzle is the fallback: no class XP
            ObstacleGroups.open(world, door);
            player.sendMessage(Text.translatable(translationKey() + ".echoed"), true);
            return;
        }
        progress.put(door.asLong(), new Progress(index, now));
    }

    // ---- Events ----

    @Override
    public void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        // Right-click: a key of a sealed door plays its note, without the vanilla pitch change
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> keyUsed(player, world, hand, hit.getBlockPos()));
        // Left-click: plays it too, without starting to break it
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> keyUsed(player, world, hand, pos));
    }

    private ActionResult keyUsed(PlayerEntity player, World world, Hand hand, BlockPos pos) {
        if (hand != Hand.MAIN_HAND || player.isSpectator() || !world.getBlockState(pos).isOf(Blocks.NOTE_BLOCK)
                || player.isCreative() && player.isSneaking()) {
            return ActionResult.PASS; // creative + sneak: edit the keys as usual
        }
        List<BlockPos> group = doorOf(world, pos);
        if (group == null) {
            return ActionResult.PASS;
        }
        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
            playKey(serverWorld, pos);
            pressed(serverWorld, group, pos.toImmutable(), serverPlayer);
        }
        return ActionResult.SUCCESS;
    }

    private void tick(MinecraftServer server) {
        if (playing.isEmpty()) {
            return;
        }
        playing.removeIf(note -> {
            if (note.world().getTime() < note.at()) {
                return false;
            }
            if (note.world().getBlockState(note.key()).isOf(Blocks.NOTE_BLOCK)) {
                playKey(note.world(), note.key());
            }
            return true;
        });
    }
}
