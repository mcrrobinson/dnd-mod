package mattonfire.dnd.magic.items;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Deck of Many Things (Legendary wondrous item, no attunement; design 10, 8.5). Its stack NBT holds the deck
 * ({@code Cards}: the 13-card deck 75% of the time, the full 22 otherwise, dealt the first time it's held),
 * {@code Declared} (1-4), {@code Drawn} and {@code FirstDrawAt}.
 * <ul>
 * <li>Sneak + right-click before the first draw: declare how many cards you'll draw (cycles 1 to 4).</li>
 * <li>Right-click: draw ({@link DeckCards}). A first draw with nothing declared declares 1. Once the declared cards
 * are drawn, the deck vanishes. Cards left undrawn {@value #AUTO_DRAW_TICKS} ticks after the first draw fly out
 * and draw themselves, one a second.</li>
 * </ul>
 * {@code /dndmagic deck <player> next <card>} rigs the next card ({@code Next}).
 */
public class DeckOfManyThingsItem extends Item {
    public static final int MAX_DECLARED = 4;
    public static final int AUTO_DRAW_TICKS = 1200;
    public static final int DRAW_COOLDOWN = 20;
    public static final float FULL_DECK_CHANCE = 0.25F;

    public DeckOfManyThingsItem(Settings settings) {
        super(settings);
    }

    public static boolean isDeck(ItemStack stack) {
        return stack.getItem() instanceof DeckOfManyThingsItem;
    }

    /** Deals the deck the first time: 13 cards (75%) or all 22. */
    public static void deal(ItemStack deck, Random random) {
        NbtCompound nbt = deck.getOrCreateNbt();
        if (nbt.contains("Cards"))
            return;
        int size = random.nextFloat() < FULL_DECK_CHANCE ? DeckCards.Card.values().length : DeckCards.Card.SMALL_DECK;
        int[] cards = new int[size];
        for (int i = 0; i < size; i++)
            cards[i] = i;
        nbt.putIntArray("Cards", cards);
    }

    /** Rigs the next card drawn from this deck (admin). */
    public static void rig(ItemStack deck, DeckCards.Card card) {
        deck.getOrCreateNbt().putString("Next", card.id());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack deck = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(deck, true);
        if (!MagicItemUse.ready(player, deck))
            return TypedActionResult.fail(deck);
        deal(deck, player.getRandom());
        NbtCompound nbt = deck.getOrCreateNbt();
        if (player.isSneaking()) {
            if (nbt.getInt("Drawn") > 0) {
                player.sendMessage(Text.translatable("item.dndclasses.deck_of_many_things.declared_already",
                        nbt.getInt("Declared")).formatted(Formatting.GRAY), true);
                return TypedActionResult.fail(deck);
            }
            int declared = nbt.getInt("Declared") % MAX_DECLARED + 1;
            nbt.putInt("Declared", declared);
            player.sendMessage(Text.translatable("item.dndclasses.deck_of_many_things.declare", declared)
                    .formatted(Formatting.LIGHT_PURPLE), true);
            world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8F,
                    1.4F);
            return TypedActionResult.success(deck, false);
        }
        player.getItemCooldownManager().set(this, DRAW_COOLDOWN);
        draw(server, deck, false);
        return TypedActionResult.success(deck, false);
    }

    /** Draws one card from the deck in the player's inventory, and makes the deck vanish when its draws are done. */
    public static void draw(ServerPlayerEntity player, ItemStack deck, boolean flewOut) {
        NbtCompound nbt = deck.getOrCreateNbt();
        if (nbt.getInt("Declared") <= 0)
            nbt.putInt("Declared", 1);
        int[] cards = nbt.getIntArray("Cards");
        if (cards.length == 0) {
            vanish(player, deck);
            return;
        }
        DeckCards.Card card = null;
        if (nbt.contains("Next")) {
            card = DeckCards.Card.byId(nbt.getString("Next"));
            nbt.remove("Next");
        }
        if (card == null)
            card = DeckCards.Card.values()[cards[player.getRandom().nextInt(cards.length)]];
        if (card.leavesDeck()) {
            List<Integer> left = new ArrayList<>();
            for (int c : cards)
                if (c != card.ordinal())
                    left.add(c);
            nbt.putIntArray("Cards", left);
        }
        int drawn = nbt.getInt("Drawn") + 1;
        nbt.putInt("Drawn", drawn);
        if (drawn == 1)
            nbt.putLong("FirstDrawAt", player.getWorld().getTime());
        DnDClasses.LOGGER.info("[Deck] {} draws {} ({}/{}){}", player.getEntityName(), card.title().getString(), drawn,
                nbt.getInt("Declared"), flewOut ? " (flew out)" : "");
        show(player, card);
        DeckCards.Outcome outcome = DeckCards.apply(player, card);
        if (outcome == DeckCards.Outcome.EXTRA_DRAW)
            nbt.putInt("Declared", nbt.getInt("Declared") + 1);
        if (outcome == DeckCards.Outcome.DECK_ENDS || drawn >= nbt.getInt("Declared"))
            vanish(player, deck);
    }

    private static void show(ServerPlayerEntity player, DeckCards.Card card) {
        Formatting color = card.bad ? Formatting.DARK_RED : Formatting.GOLD;
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(card.line().copy().formatted(Formatting.GRAY)));
        player.networkHandler.sendPacket(new TitleS2CPacket(card.title().copy().formatted(color)));
        player.sendMessage(Text.translatable("deck.dndclasses.drawn", card.title().copy().formatted(color), card.line())
                .formatted(Formatting.GRAY), false);
        World world = player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (card.bad) {
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS,
                    1.0F, 0.5F);
        } else {
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS,
                    1.0F, 1.2F);
        }
    }

    private static void vanish(ServerPlayerEntity player, ItemStack deck) {
        deck.decrement(1);
        ServerWorld world = player.getWorld();
        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getBodyY(0.6), player.getZ(), 30, 0.5, 0.5,
                0.5, 0.05);
        player.sendMessage(Text.translatable("item.dndclasses.deck_of_many_things.vanish")
                .formatted(Formatting.LIGHT_PURPLE), false);
        DnDClasses.LOGGER.info("[Deck] {}'s deck vanishes", player.getEntityName());
    }

    /** Deals the deck once it's held, and draws the cards left undrawn too long, one a second. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player))
            return;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("Cards")) {
            if (MagicData.isIdentified(stack))
                deal(stack, player.getRandom());
            return;
        }
        int drawn = nbt.getInt("Drawn");
        if (drawn <= 0 || drawn >= nbt.getInt("Declared") || world.getTime() % 20 != 0)
            return;
        if (world.getTime() - nbt.getLong("FirstDrawAt") >= AUTO_DRAW_TICKS) {
            player.sendMessage(Text.translatable("item.dndclasses.deck_of_many_things.fly")
                    .formatted(Formatting.LIGHT_PURPLE), true);
            draw(player, stack, true);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        NbtCompound nbt = stack.getNbt();
        int size = nbt != null ? nbt.getIntArray("Cards").length : 0;
        if (size > 0)
            tooltip.add(Text.translatable("item.dndclasses.deck_of_many_things.size", size).formatted(Formatting.GRAY));
        int declared = nbt != null ? nbt.getInt("Declared") : 0;
        int drawn = nbt != null ? nbt.getInt("Drawn") : 0;
        if (declared > 0) {
            tooltip.add(Text.translatable("item.dndclasses.deck_of_many_things.will_draw", declared, drawn)
                    .formatted(Formatting.LIGHT_PURPLE));
        }
        tooltip.add(Text.translatable("item.dndclasses.deck_of_many_things.tooltip").formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return MagicData.isIdentified(stack);
    }
}
