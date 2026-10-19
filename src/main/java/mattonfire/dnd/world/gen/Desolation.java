package mattonfire.dnd.world.gen;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Desolate settlements: abandoned copies of the settlements, listed in the {@code dndclasses:desolate} structure
 * tag. They're built by the same pieces as the living ones, and each start rolls how far gone it is, a
 * {@link DesolationLevel}: raided, abandoned, forsaken or overgrown. {@code StructureStartPlacingMixin} runs this
 * round each chunk's slice of the start:
 * <ul>
 *   <li>while the pieces are placed, no entity is added ({@link #active()}, checked by {@code DesolateSpawnMixin}):
 *   no residents, guards, mimics, item frames, armour stands or minecarts, and no animals in a new chunk it
 *   covers ({@link #covers}). Pieces with their own ruin ask {@link #level()}: the elven rope bridges fall
 *   ({@code BridgePiece}) and the Cogsdeep observatory's dome caves in ({@code ObservatoryPiece});</li>
 *   <li>afterwards {@link Withering} empties the containers, loots the gold, and wears the place down to its
 *   level.</li>
 * </ul>
 * The structure JSONs have no mob spawns, and nothing that checks the living settlements' ids or tags (home
 * bonuses, raids, factions, quests) sees these.
 */
public final class Desolation {
    public static final TagKey<Structure> DESOLATE = TagKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "desolate"));

    /** The level of the desolate start placing its pieces on this thread, or null. */
    private static final ThreadLocal<DesolationLevel> LEVEL = new ThreadLocal<>();
    /** Entities dropped on this thread since the start began placing, for the debug log. */
    private static final ThreadLocal<int[]> DROPPED = ThreadLocal.withInitial(() -> new int[1]);
    /** The level {@code /dndhome desolation} gives every desolate start placed from now on, or null to roll it. */
    @Nullable
    private static volatile DesolationLevel forced;

    private Desolation() {
    }

    /** Whether a desolate structure is placing its pieces on this thread (so entities are dropped). */
    public static boolean active() {
        return LEVEL.get() != null;
    }

    /** The level of the desolate structure placing its pieces on this thread, or null if there isn't one. */
    @Nullable
    public static DesolationLevel level() {
        return LEVEL.get();
    }

    public static boolean isDesolate(StructureWorldAccess world, StructureStart start) {
        Registry<Structure> structures = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        return structures.getEntry(start.getStructure()).isIn(DESOLATE);
    }

    /** The level {@code start} rolled: the same in every chunk, from where it starts. */
    public static DesolationLevel levelOf(StructureStart start) {
        if (forced != null) {
            return forced;
        }
        ChunkPos pos = start.getPos();
        return DesolationLevel.pick(roll(pos.toLong(), pos.x, 0, pos.z, 0x5EED));
    }

    /**
     * Gives every desolate start placed from now on {@code level} (null: roll it again), for testing. Not saved; a
     * start whose chunks are placed partly before and partly after a change comes out mixed.
     */
    public static void force(@Nullable DesolationLevel level) {
        forced = level;
    }

    /** Marks {@code start} as placing on this thread if it's desolate (called before its pieces are placed). */
    public static void begin(StructureWorldAccess world, StructureStart start) {
        if (start.hasChildren() && isDesolate(world, start)) {
            LEVEL.set(levelOf(start));
            DROPPED.get()[0] = 0;
        } else {
            LEVEL.remove();
        }
    }

    /** After the pieces are placed: withers {@code start}'s slice of {@code chunkBox} if it's desolate. */
    public static void end(StructureWorldAccess world, StructureStart start, BlockBox chunkBox, ChunkGenerator generator) {
        DesolationLevel level = LEVEL.get();
        if (level == null) {
            return;
        }
        LEVEL.remove();
        int[] counts = new Withering(world, start, chunkBox, generator, level).run();
        DnDClasses.LOGGER.debug("[Desolation] {} ({}) at {}: chunk {} {}: {} entities dropped, {} containers emptied, {} blocks withered, {} breaches",
                world.getRegistryManager().get(RegistryKeys.STRUCTURE).getId(start.getStructure()), level, start.getPos(),
                chunkBox.getMinX() >> 4, chunkBox.getMinZ() >> 4, DROPPED.get()[0], counts[0], counts[1], counts[2]);
    }

    /** Counts an entity dropped by {@code DesolateSpawnMixin}. */
    public static void dropped() {
        DROPPED.get()[0]++;
    }

    /**
     * Whether a desolate settlement reaches into the chunk {@code region} is centred on, so the animals vanilla
     * puts in a new chunk ({@code DesolateAnimalsMixin}) aren't added: structure spawn overrides don't cover those.
     */
    public static boolean covers(ChunkRegion region) {
        ChunkPos center = region.getCenterPos();
        Registry<Structure> structures = region.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (Map.Entry<Structure, LongSet> reference : region.getChunk(center.x, center.z).getStructureReferences().entrySet()) {
            if (!reference.getValue().isEmpty() && structures.getEntry(reference.getKey()).isIn(DESOLATE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A number in [0, 1) fixed by {@code salt} (a structure or piece), the block position and {@code k} (which
     * decision it is), so every chunk agrees and separate decisions about one block don't line up.
     */
    public static float roll(long salt, int x, int y, int z, int k) {
        long h = MathHelper.hashCode(x, y, z) ^ salt * 0x9E3779B97F4A7C15L ^ k * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ h >>> 33) * 0xFF51AFD7ED558CCDL;
        h = (h ^ h >>> 33) * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1 << 24);
    }

    /** Smooth noise in [0, 1) over x/z, in blobs about {@code size} blocks across: for patches of ground. */
    public static float patches(long salt, int x, int z, int size, int k) {
        int cx = Math.floorDiv(x, size);
        int cz = Math.floorDiv(z, size);
        float fx = (x - cx * size) / (float) size;
        float fz = (z - cz * size) / (float) size;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = roll(salt, cx, 0, cz, k);
        float b = roll(salt, cx + 1, 0, cz, k);
        float c = roll(salt, cx, 0, cz + 1, k);
        float d = roll(salt, cx + 1, 0, cz + 1, k);
        float value = MathHelper.lerp(fz, MathHelper.lerp(fx, a, b), MathHelper.lerp(fx, c, d));
        // Interpolating flattens the spread towards 0.5: stretch it back out.
        return MathHelper.clamp((value - 0.5F) * 1.6F + 0.5F, 0.0F, 0.999F);
    }
}
