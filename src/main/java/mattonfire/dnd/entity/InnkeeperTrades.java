package mattonfire.dnd.entity;

import static mattonfire.dnd.entity.NpcTrades.sell;
import static mattonfire.dnd.entity.NpcTrades.buy;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.tavern.Tavern;
import mattonfire.dnd.tavern.TavernRooms;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What an innkeeper sells (food, drink and a room for the night) and buys (the village's produce).
 * Restocks daily.
 */
final class InnkeeperTrades {

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

    /** Adds the trades a saved innkeeper from an older version doesn't have yet (the Room Key, thatch). */
    static void addMissing(TradeOfferList offers) {
        if (offers.stream().noneMatch(InnkeeperTrades::isRoomKey)) {
            // A room for the night: the key is stamped with the inn and the day as it's sold.
            offers.add(sell(TavernRooms.PRICE, Tavern.ROOM_KEY, 1));
        }
        if (offers.stream().noneMatch(offer -> offer.getSellItem().isOf(ModBlockFamilies.THATCH.base().asItem()))) {
            // Thatch for your own roof, as on the inn's.
            offers.add(sell(1, ModBlockFamilies.THATCH.base(), 8));
        }
    }

    /** Honored with the hobbits: a round of 3 ale for 1 emerald (the innkeeper adds a cake on the house). */
    static TradeOffer honoredRound() {
        return sell(1, Tavern.ALE, 3);
    }

    static boolean isRoomKey(TradeOffer offer) {
        return offer.getSellItem().isOf(Tavern.ROOM_KEY);
    }
}
