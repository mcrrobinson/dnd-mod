package mattonfire.dnd.world.gen.temple;

import static mattonfire.dnd.world.gen.PieceBlocks.*;

import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModTempleBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The courtyard at the top of the grand stair, 21 by 15: a gilded path of polished blackstone from the stair
 * to the sanctum steps that parts round the great Ember statue in the middle, with the Frost and Storm
 * guardians at the front corners ({@link StatuePiece}), Frost and Storm incense-brazier posts at the back
 * corners, paper lanterns on lacquered posts beside the path and Templars on watch.
 */
public class CourtyardPiece extends TemplePiece {
    static final int WIDTH = DragonTemplePlanner.COURT_WIDTH;
    static final int DEPTH = DragonTemplePlanner.COURT_DEPTH;
    private static final int MID = WIDTH / 2;

    public CourtyardPiece(BlockPos front, Direction facing, long seed) {
        super(DragonTempleStructures.COURTYARD, boxFrom(front, facing, WIDTH, 5, DEPTH), facing, seed);
    }

    public CourtyardPiece(NbtCompound nbt) {
        super(DragonTempleStructures.COURTYARD, nbt);
    }

    /**
     * Where a statue stands, relative to the temple's centre line and the courtyard's front edge: {x across,
     * z in} of the middle of its box's front side (its plinth starts {@link StatuePiece#SETBACK} further in). The great Ember statue in the middle of the path, the Frost and
     * Storm guardians at the front corners.
     */
    static int[] statueSpot(DragonAncestry kind) {
        return switch (kind) {
            case FROST -> new int[]{-8, 0};
            case STORM -> new int[]{8, 0};
            default -> new int[]{0, 3};
        };
    }

    /** Whether local x/z is under a statue's plinth. */
    private static boolean underStatue(int x, int z) {
        for (DragonAncestry kind : new DragonAncestry[]{DragonAncestry.FROST, DragonAncestry.EMBER, DragonAncestry.STORM}) {
            int[] spot = statueSpot(kind);
            int sx = MID + spot[0];
            int z0 = spot[1] + StatuePiece.SETBACK;
            if (Math.abs(x - sx) <= StatuePiece.plinthWidth(kind) / 2 && z >= z0 && z < z0 + StatuePiece.plinthDepth(kind)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void build(Builder b) {
        int emberStart = statueSpot(DragonAncestry.EMBER)[1] + StatuePiece.SETBACK;
        int around = StatuePiece.GREAT_WIDTH / 2 + 1;
        int emberEnd = emberStart + StatuePiece.GREAT_DEPTH;
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                if (underStatue(x, z)) {
                    continue;
                }
                int fromMid = Math.abs(x - MID);
                // The path runs straight up the middle from the stair, parts round the Ember statue and meets
                // again before the sanctum's steps.
                boolean path;
                if (z < emberStart - 1) {
                    path = fromMid <= 2;
                } else if (z == emberStart - 1 || z >= emberEnd) {
                    path = fromMid <= around + 1;
                } else {
                    path = fromMid >= around && fromMid <= around + 1;
                }
                boolean trim = !path && (z < emberStart - 1 ? fromMid == 3 : fromMid == around + 2 && z < emberEnd);
                Block floor = path ? Blocks.POLISHED_BLACKSTONE_BRICKS : trim ? Blocks.GILDED_BLACKSTONE
                        : (x + z) % 2 == 0 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE;
                b.set(x, 0, z, floor);
            }
        }
        // Incense-brazier posts at the back corners, Frost and Storm on their guardians' sides.
        for (int x : new int[]{1, WIDTH - 2}) {
            int z = DEPTH - 2;
            b.set(x, 1, z, Blocks.CHISELED_POLISHED_BLACKSTONE);
            b.fill(x, 2, z, x, 3, z, Blocks.POLISHED_BLACKSTONE_WALL);
            b.set(x, 4, z, ModTempleBlocks.brazier(x < MID ? DragonAncestry.FROST : DragonAncestry.STORM));
        }
        // Paper lanterns on lacquered posts: either side of the path at the stair, and by the sanctum steps.
        for (int x : new int[]{MID - 4, MID + 4, MID - 7, MID + 7}) {
            int z = Math.abs(x - MID) == 4 ? 1 : DEPTH - 3;
            b.fill(x, 1, z, x, 2, z, ModBlockFamilies.TEMPLE_LACQUER_PLANKS.fence());
            b.set(x, 3, z, ModTempleBlocks.PAPER_LANTERN);
        }
        // Templars on watch.
        b.dragonborn(MID - 7, 1, DEPTH - 5, ModEntityTypes.DRAGONBORN_TEMPLAR);
        b.dragonborn(MID + 7, 1, DEPTH - 5, ModEntityTypes.DRAGONBORN_TEMPLAR);
        if (b.chance(0.5F)) {
            b.dragonborn(MID, 1, 1, ModEntityTypes.DRAGONBORN_TEMPLAR);
        }
    }
}
