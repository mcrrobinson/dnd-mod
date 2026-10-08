package mattonfire.dnd.classes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.entity.MimicEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DungeonFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Monster rooms (dungeons): now and then one of the chests is a mimic, which holds the dungeon
 * loot the chest would have had.
 */
@Mixin(DungeonFeature.class)
public abstract class DungeonFeatureMixin {
    @Unique
    private static final float MIMIC_CHANCE = 0.2F;

    @WrapOperation(method = "generate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/entity/LootableContainerBlockEntity;setLootTable(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Identifier;)V"))
    private void dnd$maybeMimic(BlockView view, Random random, BlockPos pos, Identifier lootTable,
            Operation<Void> original) {
        if (view instanceof StructureWorldAccess world && random.nextFloat() < MIMIC_CHANCE) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.CHEST)) {
                MimicEntity mimic = MimicEntity.disguised(world.toServerWorld(), pos, state.get(ChestBlock.FACING), lootTable);
                if (mimic != null) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    world.spawnEntity(mimic);
                    return;
                }
            }
        }
        original.call(view, random, pos, lootTable);
    }
}
