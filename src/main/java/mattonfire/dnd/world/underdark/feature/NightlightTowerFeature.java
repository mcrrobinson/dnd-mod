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
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * A nightlight tower (design 9, 3.1): a 1×1 zurkhwood stem with rings of glowing Nightlight Cap every 4-5 blocks
 * (radius 2 on the lower half, 1 above), a domed cap of radius 2 on top, and 0-2 Glowing Spore Drips hanging under
 * the caps. It keeps 3 blocks clear under the ceiling, growing shorter under a low one.
 */
public class NightlightTowerFeature extends Feature<NightlightTowerConfig> {
    private static final int CLEARANCE = 3;
    private static final int MIN_HEIGHT = 4;

    public NightlightTowerFeature(Codec<NightlightTowerConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<NightlightTowerConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        NightlightTowerConfig config = context.getConfig();
        if (!FungusShapes.isSoil(world.getBlockState(origin.down()))) {
            return false;
        }
        int height = config.minHeight() + random.nextInt(Math.max(1, config.maxHeight() - config.minHeight() + 1));
        // The dome is two blocks over the stem's top.
        int room = FungusShapes.room(world, origin, height + 2 + CLEARANCE);
        height = Math.min(height, room - 2 - CLEARANCE);
        if (height < MIN_HEIGHT) {
            return false;
        }
        BlockState stem = UnderdarkBlocks.ZURKHWOOD_STEM.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y);
        BlockState cap = UnderdarkBlocks.NIGHTLIGHT_CAP.getDefaultState();
        for (int i = 0; i < height; i++) {
            FungusShapes.place(world, origin.up(i), stem);
        }
        List<BlockPos> caps = new ArrayList<>();
        // Rings up the stem.
        int y = 3 + random.nextInt(2);
        while (y <= height - 3) {
            int radius = y < height / 2 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((dx == 0 && dz == 0) || (Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
                        continue;
                    }
                    BlockPos pos = origin.add(dx, y, dz);
                    if (FungusShapes.place(world, pos, cap)) {
                        caps.add(pos);
                    }
                }
            }
            y += 4 + random.nextInt(2);
        }
        // The dome: a rim round the stem's top, a radius-2 layer over it and a radius-1 crown.
        BlockPos top = origin.up(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                boolean edge = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
                if (corner) {
                    continue;
                }
                if (edge && FungusShapes.place(world, top.add(dx, -1, dz), cap)) {
                    caps.add(top.add(dx, -1, dz));
                }
                if (FungusShapes.place(world, top.add(dx, 0, dz), cap)) {
                    caps.add(top.add(dx, 0, dz));
                }
                if (Math.abs(dx) + Math.abs(dz) <= 1 && FungusShapes.place(world, top.add(dx, 1, dz), cap)) {
                    caps.add(top.add(dx, 1, dz));
                }
            }
        }
        FungusShapes.dressCaps(world, caps);
        // Spore drips under a couple of caps.
        int drips = random.nextInt(3);
        for (int tries = 0; tries < 12 && drips > 0 && !caps.isEmpty(); tries++) {
            BlockPos under = caps.get(random.nextInt(caps.size())).down();
            if (world.getBlockState(under).isAir()
                    && FungusShapes.place(world, under, UnderdarkFlora.SPORE_DRIP.getDefaultState())) {
                drips--;
            }
        }
        return true;
    }
}
