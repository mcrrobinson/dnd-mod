package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Entities.FastBrewingStandBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(BrewingStandBlock.class)
public class BrewingStandBlockMixin {

    @Inject(method = "onUse", at = @At("HEAD"))
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FastBrewingStandBlockEntity) {
                if (player instanceof PlayerEntityExt) {
                    PlayerEntityExt playerExt = (PlayerEntityExt) player;
                    if (playerExt.getDndClass() != DndCharacter.PALADIN) {
                        return ActionResult.FAIL;
                    }
                }

                player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
            }
        }
        return ActionResult.SUCCESS;
    }
}
