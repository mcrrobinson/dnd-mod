package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tome of Clear Thought (Very Rare consumable): read it near an Attunement Table (within
 * {@value #TABLE_RANGE} blocks) to clear your current class's subclass, refunding its nodes and their ranks
 * ({@link Progression#clearSubclass}). Used up only when there was a subclass to clear.
 */
public class TomeOfClearThoughtItem extends Item {
    public static final int TABLE_RANGE = 4;

    public TomeOfClearThoughtItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(stack, true);
        if (!MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        if (!nearTable(server)) {
            player.sendMessage(Text.translatable("item.dndclasses.tome_of_clear_thought.table")
                    .formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }
        if (Progression.classOf(server) == DndCharacter.NONE) {
            player.sendMessage(Text.translatable("item.dndclasses.tome_of_clear_thought.none")
                    .formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        int refunded = Progression.clearSubclass(server);
        if (refunded < 0) {
            player.sendMessage(Text.translatable("item.dndclasses.tome_of_clear_thought.none")
                    .formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 60,
                0.6, 0.8, 0.6, 0.6);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F,
                0.8F);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS,
                1.0F, 0.7F);
        DnDClasses.LOGGER.info("[Magic] {} read a Tome of Clear Thought: subclass cleared, {} points refunded",
                player.getEntityName(), refunded);
        if (!player.getAbilities().creativeMode)
            stack.decrement(1);
        return TypedActionResult.success(stack, false);
    }

    /** At the table they last opened, or any Attunement Table within {@value #TABLE_RANGE} blocks. */
    private static boolean nearTable(ServerPlayerEntity player) {
        if (Progression.atAttunementTable(player))
            return true;
        BlockPos center = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-TABLE_RANGE, -TABLE_RANGE, -TABLE_RANGE),
                center.add(TABLE_RANGE, TABLE_RANGE, TABLE_RANGE))) {
            if (player.getWorld().getBlockState(pos).isOf(ModBlocks.ATTUNEMENT_TABLE))
                return true;
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (MagicData.isIdentified(stack))
            tooltip.add(Text.translatable("item.dndclasses.tome_of_clear_thought.tooltip")
                    .formatted(Formatting.GRAY));
    }
}
