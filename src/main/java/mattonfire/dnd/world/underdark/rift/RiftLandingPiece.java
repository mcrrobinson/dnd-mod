package mattonfire.dnd.world.underdark.rift;

import mattonfire.dnd.classes.Blocks.underdark.RiftAnchorBlock;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.RiftBlocks;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * A Rift Landing: a dome of radius {@link #RADIUS} and height {@link #HEIGHT} cleared over the floor, paved with
 * deepslate tiles and Faerzress Stone, a three-block Rift Anchor at its centre, four Nightlight Caps on zurkhwood
 * stems lighting it (so nothing spawns there), and a chest of supplies ({@code chests/rift_landing}). Fluids round
 * the dome are walled off with deepslate.
 */
public class RiftLandingPiece extends StructurePiece {
    public static final Identifier LOOT = new Identifier(DnDClasses.MOD_ID, "chests/rift_landing");
    public static final int RADIUS = 7;
    public static final int HEIGHT = 6;

    /** The floor's centre (the paving under the anchor). */
    private final BlockPos center;
    private final boolean carved;
    private final long seed;

    public RiftLandingPiece(BlockPos center, boolean carved, long seed) {
        super(UnderdarkRiftStructures.LANDING, 0, new BlockBox(center.getX() - RADIUS - 2, center.getY() - 3,
                center.getZ() - RADIUS - 2, center.getX() + RADIUS + 2, center.getY() + HEIGHT + 2, center.getZ() + RADIUS + 2));
        this.center = center;
        this.carved = carved;
        this.seed = seed;
    }

    public RiftLandingPiece(NbtCompound nbt) {
        super(UnderdarkRiftStructures.LANDING, nbt);
        this.center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        this.carved = nbt.getBoolean("Carved");
        this.seed = nbt.getLong("Seed");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putBoolean("Carved", this.carved);
        nbt.putLong("Seed", this.seed);
    }

    /** The bottom block of the Rift Anchor column. */
    public BlockPos anchor() {
        return this.center.up();
    }

    /** Where someone falling through the veil arrives: on the anchor's east side. */
    public BlockPos arrival() {
        return this.center.add(1, 1, 0);
    }

    /** Whether no cavern floor was found and the landing was carved out of the rock. */
    public boolean carved() {
        return this.carved;
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random chunkRandom, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockBox box = this.getBoundingBox();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int cx = this.center.getX();
        int y0 = this.center.getY();
        int cz = this.center.getZ();
        for (int x = Math.max(box.getMinX(), chunkBox.getMinX()); x <= Math.min(box.getMaxX(), chunkBox.getMaxX()); x++) {
            for (int z = Math.max(box.getMinZ(), chunkBox.getMinZ()); z <= Math.min(box.getMaxZ(), chunkBox.getMaxZ()); z++) {
                int dx = x - cx;
                int dz = z - cz;
                double flat = (dx * dx + dz * dz) / (double) (RADIUS * RADIUS);
                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                    int dy = y - y0;
                    pos.set(x, y, z);
                    BlockState old = world.getBlockState(pos);
                    boolean wet = !old.getFluidState().isEmpty();
                    if (dy >= 1) {
                        double e = flat + (dy * dy) / (double) (HEIGHT * HEIGHT);
                        if (e < 1.0) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), PieceBlocks.UPDATE);
                        } else if (e < 1.6 && wet) {
                            world.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), PieceBlocks.UPDATE);
                        }
                    } else if (dy == 0 && flat <= 1.0) {
                        float n = UnderdarkRiftStructures.noise(this.seed, x, y, z);
                        world.setBlockState(pos, paving(dx, dz, n), PieceBlocks.UPDATE);
                    } else if (flat <= 1.4 && (wet || old.isAir() || old.isReplaceable())) {
                        world.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), PieceBlocks.UPDATE);
                    }
                }
            }
        }

        // The anchor.
        for (int i = 1; i <= 3; i++) {
            set(world, chunkBox, this.center.up(i), RiftBlocks.RIFT_ANCHOR.getDefaultState().with(RiftAnchorBlock.TOP, i == 3));
        }
        // Four Nightlights on zurkhwood stems, on the diagonals.
        for (int[] d : new int[][] {{-3, -3}, {3, -3}, {3, 3}, {-3, 3}}) {
            BlockPos foot = this.center.add(d[0], 1, d[1]);
            for (int i = 0; i < 3; i++) {
                set(world, chunkBox, foot.up(i), UnderdarkBlocks.ZURKHWOOD_STEM.getDefaultState()
                        .with(PillarBlock.AXIS, Direction.Axis.Y));
            }
            BlockPos cap = foot.up(3);
            set(world, chunkBox, cap, UnderdarkBlocks.NIGHTLIGHT_CAP.getDefaultState());
            set(world, chunkBox, cap.add(Integer.signum(d[0]), 0, 0), UnderdarkBlocks.NIGHTLIGHT_CAP.getDefaultState());
            set(world, chunkBox, cap.add(0, 0, Integer.signum(d[1])), UnderdarkBlocks.NIGHTLIGHT_CAP.getDefaultState());
        }
        // Supplies left by those who came before, west of the anchor, facing it.
        BlockPos chest = this.center.add(-3, 1, 0);
        set(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST));
        if (chunkBox.contains(chest) && world.getBlockEntity(chest) instanceof LootableContainerBlockEntity container) {
            container.setLootTable(LOOT, this.seed);
        }
        set(world, chunkBox, this.center.add(-3, 1, 1), Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
    }

    /** Deepslate tiles round the anchor, a ring and four spokes of Faerzress Stone. */
    private static BlockState paving(int dx, int dz, float n) {
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r < 1.5 || (r > 3.5 && r < 4.5) || ((dx == 0 || dz == 0) && r > 5.5)) {
            return UnderdarkBlocks.FAERZRESS_STONE.getDefaultState();
        }
        return (n < 0.6F ? Blocks.DEEPSLATE_TILES : n < 0.85F ? Blocks.CRACKED_DEEPSLATE_TILES
                : Blocks.POLISHED_DEEPSLATE).getDefaultState();
    }

    private static void set(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, state, PieceBlocks.UPDATE);
        }
    }
}
