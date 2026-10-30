package mattonfire.dnd.entity.kobold;

import static mattonfire.dnd.entity.NpcTrades.buy;
import static mattonfire.dnd.entity.NpcTrades.sell;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOfferList;

/**
 * What a Kobold Trapmaker sells and buys, to players Friendly or better with the Kobold Tribes. Trades have 12 uses
 * and restock each morning.
 * <ul>
 * <li>Sells: 4 Kobold Snares (3 emeralds), a Kobold Sling (4), 32 cobblestone "sling stones" (1), a Kobold
 * Trinket (2), 2 tripwire hooks (1).</li>
 * <li>Buys, for 1 emerald: 3 gold ingots, 16 rotten flesh.</li>
 * </ul>
 */
public final class KoboldTrades {
    private KoboldTrades() {
    }

    static TradeOfferList trapmaker() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(3, new ItemStack(Kobolds.KOBOLD_SNARE, 4)));
        offers.add(sell(4, new ItemStack(Kobolds.KOBOLD_SLING)));
        offers.add(sell(1, new ItemStack(Items.COBBLESTONE, 32)));
        offers.add(sell(2, new ItemStack(Kobolds.KOBOLD_TRINKET)));
        offers.add(sell(1, new ItemStack(Items.TRIPWIRE_HOOK, 2)));
        offers.add(buy(Items.GOLD_INGOT, 3));
        offers.add(buy(Items.ROTTEN_FLESH, 16));
        return offers;
    }
}
