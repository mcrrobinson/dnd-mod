package mattonfire.dnd.entity;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;

/**
 * The trade builders every settlement merchant's offer table uses ({@code *Trades}, {@link KinStockOffers}).
 * Every offer has {@link #MAX_USES} uses (restocked each morning by {@link NpcMerchant}), gives
 * {@link #MERCHANT_XP} and has the vanilla {@link #PRICE_MULTIPLIER}.
 */
public final class NpcTrades {
    public static final int MAX_USES = 12;
    public static final int MERCHANT_XP = 2;
    public static final float PRICE_MULTIPLIER = 0.05F;

    private NpcTrades() {
    }

    /** {@code stack} for {@code emeralds}. */
    public static TradeOffer sell(int emeralds, ItemStack stack) {
        return offer(new ItemStack(Items.EMERALD, emeralds), stack, MAX_USES, MERCHANT_XP);
    }

    /** One {@code item} for {@code emeralds}. */
    public static TradeOffer sell(int emeralds, ItemConvertible item) {
        return sell(emeralds, new ItemStack(item));
    }

    /** {@code count} {@code item} for {@code emeralds}. */
    public static TradeOffer sell(int emeralds, ItemConvertible item, int count) {
        return sell(emeralds, new ItemStack(item, count));
    }

    /** One emerald for {@code count} {@code item}. */
    public static TradeOffer buy(ItemConvertible item, int count) {
        return buy(item, count, 1);
    }

    /** {@code emeralds} for {@code count} {@code item}. */
    public static TradeOffer buy(ItemConvertible item, int count, int emeralds) {
        return offer(new ItemStack(item, count), new ItemStack(Items.EMERALD, emeralds), MAX_USES, MERCHANT_XP);
    }

    /** Any other offer (other uses or XP), with the shared price multiplier. */
    public static TradeOffer offer(ItemStack price, ItemStack sold, int maxUses, int merchantXp) {
        return new TradeOffer(price, sold, maxUses, merchantXp, PRICE_MULTIPLIER);
    }
}
