package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.BrewingStandAccess;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(BrewingStandBlock.class)
public class BrewingStandBlockMixin {

    // Paladins cannot brew potions. Only the server opens the screen, so
    // cancelling there is enough.
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void dnd$blockPaladinBrewing(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient && player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.PALADIN) {
            player.sendMessage(Text.literal("Paladins cannot brew potions!").formatted(Formatting.RED), true);
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Inject(method = "onUse", at = @At("HEAD"))
    private void onUseMixin(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BrewingStandBlockEntity) {
                BrewingStandAccess access = (BrewingStandAccess) blockEntity;

                // Set the ID of the class
                if (player instanceof PlayerEntityExt) {
                    PlayerEntityExt playerEntity = (PlayerEntityExt) player;
                    access.setLastPlayer(playerEntity.getDndClass());
                }

            }
        }
    }
}
