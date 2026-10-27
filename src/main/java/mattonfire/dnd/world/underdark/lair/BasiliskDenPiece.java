package mattonfire.dnd.world.underdark.lair;

import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.gen.PetrifiedStatues;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Basilisk Den (design 9, section 10): a low dome of radius {@value #RADIUS} and height {@value #HEIGHT} hollowed
 * out of a cave floor, a garden of the basilisk's petrified victims.
 * <ul>
 * <li>The floor levelled (up to {@value #FILL} blocks filled) and strewn with gravel, tuff and bone; a
 * {@value #SHELL}-block rock shell over the dome and round its rim where the cave was open, with Faerzress veins
 * glowing in it, and a mouth {@value #MOUTH_WIDTH} wide out towards the front (local +b).</li>
 * <li>{@value #STATUES} petrified victims (armour stands posed and coloured like the Beholder's, via
 * {@link PetrifiedStatues}): drow and duergar in their gear, now and then a surface adventurer, all facing the
 * middle where the gaze caught them; {@value #REMAINS} Petrified Remains kneeling by the walls.</li>
 * <li>A chest against the back wall ({@code chests/basilisk_den}): what the victims carried.</li>
 * <li>One Basilisk, persistent, kept within {@value #BASILISK_RANGE} blocks of the middle.</li>
 * </ul>
 */
public class BasiliskDenPiece extends UnderdarkPiece {
    public static final int RADIUS = 10;
    public static final int HEIGHT = 7;
    public static final int SHELL = 2;
    public static final int FILL = 3;
    public static final int STATUES = 7;
    public static final int REMAINS = 5;
    public static final int MOUTH_WIDTH = 3;
    public static final int BASILISK_RANGE = 8;
    /** The chest, local (0, 0, {@value #CHEST_B}). */
    public static final int CHEST_B = -RADIUS + 2;

    private static final int R = RADIUS + SHELL + 1;

    public BasiliskDenPiece(BlockPos center, Direction front, long seed) {
        super(UnderdarkLairs.DEN_PIECE, center, front, seed, -R, -FILL - 1, -R, R, HEIGHT + SHELL + 1, R);
    }

    public BasiliskDenPiece(NbtCompound nbt) {
        super(UnderdarkLairs.DEN_PIECE, nbt);
    }

    /** The dome's height at {@code r} blocks out: {@value #HEIGHT} in the middle, 3 at the wall. */
    public static int height(double r) {
        double f = r / (RADIUS + 2.0D);
        return f >= 1.0D ? 0 : (int) Math.floor(HEIGHT * Math.sqrt(1.0D - f * f));
    }

    /** The chest, in the world. */
    public BlockPos chest() {
        return this.at(0, 0, CHEST_B);
    }

    /** Just inside the mouth, looking in, in the world. */
    public BlockPos mouth() {
        return this.at(0, 0, RADIUS - 2);
    }

    @Override
    protected void build(Builder b) {
        BlockState fill = Blocks.TUFF.getDefaultState();
        for (int a = -R; a <= R; a++) {
            for (int c = -R; c <= R; c++) {
                double r = Math.sqrt(a * a + c * c);
                boolean mouth = c > 0 && Math.abs(a) <= MOUTH_WIDTH / 2;
                if (r <= RADIUS) {
                    int h = height(r);
                    b.ground(a, c, FILL, h, fill, fill);
                    b.set(a, -1, c, floor(noise(this.seed, a, -1, c)));
                    for (int y = h + 1; y <= h + SHELL; y++) {
                        b.solidify(a, y, c, wall(b, noise(this.seed, a, y, c)));
                    }
                } else if (r <= RADIUS + SHELL) {
                    int rim = height(RADIUS) + SHELL;
                    for (int y = -FILL; y <= rim; y++) {
                        BlockState wall = wall(b, noise(this.seed, a, y, c));
                        if (mouth && y >= 0 && y <= 2) {
                            if (y == 0) {
                                b.ground(a, c, FILL, 2, fill, floor(noise(this.seed, a, -1, c)));
                            }
                        } else if (y < 0) {
                            b.solidify(a, y, c, wall);
                        } else {
                            b.set(a, y, c, wall);
                        }
                    }
                }
            }
        }
        this.bones(b);
        // The victims, facing the middle where the gaze caught them: mostly drow and duergar.
        double turn = b.random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < STATUES; i++) {
            double angle = turn + Math.PI * 2.0D * i / STATUES + (b.random.nextDouble() - 0.5D) * 0.5D;
            double r = 3.5D + b.random.nextDouble() * 4.0D;
            int a = (int) Math.round(Math.cos(angle) * r);
            int c = (int) Math.round(Math.sin(angle) * r);
            float who = b.random.nextFloat();
            PetrifiedStatues.Victim victim = who < 0.45F ? PetrifiedStatues.Victim.DROW
                    : who < 0.85F ? PetrifiedStatues.Victim.DUERGAR : PetrifiedStatues.Victim.ADVENTURER;
            BlockPos middle = b.pos(0, 0, 0);
            BlockPos at = b.pos(a, 0, c);
            boolean clear = !(c > RADIUS - 4 && Math.abs(a) <= 1) && !(c < CHEST_B + 2 && Math.abs(a) <= 1);
            if (clear) {
                PetrifiedStatues.statue(b.world, b.chunkBox, at, middle.getX(), middle.getZ(), victim, b.random);
            }
        }
        // Remains kneeling out by the walls.
        for (int i = 0; i < REMAINS; i++) {
            double angle = b.random.nextDouble() * Math.PI * 2.0D;
            double r = RADIUS - 2.5D + b.random.nextDouble();
            int a = (int) Math.round(Math.cos(angle) * r);
            int c = (int) Math.round(Math.sin(angle) * r);
            BlockState there = b.get(a, 0, c);
            if (there != null && there.isAir() && !(c > 0 && Math.abs(a) <= MOUTH_WIDTH)) {
                b.set(a, 0, c, PetrifiedStatues.remainsFacing(new BlockPos(a, 0, c), 0, 0));
            }
        }
        b.chest(0, 0, CHEST_B, Direction.SOUTH, UnderdarkLairs.DEN_LOOT);
        b.set(-1, 0, CHEST_B, Blocks.BONE_BLOCK);
        b.set(1, 0, CHEST_B, Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, 8));
        b.resident(0, 0, 0, ModEntityTypes.BASILISK, BASILISK_RANGE);
    }

    /** Bones and skulls strewn over the floor. */
    private void bones(Builder b) {
        for (int i = 0; i < 14; i++) {
            int a = b.random.nextInt(2 * RADIUS - 3) - RADIUS + 2;
            int c = b.random.nextInt(2 * RADIUS - 3) - RADIUS + 2;
            int kind = b.random.nextInt(4);
            int rotation = b.random.nextInt(16);
            BlockState there = b.get(a, 0, c);
            if (there == null || !there.isAir() || a * a + c * c > (RADIUS - 2) * (RADIUS - 2) || a * a + c * c < 4) {
                continue;
            }
            b.set(a, 0, c, switch (kind) {
                case 0 -> Blocks.SKELETON_SKULL.getDefaultState().with(SkullBlock.ROTATION, rotation);
                case 1 -> Blocks.BONE_BLOCK.getDefaultState();
                default -> Blocks.COBWEB.getDefaultState();
            });
        }
    }

    /** The den's floor: gravel, tuff, cobbled deepslate, the odd bone block. */
    private static BlockState floor(float n) {
        return (n < 0.35F ? Blocks.GRAVEL : n < 0.6F ? Blocks.TUFF : n < 0.85F ? Blocks.COBBLED_DEEPSLATE
                : n < 0.95F ? Blocks.DEEPSLATE : Blocks.BONE_BLOCK).getDefaultState();
    }

    /** The shell: natural rock, veined with glowing Faerzress Stone. */
    private static BlockState wall(Builder b, float n) {
        BlockState rock = rock(b).getDefaultState();
        return n < 0.1F ? UnderdarkBlocks.FAERZRESS_STONE.getDefaultState() : rock;
    }
}
