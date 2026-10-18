package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Registry.BlockFamily;
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
 * The sanctum, the temple's hearth: a pillared hall of deepslate and blackstone at the back of the courtyard,
 * cut into the mountain, under a stepped roof of Ember Dragonscale Tiles striped Frost and Storm over their
 * altars, with lacquered beams between the pillars and Ancestor Reliefs at the pillars' feet. Along its back
 * wall stand the three breath altars, each on a plinth of its colour's Dragonscale Tiles under its banner, with
 * an incense brazier either side and Breath-Scorched Stone on the floor in front:
 * <ul>
 * <li>Ember: a campfire</li>
 * <li>Frost: soul fire</li>
 * <li>Storm: a copper lightning rod</li>
 * </ul>
 * In front of them an enchanting table ringed with bookshelves; behind the Ember altar the reliquary (a
 * DC 15 locked chest). The Dragonborn Elder keeps to the sanctum. The altars are the Dragonborn shrines
 * ({@link TempleShrine}).
 */
public class SanctumPiece extends TemplePiece {
    static final int WIDTH = DragonTemplePlanner.COURT_WIDTH;
    static final int DEPTH = DragonTemplePlanner.SANCTUM_DEPTH;
    static final int HEIGHT = 10;
    /** The roof's rings of stairs on top of the walls. */
    static final int ROOF_TIERS = 3;
    private static final int MID = WIDTH / 2;
    /** The altars stand this far in (local z). */
    private static final int ALTAR_Z = DEPTH - 4;

    public SanctumPiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.SANCTUM, boxFrom(front, facing, WIDTH, HEIGHT + ROOF_TIERS, DEPTH), facing, seed);
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

    /** Where the altar of {@code kind} burns (the fire or rod on top of its plinth). */
    public BlockPos altarTop(DragonAncestry kind) {
        int x = kind == DragonAncestry.FROST ? MID - 6 : kind == DragonAncestry.STORM ? MID + 6 : MID;
        return this.worldPos(x, 2, ALTAR_Z);
    }

    /** Where to stand to use the altar of {@code kind}: two blocks in front of it. */
    public BlockPos inFrontOf(DragonAncestry kind) {
        int x = kind == DragonAncestry.FROST ? MID - 6 : kind == DragonAncestry.STORM ? MID + 6 : MID;
        return this.worldPos(x, 1, ALTAR_Z - 2);
    }

    /** The reliquary chest behind the Ember altar. */
    public BlockPos reliquary() {
        return this.worldPos(MID, 2, DEPTH - 2);
    }

    @Override
    protected void build(Builder b) {
        // The shell: walls, floor and roof, hollow inside.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                b.foundation(x, 0, z, Blocks.COBBLED_DEEPSLATE.getDefaultState());
                boolean edge = x == 0 || z == 0 || x == WIDTH - 1 || z == DEPTH - 1;
                boolean aisle = Math.abs(x - MID) <= 1 && z < ALTAR_Z;
                b.set(x, 0, z, edge ? Blocks.POLISHED_BLACKSTONE_BRICKS
                        : aisle ? Blocks.POLISHED_BLACKSTONE : (x + z) % 4 == 0 ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_TILES);
                for (int y = 1; y < HEIGHT; y++) {
                    if (y == HEIGHT - 1) {
                        b.set(x, y, z, edge ? Blocks.GILDED_BLACKSTONE : masonry(b));
                    } else if (edge) {
                        b.set(x, y, z, y == 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS : y == HEIGHT - 2 ? Blocks.CHISELED_DEEPSLATE : masonry(b));
                    } else {
                        b.set(x, y, z, Blocks.AIR);
                    }
                }
            }
        }
        // A gilded doorway onto the courtyard.
        b.air(MID - 1, 1, 0, MID + 1, 4, 0);
        b.fill(MID - 2, 1, 0, MID - 2, 5, 0, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
        b.fill(MID + 2, 1, 0, MID + 2, 5, 0, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
        b.fill(MID - 2, 6, 0, MID + 2, 6, 0, Blocks.GOLD_BLOCK);
        b.set(MID - 1, 4, 0, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true));
        b.set(MID + 1, 4, 0, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true));

        // Pillars down both sides, an Ancestor Relief at each one's foot facing the aisle, and a lacquered
        // beam across between each pair.
        for (int z = 3; z < DEPTH - 1; z += 3) {
            for (int x : new int[]{2, WIDTH - 3}) {
                b.set(x, 1, z, facing(ModTempleBlocks.ANCESTOR_RELIEF, x < MID ? Direction.EAST : Direction.WEST));
                b.fill(x, 2, z, x, HEIGHT - 3, z, pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y));
                b.set(x, HEIGHT - 2, z, Blocks.GOLD_BLOCK);
            }
            b.fill(3, HEIGHT - 2, z, WIDTH - 4, HEIGHT - 2, z, ModBlockFamilies.TEMPLE_LACQUER_PLANKS.base());
        }

        // The enchanting table, ringed with bookshelves two blocks off.
        int tz = 4;
        b.set(MID, 1, tz, Blocks.ENCHANTING_TABLE);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                // Open at the front so you can walk in from the door.
                if (ring && !(dz == -2 && Math.abs(dx) <= 1)) {
                    b.set(MID + dx, 1, tz + dz, Blocks.BOOKSHELF);
                    if (Math.abs(dx) == 2 || dz == 2) {
                        b.set(MID + dx, 2, tz + dz, Blocks.BOOKSHELF);
                    }
                }
            }
        }

        // The three breath altars along the back, each under its colour's banner.
        this.altar(b, MID - 6, DragonAncestry.FROST);
        this.altar(b, MID, DragonAncestry.EMBER);
        this.altar(b, MID + 6, DragonAncestry.STORM);

        // The reliquary behind the Ember altar, on a gilded step.
        b.set(MID, 1, DEPTH - 2, Blocks.GILDED_BLACKSTONE);
        b.chest(MID, 2, DEPTH - 2, FRONT, RELIQUARY);
        b.set(MID - 1, 1, DEPTH - 2, b.candle(Blocks.YELLOW_CANDLE));
        b.set(MID + 1, 1, DEPTH - 2, b.candle(Blocks.YELLOW_CANDLE));

        // Soul lanterns from the ceiling.
        for (int x = MID - 6; x <= MID + 6; x += 6) {
            for (int z = 2; z < DEPTH - 1; z += 5) {
                b.set(x, HEIGHT - 2, z, Blocks.CHAIN);
                b.set(x, HEIGHT - 3, z, soulLantern(true));
            }
        }

        // The roof: Ember tiles, with a Frost and a Storm stripe over their altars.
        roof(b, WIDTH, DEPTH, HEIGHT, ROOF_TIERS, x -> x == MID - 6 ? ModBlockFamilies.FROST_DRAGONSCALE_TILES
                : x == MID + 6 ? ModBlockFamilies.STORM_DRAGONSCALE_TILES : ModBlockFamilies.EMBER_DRAGONSCALE_TILES);

        // The Elder, and a Templar at the door.
        b.dragonborn(MID, 1, ALTAR_Z - 2, ModEntityTypes.DRAGONBORN_ELDER);
        b.dragonborn(MID + 2, 1, 1, ModEntityTypes.DRAGONBORN_TEMPLAR);
    }

    private void altar(Builder b, int x, DragonAncestry kind) {
        int z = ALTAR_Z;
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
                b.set(x + dx, 0, z + dz, Blocks.GILDED_BLACKSTONE);
            }
        }
        b.set(x, 1, z, tiles.base());
        b.set(x, 2, z, top);
        b.banner(x, 4, DEPTH - 2, FRONT, color);
        b.set(x - 1, 1, z, stairs(tiles, Direction.EAST, false));
        b.set(x + 1, 1, z, stairs(tiles, Direction.WEST, false));
        // An incense brazier either side, in front.
        b.set(x - 2, 1, z - 1, ModTempleBlocks.brazier(kind));
        b.set(x + 2, 1, z - 1, ModTempleBlocks.brazier(kind));
        // The floor in front scorched by the breath: solid in the middle, ragged at the edges.
        Block scorched = ModBlockFamilies.scorchedStone(kind).base();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 2; dz <= 4; dz++) {
                boolean core = Math.abs(dx) <= 1 && dz <= 3;
                boolean ragged = b.chance(0.45F);
                if (core || ragged) {
                    b.set(x + dx, 0, z - dz, scorched);
                }
            }
        }
    }
}
