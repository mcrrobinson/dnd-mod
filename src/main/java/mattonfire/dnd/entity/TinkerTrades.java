package mattonfire.dnd.entity;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What a Gnome Tinker sells and buys. Trades have 12 uses and restock each morning; Gnomes pay kin prices
 * ({@link KinPrices}).
 * <ul>
 * <li>Sells: a spyglass (4 emeralds), a clock (4), a compass (3), 8 redstone dust (1), 2 repeaters (2), a
 * comparator (3), an observer (3), a piston (2), a sticky piston (3), 2 redstone lamps (2), a hopper (3).</li>
 * <li>Buys, for 1 emerald: 10 copper ingots, 6 amethyst shards, 3 iron ingots, 12 redstone dust.</li>
 * <li>Kin stock, to Gnomes only: the Gnomish Repeater and Tinker's Goggles ({@link KinStockOffers#cogsdeep()}).</li>
 * </ul>
 */
public final class TinkerTrades {
    private static final int MAX_USES = 12;
    private static final int MERCHANT_XP = 2;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private TinkerTrades() {
    }

    static TradeOfferList tinker() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(4, new ItemStack(Items.SPYGLASS)));
        offers.add(sell(4, new ItemStack(Items.CLOCK)));
        offers.add(sell(3, new ItemStack(Items.COMPASS)));
        offers.add(sell(1, new ItemStack(Items.REDSTONE, 8)));
        offers.add(sell(2, new ItemStack(Items.REPEATER, 2)));
        offers.add(sell(3, new ItemStack(Items.COMPARATOR)));
        offers.add(sell(3, new ItemStack(Items.OBSERVER)));
        offers.add(sell(2, new ItemStack(Items.PISTON)));
        offers.add(sell(3, new ItemStack(Items.STICKY_PISTON)));
        offers.add(sell(2, new ItemStack(Items.REDSTONE_LAMP, 2)));
        offers.add(sell(3, new ItemStack(Items.HOPPER)));
        offers.add(buy(Items.COPPER_INGOT, 10));
        offers.add(buy(Items.AMETHYST_SHARD, 6));
        offers.add(buy(Items.IRON_INGOT, 3));
        offers.add(buy(Items.REDSTONE, 12));
        return offers;
    }

    private static TradeOffer sell(int emeralds, ItemStack stack) {
        return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), stack, MAX_USES, MERCHANT_XP, PRICE_MULTIPLIER);
    }

    private static TradeOffer buy(ItemConvertible item, int count) {
        return new TradeOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD), MAX_USES, MERCHANT_XP, PRICE_MULTIPLIER);
    }
}
