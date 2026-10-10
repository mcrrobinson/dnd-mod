package mattonfire.dnd.world.gen.temple;

import mattonfire.dnd.classes.Race.DragonAncestry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * One of the courtyard's three dragon statues, block-built on a gilded plinth and 10 blocks tall: a dragon
 * sitting up on its haunches with its wings half spread, looking down the stair.
 * <ul>
 * <li>Ember: red nether brick, magma eyes and red horns (red nether brick walls stand in for fire coral,
 * which dies out of water).</li>
 * <li>Frost: quartz, packed ice and snow.</li>
 * <li>Storm: lapis, prismarine and lightning-rod horns.</li>
 * </ul>
 * Each statue is its own piece, so "is this block part of a statue" is a box test (damaging one is a temple
 * grudge, see {@code TempleLaw}).
 */
public class StatuePiece extends TemplePiece {
    static final int SIZE = 5;
    static final int HEIGHT = 11;
    private final DragonAncestry kind;

    public StatuePiece(BlockPos front, Direction facing, DragonAncestry kind, long seed) {
        super(DragonTempleStructures.STATUE, boxFrom(front, facing, SIZE, HEIGHT, SIZE), facing, seed);
        this.kind = kind;
    }

    public StatuePiece(NbtCompound nbt) {
        super(DragonTempleStructures.STATUE, nbt);
        DragonAncestry kind;
        try {
            kind = DragonAncestry.fromValue(nbt.getInt("Kind"));
        } catch (IllegalArgumentException e) {
            kind = DragonAncestry.EMBER;
        }
        this.kind = kind;
    }

    public DragonAncestry kind() {
        return this.kind;
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Kind", this.kind.getValue());
    }

    /** body, wings, belly and eyes; horns are placed separately. */
    private Block[] palette() {
        return switch (this.kind) {
            case FROST -> new Block[]{Blocks.QUARTZ_BLOCK, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.BLUE_ICE};
            case STORM -> new Block[]{Blocks.LAPIS_BLOCK, Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN};
            default -> new Block[]{Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_WART_BLOCK, Blocks.MAGMA_BLOCK};
        };
    }

    private BlockState horn() {
        return switch (this.kind) {
            case FROST -> pillar(Blocks.QUARTZ_PILLAR, Direction.Axis.Y);
            case STORM -> Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP);
            default -> Blocks.RED_NETHER_BRICK_WALL.getDefaultState();
        };
    }

    @Override
    protected void build(Builder b) {
        Block[] p = this.palette();
        Block body = p[0];
        Block wing = p[1];
        Block belly = p[2];
        Block eye = p[3];
        // The plinth: polished blackstone with gilded corners.
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean corner = (x == 0 || x == SIZE - 1) && (z == 0 || z == SIZE - 1);
                b.set(x, 0, z, corner ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE);
            }
        }
        // Haunches and the tail curled round the back.
        b.fill(1, 1, 2, 3, 2, 4, body);
        b.set(4, 1, 4, body);
        b.set(4, 1, 3, wing);
        b.set(0, 1, 4, wing);
        // Forelegs.
        b.fill(1, 1, 0, 1, 3, 1, body);
        b.fill(3, 1, 0, 3, 3, 1, body);
        // Claws: slabs at the toes.
        BlockState toe = slab(this.kind == DragonAncestry.FROST ? Blocks.QUARTZ_SLAB
                : this.kind == DragonAncestry.STORM ? Blocks.PRISMARINE_BRICK_SLAB : Blocks.RED_NETHER_BRICK_SLAB, false);
        b.set(1, 1, 0, toe);
        b.set(3, 1, 0, toe);
        // Chest and belly.
        b.fill(1, 3, 2, 3, 6, 3, body);
        b.fill(2, 3, 1, 2, 5, 1, belly);
        // Wings, half spread: up from the shoulders and out to the sides.
        for (int y = 5; y <= 8; y++) {
            b.fill(0, y, 3, 0, y, 4 - (y - 5) / 2, wing);
            b.fill(4, y, 3, 4, y, 4 - (y - 5) / 2, wing);
        }
        b.set(0, 9, 4, wing);
        b.set(4, 9, 4, wing);
        // The neck, leaning forward.
        b.fill(2, 7, 2, 2, 8, 2, body);
        b.set(2, 8, 1, body);
        // The head, looking down the stair, with its eyes, snout and horns.
        b.fill(1, 9, 1, 3, 9, 2, body);
        b.set(2, 9, 0, body);
        b.set(1, 9, 1, eye);
        b.set(3, 9, 1, eye);
        b.set(2, 8, 0, belly);
        b.set(1, 10, 2, this.horn());
        b.set(3, 10, 2, this.horn());
    }
}
