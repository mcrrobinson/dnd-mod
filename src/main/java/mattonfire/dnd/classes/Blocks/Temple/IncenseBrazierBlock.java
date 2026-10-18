package mattonfire.dnd.classes.Blocks.Temple;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Incense Brazier: a dark bronze bowl on four legs with embers glowing in its ancestry's colour, giving off
 * smoke of that colour (Ember red, Frost pale blue, Storm teal). The Dragon Temple's light in front of the
 * altars and round the courtyard, in place of campfires. Purely decorative: it doesn't burn or cook.
 */
public class IncenseBrazierBlock extends Block {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(2, 5, 2, 14, 11, 14),
            Block.createCuboidShape(3, 0, 3, 5, 5, 5), Block.createCuboidShape(11, 0, 3, 13, 5, 5),
            Block.createCuboidShape(3, 0, 11, 5, 5, 13), Block.createCuboidShape(11, 0, 11, 13, 5, 13));

    /** The smoke's colour, 0xRRGGBB. */
    private final int smoke;

    public IncenseBrazierBlock(int smoke, Settings settings) {
        super(settings);
        this.smoke = smoke;
    }

    public int smokeColor() {
        return this.smoke;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 11 / 16.0;
        double z = pos.getZ() + 0.5;
        double r = ((this.smoke >> 16) & 0xFF) / 255.0;
        double g = ((this.smoke >> 8) & 0xFF) / 255.0;
        double b = (this.smoke & 0xFF) / 255.0;
        // Coloured incense swirls (the entity-effect particle takes its colour as its "velocity").
        for (int i = 0; i < 3; i++) {
            world.addParticle(ParticleTypes.ENTITY_EFFECT, x + (random.nextDouble() - 0.5) * 0.5,
                    y + random.nextDouble() * 0.3, z + (random.nextDouble() - 0.5) * 0.5, r, g, b);
        }
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y,
                    z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(12) == 0) {
            world.playSound(x, y, z, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS,
                    0.4F + random.nextFloat() * 0.3F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }
}
