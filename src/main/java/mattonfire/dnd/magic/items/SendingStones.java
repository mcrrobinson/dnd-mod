package mattonfire.dnd.magic.items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import mattonfire.dnd.magic.MagicData;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Sending Stones on the server (design 10, 8.2): the {@value #C2S_SEND_PATH} packet from {@code SendingStoneScreen},
 * delivery to whoever carries the twin (any dimension, main inventory or offhand only: not inside a Bag of Holding or
 * a chest), charges and the free reply, the long-rest recharge, and the {@code dndclasses:sending_stone_pair} loot
 * function.
 * <ul>
 * <li>Delivered: each receiver sees {@code [Sending] <sender>: <text>} in italic aqua with a chime, and may reply once
 * for free within {@value #FREE_REPLY_TICKS} ticks. The sender spends a charge (not for a free reply).</li>
 * <li>Nobody carries the twin: "The stone stays cold", no charge spent. The sender carries both: "The stones hum".</li>
 * <li>The text goes through the server's chat filter, like a sign. The log records who and how many words, never the
 * text: {@code [Sending] Dev -> DevGuest (12 words)}.</li>
 * </ul>
 */
public final class SendingStones {
    public static final int MAX_WORDS = 25;
    public static final int MAX_CHARS = 256;
    public static final int FREE_REPLY_TICKS = 1200;
    private static final String C2S_SEND_PATH = "sending_stone_send";
    public static final Identifier C2S_SEND = new Identifier(DnDClasses.MOD_ID, C2S_SEND_PATH);

    public static final LootFunctionType PAIR_FUNCTION = new LootFunctionType(new SendingStonePairLootFunction.Serializer());

    private SendingStones() {
    }

    public static void register() {
        MagicItems.register(MagicItemDef.of(ModItems.SENDING_STONE, MagicTier.UNCOMMON, MagicKind.WONDROUS, "stone"));
        Registry.register(Registries.LOOT_FUNCTION_TYPE, new Identifier(DnDClasses.MOD_ID, "sending_stone_pair"),
                PAIR_FUNCTION);
        ServerPlayNetworking.registerGlobalReceiver(C2S_SEND, (server, player, handler, buf, sender) -> {
            int slot = buf.readVarInt();
            String text = buf.readString(MAX_CHARS * 2);
            server.execute(() -> send(server, player, slot, text));
        });
        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind != RestKind.LONG)
                return;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(ModItems.SENDING_STONE))
                    SendingStoneItem.recharge(stack);
            }
        });
    }

    /** Whether this stone's next send is a free reply (a message reached it less than a minute ago). */
    public static boolean freeReply(ServerPlayerEntity player, ItemStack stone) {
        var nbt = stone.getNbt();
        return nbt != null && nbt.contains(SendingStoneItem.REPLY_UNTIL)
                && player.getWorld().getTime() <= nbt.getLong(SendingStoneItem.REPLY_UNTIL);
    }

    /** Words in a message: runs of non-whitespace. */
    public static int words(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private static ItemStack stoneAt(ServerPlayerEntity player, int slot) {
        PlayerInventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.size())
            return ItemStack.EMPTY;
        ItemStack stack = inventory.getStack(slot);
        return stack.isOf(ModItems.SENDING_STONE) ? stack : ItemStack.EMPTY;
    }

    private static void tell(ServerPlayerEntity player, String key, Formatting color) {
        player.sendMessage(Text.translatable(key).formatted(color), true);
    }

    static void send(MinecraftServer server, ServerPlayerEntity player, int slot, String raw) {
        ItemStack stone = stoneAt(player, slot);
        if (stone.isEmpty() || !player.isAlive() || player.isSpectator())
            return;
        if (!MagicItemUse.ready(player, stone))
            return;
        String text = raw.trim().replaceAll("\\s+", " ");
        int words = words(text);
        if (words == 0)
            return;
        if (words > MAX_WORDS || text.length() > MAX_CHARS) {
            tell(player, "item.dndclasses.sending_stone.too_long", Formatting.RED);
            return;
        }
        boolean free = freeReply(player, stone);
        if (!free && SendingStoneItem.charges(stone) <= 0) {
            tell(player, "item.dndclasses.sending_stone.empty", Formatting.GRAY);
            DnDClasses.LOGGER.info("[Sending] {}: no charges left", player.getEntityName());
            return;
        }
        player.getTextStream().filterText(text).thenAcceptAsync(message -> deliver(server, player, slot, message,
                words), server);
    }

    private static void deliver(MinecraftServer server, ServerPlayerEntity player, int slot, FilteredMessage message,
            int words) {
        ItemStack stone = stoneAt(player, slot);
        if (stone.isEmpty())
            return;
        UUID pair = SendingStoneItem.ensurePair(stone);
        List<ServerPlayerEntity> receivers = new ArrayList<>();
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other != player && !carriedTwins(other, pair).isEmpty())
                receivers.add(other);
        }
        if (receivers.isEmpty()) {
            boolean both = stone.getCount() > 1 || carriedTwins(player, pair).size() > 1;
            if (both) {
                tell(player, "item.dndclasses.sending_stone.hum", Formatting.AQUA);
                DnDClasses.LOGGER.info("[Sending] {}: holds both stones", player.getEntityName());
            } else {
                tell(player, "item.dndclasses.sending_stone.cold", Formatting.GRAY);
                DnDClasses.LOGGER.info("[Sending] {}: no one holds the twin", player.getEntityName());
            }
            return;
        }

        boolean free = freeReply(player, stone);
        if (free) {
            stone.getOrCreateNbt().remove(SendingStoneItem.REPLY_UNTIL);
        } else {
            if (SendingStoneItem.charges(stone) <= 0)
                return; // spent meanwhile
            if (!player.getAbilities().creativeMode)
                MagicData.setCharges(stone, SendingStoneItem.charges(stone) - 1);
        }
        for (ServerPlayerEntity receiver : receivers) {
            String shown = player.shouldFilterMessagesSentTo(receiver) ? message.getString() : message.raw();
            receiver.sendMessage(Text.translatable("item.dndclasses.sending_stone.received", player.getDisplayName(),
                    shown).formatted(Formatting.AQUA, Formatting.ITALIC), false);
            receiver.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 1.4F);
            long replyUntil = receiver.getWorld().getTime() + FREE_REPLY_TICKS;
            for (ItemStack twin : carriedTwins(receiver, pair))
                twin.getOrCreateNbt().putLong(SendingStoneItem.REPLY_UNTIL, replyUntil);
            DnDClasses.LOGGER.info("[Sending] {} -> {} ({} words{})", player.getEntityName(),
                    receiver.getEntityName(), words, free ? ", free reply" : "");
        }
        tell(player, "item.dndclasses.sending_stone.sent", Formatting.AQUA);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6F, 1.0F);
        if (!free) {
            DnDClasses.LOGGER.info("[Sending] {}: {} charges left", player.getEntityName(),
                    SendingStoneItem.charges(stone));
        }
    }

    /**
     * The stones of this pair {@code player} carries where they work: the main inventory (hotbar included) and the
     * offhand. Armour slots can't hold one; a Bag of Holding's contents are extradimensional and don't count.
     */
    public static List<ItemStack> carriedTwins(ServerPlayerEntity player, UUID pair) {
        List<ItemStack> found = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.main)
            if (stack.isOf(ModItems.SENDING_STONE) && pair.equals(SendingStoneItem.pair(stack)))
                found.add(stack);
        for (ItemStack stack : inventory.offHand)
            if (stack.isOf(ModItems.SENDING_STONE) && pair.equals(SendingStoneItem.pair(stack)))
                found.add(stack);
        return found;
    }
}
