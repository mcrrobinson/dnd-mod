package mattonfire.dnd.world.underdark.lair;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Troll Bridge (design 9, section 10): a Zurkhwood bridge across a Darklake channel, laid along local b (the front,
 * +b, is one end), with a bone-strewn nook under one end where the troll lives.
 * <ul>
 * <li>The deck: 3 wide, of Zurkhwood planks at local y -1, carried {@value TrollBridgeStructure#ONTO_BANK} blocks onto
 * the bank at the plain end and over the nook at the other ({@value #NOOK_DEPTH} more), then stairs down to the
 * bank. Zurkhwood beams along its sides, fence rails on them with a post every {@value #POST_EVERY} blocks, skulls on
 * the end posts and soul lanterns on the middle ones; Zurkhwood piles down to the lake bed under the beams every
 * {@value #POST_EVERY} blocks over the water. It stands at least 4 blocks over the water ({@link #deckY}).</li>
 * <li>The nook: dug into one bank at the water's level under the deck, {@value #NOOK_WIDTH} wide and
 * {@value #NOOK_DEPTH} deep, open to the channel. Bone blocks, skulls and webs, a soul lantern, and the toll chest
 * ({@code chests/troll_toll}) at the back.</li>
 * <li>One Troll, persistent, kept within {@value #TROLL_RANGE} blocks of the nook.</li>
 * </ul>
 * Saved: the distances from the centre to the first land each way ({@code Near} towards -b, {@code Far} towards
 * +b), the banks' heights over the water, the water's surface (local y) and which end has the nook.
 */
public class TrollBridgePiece extends UnderdarkPiece {
    public static final int POST_EVERY = 4;
    public static final int NOOK_WIDTH = 5;
    public static final int NOOK_DEPTH = 4;
    public static final int TROLL_RANGE = 10;
    /** Air over the deck that's cleared. */
    private static final int CLEAR = 3;

    private final int near;
    private final int far;
    private final int nearBank;
    private final int farBank;
    /** The water's top block, local y (negative). */
    private final int surface;
    /** +1: the nook is at the +b end; -1: at the -b end. */
    private final int nookSide;

    public TrollBridgePiece(BlockPos center, Direction front, long seed, int near, int far, int nearBank, int farBank,
                            int surface) {
        super(UnderdarkLairs.BRIDGE_PIECE, center, front, seed, -4, surface - 24, -near - NOOK_DEPTH - 8, 4, CLEAR + 2,
                far + NOOK_DEPTH + 8);
        this.near = near;
        this.far = far;
        this.nearBank = nearBank;
        this.farBank = farBank;
        this.surface = surface;
        this.nookSide = (seed & 1L) == 0L ? 1 : -1;
    }

    public TrollBridgePiece(NbtCompound nbt) {
        super(UnderdarkLairs.BRIDGE_PIECE, nbt);
        this.near = nbt.getInt("Near");
        this.far = nbt.getInt("Far");
        this.nearBank = nbt.getInt("NearBank");
        this.farBank = nbt.getInt("FarBank");
        this.surface = nbt.getInt("Surface");
        this.nookSide = nbt.getInt("Nook");
    }

    @Override
    protected void writeNbt(net.minecraft.structure.StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putInt("Near", this.near);
        nbt.putInt("Far", this.far);
        nbt.putInt("NearBank", this.nearBank);
        nbt.putInt("FarBank", this.farBank);
        nbt.putInt("Surface", this.surface);
        nbt.putInt("Nook", this.nookSide);
    }

    /** The deck block's world y for a lake surface at {@code surface} and the higher bank {@code bank} above it. */
    public static int deckY(int surface, int bank) {
        return surface + Math.max(4, bank + 1);
    }

    /** Distance from the centre to the first land at the nook's end. */
    private int nookLand() {
        return this.nookSide > 0 ? this.far : this.near;
    }

    /** How far the deck runs towards {@code sign} (+1: +b). */
    private int end(int sign) {
        int land = sign > 0 ? this.far : this.near;
        return land + (sign == this.nookSide ? NOOK_DEPTH + 1 : TrollBridgeStructure.ONTO_BANK);
    }

    /** Local b of a spot {@code d} blocks into the nook from its mouth. */
    private int nookB(int d) {
        return this.nookSide * (this.nookLand() + d);
    }

    /** The deck's length, end to end. */
    public int length() {
        return this.end(1) + this.end(-1) + 1;
    }

    /** The middle of the deck, where you stand, in the world. */
    public BlockPos middle() {
        return this.center;
    }

    /** The toll chest, in the world. */
    public BlockPos chest() {
        return this.at(0, this.surface + 1, this.nookB(NOOK_DEPTH - 1));
    }

    /** Where the troll lives: the nook's mouth, in the world. */
    public BlockPos nook() {
        return this.at(0, this.surface + 1, this.nookB(1));
    }

    /** Over the water in front of the nook, looking in, in the world. */
    public BlockPos nookView() {
        return this.at(0, this.surface + 1, this.nookB(-3));
    }

    /** Out over the water beside the bridge, at the deck's height, looking at its side, in the world. */
    public BlockPos side() {
        return this.at(9, 2, this.nookB(-2));
    }

    /** The deck's blocks along its middle line, in the world. */
    public java.util.List<BlockPos> deck() {
        java.util.List<BlockPos> deck = new java.util.ArrayList<>();
        for (int c = -this.end(-1); c <= this.end(1); c++) {
            deck.add(this.at(0, -1, c));
        }
        return deck;
    }

    @Override
    protected void build(Builder b) {
        BlockState planks = ModBlockFamilies.ZURKHWOOD_PLANKS.base().getDefaultState();
        BlockState beam = PieceBlocks.pillar(UnderdarkBlocks.ZURKHWOOD_STEM, Direction.Axis.Z);
        BlockState pile = PieceBlocks.pillar(UnderdarkBlocks.ZURKHWOOD_STEM, Direction.Axis.Y);
        BlockState fence = ModBlockFamilies.ZURKHWOOD_PLANKS.fence().getDefaultState();
        BlockState fill = Blocks.COBBLED_DEEPSLATE.getDefaultState();
        int minus = -this.end(-1);
        int plus = this.end(1);
        this.nook(b);
        for (int c = minus; c <= plus; c++) {
            boolean overWater = c > -this.near && c < this.far;
            boolean overNook = this.inNook(c);
            for (int a = -2; a <= 2; a++) {
                for (int y = 0; y <= CLEAR; y++) {
                    b.clear(a, y, c);
                }
                b.set(a, -1, c, Math.abs(a) == 2 ? beam : planks);
                if (!overWater && !overNook) {
                    b.propUp(a, -1, c, fill, 10, false);
                }
            }
            // Rails, posts every few blocks.
            boolean post = c == minus || c == plus || Math.floorMod(c, POST_EVERY) == 0;
            for (int side : new int[] {-2, 2}) {
                b.set(side, 0, c, fence);
                if (post) {
                    b.set(side, 1, c, fence);
                    if (c == minus || c == plus) {
                        b.set(side, 2, c, Blocks.SKELETON_SKULL.getDefaultState()
                                .with(SkullBlock.ROTATION, c == plus ? 0 : 8));
                    } else if (Math.abs(c) <= POST_EVERY) {
                        b.set(side, 2, c, Blocks.SOUL_LANTERN.getDefaultState());
                    }
                }
                if (overWater && Math.floorMod(c, POST_EVERY) == 0) {
                    b.propUp(side, -1, c, pile, 30, false);
                }
            }
        }
        this.stairs(b, 1, plus, this.farBank);
        this.stairs(b, -1, minus, this.nearBank);
    }

    private boolean inNook(int c) {
        int d = c * this.nookSide - this.nookLand();
        return d >= -1 && d <= NOOK_DEPTH;
    }

    /** Stairs from the deck's end {@code end} down to a bank {@code bank} blocks over the water. */
    private void stairs(Builder b, int sign, int end, int bank) {
        int bankTop = this.surface + bank;
        BlockState stair = PieceBlocks.stairs(ModBlockFamilies.ZURKHWOOD_PLANKS, sign > 0 ? Direction.NORTH
                : Direction.SOUTH, false);
        int steps = -2 - bankTop;
        for (int i = 1; i <= steps; i++) {
            int c = end + sign * i;
            int y = -1 - i;
            for (int a = -1; a <= 1; a++) {
                b.set(a, y, c, stair);
                b.propUp(a, y, c, Blocks.COBBLED_DEEPSLATE.getDefaultState(), 8, false);
                for (int up = y + 1; up <= y + CLEAR; up++) {
                    b.clear(a, up, c);
                }
            }
        }
    }

    /** The troll's nook, its bones, the toll chest and the troll. */
    private void nook(Builder b) {
        int floor = this.surface;
        int half = NOOK_WIDTH / 2;
        for (int d = 0; d <= NOOK_DEPTH; d++) {
            int c = this.nookB(d);
            for (int a = -half - 1; a <= half + 1; a++) {
                boolean wall = Math.abs(a) > half || d == NOOK_DEPTH;
                for (int y = floor - 1; y <= -1; y++) {
                    if (wall) {
                        b.set(a, y, c, rock(b).getDefaultState());
                    } else if (y == floor - 1) {
                        b.solidify(a, y, c, Blocks.COBBLED_DEEPSLATE.getDefaultState());
                    } else if (y == floor) {
                        float n = noise(this.seed, a, y, c);
                        b.set(a, y, c, (n < 0.4F ? Blocks.GRAVEL : n < 0.7F ? Blocks.COARSE_DIRT : n < 0.85F
                                ? Blocks.MUD : Blocks.BONE_BLOCK).getDefaultState());
                    } else if (y < -1) {
                        b.clear(a, y, c);
                    }
                }
            }
        }
        int y = floor + 1;
        int back = this.nookB(NOOK_DEPTH - 1);
        // Heaps of bone in the back corners, skulls and webs about.
        for (int side : new int[] {-half, half}) {
            b.set(side, y, back, Blocks.BONE_BLOCK);
            b.set(side, y + 1, back, Blocks.COBWEB);
            b.set(side, y, this.nookB(NOOK_DEPTH - 2), Blocks.SKELETON_SKULL.getDefaultState()
                    .with(SkullBlock.ROTATION, b.random.nextInt(16)));
        }
        b.set(-half, y, this.nookB(1), Blocks.SKELETON_SKULL.getDefaultState()
                .with(SkullBlock.ROTATION, b.random.nextInt(16)));
        b.set(half, y + 2, this.nookB(1), Blocks.COBWEB);
        b.set(half, y, this.nookB(2), Blocks.SOUL_LANTERN);
        b.chest(0, y, back, this.nookSide > 0 ? Direction.NORTH : Direction.SOUTH, UnderdarkLairs.TOLL_LOOT);
        b.resident(0, y, this.nookB(1), ModEntityTypes.TROLL, TROLL_RANGE);
    }
}
