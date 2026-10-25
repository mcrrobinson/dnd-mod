package mattonfire.dnd.world.underdark.drow;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.gen.PieceBlocks;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The Drow Spire's cavern: the dome the Spire hangs in, carved wherever the cave isn't that big already, with the
 * Spire's front (local +b) towards the path.
 * <ul>
 * <li>A dome of radius {@value #RADIUS} and height {@value #HEIGHT}, flattened at the top (its height at radius r is
 * {@code HEIGHT * sqrt(1 - (r / RADIUS)^4)}), cleared to air; the floor levelled under it (up to {@value #FILL} blocks
 * filled with deepslate, veins of Faerzress Stone in it) and a {@value #SHELL}-block shell of rock round the dome and
 * its rim where the cave was open, liquids included, so neighbouring caves and lakes don't spill in.</li>
 * <li>A path of Polished Faerzress from the Spire's door to the dome's edge, with Faerie Fire Lanterns on posts.</li>
 * <li>The spider pit at the Spire's foot (local {@value #PIT_A}, {@value #PIT_B}): {@value #PIT_SIZE} by
 * {@value #PIT_SIZE}, {@value #PIT_DEPTH} deep, lined with Drowstone, a low wall round it with a gap, cobwebs, and a
 * cave spider spawner (2 at a time) on its floor.</li>
 * </ul>
 * Its box is the whole spire, so the structure's {@code spawn_overrides} cover all of it.
 */
public class DrowSpireCavernPiece extends UnderdarkPiece {
    public static final int RADIUS = 28;
    public static final int HEIGHT = 44;
    public static final int SHELL = 2;
    public static final int FILL = 4;
    public static final int PIT_A = 13;
    public static final int PIT_B = -8;
    public static final int PIT_SIZE = 7;
    public static final int PIT_DEPTH = 4;
    /** The pit's spawner (SNBT, as CLAUDE.md's: a daylight-proof cave spider spawner, 2 at a time). */
    static final String SPAWNER_NBT = "{SpawnData:{entity:{id:\"minecraft:cave_spider\"},custom_spawn_rules:{"
            + "block_light_limit:[0,15],sky_light_limit:[0,15]}},Delay:100,MinSpawnDelay:200,MaxSpawnDelay:500,"
            + "SpawnCount:2,MaxNearbyEntities:6,RequiredPlayerRange:16,SpawnRange:3}";

    private static final int R = RADIUS + SHELL + 1;

    public DrowSpireCavernPiece(BlockPos center, Direction front, long seed) {
        super(DrowSpires.CAVERN, center, front, seed, -R, -FILL - PIT_DEPTH - 2, -R, R, HEIGHT + SHELL + 1, R);
    }

    public DrowSpireCavernPiece(NbtCompound nbt) {
        super(DrowSpires.CAVERN, nbt);
    }

    /** The dome's height at {@code r} blocks out (0 past the edge). */
    public static int height(double r) {
        if (r >= RADIUS) {
            return 0;
        }
        double f = r / RADIUS;
        return (int) Math.floor(HEIGHT * Math.sqrt(1.0D - f * f * f * f));
    }

    /** The pit spawner, in the world. */
    public BlockPos spawner() {
        return this.at(PIT_A, -PIT_DEPTH - 1, PIT_B);
    }

    /** The middle of the pit's floor (where you stand), in the world. */
    public BlockPos pitFloor() {
        return this.at(PIT_A + 1, -PIT_DEPTH, PIT_B);
    }

    @Override
    protected void build(Builder b) {
        BlockState fill = Blocks.DEEPSLATE.getDefaultState();
        BlockState faerzress = UnderdarkBlocks.FAERZRESS_STONE.getDefaultState();
        int rimHeight = height(RADIUS - 1) + SHELL;
        for (int a = -R; a <= R; a++) {
            for (int c = -R; c <= R; c++) {
                double r = Math.sqrt(a * a + c * c);
                if (r <= RADIUS) {
                    int h = height(r);
                    b.ground(a, c, FILL, h, fill, noise(this.seed, a, -1, c) < 0.18F ? faerzress : fill);
                    for (int y = h + 1; y <= h + SHELL; y++) {
                        b.solidify(a, y, c, rock(b).getDefaultState());
                    }
                    // Veins of faerzress in the floor, levelled or not.
                    if (noise(this.seed, a, -2, c) < 0.12F && r < RADIUS - 2) {
                        BlockState floor = b.get(a, -1, c);
                        if (floor != null && !floor.isAir() && floor.getFluidState().isEmpty()) {
                            b.set(a, -1, c, faerzress);
                        }
                    }
                } else if (r <= RADIUS + SHELL) {
                    for (int y = -FILL; y <= rimHeight; y++) {
                        b.solidify(a, y, c, rock(b).getDefaultState());
                    }
                }
            }
        }
        this.path(b);
        this.pit(b);
        // Faerzress veins glowing on the dome's walls: a few lanterns hung from the roof well off the Spire.
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0D + 0.4D;
            int a = (int) Math.round(Math.cos(angle) * 22);
            int c = (int) Math.round(Math.sin(angle) * 22);
            int top = height(Math.sqrt(a * a + c * c));
            if (top > 4) {
                b.hangingLantern(a, top, c, 3, UnderdarkBlocks.FAERIE_LANTERN);
            }
        }
    }

    /** Polished Faerzress from the Spire's door to the edge, lantern posts beside it. */
    private void path(Builder b) {
        BlockState polished = ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState();
        for (int c = DrowSpirePiece.DOOR_OUT; c <= RADIUS - 2; c++) {
            for (int a = -1; a <= 1; a++) {
                b.set(a, -1, c, polished);
            }
            if ((c - DrowSpirePiece.DOOR_OUT) % 6 == 3) {
                for (int side : new int[] {-2, 2}) {
                    b.set(side, 0, c, PieceBlocks.pillar(ModBlockFamilies.DROWSTONE_BRICKS, Direction.Axis.Y));
                    b.lantern(side, 1, c, UnderdarkBlocks.FAERIE_LANTERN);
                }
            }
        }
    }

    /** The spider pit: lined, walled round with a gap towards the path, webbed, a spawner on its floor. */
    private void pit(Builder b) {
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        BlockState wall = ModBlockFamilies.DROWSTONE_BRICKS.wall().getDefaultState();
        int half = PIT_SIZE / 2;
        for (int da = -half - 1; da <= half + 1; da++) {
            for (int dc = -half - 1; dc <= half + 1; dc++) {
                int a = PIT_A + da;
                int c = PIT_B + dc;
                boolean edge = Math.abs(da) == half + 1 || Math.abs(dc) == half + 1;
                for (int y = -PIT_DEPTH - 1; y <= -1; y++) {
                    if (edge || y == -PIT_DEPTH - 1) {
                        b.set(a, y, c, bricks);
                    } else {
                        b.set(a, y, c, noise(this.seed, a, y, c) < 0.3F ? Blocks.COBWEB.getDefaultState()
                                : Blocks.AIR.getDefaultState());
                    }
                }
                if (edge) {
                    // A low wall round the rim, with a gap facing the Spire's front.
                    b.set(a, 0, c, dc == half + 1 && da == 0 ? Blocks.AIR.getDefaultState() : wall);
                }
            }
        }
        BlockPos spawner = b.pos(PIT_A, -PIT_DEPTH - 1, PIT_B);
        b.set(PIT_A, -PIT_DEPTH - 1, PIT_B, Blocks.SPAWNER.getDefaultState());
        b.set(PIT_A, -PIT_DEPTH, PIT_B, Blocks.AIR.getDefaultState());
        if (b.chunkBox.contains(spawner) && b.world.getBlockEntity(spawner) instanceof MobSpawnerBlockEntity be) {
            try {
                NbtCompound nbt = StringNbtReader.parse(SPAWNER_NBT);
                // No world yet in a chunk being generated; the spawn data needs none.
                be.getLogic().readNbt(null, spawner, nbt);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
