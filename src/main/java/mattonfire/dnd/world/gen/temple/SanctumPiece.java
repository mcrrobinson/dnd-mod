package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Registry.BlockFamily;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModTempleBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * The sanctum, the temple's hearth, at the back of the courtyard against the mountain: a hall on a podium with
 * broad steps along its front, under a double-eaved roof of Ember Dragonscale Tiles striped Frost and Storm over
 * their altars. A colonnade of red-lacquered pillars on carved blackstone bases, with gilded capitals and a
 * lacquered architrave, runs round the outside under the lower roof, whose eaves sweep up at the corners. The
 * hall's walls stand two blocks in, framed by lacquered posts, with lattice windows, a frieze, and a gilded
 * arched door. Above them a clerestory with its own lattice windows carries the upper roof, ridged, with
 * horned finials and an incense brazier on the ridge. Inside, the hall is open to the clerestory, crossed by
 * lacquered beams hung with soul lanterns, with Ancestor Reliefs on the walls.
 *
 * <p>Along its back wall stand the three breath altars, each on a plinth of its colour's Dragonscale Tiles
 * under its banner, with an incense brazier either side and Breath-Scorched Stone on the floor in front:
 * <ul>
 * <li>Ember: a campfire</li>
 * <li>Frost: soul fire</li>
 * <li>Storm: a copper lightning rod</li>
 * </ul>
 * Behind the Ember altar the reliquary (a DC 15 locked chest). The Dragonborn Elder keeps to the sanctum. The
 * altars are the Dragonborn shrines ({@link TempleShrine}).
 */
public class SanctumPiece extends TemplePiece {
    static final int WIDTH = DragonTemplePlanner.COURT_WIDTH;
    static final int DEPTH = DragonTemplePlanner.SANCTUM_DEPTH;
    static final int HEIGHT = 21;
    private static final int MID = WIDTH / 2;
    /** The podium's top: the floor everything inside stands on. */
    static final int FLOOR = 1;
    /** The altars stand this far in (local z), and this far either side of the middle. */
    private static final int ALTAR_Z = 9;
    private static final int ALTAR_SPREAD = 5;
    /** The rings round the footprint: the colonnade, the hall's walls, the clerestory. */
    private static final int COLONNADE = 1;
    private static final int WALL = 2;
    private static final int CLERESTORY = 3;
    private static final int LOWER_EAVE = 8;
    private static final int UPPER_EAVE = 13;
    private static final int[] PILLARS_X = {1, 4, 7, 13, 16, 19};
    private static final int[] PILLARS_Z = {1, 4, 7, 10, 13};

    public SanctumPiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.SANCTUM, boxFrom(front, facing, WIDTH, HEIGHT, DEPTH), facing, seed);
    }

    public SanctumPiece(NbtCompound nbt) {
        super(DragonTempleStructures.SANCTUM, nbt);
    }

    /**
     * The ancestry whose altar {@code state} is part of (fire, plinth or rod), or null. Plinths are Dragonscale
     * Tiles; temples generated before them have red nether brick, blue ice and cut copper.
     */
    @Nullable
    public static DragonAncestry altarOf(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.CAMPFIRE || block == Blocks.RED_NETHER_BRICKS
                || block == ModBlockFamilies.EMBER_DRAGONSCALE_TILES.base()) {
            return DragonAncestry.EMBER;
        }
        if (block == Blocks.SOUL_CAMPFIRE || block == Blocks.BLUE_ICE
                || block == ModBlockFamilies.FROST_DRAGONSCALE_TILES.base()) {
            return DragonAncestry.FROST;
        }
        if (block == Blocks.LIGHTNING_ROD || block == Blocks.WAXED_CUT_COPPER
                || block == ModBlockFamilies.STORM_DRAGONSCALE_TILES.base()) {
            return DragonAncestry.STORM;
        }
        return null;
    }

    private static int altarX(DragonAncestry kind) {
        return kind == DragonAncestry.FROST ? MID - ALTAR_SPREAD : kind == DragonAncestry.STORM ? MID + ALTAR_SPREAD : MID;
    }

    /** Where the altar of {@code kind} burns (the fire or rod on top of its plinth). */
    public BlockPos altarTop(DragonAncestry kind) {
        return this.worldPos(altarX(kind), FLOOR + 2, ALTAR_Z);
    }

    /** Where to stand to use the altar of {@code kind}: two blocks in front of it. */
    public BlockPos inFrontOf(DragonAncestry kind) {
        return this.worldPos(altarX(kind), FLOOR + 1, ALTAR_Z - 2);
    }

    /** The reliquary chest behind the Ember altar. */
    public BlockPos reliquary() {
        return this.worldPos(MID, FLOOR + 2, DEPTH - 4);
    }

    /** How many blocks in from the footprint's edge local x/z is. */
    private static int ring(int x, int z) {
        return Math.min(Math.min(x, WIDTH - 1 - x), Math.min(z, DEPTH - 1 - z));
    }

    private static boolean pillarAt(int x, int z) {
        if (ring(x, z) != COLONNADE) {
            return false;
        }
        if (z == COLONNADE || z == DEPTH - 1 - COLONNADE) {
            for (int px : PILLARS_X) {
                if (px == x) {
                    return true;
                }
            }
            return false;
        }
        for (int pz : PILLARS_Z) {
            if (pz == z) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void build(Builder b) {
        BlockFamily lacquer = ModBlockFamilies.TEMPLE_LACQUER_PLANKS;
        // The podium: the whole footprint a block up, broad steps along the front.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                b.foundation(x, 0, z, Blocks.COBBLED_DEEPSLATE.getDefaultState());
                b.set(x, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
                b.air(x, FLOOR + 1, z, x, HEIGHT - 1, z);
                int ring = ring(x, z);
                BlockState floor;
                if (z == 0) {
                    floor = stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, BACK, false);
                } else if (ring == 0) {
                    floor = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                } else if (ring <= WALL) {
                    floor = Blocks.POLISHED_DEEPSLATE.getDefaultState();
                } else if (Math.abs(x - MID) <= 1 && z < ALTAR_Z) {
                    floor = Blocks.POLISHED_BLACKSTONE.getDefaultState();
                } else {
                    floor = ((x + z) % 4 == 0 ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_TILES).getDefaultState();
                }
                b.set(x, FLOOR, z, floor);
            }
        }

        // The colonnade: lacquered pillars on carved bases, gilded capitals, and the architrave between them.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                if (ring(x, z) != COLONNADE) {
                    continue;
                }
                if (pillarAt(x, z)) {
                    b.set(x, FLOOR + 1, z, Blocks.CHISELED_POLISHED_BLACKSTONE);
                    b.fill(x, FLOOR + 2, z, x, FLOOR + 5, z, lacquer.base());
                    b.set(x, FLOOR + 6, z, Blocks.GOLD_BLOCK);
                } else {
                    b.set(x, FLOOR + 6, z, lacquer.base());
                    // Brackets under the architrave beside each pillar.
                    Direction toPillar = this.pillarBeside(x, z);
                    if (toPillar != null) {
                        b.set(x, FLOOR + 5, z, stairs(lacquer, toPillar, true));
                    }
                }
            }
        }

        this.walls(b, lacquer);
        this.clerestory(b, lacquer);

        // Inside: lacquered beams across under the clerestory, hung with soul lanterns.
        for (int z : new int[]{5, ALTAR_Z - 1}) {
            b.fill(CLERESTORY, LOWER_EAVE + 1, z, WIDTH - 1 - CLERESTORY, LOWER_EAVE + 1, z, lacquer.base());
            for (int x : new int[]{MID - 4, MID + 4}) {
                b.set(x, LOWER_EAVE, z, Blocks.CHAIN);
                b.set(x, LOWER_EAVE - 1, z, soulLantern(true));
            }
        }

        // The three breath altars along the back, each under its colour's banner.
        this.altar(b, DragonAncestry.FROST);
        this.altar(b, DragonAncestry.EMBER);
        this.altar(b, DragonAncestry.STORM);

        // The reliquary behind the Ember altar, on a gilded step.
        int rz = DEPTH - 4;
        b.set(MID, FLOOR + 1, rz, Blocks.GILDED_BLACKSTONE);
        b.chest(MID, FLOOR + 2, rz, FRONT, RELIQUARY);
        b.set(MID - 1, FLOOR + 1, rz, b.candle(Blocks.YELLOW_CANDLE));
        b.set(MID + 1, FLOOR + 1, rz, b.candle(Blocks.YELLOW_CANDLE));

        // The roofs: the lower one round the colonnade, the upper one over the clerestory, both Ember tiles
        // with a Frost and a Storm stripe over their altars.
        java.util.function.IntFunction<BlockFamily> tiles = x -> x == altarX(DragonAncestry.FROST) ? ModBlockFamilies.FROST_DRAGONSCALE_TILES
                : x == altarX(DragonAncestry.STORM) ? ModBlockFamilies.STORM_DRAGONSCALE_TILES : ModBlockFamilies.EMBER_DRAGONSCALE_TILES;
        hipRoof(b, 0, 0, WIDTH - 1, DEPTH - 1, LOWER_EAVE, new int[]{0, 0, 1}, tiles, true);
        hipRoof(b, WALL, WALL, WIDTH - 1 - WALL, DEPTH - 1 - WALL, UPPER_EAVE, new int[]{0, 0, 1, 2, 3, 4}, tiles, true);
        // The ridge: a dark crest between horned, gilded finials, with an incense brazier smoking in the middle.
        int ridgeZ = DEPTH / 2;
        int ridgeY = UPPER_EAVE + 5;
        int end = WALL + 5;
        b.fill(end + 1, ridgeY, ridgeZ, WIDTH - 2 - end, ridgeY, ridgeZ, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        for (int x : new int[]{end, WIDTH - 1 - end}) {
            b.set(x, ridgeY, ridgeZ, Blocks.GILDED_BLACKSTONE);
            b.set(x, ridgeY + 1, ridgeZ, facing(ModTempleBlocks.DRAGON_HORN, Direction.UP));
        }
        b.set(MID, ridgeY, ridgeZ, Blocks.GILDED_BLACKSTONE);
        b.set(MID, ridgeY + 1, ridgeZ, ModTempleBlocks.EMBER_INCENSE_BRAZIER);

        // The Elder, and a Templar at the door.
        b.dragonborn(MID, FLOOR + 1, ALTAR_Z - 2, ModEntityTypes.DRAGONBORN_ELDER);
        b.dragonborn(MID + 2, FLOOR + 1, COLONNADE, ModEntityTypes.DRAGONBORN_TEMPLAR);
    }

    /** The direction to the colonnade pillar next to local x/z along the colonnade, or null. */
    @Nullable
    private Direction pillarBeside(int x, int z) {
        if (z == COLONNADE || z == DEPTH - 1 - COLONNADE) {
            return pillarAt(x - 1, z) ? Direction.WEST : pillarAt(x + 1, z) ? Direction.EAST : null;
        }
        return pillarAt(x, z - 1) ? FRONT : pillarAt(x, z + 1) ? BACK : null;
    }

    /** The hall's walls: lacquered posts framing masonry panels, lattice windows, a frieze, the door. */
    private void walls(Builder b, BlockFamily lacquer) {
        for (int x = WALL; x < WIDTH - WALL; x++) {
            for (int z = WALL; z < DEPTH - WALL; z++) {
                if (ring(x, z) != WALL) {
                    continue;
                }
                boolean frontBack = z == WALL || z == DEPTH - 1 - WALL;
                int along = frontBack ? x : z;
                boolean corner = (x == WALL || x == WIDTH - 1 - WALL) && frontBack;
                boolean post = corner || (frontBack ? contains(PILLARS_X, x) : contains(PILLARS_Z, z));
                for (int y = FLOOR + 1; y <= LOWER_EAVE; y++) {
                    BlockState state;
                    if (y == LOWER_EAVE) {
                        state = Blocks.CHISELED_DEEPSLATE.getDefaultState();
                    } else if (y == FLOOR + 6) {
                        state = lacquer.base().getDefaultState();
                    } else if (post) {
                        state = lacquer.base().getDefaultState();
                    } else if (y == FLOOR + 1) {
                        state = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                    } else if ((y == FLOOR + 3 || y == FLOOR + 4) && this.window(frontBack, along, z)) {
                        state = lacquer.fence().getDefaultState();
                    } else {
                        state = masonry(b).getDefaultState();
                    }
                    b.set(x, y, z, state);
                }
            }
        }
        // Ancestor Reliefs on the side walls, looking in.
        for (int z : new int[]{4, 10}) {
            b.set(WALL, FLOOR + 3, z, facing(ModTempleBlocks.ANCESTOR_RELIEF, Direction.EAST));
            b.set(WIDTH - 1 - WALL, FLOOR + 3, z, facing(ModTempleBlocks.ANCESTOR_RELIEF, Direction.WEST));
        }
        // The door: an arched opening framed in lacquer under a gilded lintel.
        b.air(MID - 1, FLOOR + 1, WALL, MID + 1, FLOOR + 3, WALL);
        b.set(MID, FLOOR + 4, WALL, Blocks.AIR);
        b.set(MID - 1, FLOOR + 4, WALL, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true));
        b.set(MID + 1, FLOOR + 4, WALL, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true));
        b.fill(MID - 2, FLOOR + 1, WALL, MID - 2, FLOOR + 5, WALL, lacquer.base());
        b.fill(MID + 2, FLOOR + 1, WALL, MID + 2, FLOOR + 5, WALL, lacquer.base());
        b.fill(MID - 1, FLOOR + 5, WALL, MID + 1, FLOOR + 5, WALL, Blocks.GOLD_BLOCK);
        b.set(MID, FLOOR + 7, WALL, facing(ModTempleBlocks.ANCESTOR_RELIEF, FRONT));
    }

    /** Whether a lattice window goes at this spot of the walls ({@code along} the wall). */
    private boolean window(boolean frontBack, int along, int z) {
        if (!frontBack) {
            return along == 5 || along == 6;
        }
        // The front wall only: the back stands against the mountain.
        return z == WALL && (along == 5 || along == 6 || along == WIDTH - 6 || along == WIDTH - 7);
    }

    /** The clerestory over the hall: a beam ring on the walls' tops, lattice windows, a beam under the roof. */
    private void clerestory(Builder b, BlockFamily lacquer) {
        for (int x = CLERESTORY; x < WIDTH - CLERESTORY; x++) {
            for (int z = CLERESTORY; z < DEPTH - CLERESTORY; z++) {
                if (ring(x, z) != CLERESTORY) {
                    continue;
                }
                boolean frontBack = z == CLERESTORY || z == DEPTH - 1 - CLERESTORY;
                int along = frontBack ? x - CLERESTORY : z - CLERESTORY;
                boolean post = along % 4 == 0 || (frontBack ? x == WIDTH - 1 - CLERESTORY : z == DEPTH - 1 - CLERESTORY);
                b.set(x, LOWER_EAVE + 1, z, lacquer.base());
                for (int y = LOWER_EAVE + 2; y < UPPER_EAVE - 1; y++) {
                    b.set(x, y, z, post ? lacquer.base().getDefaultState()
                            : y == LOWER_EAVE + 2 || y == LOWER_EAVE + 3 ? lacquer.fence().getDefaultState()
                            : Blocks.DEEPSLATE_TILES.getDefaultState());
                }
                b.set(x, UPPER_EAVE - 1, z, lacquer.base());
            }
        }
    }

    private static boolean contains(int[] values, int v) {
        for (int value : values) {
            if (value == v) {
                return true;
            }
        }
        return false;
    }

    private void altar(Builder b, DragonAncestry kind) {
        int x = altarX(kind);
        int z = ALTAR_Z;
        int y = FLOOR + 1;
        BlockFamily tiles = ModBlockFamilies.dragonscale(kind);
        BlockState top;
        DyeColor color;
        switch (kind) {
            case FROST -> {
                top = Blocks.SOUL_CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, FRONT);
                color = DyeColor.LIGHT_BLUE;
            }
            case STORM -> {
                top = Blocks.LIGHTNING_ROD.getDefaultState().with(LightningRodBlock.FACING, Direction.UP);
                color = DyeColor.BLUE;
            }
            default -> {
                top = Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.FACING, FRONT);
                color = DyeColor.RED;
            }
        }
        // A step of gilded blackstone round the plinth.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                b.set(x + dx, FLOOR, z + dz, Blocks.GILDED_BLACKSTONE);
            }
        }
        b.set(x, y, z, tiles.base());
        b.set(x, y + 1, z, top);
        b.banner(x, y + 3, DEPTH - 1 - WALL - 1, FRONT, color, ModBannerPatterns.THREE_BREATHS.in(DyeColor.YELLOW));
        b.set(x - 1, y, z, stairs(tiles, Direction.EAST, false));
        b.set(x + 1, y, z, stairs(tiles, Direction.WEST, false));
        // An incense brazier either side, in front.
        b.set(x - 2, y, z - 1, ModTempleBlocks.brazier(kind));
        b.set(x + 2, y, z - 1, ModTempleBlocks.brazier(kind));
        // The floor in front scorched by the breath: solid in the middle, ragged at the edges.
        Block scorched = ModBlockFamilies.scorchedStone(kind).base();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 2; dz <= 4; dz++) {
                boolean core = Math.abs(dx) <= 1 && dz <= 3;
                boolean ragged = b.chance(0.45F);
                if (core || ragged) {
                    b.set(x + dx, FLOOR, z - dz, scorched);
                }
            }
        }
    }
}
