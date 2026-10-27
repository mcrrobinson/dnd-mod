package mattonfire.dnd.world.underdark.duergar;

import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.DuergarEntities;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.PieceBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Duergar Hold's mine, its door towards local +b. The mine head is 7 x 7 with its corners cut, walls 5 high on a
 * battered plinth under a stepped roof, a recessed portal and shuttered windows; over the stairwell the winding house
 * rises to 8-10 high under its own roof, the winch chain hanging down it. The
 * stair (three wide, a block down each step) runs back under the hold to a gallery {@value #GALLERY_FLOOR} blocks down:
 * 9 x 9, 4 high, cut from tuff and deepslate with ore showing in the walls, basalt props, soul lanterns, a rail line
 * with two chest minecarts ({@code chests/duergar_mine}), heaps of raw iron and raw gold blocks and the ore chest
 * ({@code chests/duergar_mine}, guarded by the Grey Clans' law). A soldier keeps watch.
 * <p>
 * In the gallery's side wall is the <b>slave pen</b>: two cells, each behind a locked gate two
 * {@link ChainCageBlock Chain Cages} high. The first always holds a captive gnome, the second half the time; opening a
 * gate frees its gnome for +{@value mattonfire.dnd.world.underdark.Captives#REPUTATION} with the Tinkers' Guild
 * ({@code Captives}).
 * <p>
 * Everything round the stair and the gallery is set solid first, so caves and liquids under the hold can't break in.
 * A Rust Monster (design 9, section 10) roams the gallery, persistent, kept within 6 blocks of the ore face.
 */
public class DuergarMinePiece extends DuergarPiece {
    /** The mine head's half-size, and its walls' top course. */
    public static final int HEAD = 3;
    public static final int HEAD_ROOF = 4;
    /** Blocks from the centre to just outside the door. */
    public static final int DOOR = HEAD + 2;
    /** The gallery's floor block (its people stand a block above). */
    public static final int GALLERY_FLOOR = -8;
    /** The gallery: a from -4 to 4, b from {@value #GALLERY_BACK} to {@value #GALLERY_FRONT}. */
    public static final int GALLERY_BACK = -14;
    public static final int GALLERY_FRONT = -6;
    /** Steps of the stair (the first is flush with the mine head's floor). */
    public static final int STEPS = 7;
    /** The pen gates' local b (both at a = 4). */
    static final int[] GATES = {-12, -9};

    public DuergarMinePiece(BlockPos center, Direction front, long seed, boolean mirror, int style) {
        super(DuergarHolds.MINE, center, front, seed, mirror, style, -6, GALLERY_FLOOR - 1, GALLERY_BACK - 1, 8, 13, HEAD + 2);
    }

    public DuergarMinePiece(NbtCompound nbt) {
        super(DuergarHolds.MINE, nbt);
    }

    /** Pen gate {@code i}'s lower cage, in the world. */
    public BlockPos gate(int i) {
        return this.at(4, GALLERY_FLOOR + 1, GATES[i]);
    }

    /** Where you stand in the gallery in front of pen gate {@code i}. */
    public BlockPos atGate(int i) {
        return this.at(2, GALLERY_FLOOR + 1, GATES[i]);
    }

    /** The middle of the gallery. */
    public BlockPos gallery() {
        return this.at(0, GALLERY_FLOOR + 1, -9);
    }

    /** The ore chest. */
    public BlockPos oreChest() {
        return this.at(-4, GALLERY_FLOOR + 1, -9);
    }

    /** The stair's steps (the stair blocks of its middle column), top first, in the world. */
    public java.util.List<BlockPos> steps() {
        java.util.List<BlockPos> steps = new java.util.ArrayList<>();
        for (int i = 0; i < STEPS; i++) {
            steps.add(this.at(0, -1 - i, 1 - i));
        }
        return steps;
    }

    /** Just inside the mine head's door. */
    public BlockPos inside() {
        return this.at(0, 0, 2);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = DuergarHolds.bricks();
        // Solid rock under the mine head and round the stair and the gallery.
        for (int a = -6; a <= 8; a++) {
            for (int c = GALLERY_BACK - 1; c <= HEAD - 1; c++) {
                for (int y = GALLERY_FLOOR - 1; y <= -2; y++) {
                    float r = b.random.nextFloat();
                    b.set(a, y, c, r < 0.03F ? Blocks.DEEPSLATE_IRON_ORE.getDefaultState()
                            : r < 0.04F ? Blocks.DEEPSLATE_GOLD_ORE.getDefaultState()
                            : r < 0.6F ? Blocks.TUFF.getDefaultState() : Blocks.DEEPSLATE.getDefaultState());
                }
            }
        }
        this.head(b, bricks);
        this.stair(b);
        this.gallery(b);
        this.pen(b);
        // A Rust Monster has got into the gallery (design 9, section 10), drawn by the ore. Last, so the draws
        // above stay as they were.
        b.resident(2, GALLERY_FLOOR + 1, -13, ModEntityTypes.RUST_MONSTER, 6);
    }

    /** Where the gallery's Rust Monster starts, in the world. */
    public BlockPos rustMonster() {
        return this.at(2, GALLERY_FLOOR + 1, -13);
    }

    private void head(Builder b, BlockState bricks) {
        DuergarMasonry.Style style = this.style();
        int winding = 7 + b.random.nextInt(3);
        // The mine head: 7 x 7, corners cut, walls 5 high; the winding house over the stairwell rises above it.
        DuergarMasonry.Mass mass = new DuergarMasonry.Mass(-HEAD, HEAD, -HEAD, HEAD)
                .box(-HEAD, HEAD, -HEAD, HEAD, 1, HEAD_ROOF)
                .box(-1, 1, -2, 0, 0, winding);
        DuergarMasonry.build(b, mass, style, 2, DuergarMasonry.Open.NONE);
        DuergarMasonry.portal(b, 0, 1, 3, HEAD);
        // A railing round the stairwell, the winch chain down the winding house.
        for (int c = -2; c <= 1; c++) {
            b.set(-2, 0, c, DuergarMasonry.brickWall());
            b.set(2, 0, c, DuergarMasonry.brickWall());
        }
        for (int y = 1; y < winding; y++) {
            b.set(0, y, -1, Blocks.CHAIN.getDefaultState());
        }
        b.set(0, winding, -1, DuergarMasonry.greysteel());
        // Slit windows in the winding house, shuttered ones in the head's sides.
        for (int y = HEAD_ROOF + 2; y <= HEAD_ROOF + 3 && y < winding; y++) {
            b.set(0, y, 0, Blocks.IRON_BARS.getDefaultState());
            b.set(-1, y, -1, Blocks.IRON_BARS.getDefaultState());
            b.set(1, y, -1, Blocks.IRON_BARS.getDefaultState());
        }
        DuergarMasonry.window(b, -HEAD, 1, 0, Direction.WEST, 2, true);
        DuergarMasonry.window(b, HEAD, 1, 0, Direction.EAST, 2, true);
        DuergarMasonry.buttress(b, -1, -HEAD - 1, Direction.NORTH, 4);
        DuergarMasonry.buttress(b, 1, -HEAD - 1, Direction.NORTH, 4);
        b.hangingLantern(-1, HEAD_ROOF, 1, 1, Blocks.SOUL_LANTERN);
        b.hangingLantern(1, HEAD_ROOF, 1, 1, Blocks.SOUL_LANTERN);
        // Barrels inside the door, braziers outside it.
        b.set(-2, 0, 2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(2, 0, 2, PieceBlocks.facing(Blocks.BARREL, Direction.UP));
        b.set(-3, 0, HEAD + 2, DuergarHolds.brazier());
        b.set(3, 0, HEAD + 2, DuergarHolds.brazier());
    }

    /** The stair down: step i at b = 1 - i, its top at y = -i; air three high over the steps under the rock. */
    private void stair(Builder b) {
        BlockState step = ModBlockFamilies.BASALT_FORGE_BRICKS.stairs().getDefaultState()
                .with(StairsBlock.FACING, Direction.SOUTH);
        for (int i = 0; i < STEPS; i++) {
            int c = 1 - i;
            int y = -1 - i;
            boolean underHead = c >= -HEAD + 1;
            for (int a = -1; a <= 1; a++) {
                b.set(a, y, c, step);
                int top = underHead ? -1 : y + 3;
                for (int air = y + 1; air <= top; air++) {
                    b.clear(a, air, c);
                }
            }
        }
    }

    private void gallery(Builder b) {
        BlockState[] floors = {Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.COBBLED_DEEPSLATE.getDefaultState(),
                Blocks.GRAVEL.getDefaultState()};
        for (int a = -4; a <= 4; a++) {
            for (int c = GALLERY_BACK; c <= GALLERY_FRONT; c++) {
                b.set(a, GALLERY_FLOOR, c, floors[b.random.nextInt(floors.length)]);
                for (int y = GALLERY_FLOOR + 1; y <= GALLERY_FLOOR + 4; y++) {
                    b.clear(a, y, c);
                }
            }
        }
        int y = GALLERY_FLOOR + 1;
        // Basalt props along the far wall.
        for (int c : new int[] {-7, -11, -14}) {
            for (int dy = 0; dy < 4; dy++) {
                b.set(-4, y + dy, c, PieceBlocks.pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
            }
        }
        // The rail line and its carts.
        for (int c = GALLERY_BACK; c <= GALLERY_FRONT; c++) {
            b.set(0, y, c, Blocks.RAIL);
        }
        this.cart(b, 0, y, -10, DuergarHolds.MINE_LOOT);
        this.cart(b, 0, y, -13, DuergarHolds.MINE_LOOT);
        // Heaps of ore at the face.
        b.set(-3, y, -14, Blocks.RAW_IRON_BLOCK);
        b.set(-2, y, -14, Blocks.RAW_IRON_BLOCK);
        b.set(-3, y + 1, -14, Blocks.RAW_IRON_BLOCK);
        b.set(-3, y, -13, Blocks.RAW_GOLD_BLOCK);
        b.set(2, y, -14, Blocks.RAW_GOLD_BLOCK);
        b.set(3, y, -14, Blocks.RAW_IRON_BLOCK);
        b.chest(-4, y, -9, Direction.EAST, DuergarHolds.MINE_LOOT);
        for (int c : new int[] {-8, -12}) {
            b.hangingLantern(-2, GALLERY_FLOOR + 4, c, 1, Blocks.SOUL_LANTERN);
            b.hangingLantern(2, GALLERY_FLOOR + 4, c, 1, Blocks.SOUL_LANTERN);
        }
        DuergarHolds.duergar(b, 1, y, -8, DuergarEntities.DUERGAR_SOLDIER, 6);
    }

    /** The slave pen: two cells off the gallery's side, each behind a locked gate of two Chain Cages. */
    private void pen(Builder b) {
        int y = GALLERY_FLOOR + 1;
        BlockState gate = UnderdarkBlocks.CHAIN_CAGE.getDefaultState().with(ChainCageBlock.FACING, Direction.EAST)
                .with(ChainCageBlock.LOCKED, true);
        for (int g : GATES) {
            for (int a = 5; a <= 6; a++) {
                for (int c = g - 1; c <= g; c++) {
                    b.set(a, GALLERY_FLOOR, c, Blocks.COBBLED_DEEPSLATE.getDefaultState());
                    b.clear(a, y, c);
                    b.clear(a, y + 1, c);
                }
            }
            b.set(4, y, g, gate);
            b.set(4, y + 1, g, gate);
            b.set(4, y, g - 1, Blocks.IRON_BARS);
            b.set(4, y + 1, g - 1, Blocks.IRON_BARS);
            b.set(6, y + 1, g - 1, Blocks.CHAIN);
        }
        b.captive(5, y, GATES[0], ModEntityTypes.GNOME_TINKER, Direction.WEST);
        if (b.random.nextBoolean()) {
            b.captive(5, y, GATES[1], ModEntityTypes.GNOME_TINKER, Direction.WEST);
        }
    }

    /** A chest minecart on the rail at a/y/c, filled from a loot table when opened. */
    private void cart(Builder b, int a, int y, int c, Identifier loot) {
        long lootSeed = b.random.nextLong();
        BlockPos pos = b.pos(a, y, c);
        if (!b.chunkBox.contains(pos)) {
            return;
        }
        ChestMinecartEntity cart = new ChestMinecartEntity(b.world.toServerWorld(), pos.getX() + 0.5D,
                pos.getY() + 0.0625D, pos.getZ() + 0.5D);
        cart.setLootTable(loot, lootSeed);
        b.world.spawnEntity(cart);
    }
}
