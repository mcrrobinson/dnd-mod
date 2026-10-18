package mattonfire.dnd.entity;

import java.util.Map;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.tavern.Tavern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.Nullable;

/**
 * Each racial home's merchant sells its banner pattern ({@link ModBannerPatterns}) for {@link #PRICE} emeralds (kin
 * pay less, as for every offer). {@link NpcMerchant} adds the offer to the end of a merchant's offers when it makes
 * them and when it loads them, if they don't have it yet, so merchants from before the patterns get it too. The
 * dwarves barter theirs instead ({@code gameplay/dwarf_barter}), and the Goblin Warlord drops the goblin skull.
 */
public final class RacialBannerOffers {
    public static final int PRICE = 3;

    private static final Map<EntityType<?>, ModBannerPatterns.Pattern> SOLD_BY = Map.of(
            ModEntityTypes.ORC_QUARTERMASTER, ModBannerPatterns.BLOODTUSK,
            ModEntityTypes.ELF_SPEAKER, ModBannerPatterns.SYLVAN_LEAF,
            ModEntityTypes.DRAGONBORN_ELDER, ModBannerPatterns.THREE_BREATHS,
            ModEntityTypes.TIEFLING_PACTBROKER, ModBannerPatterns.INFERNAL_PACT,
            ModEntityTypes.GNOME_TINKER, ModBannerPatterns.GNOME_COG,
            Tavern.INNKEEPER, ModBannerPatterns.GREEN_DRAGON);

    private RacialBannerOffers() {
    }

    /** The pattern this merchant sells, or null. */
    @Nullable
    public static ModBannerPatterns.Pattern soldBy(Entity merchant) {
        return SOLD_BY.get(merchant.getType());
    }

    /** Adds the merchant's pattern to {@code offers} if it sells one and they don't have it yet. */
    static void addMissing(Entity merchant, TradeOfferList offers) {
        ModBannerPatterns.Pattern pattern = soldBy(merchant);
        if (pattern != null && offers.stream().noneMatch(offer -> offer.getSellItem().isOf(pattern.item()))) {
            offers.add(NpcTrades.sell(PRICE, pattern.item()));
        }
    }
}
