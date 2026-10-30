package mattonfire.dnd.magic.items;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Portable Hole (Rare wondrous item, design 10 section 8.4). A folded black cloth:
 * <ul>
 * <li>Right-click: a {@value #SIZE}-slot (9x6) pocket dimension, kept in {@link PortableHoleStorage} under the
 * cloth's uuid, not in the item. A Portable Hole won't fold into itself; a Bag of Holding inside opens an
 * {@link AstralRift}.</li>
 * <li>Sneak + right-click a block face within reach: lays the hole as a passage 1 wide and 2 high (1x1 in a floor
 * or ceiling), up to {@value PortableHolePassages#MAX_DEPTH} deep ({@link PortableHolePassages}), for
 * {@value #PASSAGE_TICKS} ticks. Using the cloth again, or right-clicking the passage, folds it back up.</li>
 * </ul>
 */
public class PortableHoleItem extends Item {
    public static final int SIZE = 54;
    public static final int PASSAGE_TICKS = 300;
    public static final int COOLDOWN_TICKS = 40;
    private static final String LAID_KEY = "laid";

    public PortableHoleItem(Settings settings) {
        super(settings);
    }

    public static boolean isHole(ItemStack stack) {
        return stack.getItem() instanceof PortableHoleItem;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack cloth = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(cloth, true);
        if (!MagicItemUse.ready(player, cloth))
            return TypedActionResult.fail(cloth);
        UUID id = MagicData.ensureUuid(cloth);
        if (PortableHolePassages.closeHole(server.getServer(), id, player.getEntityName() + " used the cloth")) {
            forgetLaid(cloth);
            player.sendMessage(Text.translatable("item.dndclasses.portable_hole.folded").formatted(Formatting.GRAY),
                    true);
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            return TypedActionResult.success(cloth, false);
        }
        int invSlot = hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT;
        SimpleInventory contents = PortableHoleStorage.get(server.getServer()).inventory(id);
        server.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new HoleScreenHandler(syncId, playerInventory, cloth, invSlot, contents,
                        id),
                cloth.getName()));
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F,
                0.5F);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.PLAYERS, 0.1F, 2.0F);
        return TypedActionResult.success(cloth, false);
    }

    /** Sneak + right-click a block: lay the hole on it. A plain right-click on a block opens the hole as usual. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking())
            return ActionResult.PASS;
        if (context.getWorld().isClient)
            return ActionResult.SUCCESS;
        ItemStack cloth = context.getStack();
        ServerWorld world = (ServerWorld) context.getWorld();
        if (!MagicItemUse.ready(player, cloth))
            return ActionResult.FAIL;
        UUID id = MagicData.ensureUuid(cloth);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        if (PortableHolePassages.closeHole(world.getServer(), id, player.getEntityName() + " used the cloth")) {
            forgetLaid(cloth);
            player.sendMessage(Text.translatable("item.dndclasses.portable_hole.folded").formatted(Formatting.GRAY),
                    true);
            return ActionResult.SUCCESS;
        }
        Direction side = context.getSide();
        BlockPos hit = context.getBlockPos();
        BlockPos base = hit;
        if (side.getAxis() != Direction.Axis.Y) {
            // At the player's height: the clicked block or the one below it
            int feet = player.getBlockPos().getY();
            base = new BlockPos(hit.getX(), MathHelper.clamp(feet, hit.getY() - 1, hit.getY()), hit.getZ());
        }
        PortableHolePassages.Result result = PortableHolePassages.get(world).open(world, player, id, base, side);
        if (result.passage() == null) {
            player.sendMessage(Text.translatable("item.dndclasses.portable_hole.refuse").formatted(Formatting.GRAY),
                    true);
            DnDClasses.LOGGER.info("[Magic] {} couldn't lay a Portable Hole at {} ({})", player.getEntityName(),
                    base.toShortString(), result.refusal());
            return ActionResult.FAIL;
        }
        NbtCompound laid = new NbtCompound();
        laid.put("Pos", NbtHelper.fromBlockPos(base));
        laid.putString("Dim", world.getRegistryKey().getValue().toString());
        MagicData.getOrCreate(cloth).put(LAID_KEY, laid);
        world.playSound(null, base, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.BLOCKS, 1.0F, 0.5F);
        world.playSound(null, base, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 0.15F, 2.0F);
        player.sendMessage(Text.translatable("item.dndclasses.portable_hole.laid",
                result.passage().depth).formatted(Formatting.DARK_AQUA), true);
        return ActionResult.SUCCESS;
    }

    private static void forgetLaid(ItemStack cloth) {
        NbtCompound magic = MagicData.get(cloth);
        if (magic != null)
            magic.remove(LAID_KEY);
    }

    /** Forgets a passage that closed on its own, so the tooltip stays right. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || world.getTime() % 20 != 0 || world.getServer() == null)
            return;
        NbtCompound magic = MagicData.get(stack);
        if (magic == null || !magic.contains(LAID_KEY))
            return;
        UUID id = MagicData.uuid(stack);
        if (id == null || !PortableHolePassages.isLaid(world.getServer(), id))
            magic.remove(LAID_KEY);
    }

    /** The 9x6 chest screen over the hole: refuses Holes, locks the open cloth, and a Bag inside tears a rift. */
    static class HoleScreenHandler extends GenericContainerScreenHandler {
        private final ItemStack cloth;
        private final int clothInvSlot;
        private final UUID hole;

        HoleScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack cloth, int clothInvSlot,
                SimpleInventory contents, UUID hole) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, contents, 6);
            this.cloth = cloth;
            this.clothInvSlot = clothInvSlot;
            this.hole = hole;
            for (int i = 0; i < slots.size(); i++) {
                Slot old = slots.get(i);
                Slot replacement;
                if (old.inventory == contents) {
                    replacement = new Slot(contents, old.getIndex(), old.x, old.y) {
                        @Override
                        public boolean canInsert(ItemStack stack) {
                            return !isHole(stack);
                        }
                    };
                } else if (old.inventory == playerInventory && old.getIndex() == clothInvSlot) {
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
            if (actionType == SlotActionType.SWAP) {
                if (button == clothInvSlot)
                    return;
                if (slotIndex >= 0 && slotIndex < SIZE && isHole(player.getInventory().getStack(button))) {
                    refuse(player);
                    return;
                }
            }
            if (slotIndex >= 0 && slotIndex < SIZE && actionType == SlotActionType.PICKUP && isHole(getCursorStack())) {
                refuse(player);
                return;
            }
            super.onSlotClick(slotIndex, button, actionType, player);
            if (!player.getWorld().isClient && containsBag())
                AstralRift.schedule(player, AstralRift.Cause.BAG_IN_HOLE, hole);
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            if (slot >= SIZE && slot < slots.size() && isHole(slots.get(slot).getStack())) {
                refuse(player);
                return ItemStack.EMPTY;
            }
            return super.quickMove(player, slot);
        }

        private boolean containsBag() {
            for (int i = 0; i < SIZE; i++)
                if (BagOfHoldingItem.isBag(getInventory().getStack(i)))
                    return true;
            return false;
        }

        private static void refuse(PlayerEntity player) {
            if (!player.getWorld().isClient)
                player.sendMessage(Text.translatable("item.dndclasses.portable_hole.nested")
                        .formatted(Formatting.DARK_PURPLE), true);
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.isAlive() && player.getInventory().getStack(clothInvSlot) == cloth;
        }

        @Override
        public void onClosed(PlayerEntity player) {
            super.onClosed(player);
            if (player instanceof ServerPlayerEntity server)
                DnDClasses.LOGGER.info("[Magic] {} closed a Portable Hole ({} stacks inside)", player.getEntityName(),
                        PortableHoleStorage.get(server.getServer()).used(hole));
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.portable_hole.tooltip", SIZE, PASSAGE_TICKS / 20)
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.dndclasses.portable_hole.warning").formatted(Formatting.DARK_RED,
                Formatting.ITALIC));
        NbtCompound magic = MagicData.get(stack);
        if (magic != null && magic.contains(LAID_KEY)) {
            BlockPos pos = NbtHelper.toBlockPos(magic.getCompound(LAID_KEY).getCompound("Pos"));
            tooltip.add(Text.translatable("item.dndclasses.portable_hole.laid_at", pos.getX() + " " + pos.getY() + " "
                    + pos.getZ()).formatted(Formatting.DARK_AQUA));
        }
    }

    /** Keeps Holes out of shulker boxes and bundles. */
    @Override
    public boolean canBeNested() {
        return false;
    }
}
