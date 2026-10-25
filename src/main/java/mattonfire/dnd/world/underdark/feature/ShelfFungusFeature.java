package mattonfire.dnd.world.underdark.feature;

import com.mojang.serialization.Codec;
import mattonfire.dnd.classes.Blocks.underdark.RipplebarkBlock;
import mattonfire.dnd.classes.Registry.UnderdarkFlora;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * A cluster of 2-6 Ripplebark shelves (random ages) on a cave wall near the origin (design 9, 3.1). It looks for open
 * air beside a solid wall within 4 blocks across and 3 up or down.
 */
public class ShelfFungusFeature extends Feature<DefaultFeatureConfig> {
    public ShelfFungusFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        // Not on a drow outpost's (or other settlement's) walls: they're built before a neighbour's features run.
        if (mattonfire.dnd.world.underdark.UnderdarkSite.inSettlement(world, origin)) {
            return false;
        }
        int wanted = 2 + random.nextInt(5);
        int placed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int tries = 0; tries < 48 && placed < wanted; tries++) {
            pos.set(origin.getX() + random.nextInt(9) - 4, origin.getY() + random.nextInt(7) - 3,
                    origin.getZ() + random.nextInt(9) - 4);
            if (!world.getBlockState(pos).isAir()) {
                continue;
            }
            Direction wall = Direction.Type.HORIZONTAL.random(random);
            if (!RipplebarkBlock.canHang(world, pos, wall)) {
                continue;
            }
            world.setBlockState(pos, UnderdarkFlora.RIPPLEBARK.getDefaultState().with(RipplebarkBlock.FACING, wall)
                    .with(RipplebarkBlock.AGE, random.nextInt(RipplebarkBlock.MAX_AGE + 1)), 2);
            placed++;
        }
        return placed > 0;
    }
}
