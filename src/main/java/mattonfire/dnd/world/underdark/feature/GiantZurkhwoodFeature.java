package mattonfire.dnd.world.underdark.feature;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.classes.Registry.UnderdarkFlora;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * A giant zurkhwood (design 9, 3.1): a 1×1 stem 9-14 tall (60%) or a 2×2 one 15-24 tall (40%), leaning one block
 * every {@value #LEAN_EVERY}, under a flat cap of radius 4-7 with a one-block downturned rim and Zurkhwood Gills
 * underneath, and 0-3 small shelf caps on the stem. It keeps {@value #CLEARANCE} blocks clear under the cave ceiling,
 * growing shorter under a low one, and gives up below {@value #MIN_HEIGHT}.
 */
public class GiantZurkhwoodFeature extends Feature<DefaultFeatureConfig> {
    static final int LEAN_EVERY = 6;
    static final int CLEARANCE = 3;
    static final int MIN_HEIGHT = 5;

    public GiantZurkhwoodFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        // Not through a drow outpost or other settlement (they're built after the fungi).
        if (mattonfire.dnd.world.underdark.UnderdarkSite.inSettlement(world, origin)) {
            return false;
        }
        if (!FungusShapes.isSoil(world.getBlockState(origin.down()))) {
            return false;
        }
        boolean thick = random.nextFloat() < 0.4F && fits2x2(world, origin);
        int height = thick ? 15 + random.nextInt(10) : 9 + random.nextInt(6);
        int radius = 4 + random.nextInt(4);
        // The cap sits on the stem's top, with CLEARANCE free blocks above it.
        int room = FungusShapes.room(world, origin, height + 1 + CLEARANCE);
        height = Math.min(height, room - 1 - CLEARANCE);
        if (height < MIN_HEIGHT) {
            return false;
        }
        if (height < 12) {
            radius = Math.min(radius, 3 + height / 3);
        }
        Direction lean = Direction.Type.HORIZONTAL.random(random);
        BlockState stem = UnderdarkBlocks.ZURKHWOOD_STEM.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        int size = thick ? 2 : 1;
        BlockPos top = origin;
        for (int i = 0; i < height; i++) {
            top = origin.offset(lean, i / LEAN_EVERY).up(i);
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    FungusShapes.place(world, top.add(dx, 0, dz), stem);
                }
            }
        }
        List<BlockPos> caps = new ArrayList<>();
        BlockState cap = UnderdarkBlocks.ZURKHWOOD_CAP.getDefaultState();
        BlockState gills = UnderdarkFlora.ZURKHWOOD_GILLS.getDefaultState();
        double centre = (size - 1) / 2.0;
        BlockPos capCentre = top.up();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double dist = Math.hypot(dx - centre, dz - centre);
                if (dist > radius + 0.4) {
                    continue;
                }
                BlockPos capPos = capCentre.add(dx, 0, dz);
                if (FungusShapes.place(world, capPos, cap)) {
                    caps.add(capPos);
                }
                BlockPos below = capPos.down();
                if (dist > radius - 0.6) {
                    if (FungusShapes.place(world, below, cap)) {
                        caps.add(below.toImmutable());
                    }
                } else {
                    FungusShapes.place(world, below, gills);
                }
            }
        }
        int shelves = random.nextInt(4);
        for (int s = 0; s < shelves; s++) {
            int y = height * 35 / 100 + random.nextInt(Math.max(1, height * 40 / 100));
            BlockPos at = origin.offset(lean, y / LEAN_EVERY).up(y);
            Direction side = Direction.Type.HORIZONTAL.random(random);
            int shelfRadius = 1 + random.nextInt(2);
            BlockPos shelfCentre = at.offset(side, side.getDirection() == Direction.AxisDirection.POSITIVE
                    ? size + shelfRadius - 1 : shelfRadius);
            for (int dx = -shelfRadius; dx <= shelfRadius; dx++) {
                for (int dz = -shelfRadius; dz <= shelfRadius; dz++) {
                    if (dx * dx + dz * dz <= shelfRadius * shelfRadius + 1) {
                        BlockPos shelfPos = shelfCentre.add(dx, 0, dz);
                        if (FungusShapes.place(world, shelfPos, cap)) {
                            caps.add(shelfPos);
                        }
                    }
                }
            }
        }
        FungusShapes.dressCaps(world, caps);
        return true;
    }

    /** Whether the other three columns of a 2×2 stem have soil under them and room to start. */
    private static boolean fits2x2(StructureWorldAccess world, BlockPos origin) {
        for (BlockPos pos : List.of(origin.east(), origin.south(), origin.south().east())) {
            if (!FungusShapes.canReplace(world, pos) || !FungusShapes.isSoil(world.getBlockState(pos.down()))) {
                return false;
            }
        }
        return true;
    }
}
