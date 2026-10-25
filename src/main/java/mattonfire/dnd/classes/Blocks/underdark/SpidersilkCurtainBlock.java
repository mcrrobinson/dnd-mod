package mattonfire.dnd.classes.Blocks.underdark;

import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Spidersilk Curtain: pale silver silk hung like a pane between walls (it joins up like glass panes). You can walk
 * through it, but it clings like a cobweb: inside it you move at about 30% of your speed (a cobweb leaves 10-15%).
 * Spiders, and whatever else is in {@link UnderdarkBlocks#WEB_WALKERS} (the drow, once they exist), pass freely.
 */
public class SpidersilkCurtainBlock extends PaneBlock {
    /**
     * The cobweb mechanism ({@link Entity#slowMovement}), which scales one tick's movement and then stops the
     * entity. 0.66 leaves a walking player about 30% of their open-floor speed (cobweb: 0.25).
     */
    public static final Vec3d SLOW = new Vec3d(0.66, 0.75, 0.66);

    public SpidersilkCurtainBlock(Settings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!entity.getType().isIn(UnderdarkBlocks.WEB_WALKERS)) {
            entity.slowMovement(state, SLOW);
        }
    }
}
