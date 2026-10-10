package mattonfire.dnd.world.gen.enclave;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.Direction;

/**
 * A talan: a treetop platform 7x7 on a giant birch with a 3x3 trunk, at the bridge deck's height
 * ({@link ElvenEnclavePlanner#DECK} above the Heart Tree's foot, so 10-22 above its own ground). A ladder
 * runs up the trunk's north face, vines hang down the others, and a canopy roofs the platform. Each is
 * furnished as a dwelling (bedroll and chest), a scout post (cartography table and barrel) or a herbalist's
 * (brewing stand and flower pots).
 */
public class TalanPiece extends EnclavePiece {
    /** Half the platform's width (7x7). */
    static final int EDGE = 3;
    static final int REACH = 6;
    /** The ground the trunk and ladder take up, which the ground pieces keep off. */
    static final int FOOTPRINT = 3;
    private static final int CANOPY_RADIUS = 5;

    /** Local y of the platform (its floor block). */
    private final int platform;

    public TalanPiece(int x, int y, int z, int platform, long seed) {
        super(ElvenEnclaveStructures.TALAN, box(x, y, z, -REACH, -3, -REACH, REACH, platform + 11, REACH), x, y, z, seed);
        this.platform = platform;
    }

    public TalanPiece(NbtCompound nbt) {
        super(ElvenEnclaveStructures.TALAN, nbt);
        this.platform = nbt.getInt("Platform");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Platform", this.platform);
    }

    @Override
    protected void build(Builder b) {
        int p = this.platform;
        int top = p + 7;
        // Trunk, with stripped inlays on the faces.
        for (int y = 0; y <= top; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    boolean corner = x != 0 && z != 0;
                    b.set(x, y, z, !corner && (x != 0 || z != 0) && y % 4 == 1 ? Blocks.STRIPPED_BIRCH_WOOD : Blocks.BIRCH_WOOD);
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                b.anchor(x, 0, z, Blocks.ROOTED_DIRT.getDefaultState());
            }
        }
        // Small roots on the east, south and west (the ladder's on the north).
        for (Direction d : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            int x = d.getOffsetX() * 2;
            int z = d.getOffsetZ() * 2;
            b.set(x, 0, z, Blocks.BIRCH_WOOD);
            b.set(x, 1, z, Blocks.BIRCH_WOOD);
            b.anchor(x, 0, z, Blocks.ROOTED_DIRT.getDefaultState());
        }
        // Ladder up the north face, through a hole in the floor.
        for (int y = 1; y <= p; y++) {
            b.set(0, y, -2, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
        }
        b.anchor(0, 1, -2, Blocks.DIRT.getDefaultState());
        // Vines down the other faces.
        for (Direction d : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            int length = 3 + b.random.nextInt(6);
            for (int y = p - 1; y >= p - length && y > 2; y--) {
                // A vine on the face of the trunk: the vine block sits outside it, attached back towards it.
                BlockState vine = Blocks.VINE.getDefaultState().with(VineBlock.getFacingProperty(d.getOpposite()), true);
                b.set(d.getOffsetX() * 2 + (d.getAxis() == Direction.Axis.Z ? 1 : 0), y,
                        d.getOffsetZ() * 2 + (d.getAxis() == Direction.Axis.X ? 1 : 0), vine);
            }
        }
        this.platform(b, p);
        this.canopy(b, p);
        this.furnish(b, p);
        b.elf(-2, p + 1, 0, b.chance(0.6F) ? ModEntityTypes.ELF_WARDEN : ModEntityTypes.WOOD_ELF);
        if (b.chance(0.25F)) {
            b.elf(0, p + 1, 2, ModEntityTypes.WOOD_ELF);
        }
    }

    private void platform(Builder b, int p) {
        for (int x = -EDGE; x <= EDGE; x++) {
            for (int z = -EDGE; z <= EDGE; z++) {
                boolean trunk = Math.abs(x) <= 1 && Math.abs(z) <= 1;
                if (!trunk) {
                    b.air(x, p + 1, z, x, p + 3, z);
                    b.set(x, p, z, x == 0 && z == -2 ? Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH)
                            : Blocks.BIRCH_PLANKS.getDefaultState());
                }
                boolean edge = Math.abs(x) == EDGE || Math.abs(z) == EDGE;
                if (edge) {
                    b.set(x, p + 1, z, Blocks.BIRCH_FENCE);
                }
            }
        }
        // Beams round the rim under the floor, and braces down to the trunk.
        for (int s = -EDGE; s <= EDGE; s++) {
            b.set(s, p - 1, -EDGE, Blocks.STRIPPED_BIRCH_WOOD);
            b.set(s, p - 1, EDGE, Blocks.STRIPPED_BIRCH_WOOD);
            b.set(-EDGE, p - 1, s, Blocks.STRIPPED_BIRCH_WOOD);
            b.set(EDGE, p - 1, s, Blocks.STRIPPED_BIRCH_WOOD);
        }
        for (int[] c : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            b.set(c[0] * 2, p - 2, c[1] * 2, Blocks.BIRCH_FENCE);
            b.set(c[0] * 2, p - 1, c[1] * 2, Blocks.STRIPPED_BIRCH_WOOD);
            b.set(c[0] * EDGE, p + 1, c[1] * EDGE, lantern(false));
        }
        // Keep the ladder's way up clear of the rim beam.
        b.set(0, p - 1, -2, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
    }

    private void canopy(Builder b, int p) {
        int cy = p + 7;
        for (int x = -CANOPY_RADIUS; x <= CANOPY_RADIUS; x++) {
            for (int z = -CANOPY_RADIUS; z <= CANOPY_RADIUS; z++) {
                for (int y = cy - 3; y <= cy + 3; y++) {
                    double dy = (y - cy) / 3.0D;
                    double d = (x * x + z * z) / (double) (CANOPY_RADIUS * CANOPY_RADIUS) + dy * dy;
                    float n = this.noise(x, y, z);
                    boolean trunk = Math.abs(x) <= 1 && Math.abs(z) <= 1 && y <= p + 7;
                    if (trunk || d > 1.0D - 0.3D * n) {
                        continue;
                    }
                    b.set(x, y, z, leaves(n < 0.15F ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.BIRCH_LEAVES));
                }
            }
        }
    }

    private void furnish(Builder b, int p) {
        int y = p + 1;
        switch (b.random.nextInt(3)) {
            case 0 -> {
                // Dwelling: a bedroll and a chest.
                BlockState bed = Blocks.GREEN_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);
                b.set(2, y, 1, bed.with(BedBlock.PART, BedPart.FOOT));
                b.set(2, y, 2, bed.with(BedBlock.PART, BedPart.HEAD));
                b.container(-2, y, 2, facing(Blocks.CHEST, Direction.NORTH), TALAN_LOOT);
            }
            case 1 -> {
                // Scout post.
                b.set(2, y, 2, Blocks.CARTOGRAPHY_TABLE);
                b.container(-2, y, 2, facing(Blocks.BARREL, Direction.UP), TALAN_LOOT);
            }
            default -> {
                // Herbalist.
                b.set(2, y, 2, Blocks.BREWING_STAND);
                b.set(-2, y, 2, Blocks.POTTED_FERN);
                b.set(2, y, -2, Blocks.POTTED_BLUE_ORCHID);
                b.container(-2, y, -2, facing(Blocks.BARREL, Direction.UP), TALAN_LOOT);
            }
        }
    }
}
