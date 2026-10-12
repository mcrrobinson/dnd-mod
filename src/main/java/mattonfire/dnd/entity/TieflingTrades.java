package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What the Pactbroker sells and buys. Trades have 12 uses (the netherite scrap 1) and restock each morning;
 * Tieflings pay kin prices ({@link KinPrices}).
 * <ul>
 * <li>Sells: 2 blaze rods (1 emerald), a ghast tear (3), 4 magma cream (2), a Potion of Fire Resistance (4),
 * and 1 netherite scrap a day (24).</li>
 * <li>Buys, for 1 emerald: 20 quartz, 22 nether wart, 3 gold ingots, 2 crying obsidian.</li>
 * <li>Kin stock, to Tieflings only: Infernal Regalia ({@link KinStockOffers#brimstoneSanctum()}).</li>
 * <li>Class stock: a Warlock may buy one piece of the Wither set (20 emeralds; which piece is rolled per
 * Pactbroker), an Alchemist a Fast Brewing Stand (12).</li>
 * </ul>
 */
public final class TieflingTrades {
    private static final int MAX_USES = 12;
    private static final int MERCHANT_XP = 2;
    private static final float PRICE_MULTIPLIER = 0.05F;
    public static final int WITHER_PRICE = 20;
    public static final int BREWING_STAND_PRICE = 12;
    public static final int SCRAP_PRICE = 24;

    private TieflingTrades() {
    }

    static TradeOfferList pactbroker() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(1, new ItemStack(Items.BLAZE_ROD, 2)));
        offers.add(sell(3, new ItemStack(Items.GHAST_TEAR)));
        offers.add(sell(2, new ItemStack(Items.MAGMA_CREAM, 4)));
        offers.add(sell(4, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.FIRE_RESISTANCE)));
        offers.add(new TradeOffer(new ItemStack(Items.EMERALD, SCRAP_PRICE), new ItemStack(Items.NETHERITE_SCRAP), 1,
                MERCHANT_XP * 5, PRICE_MULTIPLIER));
        offers.add(buy(Items.QUARTZ, 20));
        offers.add(buy(Items.NETHER_WART, 22));
        offers.add(buy(Items.GOLD_INGOT, 3));
        offers.add(buy(Items.CRYING_OBSIDIAN, 2));
        return offers;
    }

    /** Warlocks only: one piece of the Wither set, rolled when the Pactbroker first trades. */
    static TradeOfferList warlock(Random random) {
        Item[] pieces = {ModItems.WITHER_HELMET, ModItems.WITHER_CHESTPLATE, ModItems.WITHER_LEGGINGS, ModItems.WITHER_BOOTS};
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(WITHER_PRICE, new ItemStack(pieces[random.nextInt(pieces.length)])));
        return offers;
    }

    /** Alchemists only: a Fast Brewing Stand. */
    static TradeOfferList alchemist() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(BREWING_STAND_PRICE, new ItemStack(ModBlocks.FAST_BREWING_STAND_BLOCK)));
        return offers;
    }

    static boolean isWarlock(PlayerEntity player, Entity npc) {
        return Progression.classOf(player) == DndCharacter.WARLOCK;
    }

    static boolean isAlchemist(PlayerEntity player, Entity npc) {
        return Progression.classOf(player) == DndCharacter.ALCHEMIST;
    }

    private static TradeOffer sell(int emeralds, ItemStack stack) {
        return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), stack, MAX_USES, MERCHANT_XP, PRICE_MULTIPLIER);
    }

    private static TradeOffer buy(ItemConvertible item, int count) {
        return new TradeOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD), MAX_USES, MERCHANT_XP, PRICE_MULTIPLIER);
    }
}
