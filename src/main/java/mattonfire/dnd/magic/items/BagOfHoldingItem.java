package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Bag of Holding (Uncommon wondrous item): right-click opens a private {@value #SIZE}-slot inventory kept in
 * the bag's own NBT ({@value #ITEMS_KEY}), so it travels with the item. A Bag can't go inside a Bag ("That
 * would tear a hole in space."), and the open bag's own slot is locked so it can't be moved while open.
 * The screen is the vanilla 9x3 chest screen, so the client needs nothing extra.
 */
public class BagOfHoldingItem extends Item {
    public static final int SIZE = 27;
    public static final String ITEMS_KEY = "BagItems";

    public BagOfHoldingItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack bag = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(bag, true);
        if (!MagicItemUse.ready(player, bag))
            return TypedActionResult.fail(bag);
        int invSlot = hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT;
        server.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new BagScreenHandler(syncId, playerInventory, bag, invSlot),
                bag.getName()));
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS,
                0.8F, 0.7F);
        return TypedActionResult.success(bag, false);
    }

    public static boolean isBag(ItemStack stack) {
        return stack.getItem() instanceof BagOfHoldingItem;
    }

    /** The bag's contents, read from its NBT. */
    public static DefaultedList<ItemStack> load(ItemStack bag) {
        DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        NbtCompound nbt = bag.getNbt();
        if (nbt != null && nbt.contains(ITEMS_KEY, NbtElement.COMPOUND_TYPE))
            Inventories.readNbt(nbt.getCompound(ITEMS_KEY), items);
        return items;
    }

    /** Writes the contents back into the bag's NBT (removing the key when it's empty). */
    public static void save(ItemStack bag, Inventory inventory) {
        DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        boolean empty = true;
        for (int i = 0; i < SIZE; i++) {
            items.set(i, inventory.getStack(i));
            empty &= inventory.getStack(i).isEmpty();
        }
        if (empty) {
            bag.removeSubNbt(ITEMS_KEY);
        } else {
            bag.setSubNbt(ITEMS_KEY, Inventories.writeNbt(new NbtCompound(), items, true));
        }
    }

    /** The vanilla 9x3 chest handler over the bag's contents, refusing Bags and locking the open bag. */
    static class BagScreenHandler extends GenericContainerScreenHandler {
        private final ItemStack bag;
        private final int bagInvSlot;

        BagScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack bag, int bagInvSlot) {
            this(syncId, playerInventory, bag, bagInvSlot, new SimpleInventory(SIZE));
        }

        private BagScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack bag, int bagInvSlot,
                SimpleInventory contents) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, contents, 3);
            this.bag = bag;
            this.bagInvSlot = bagInvSlot;
            DefaultedList<ItemStack> items = load(bag);
            for (int i = 0; i < SIZE; i++)
                contents.setStack(i, items.get(i));
            contents.addListener(inv -> save(bag, inv));
            for (int i = 0; i < slots.size(); i++) {
                Slot old = slots.get(i);
                Slot replacement;
                if (old.inventory == contents) {
                    replacement = new Slot(contents, old.getIndex(), old.x, old.y) {
                        @Override
                        public boolean canInsert(ItemStack stack) {
                            return !isBag(stack);
                        }
                    };
                } else if (old.inventory == playerInventory && old.getIndex() == bagInvSlot) {
                    replacement = new Slot(playerInventory, old.getIndex(), old.x, old.y) {
                        @Override
                        public boolean canTakeItems(PlayerEntity player) {
                            return false;
                        }

                        @Override
                        public boolean canInsert(ItemStack stack) {
                            return false;
                        }
                    };
                } else {
                    continue;
                }
                replacement.id = old.id;
                slots.set(i, replacement);
            }
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
            // Number keys and F swap through the player inventory directly, past the slot checks.
            if (actionType == SlotActionType.SWAP) {
                if (button == bagInvSlot)
                    return;
                if (slotIndex >= 0 && slotIndex < SIZE
                        && isBag(player.getInventory().getStack(button))) {
                    refuse(player);
                    return;
                }
            }
            if (slotIndex >= 0 && slotIndex < slots.size() && actionType == SlotActionType.PICKUP
                    && slotIndex < SIZE && isBag(getCursorStack())) {
                refuse(player);
                return;
            }
            super.onSlotClick(slotIndex, button, actionType, player);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            if (slot >= SIZE && slot < slots.size() && isBag(slots.get(slot).getStack())) {
                refuse(player);
                return ItemStack.EMPTY;
            }
            return super.quickMove(player, slot);
        }

        private static void refuse(PlayerEntity player) {
            if (!player.getWorld().isClient)
                player.sendMessage(Text.translatable("item.dndclasses.bag_of_holding.refuse")
                        .formatted(Formatting.DARK_PURPLE), true);
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.isAlive() && player.getInventory().getStack(bagInvSlot) == bag;
        }

        @Override
        public void onClosed(PlayerEntity player) {
            super.onClosed(player);
            save(bag, getInventory());
            DnDClasses.LOGGER.info("[Magic] {} closed a Bag of Holding ({} stacks inside)",
                    player.getEntityName(), load(bag).stream().filter(s -> !s.isEmpty()).count());
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        long used = load(stack).stream().filter(s -> !s.isEmpty()).count();
        tooltip.add(Text.translatable("item.dndclasses.bag_of_holding.tooltip", used, SIZE)
                .formatted(Formatting.GRAY));
    }

    /** Keeps Bags out of shulker boxes and bundles too. */
    @Override
    public boolean canBeNested() {
        return false;
    }
}
