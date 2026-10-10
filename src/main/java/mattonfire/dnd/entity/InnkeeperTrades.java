package mattonfire.dnd.entity;

import mattonfire.dnd.tavern.Tavern;
import mattonfire.dnd.tavern.TavernRooms;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What an innkeeper sells (food, drink and a room for the night) and buys (the village's produce).
 * Restocks daily.
 */
final class InnkeeperTrades {
    private static final int MAX_USES = 12;
    private static final int MERCHANT_XP = 2;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private InnkeeperTrades() {
    }

    static TradeOfferList create() {
        TradeOfferList offers = new TradeOfferList();
        // Food and drink.
        offers.add(sell(1, Tavern.ALE, 2));
        offers.add(sell(1, Items.BREAD, 4));
        offers.add(sell(1, Items.COOKED_CHICKEN, 3));
        offers.add(sell(1, Items.PUMPKIN_PIE, 2));
        offers.add(sell(1, Items.COOKIE, 8));
        offers.add(sell(1, Items.HONEY_BOTTLE, 2));
        offers.add(sell(2, Items.RABBIT_STEW, 1));
        offers.add(sell(3, Items.CAKE, 1));
        // Produce for the kitchen.
        offers.add(buy(Items.WHEAT, 20));
        offers.add(buy(Items.POTATO, 24));
        offers.add(buy(Items.CARROT, 22));
        offers.add(buy(Items.BROWN_MUSHROOM, 10));
        offers.add(buy(Items.PUMPKIN, 6));
        addMissing(offers);
        return offers;
    }

    /** Adds the trades a saved innkeeper from an older version doesn't have yet (the Room Key). */
    static void addMissing(TradeOfferList offers) {
        if (offers.stream().noneMatch(InnkeeperTrades::isRoomKey)) {
            // A room for the night: the key is stamped with the inn and the day as it's sold.
            offers.add(sell(TavernRooms.PRICE, Tavern.ROOM_KEY, 1));
        }
    }

    static boolean isRoomKey(TradeOffer offer) {
        return offer.getSellItem().isOf(Tavern.ROOM_KEY);
    }

    private static TradeOffer sell(int emeralds, ItemConvertible item, int count) {
        return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item, count), MAX_USES, MERCHANT_XP,
                PRICE_MULTIPLIER);
    }

    private static TradeOffer buy(Item item, int count) {
        return new TradeOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD), MAX_USES, MERCHANT_XP,
                PRICE_MULTIPLIER);
    }
}
