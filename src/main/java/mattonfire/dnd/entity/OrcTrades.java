package mattonfire.dnd.entity;

import static mattonfire.dnd.entity.NpcTrades.sell;
import static mattonfire.dnd.entity.NpcTrades.buy;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What the Orc Quartermaster sells and buys. Every trade has 12 uses and restocks each morning; Half-Orcs pay
 * kin prices ({@link KinPrices}).
 * <ul>
 * <li>Sells: Grog (2 emeralds), the Orcish Greataxe (14, {@link KinStockOffers#orcishGreataxe()}), a saddle (6)
 * an enchanted iron axe (8) and 8 Tanned Hide (3).</li>
 * <li>Buys, for 1 emerald: 32 rotten flesh, 16 bones, 8 leather, 4 iron ingots; and 3 goblin war tokens for 2.</li>
 * <li>Kin stock, to Half-Orcs only: Orcish Warplate ({@link KinStockOffers#orcStronghold()}).</li>
 * </ul>
 */
public final class OrcTrades {

    private OrcTrades() {
    }

    static TradeOfferList quartermaster(Random random) {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(2, new ItemStack(ModItems.GROG)));
        offers.add(KinStockOffers.orcishGreataxe());
        offers.add(sell(6, new ItemStack(Items.SADDLE)));
        offers.add(sell(8, EnchantmentHelper.enchant(random, new ItemStack(Items.IRON_AXE), 15, false)));
        offers.add(tannedHide());
        offers.add(buy(Items.ROTTEN_FLESH, 32, 1));
        offers.add(buy(Items.BONE, 16, 1));
        offers.add(buy(Items.LEATHER, 8, 1));
        offers.add(buy(Items.IRON_INGOT, 4, 1));
        offers.add(buy(ModItems.GOBLIN_WAR_TOKEN, 3, 2));
        return offers;
    }

    /** 8 Tanned Hide, the yurts' building material, for 3 emeralds. */
    private static TradeOffer tannedHide() {
        return sell(3, ModBlockFamilies.TANNED_HIDE.base(), 8);
    }

    /** Adds the trades a saved Quartermaster from an older version doesn't have yet (the Tanned Hide). */
    static void addMissing(TradeOfferList offers) {
        if (offers.stream().noneMatch(offer -> offer.getSellItem().isOf(ModBlockFamilies.TANNED_HIDE.base().asItem()))) {
            offers.add(tannedHide());
        }
    }
}
