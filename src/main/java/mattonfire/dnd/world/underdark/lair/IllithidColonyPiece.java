package mattonfire.dnd.world.underdark.lair;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.MindFlayerEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * An Illithid Colony (design 9, section 10): organic, purple-grey chambers swelling out of a cavern floor, laid out
 * round the core chamber with the entrance towards the front (local +b).
 * <ul>
 * <li>Four chambers, each a rough dome ({@link #CHAMBERS}: the core, radius 9 and 8 high; the larder behind it, the
 * den and the hall either side, radius 5 and 5 high) running into one another, with a {@value #SHELL}-block shell of
 * Polished Faerzress, Faerzress Stone, tuff, crying obsidian and amethyst, sculk spreading over the floor. A tunnel
 * through the core's front wall is the way in.</li>
 * <li>The <b>brine pool</b> in the middle of the core: radius {@value #POOL_RADIUS}, {@value #POOL_DEPTH} deep, magma
 * under it pulling bubbles down. A future Elder Brain's arena ({@link #pool}).</li>
 * <li>2 or 3 Mind Flayers (the core, the den, and half the time the hall), persistent, calling up no thralls of their
 * own; 3 to 5 thralls from {@code #dndclasses:underdark/thralls} (drow and duergar, bosses left out), persistent and
 * tagged {@value MindFlayerEntity#COLONY_THRALL_TAG}, which a flayer near them charms within a second
 * ({@link MindFlayerEntity#claimColonyThralls}).</li>
 * <li>The larder: three cells against its back wall behind locked Chain Cage gates; the first two hold a kneeling
 * captive (a hobbit, a mountain dwarf or a wood elf), the third half the time.</li>
 * <li>A chest in the den ({@code chests/illithid_colony}).</li>
 * </ul>
 */
public class IllithidColonyPiece extends UnderdarkPiece {
    /** The chambers: local a, b of the floor's centre, radius, height. Core first. */
    public static final int[][] CHAMBERS = {{0, 0, 9, 8}, {0, -12, 5, 5}, {11, 5, 5, 5}, {-11, 5, 5, 5}};
    public static final int SHELL = 2;
    public static final int FILL = 3;
    public static final double POOL_RADIUS = 3.5D;
    public static final int POOL_DEPTH = 3;
    /** The entrance tunnel runs out to this local b. */
    public static final int ENTRANCE = 15;
    /** The larder cells (local a); captives kneel at b = {@value #CELL_B}, the gates are one block in front. */
    public static final int[] CELLS = {-2, 0, 2};
    public static final int CELL_B = -15;
    /** Mind Flayer spots (local a, b): the core, the den, the hall (only with 3). */
    static final int[][] FLAYERS = {{-3, -5}, {12, 7}, {-12, 7}};
    /** Thrall spots (local a, b). */
    static final int[][] THRALLS = {{5, 3}, {-5, 4}, {9, 3}, {-9, 3}, {3, -9}};
    /** The den's chest (local a, b). */
    public static final int[] CHEST = {14, 5};
    public static final int FLAYER_RANGE = 12;
    public static final int THRALL_RANGE = 14;
    private static final List<EntityType<? extends MobEntity>> CAPTIVES = List.of(ModEntityTypes.HOBBIT,
            ModEntityTypes.MOUNTAIN_DWARF, ModEntityTypes.WOOD_ELF);

    private static final int A = 19;
    private static final int B1 = -20;
    private static final int B2 = ENTRANCE + 1;
    private static final int TOP = 8 + SHELL + 1;

    public IllithidColonyPiece(BlockPos center, Direction front, long seed) {
        super(UnderdarkLairs.COLONY_PIECE, center, front, seed, -A, -POOL_DEPTH - 2, B1, A, TOP, B2);
    }

    public IllithidColonyPiece(NbtCompound nbt) {
        super(UnderdarkLairs.COLONY_PIECE, nbt);
    }

    /** The middle of the brine pool's surface, in the world. */
    public BlockPos pool() {
        return this.at(0, -1, 0);
    }

    /** The entrance's mouth, in the world. */
    public BlockPos entrance() {
        return this.at(0, 0, ENTRANCE);
    }

    /** Larder cell {@code i}'s gate (its lower cage), in the world. */
    public BlockPos gate(int i) {
        return this.at(CELLS[i], 0, CELL_B + 1);
    }

    /** The den's chest, in the world. */
    public BlockPos chest() {
        return this.at(CHEST[0], 0, CHEST[1]);
    }

    @Override
    protected void build(Builder b) {
        for (int a = -A; a <= A; a++) {
            for (int c = B1; c <= B2; c++) {
                this.column(b, a, c);
            }
        }
        this.entrance(b);
        this.larder(b);
        this.folk(b);
        b.chest(CHEST[0], 0, CHEST[1], Direction.WEST, UnderdarkLairs.COLONY_LOOT);
        b.set(CHEST[0], 0, CHEST[1] - 1, Blocks.SKELETON_SKULL);
        b.set(CHEST[0], 0, CHEST[1] + 1, Blocks.CANDLE.getDefaultState()
                .with(net.minecraft.block.CandleBlock.LIT, true).with(net.minecraft.block.CandleBlock.CANDLES, 3));
    }

    /** One column of the chambers: floor, pool, air, shell. */
    private void column(Builder b, int a, int c) {
        double rough = (noise(this.seed, a >> 1, 0, c >> 1) - 0.5D) * 1.4D;
        double foot = Double.MAX_VALUE;
        for (int[] ch : CHAMBERS) {
            foot = Math.min(foot, Math.sqrt(sq(a - ch[0]) + sq(c - ch[1])) - ch[2]);
        }
        foot += rough;
        if (foot > SHELL + 1) {
            return;
        }
        double pool = Math.sqrt(a * a + c * c);
        for (int y = -POOL_DEPTH - 1; y <= TOP; y++) {
            float n = noise(this.seed, a, y, c);
            if (y < 0) {
                if (y < -FILL && pool > POOL_RADIUS + 1.0D) {
                    continue;
                }
                if (pool <= POOL_RADIUS && y >= -POOL_DEPTH) {
                    // Bubbles pulled down over the magma, still water round its edge.
                    b.set(a, y, c, pool <= POOL_RADIUS - 1.0D
                            ? Blocks.BUBBLE_COLUMN.getDefaultState().with(BubbleColumnBlock.DRAG, true)
                            : Blocks.WATER.getDefaultState());
                } else if (pool <= POOL_RADIUS && y == -POOL_DEPTH - 1 && pool <= POOL_RADIUS - 1.0D) {
                    b.set(a, y, c, Blocks.MAGMA_BLOCK.getDefaultState());
                } else if (pool <= POOL_RADIUS + 1.0D && y >= -POOL_DEPTH - 1) {
                    b.set(a, y, c, ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState());
                } else if (foot <= 0.0D) {
                    b.set(a, y, c, y == -1 ? floor(n) : n < 0.2F ? UnderdarkBlocks.FAERZRESS_STONE.getDefaultState()
                            : Blocks.DEEPSLATE.getDefaultState());
                } else if (foot <= SHELL) {
                    b.set(a, y, c, wall(n));
                } else {
                    b.solidify(a, y, c, wall(n));
                }
                continue;
            }
            double out = Double.MAX_VALUE;
            for (int[] ch : CHAMBERS) {
                double hr = Math.sqrt(sq(a - ch[0]) + sq(c - ch[1]));
                double v = Math.sqrt(sq(hr / ch[2]) + sq(y / (double) ch[3]));
                out = Math.min(out, (v - 1.0D) * ch[2]);
            }
            out += rough;
            if (out < 0.0D) {
                b.clear(a, y, c);
            } else if (out < SHELL) {
                b.set(a, y, c, wall(n));
            } else if (out < SHELL + 1) {
                b.solidify(a, y, c, wall(n));
            }
        }
    }

    /** The tunnel in through the core's front wall. */
    private void entrance(Builder b) {
        BlockState polished = ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState();
        for (int c = 6; c <= ENTRANCE; c++) {
            for (int a = -2; a <= 2; a++) {
                for (int y = -1; y <= 3; y++) {
                    boolean side = Math.abs(a) == 2 || y == 3;
                    if (y == -1) {
                        b.set(a, y, c, polished);
                    } else if (side) {
                        if (c >= 9) {
                            b.set(a, y, c, wall(noise(this.seed, a, y, c)));
                        }
                    } else {
                        b.clear(a, y, c);
                    }
                }
            }
        }
        b.set(-2, 1, ENTRANCE, Blocks.CRYING_OBSIDIAN);
        b.set(2, 1, ENTRANCE, Blocks.CRYING_OBSIDIAN);
    }

    /** The larder's cells: Polished Faerzress stalls behind locked Chain Cage gates, and their captives. */
    private void larder(Builder b) {
        BlockState polished = ModBlockFamilies.POLISHED_FAERZRESS.base().getDefaultState();
        BlockState gate = UnderdarkBlocks.CHAIN_CAGE.getDefaultState().with(ChainCageBlock.FACING, Direction.SOUTH)
                .with(ChainCageBlock.LOCKED, true);
        for (int a = -3; a <= 3; a++) {
            for (int c = CELL_B - 1; c <= CELL_B + 1; c++) {
                b.set(a, -1, c, polished);
                for (int y = 0; y <= 2; y++) {
                    boolean cell = (a == -2 || a == 0 || a == 2) && c == CELL_B && y <= 1;
                    boolean front = (a == -2 || a == 0 || a == 2) && c == CELL_B + 1 && y <= 1;
                    b.set(a, y, c, cell ? Blocks.AIR.getDefaultState() : front ? gate : polished);
                }
            }
        }
        for (int i = 0; i < CELLS.length; i++) {
            EntityType<? extends MobEntity> type = CAPTIVES.get(b.random.nextInt(CAPTIVES.size()));
            boolean held = i < 2 || b.random.nextBoolean();
            if (held) {
                b.captive(CELLS[i], 0, CELL_B, type, Direction.SOUTH);
            }
        }
    }

    /** The Mind Flayers and their thralls. */
    private void folk(Builder b) {
        int flayers = 2 + (b.random.nextBoolean() ? 1 : 0);
        for (int i = 0; i < flayers; i++) {
            b.resident(FLAYERS[i][0], 0, FLAYERS[i][1], ModEntityTypes.MIND_FLAYER, FLAYER_RANGE,
                    flayer -> flayer.setPendingThralls(0));
        }
        int thralls = 3 + b.random.nextInt(3);
        List<EntityType<?>> kinds = thrallKinds(b);
        for (int i = 0; i < thralls; i++) {
            int roll = b.random.nextInt(Math.max(1, kinds.size()));
            if (kinds.isEmpty()) {
                continue;
            }
            this.thrall(b, THRALLS[i][0], THRALLS[i][1], kinds.get(roll));
        }
    }

    @SuppressWarnings("unchecked")
    private void thrall(Builder b, int a, int c, EntityType<?> type) {
        try {
            b.resident(a, 0, c, (EntityType<MobEntity>) type, THRALL_RANGE,
                    mob -> mob.addCommandTag(MindFlayerEntity.COLONY_THRALL_TAG));
        } catch (ClassCastException e) {
            // Not a mob: no thrall.
        }
    }

    /** {@code #dndclasses:underdark/thralls}, without bosses or anything that isn't a mob. */
    private static List<EntityType<?>> thrallKinds(Builder b) {
        List<EntityType<?>> kinds = new ArrayList<>();
        for (RegistryEntry<EntityType<?>> entry : Registries.ENTITY_TYPE.iterateEntries(Drow.THRALLS_TAG)) {
            Entity probe = entry.value().create(b.world.toServerWorld());
            if (probe instanceof MobEntity && !(probe instanceof Boss)) {
                kinds.add(entry.value());
            }
            if (probe != null) {
                probe.discard();
            }
        }
        return kinds;
    }

    /** The chambers' floor: Polished Faerzress, Faerzress Stone and sculk spreading over it, crying obsidian. */
    private static BlockState floor(float n) {
        return (n < 0.45F ? ModBlockFamilies.POLISHED_FAERZRESS.base() : n < 0.62F ? Blocks.SCULK
                : n < 0.8F ? UnderdarkBlocks.FAERZRESS_STONE : n < 0.92F ? Blocks.TUFF : Blocks.CRYING_OBSIDIAN)
                .getDefaultState();
    }

    /** The chambers' organic shell. */
    private static BlockState wall(float n) {
        return (n < 0.08F ? Blocks.CRYING_OBSIDIAN : n < 0.13F ? Blocks.AMETHYST_BLOCK : n < 0.55F
                ? ModBlockFamilies.POLISHED_FAERZRESS.base() : n < 0.72F ? UnderdarkBlocks.FAERZRESS_STONE
                : n < 0.86F ? Blocks.TUFF : Blocks.SMOOTH_BASALT).getDefaultState();
    }

    private static double sq(double d) {
        return d * d;
    }
}
