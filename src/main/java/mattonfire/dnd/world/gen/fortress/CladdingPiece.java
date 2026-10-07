package mattonfire.dnd.world.gen.fortress;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Keeps the fortress inside its mountain. Where the mountainside is too thin to cover a room,
 * this heaps rock over and around it, sloping down to the natural ground, so no bare walls or
 * roofs stick out. It only fills open air from above (never a room's inside, which is roofed
 * over) and leaves the approach to the gate clear. Added after every room, so it runs last.
 */
public class CladdingPiece extends StructurePiece {
    /** How far out from a room the heap spreads. */
    private static final int MARGIN = 7;
    /** Rock over a room's roof. */
    static final int COVER = 2;
    /** How quickly the heap falls away from a room's walls, per block out. */
    private static final double SLOPE = 2.5D;
    /** How far below the fortress floor a column is filled at most. */
    private static final int MAX_FILL = 40;

    /** Room boxes as minX, minZ, maxX, maxZ, maxY. */
    private final int[] rooms;
    /** The gate front: its middle, which way the fortress runs, and how deep the facade is. */
    private final int gateX;
    private final int gateZ;
    private final Direction in;
    private final int clearDepth;

    public CladdingPiece(List<BlockBox> rooms, BlockPos gate, Direction in, int clearDepth) {
        super(DwarvenFortressStructures.CLADDING, 0, bounds(rooms));
        this.rooms = new int[rooms.size() * 5];
        for (int i = 0; i < rooms.size(); i++) {
            BlockBox box = rooms.get(i);
            this.rooms[i * 5] = box.getMinX();
            this.rooms[i * 5 + 1] = box.getMinZ();
            this.rooms[i * 5 + 2] = box.getMaxX();
            this.rooms[i * 5 + 3] = box.getMaxZ();
            this.rooms[i * 5 + 4] = box.getMaxY();
        }
        this.gateX = gate.getX();
        this.gateZ = gate.getZ();
        this.in = in;
        this.clearDepth = clearDepth;
    }

    public CladdingPiece(NbtCompound nbt) {
        super(DwarvenFortressStructures.CLADDING, nbt);
        this.rooms = nbt.getIntArray("Rooms");
        this.gateX = nbt.getInt("GateX");
        this.gateZ = nbt.getInt("GateZ");
        this.in = Direction.fromHorizontal(nbt.getInt("In"));
        this.clearDepth = nbt.getInt("ClearDepth");
    }

    private static BlockBox bounds(List<BlockBox> rooms) {
        BlockBox all = BlockBox.encompass(rooms).orElseThrow();
        return new BlockBox(all.getMinX() - MARGIN, all.getMinY() - MAX_FILL, all.getMinZ() - MARGIN,
                all.getMaxX() + MARGIN, all.getMaxY() + COVER, all.getMaxZ() + MARGIN);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putIntArray("Rooms", this.rooms);
        nbt.putInt("GateX", this.gateX);
        nbt.putInt("GateZ", this.gateZ);
        nbt.putInt("In", this.in.getHorizontal());
        nbt.putInt("ClearDepth", this.clearDepth);
    }

    /** The top of the heap over a column, or MIN_VALUE where there isn't one. */
    private int heapTop(int x, int z) {
        // In front of the facade stays open.
        int along = (x - this.gateX) * this.in.getOffsetX() + (z - this.gateZ) * this.in.getOffsetZ();
        if (along < this.clearDepth) {
            return Integer.MIN_VALUE;
        }
        int top = Integer.MIN_VALUE;
        for (int i = 0; i < this.rooms.length; i += 5) {
            int dx = Math.max(0, Math.max(this.rooms[i] - x, x - this.rooms[i + 2]));
            int dz = Math.max(0, Math.max(this.rooms[i + 1] - z, z - this.rooms[i + 3]));
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > MARGIN) {
                continue;
            }
            // A little lumpiness so the heap doesn't look poured from a mould.
            int lump = (int) ((MathHelper.hashCode(x, 0, z) >>> 20) & 1);
            top = Math.max(top, this.rooms[i + 4] + COVER + lump - (int) Math.round(d * SLOPE));
        }
        return top;
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int bottom = this.boundingBox.getMinY();
        Block local = localSurface(world, chunkBox);
        for (int x = chunkBox.getMinX(); x <= chunkBox.getMaxX(); x++) {
            for (int z = chunkBox.getMinZ(); z <= chunkBox.getMaxZ(); z++) {
                int top = this.heapTop(x, z);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                // Find what the heap would rest on: the first solid block down from its top.
                int y = top;
                pos.set(x, y, z);
                while (y > bottom && isOpen(world.getBlockState(pos))) {
                    pos.setY(--y);
                }
                if (y >= top) {
                    continue;
                }
                Block surface = naturalSurface(world.getBlockState(pos));
                if (surface == null) {
                    // Resting on a room roof: blend in with the hillside around instead.
                    surface = local;
                }
                for (int fy = y + 1; fy <= top; fy++) {
                    pos.setY(fy);
                    int depth = top - fy;
                    BlockState state;
                    if (depth == 0) {
                        state = surface.getDefaultState();
                    } else if (surface == Blocks.GRASS_BLOCK && depth <= 2) {
                        state = Blocks.DIRT.getDefaultState();
                    } else {
                        state = ((MathHelper.hashCode(x, fy, z) >>> 24) & 7) == 0 ? Blocks.ANDESITE.getDefaultState() : Blocks.STONE.getDefaultState();
                    }
                    world.setBlockState(pos, state, 2);
                }
                // The top half of a tall plant that stood on the old ground would be left floating.
                pos.setY(top + 1);
                BlockState above = world.getBlockState(pos);
                if (!above.isAir() && isOpen(above)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
    }

    /** Air, plants, snow layers and the like: things a heap of rock can be piled through. */
    private static boolean isOpen(BlockState state) {
        return state.isAir() || (state.isReplaceable() && state.getFluidState().isEmpty())
                || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.FLOWERS);
    }

    /** The commonest natural ground at the surface of this chunk, for heaps that rest on roofs. */
    private static Block localSurface(StructureWorldAccess world, BlockBox chunkBox) {
        java.util.Map<Block, Integer> counts = new java.util.HashMap<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = chunkBox.getMinX(); x <= chunkBox.getMaxX(); x += 5) {
            for (int z = chunkBox.getMinZ(); z <= chunkBox.getMaxZ(); z += 5) {
                pos.set(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                Block block = naturalSurface(world.getBlockState(pos));
                if (block != null) {
                    counts.merge(block, 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey).orElse(Blocks.STONE);
    }

    /** The heap's top block, to blend in with the natural ground it rests on; null if it isn't natural. */
    @org.jetbrains.annotations.Nullable
    private static Block naturalSurface(BlockState ground) {
        if (ground.isOf(Blocks.GRASS_BLOCK) || ground.isOf(Blocks.DIRT) || ground.isOf(Blocks.PODZOL)) {
            return Blocks.GRASS_BLOCK;
        }
        if (ground.isOf(Blocks.SNOW_BLOCK) || ground.isOf(Blocks.POWDER_SNOW) || ground.isOf(Blocks.PACKED_ICE)) {
            return Blocks.SNOW_BLOCK;
        }
        if (ground.isOf(Blocks.GRAVEL) || ground.isOf(Blocks.CALCITE) || ground.isOf(Blocks.ANDESITE)
                || ground.isOf(Blocks.STONE) || ground.isOf(Blocks.GRANITE) || ground.isOf(Blocks.DIORITE)) {
            return ground.getBlock();
        }
        return null;
    }
}
