package mattonfire.dnd.magic.items;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * A Sending Stone (Uncommon wondrous item; design 10, 8.2; docs/items/magic-items.md). They come as a stack of two
 * that share an NBT {@value #PAIR} id: split the stack and give one away. Right-click opens a one-line message
 * screen ({@code SendingStoneScreen}, at most {@value SendingStones#MAX_WORDS} words) that goes to every other
 * online player carrying the twin ({@link SendingStones}). {@value #MAX_CHARGES} charges, all back at dawn and on a
 * long rest; the receiver may reply once for free within a minute ({@value #REPLY_UNTIL}).
 */
public class SendingStoneItem extends Item {
    public static final int MAX_CHARGES = 3;
    /** The pair id, a UUID shared by the two stones of a pair. */
    public static final String PAIR = "Pair";
    /** World time until which this stone's next send is a free reply. */
    public static final String REPLY_UNTIL = "ReplyUntil";

    /** Set by the client initializer: opens the message screen for this inventory slot of the local player. */
    public static BiConsumer<PlayerEntity, Integer> clientOpener = (player, slot) -> {
    };

    public SendingStoneItem(Settings settings) {
        super(settings);
    }

    public static @Nullable UUID pair(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.containsUuid(PAIR) ? nbt.getUuid(PAIR) : null;
    }

    /** The stone's pair id, given a new one if it has none (a stone from the creative tab or a trade). */
    public static UUID ensurePair(ItemStack stack) {
        UUID pair = pair(stack);
        if (pair == null) {
            pair = UUID.randomUUID();
            stack.getOrCreateNbt().putUuid(PAIR, pair);
        }
        return pair;
    }

    public static void setPair(ItemStack stack, UUID pair) {
        stack.getOrCreateNbt().putUuid(PAIR, pair);
    }

    public static int charges(ItemStack stack) {
        return MathHelper.clamp(MagicData.charges(stack, MAX_CHARGES), 0, MAX_CHARGES);
    }

    /** The inventory slot of the stone in {@code hand}: the selected hotbar slot, or the offhand's. */
    public static int slotOf(PlayerEntity player, Hand hand) {
        return hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            ensurePair(stack);
            if (!MagicItemUse.ready(player, stack))
                return TypedActionResult.fail(stack);
            return TypedActionResult.success(stack, false);
        }
        // The server says why if it's asleep; the client only opens the screen for a stone that's awake
        if (MagicData.isIdentified(stack))
            clientOpener.accept(player, slotOf(player, hand));
        return TypedActionResult.success(stack, true);
    }

    /** Gives a stone its pair id as soon as it's carried, while a bought or crafted pair is still one stack. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient)
            return;
        if (pair(stack) == null)
            ensurePair(stack);
        if (world.getTime() % 20 == 0 && MagicData.newDawn(stack, world))
            recharge(stack);
    }

    /** All charges back (dawn, long rest). */
    public static void recharge(ItemStack stack) {
        if (charges(stack) < MAX_CHARGES)
            MagicData.setCharges(stack, MAX_CHARGES);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return MagicData.isIdentified(stack) && charges(stack) < MAX_CHARGES;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0F * charges(stack) / MAX_CHARGES);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x7FDFFF;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.sending_stone.charges", charges(stack), MAX_CHARGES)
                .formatted(Formatting.AQUA));
        UUID pair = pair(stack);
        if (pair != null) {
            tooltip.add(Text.translatable("item.dndclasses.sending_stone.mark",
                    pair.toString().substring(0, 4).toUpperCase()).formatted(Formatting.DARK_AQUA));
        }
        // One tooltip line per newline in the translation, so long descriptions don't run off the screen
        for (String line : Text.translatable("item.dndclasses.sending_stone.tooltip").getString().split("\n"))
            tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
    }
}
