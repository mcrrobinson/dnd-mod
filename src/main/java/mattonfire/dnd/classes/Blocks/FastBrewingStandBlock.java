package mattonfire.dnd.classes.Blocks;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Entities.FastBrewingStandBlockEntity;
import mattonfire.dnd.classes.Registry.ModEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FastBrewingStandBlock extends BrewingStandBlock {
    public FastBrewingStandBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FastBrewingStandBlockEntity(pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FastBrewingStandBlockEntity) {
                ItemScatterer.spawn(world, pos, (FastBrewingStandBlockEntity) blockEntity);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FastBrewingStandBlockEntity) {
                ((FastBrewingStandBlockEntity) blockEntity).setCustomName(itemStack.getName());
            }
        }

    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FastBrewingStandBlockEntity) {
                if (player instanceof PlayerEntityExt) {
                    PlayerEntityExt playerExt = (PlayerEntityExt) player;
                    if (playerExt.getDndClass() == DndCharacter.PALADIN) {
                        player.sendMessage(Text.literal("Paladins cannot brew potions!").formatted(Formatting.RED),
                                true);
                        return ActionResult.FAIL;
                    }
                    if (playerExt.getDndClass() != DndCharacter.ALCHEMIST) {
                        player.sendMessage(Text.translatable("message.dndclasses.fast_brewing_stand.alchemist_only")
                                .formatted(Formatting.RED), true);
                        return ActionResult.FAIL;
                    }
                }

                ((FastBrewingStandBlockEntity) blockEntity).setBrewer(player.getUuid());
                player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> type) {
        return checkType(type, ModEntities.FAST_BREWING_STAND, FastBrewingStandBlockEntity::tick);
    }
}