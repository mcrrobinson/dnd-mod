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
 * to the sanctum door that parts round the Ember statue in the middle, with the Frost and Storm statues
 * either side ({@link StatuePiece}), incense-brazier posts at the corners (Ember at the front, Frost and Storm at
 * the back on their statues' sides), paper lanterns on lacquered posts beside the path and Templars on watch.
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
     * z in} of the middle of its front side. Ember in the middle of the path, Frost and Storm either side.
     */
    static int[] statueSpot(DragonAncestry kind) {
        return switch (kind) {
            case FROST -> new int[]{-7, 3};
            case STORM -> new int[]{7, 3};
            default -> new int[]{0, 5};
        };
    }

    /** Whether local x/z is under a statue (statues are 5 by 5). */
    private static boolean underStatue(int x, int z) {
        for (DragonAncestry kind : new DragonAncestry[]{DragonAncestry.FROST, DragonAncestry.EMBER, DragonAncestry.STORM}) {
            int[] spot = statueSpot(kind);
            int sx = MID + spot[0];
            if (Math.abs(x - sx) <= 2 && z >= spot[1] && z < spot[1] + StatuePiece.SIZE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void build(Builder b) {
        int[] ember = statueSpot(DragonAncestry.EMBER);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                if (underStatue(x, z)) {
                    continue;
                }
                int fromMid = Math.abs(x - MID);
                // The path runs straight up the middle, and parts round the Ember statue.
                boolean around = z >= ember[1] - 1 && z <= ember[1] + StatuePiece.SIZE;
                boolean path = around ? fromMid >= 3 && fromMid <= 4 || z == ember[1] - 1 && fromMid <= 4
                        || z == ember[1] + StatuePiece.SIZE && fromMid <= 4 : fromMid <= 2;
                boolean trim = !path && (around ? fromMid == 5 : fromMid == 3);
                Block floor = path ? Blocks.POLISHED_BLACKSTONE_BRICKS : trim ? Blocks.GILDED_BLACKSTONE
                        : (x + z) % 2 == 0 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE;
                b.set(x, 0, z, floor);
            }
        }
        // Incense-brazier posts at the corners: Ember at the front, Frost and Storm at the back.
        for (int x : new int[]{1, WIDTH - 2}) {
            for (int z : new int[]{1, DEPTH - 2}) {
                b.set(x, 1, z, Blocks.CHISELED_POLISHED_BLACKSTONE);
                b.fill(x, 2, z, x, 3, z, Blocks.POLISHED_BLACKSTONE_WALL);
                DragonAncestry kind = z == 1 ? DragonAncestry.EMBER : x < MID ? DragonAncestry.FROST : DragonAncestry.STORM;
                b.set(x, 4, z, ModTempleBlocks.brazier(kind));
            }
        }
        // Paper lanterns on lacquered posts either side of the path, at the stair and at the sanctum door.
        for (int x : new int[]{MID - 4, MID + 4}) {
            for (int z : new int[]{1, DEPTH - 3}) {
                b.fill(x, 1, z, x, 2, z, ModBlockFamilies.TEMPLE_LACQUER_PLANKS.fence());
                b.set(x, 3, z, ModTempleBlocks.PAPER_LANTERN);
            }
        }
        // Templars on watch.
        b.dragonborn(MID - 6, 1, DEPTH - 3, ModEntityTypes.DRAGONBORN_TEMPLAR);
        b.dragonborn(MID + 6, 1, DEPTH - 3, ModEntityTypes.DRAGONBORN_TEMPLAR);
        if (b.chance(0.5F)) {
            b.dragonborn(MID, 1, 1, ModEntityTypes.DRAGONBORN_TEMPLAR);
        }
    }
}
