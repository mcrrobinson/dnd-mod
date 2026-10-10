package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * The racial gear each settlement's merchants sell to their own race only (kin stock, see {@link KinStock}).
 * Every offer has 12 uses and restocks each morning, like the merchants' other trades.
 * <p>
 * Dwarves have no merchant: their kin stock is the {@code gameplay/dwarf_kin_stock} barter table
 * ({@link MountainDwarfEntity}). The settlements that don't exist yet get their lists here, ready for their
 * vendors: {@link #orcStronghold()}, {@link #brimstoneSanctum()} and {@link #cogsdeep()}.
 */
public final class KinStockOffers {
    private static final int MAX_USES = 12;
    private static final int MERCHANT_XP = 2;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private KinStockOffers() {
    }

    /** The enclave Speaker, to Elves: Elven Chain (helmet 10, chestplate 16, leggings 14, boots 8 emeralds). */
    public static TradeOfferList elvenSpeaker() {
        return armor(ModItems.ELVEN_CHAIN_HELMET, 10, ModItems.ELVEN_CHAIN_CHESTPLATE, 16,
                ModItems.ELVEN_CHAIN_LEGGINGS, 14, ModItems.ELVEN_CHAIN_BOOTS, 8);
    }

    /** The innkeeper, to Halflings: the Halfling Sling (6 emeralds). */
    public static TradeOfferList halflingInnkeeper() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(6, ModItems.HALFLING_SLING));
        return offers;
    }

    /** The Dragonborn Elder, to Dragonborn: Draconic Scale (helmet 12, chestplate 20, leggings 18, boots 10). */
    public static TradeOfferList dragonTemple() {
        return armor(ModItems.DRACONIC_SCALE_HELMET, 12, ModItems.DRACONIC_SCALE_CHESTPLATE, 20,
                ModItems.DRACONIC_SCALE_LEGGINGS, 18, ModItems.DRACONIC_SCALE_BOOTS, 10);
    }

    /**
     * Hook for the Orc Stronghold's vendor (not built yet), to Half-Orcs: Orcish Warplate (helmet 10,
     * chestplate 16, leggings 14, boots 8). The Orcish Greataxe (14 emeralds) is sold to everyone there, so it
     * belongs in the vendor's normal offers ({@link #orcishGreataxe()}).
     */
    public static TradeOfferList orcStronghold() {
        return armor(ModItems.ORCISH_WARPLATE_HELMET, 10, ModItems.ORCISH_WARPLATE_CHESTPLATE, 16,
                ModItems.ORCISH_WARPLATE_LEGGINGS, 14, ModItems.ORCISH_WARPLATE_BOOTS, 8);
    }

    /** The Orcish Greataxe for 14 emeralds, for the Orc Stronghold vendor's normal offers. */
    public static TradeOffer orcishGreataxe() {
        return sell(14, ModItems.ORCISH_GREATAXE);
    }

    /**
     * Hook for the Brimstone Sanctum's vendor (not built yet), to Tieflings: Infernal Regalia (helmet 12,
     * chestplate 20, leggings 18, boots 10).
     */
    public static TradeOfferList brimstoneSanctum() {
        return armor(ModItems.INFERNAL_REGALIA_HELMET, 12, ModItems.INFERNAL_REGALIA_CHESTPLATE, 20,
                ModItems.INFERNAL_REGALIA_LEGGINGS, 18, ModItems.INFERNAL_REGALIA_BOOTS, 10);
    }

    /**
     * Hook for Cogsdeep's vendor (not built yet), to Gnomes. The Gnomish Repeater and Tinker's Goggles belong to
     * the Cogsdeep ticket, so this is empty for now.
     */
    public static TradeOfferList cogsdeep() {
        return new TradeOfferList();
    }

    private static TradeOfferList armor(Item helmet, int helmetPrice, Item chest, int chestPrice, Item legs,
            int legsPrice, Item boots, int bootsPrice) {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(helmetPrice, helmet));
        offers.add(sell(chestPrice, chest));
        offers.add(sell(legsPrice, legs));
        offers.add(sell(bootsPrice, boots));
        return offers;
    }

    private static TradeOffer sell(int emeralds, Item item) {
        return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item), MAX_USES, MERCHANT_XP,
                PRICE_MULTIPLIER);
    }
}
