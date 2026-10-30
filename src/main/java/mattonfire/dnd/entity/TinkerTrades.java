package mattonfire.dnd.entity;

import static mattonfire.dnd.entity.NpcTrades.sell;
import static mattonfire.dnd.entity.NpcTrades.buy;

import mattonfire.dnd.classes.Registry.CogsdeepBlocks;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What a Gnome Tinker sells and buys. Trades have 12 uses and restock each morning; Gnomes pay kin prices
 * ({@link KinPrices}).
 * <ul>
 * <li>Sells: a spyglass (4 emeralds), a clock (4), a compass (3), 8 redstone dust (1), 2 repeaters (2), a
 * comparator (3), an observer (3), a piston (2), a sticky piston (3), 2 redstone lamps (2), a hopper (3); and the
 * burrow's building blocks: 8 Riveted Brass (2), 16 Gnomish Bricks (2), 4 Cogwork Panels (3).</li>
 * <li>Buys, for 1 emerald: 10 copper ingots, 6 amethyst shards, 3 iron ingots, 12 redstone dust, a Kobold Trinket.</li>
 * <li>Kin stock, to Gnomes only: the Gnomish Repeater and Tinker's Goggles ({@link KinStockOffers#cogsdeep()}).</li>
 * </ul>
 */
public final class TinkerTrades {

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
        addBuildingBlocks(offers);
        offers.add(buy(Items.COPPER_INGOT, 10));
        offers.add(buy(Items.AMETHYST_SHARD, 6));
        offers.add(buy(Items.IRON_INGOT, 3));
        offers.add(buy(Items.REDSTONE, 12));
        offers.add(koboldTrinket());
        return offers;
    }

    /** One emerald for a Kobold Trinket: gnomes collect what kobolds steal from them. */
    private static TradeOffer koboldTrinket() {
        return buy(mattonfire.dnd.entity.kobold.Kobolds.KOBOLD_TRINKET, 1);
    }

    /** Cogsdeep's building blocks (they come after the redstone, before what the Tinker buys). */
    private static void addBuildingBlocks(TradeOfferList offers) {
        offers.add(sell(2, new ItemStack(ModBlockFamilies.RIVETED_BRASS.base(), 8)));
        offers.add(sell(2, new ItemStack(ModBlockFamilies.GNOMISH_BRICKS.base(), 16)));
        offers.add(sell(3, new ItemStack(CogsdeepBlocks.COGWORK_PANEL, 4)));
    }

    /** Adds the building-block offers to a Tinker saved before they were sold (at the end of the list). */
    static void addMissing(TradeOfferList offers) {
        if (offers.stream().noneMatch(offer -> offer.getSellItem().isOf(ModBlockFamilies.RIVETED_BRASS.base().asItem()))) {
            addBuildingBlocks(offers);
        }
        if (offers.stream().noneMatch(offer -> offer.getOriginalFirstBuyItem().isOf(mattonfire.dnd.entity.kobold.Kobolds.KOBOLD_TRINKET))) {
            offers.add(koboldTrinket());
        }
    }
}
