package mattonfire.dnd.entity.raid;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;

/**
 * How much of a raided settlement is still standing. It starts at {@link #MAX} and the raid wears it down:
 * settlement folk killed, buildings broken or burned, and raiders holding the rally point (village green,
 * gate, Moonwell, courtyard) with no one contesting it. At 0 the settlement has fallen and the raid is lost;
 * a raid won with less left pays out less (see {@link #rewardScale()}).
 *
 * <p>Buildings are tracked by sampling: when the raid starts, up to {@link #SAMPLE_SIZE} building blocks
 * (not air, plants, fluids or natural ground) are picked at random inside the settlement's structure pieces.
 * Every {@link #CHECK_INTERVAL} ticks each sampled block that has become air, fire or another replaceable
 * block counts as lost, whatever broke it, except blocks a player broke by hand. Losing every sampled block
 * costs {@link #BUILDINGS_WEIGHT} health, so each lost block costs {@code BUILDINGS_WEIGHT / sample size}.
 *
 * <p>Every number is a constant here.
 */
public final class SettlementHealth {
    /** Health at the start of a raid. */
    public static final float MAX = 100.0F;
    /** Lost for each member of the settlement's faction (hobbit, dwarf, elf, templar, ...) killed during the raid. */
    public static final float FOLK_KILLED = 10.0F;
    /** Folk count if they die within this many blocks (horizontally) of the rally point. */
    public static final double FOLK_RADIUS = 64.0D;
    /** Lost if every sampled building block is gone: 3 health per 1% of the buildings. */
    public static final float BUILDINGS_WEIGHT = 300.0F;
    /** Building blocks sampled when the raid starts. */
    public static final int SAMPLE_SIZE = 512;
    /** With fewer building blocks than this (open ground), buildings don't count. */
    public static final int MIN_SAMPLE = 32;
    /** At most this many positions are looked at when sampling; bigger settlements are thinned out. */
    public static final int SAMPLE_BUDGET = 400_000;
    /** Ticks between building checks. */
    public static final int CHECK_INTERVAL = 20;
    /** Raiders within this many blocks (horizontally, 4 vertically) of the rally point hold it... */
    public static final double HOLD_RADIUS = 8.0D;
    /** ...and wear the settlement down by this much a second, unless a defender or folk stands there too. */
    public static final float HOLD_PER_SECOND = 0.5F;
    /** A won raid with at least this much health left pays out full loot; below it, loot is a chance of health / this. */
    public static final float FULL_LOOT_HEALTH = 50.0F;

    private float health = MAX;
    /** Whether the buildings have been sampled (raids saved before settlement health sample on their next tick). */
    private boolean sampled;
    private final LongOpenHashSet sample = new LongOpenHashSet();
    private int sampleSize;
    private int lost;
    private int folkKilled;

    public float get() {
        return this.health;
    }

    /** Health shown to players: rounded up, so it only reads 0 once the settlement has fallen. */
    public int shown() {
        return MathHelper.ceil(this.health);
    }

    public boolean hasFallen() {
        return this.health <= 0.0F;
    }

    public void set(float health) {
        this.health = MathHelper.clamp(health, 0.0F, MAX);
    }

    public void damage(float amount) {
        this.set(this.health - amount);
    }

    public int folkKilled() {
        return this.folkKilled;
    }

    public void folkKilled(int count) {
        this.folkKilled += count;
        this.damage(FOLK_KILLED * count);
    }

    public int buildingsLost() {
        return this.lost;
    }

    public int sampleSize() {
        return this.sampleSize;
    }

    public boolean isSampled() {
        return this.sampled;
    }

    /** Share of the victory rewards (XP, settlement reputation) a win pays out: the health left, as a fraction. */
    public float rewardScale() {
        return this.health / MAX;
    }

    /** Chance of each victory loot roll paying out. */
    public float lootChance() {
        return Math.min(1.0F, this.health / FULL_LOOT_HEALTH);
    }

    // ---- Buildings ----

    /** Picks the building blocks to watch, in loaded chunks inside {@code boxes}. */
    public void takeSample(ServerWorld world, List<BlockBox> boxes, Random random) {
        this.sampled = true;
        this.sample.clear();
        long volume = 0;
        for (BlockBox box : boxes) {
            volume += (long) box.getBlockCountX() * box.getBlockCountY() * box.getBlockCountZ();
        }
        float keep = volume <= SAMPLE_BUDGET ? 1.0F : (float) SAMPLE_BUDGET / volume;
        // Reservoir sampling over the building blocks found.
        long[] reservoir = new long[SAMPLE_SIZE];
        int seen = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (BlockBox box : boxes) {
            int minY = Math.max(box.getMinY(), world.getBottomY());
            int maxY = Math.min(box.getMaxY(), world.getTopY() - 1);
            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) {
                        continue;
                    }
                    for (int y = minY; y <= maxY; y++) {
                        if (keep < 1.0F && random.nextFloat() >= keep) {
                            continue;
                        }
                        pos.set(x, y, z);
                        if (!isBuilding(world, pos, world.getBlockState(pos))) {
                            continue;
                        }
                        if (seen < SAMPLE_SIZE) {
                            reservoir[seen] = pos.asLong();
                        } else {
                            int slot = random.nextInt(seen + 1);
                            if (slot < SAMPLE_SIZE) {
                                reservoir[slot] = pos.asLong();
                            }
                        }
                        seen++;
                    }
                }
            }
        }
        for (int i = 0; i < Math.min(seen, SAMPLE_SIZE); i++) {
            this.sample.add(reservoir[i]);
        }
        this.sampleSize = this.sample.size() >= MIN_SAMPLE ? this.sample.size() : 0;
        if (this.sampleSize == 0) {
            this.sample.clear();
        }
    }

    /**
     * Counts sampled blocks that are gone since the last check and takes their health. Returns how many were
     * lost this time.
     */
    public int checkBuildings(ServerWorld world) {
        if (this.sampleSize == 0) {
            return 0;
        }
        int gone = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        var iterator = this.sample.iterator();
        while (iterator.hasNext()) {
            pos.set(iterator.nextLong());
            if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.isIn(BlockTags.FIRE) || state.getMaterial().isReplaceable()) {
                iterator.remove();
                gone++;
            }
        }
        if (gone > 0) {
            this.lost += gone;
            this.damage(BUILDINGS_WEIGHT * gone / this.sampleSize);
        }
        return gone;
    }

    /** A player broke the block at {@code pos} by hand: it no longer counts either way. */
    public void playerBroke(BlockPos pos) {
        if (this.sample.remove(pos.asLong())) {
            // The weight of the blocks still watched stays the same.
            this.sampleSize = Math.max(1, this.sampleSize - 1);
        }
    }

    /** A block worth watching: solid, built, not natural ground, plants or fluids. */
    static boolean isBuilding(BlockView world, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getMaterial().isReplaceable()
                || state.getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        return !(state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.SAND)
                || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.ICE) || state.isIn(BlockTags.SNOW)
                || state.isIn(BlockTags.TERRACOTTA) || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.BEDROCK)
                || state.isOf(Blocks.DIRT_PATH) || state.isOf(Blocks.FARMLAND) || state.isOf(Blocks.CLAY));
    }

    // ---- Saving ----

    void writeNbt(NbtCompound nbt) {
        nbt.putFloat("Health", this.health);
        nbt.putBoolean("Sampled", this.sampled);
        nbt.putLongArray("Sample", this.sample.toLongArray());
        nbt.putInt("SampleSize", this.sampleSize);
        nbt.putInt("Lost", this.lost);
        nbt.putInt("FolkKilled", this.folkKilled);
    }

    /** Reads what {@link #writeNbt} wrote; a raid saved before settlement health starts at full health, unsampled. */
    static SettlementHealth fromNbt(NbtCompound nbt) {
        SettlementHealth health = new SettlementHealth();
        if (!nbt.contains("Health")) {
            return health;
        }
        health.set(nbt.getFloat("Health"));
        health.sampled = nbt.getBoolean("Sampled");
        for (long pos : nbt.getLongArray("Sample")) {
            health.sample.add(pos);
        }
        health.sampleSize = nbt.getInt("SampleSize");
        health.lost = nbt.getInt("Lost");
        health.folkKilled = nbt.getInt("FolkKilled");
        return health;
    }
}
