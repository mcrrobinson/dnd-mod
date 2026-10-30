package mattonfire.dnd.entity;

import static mattonfire.dnd.entity.NpcTrades.sell;
import static mattonfire.dnd.entity.NpcTrades.buy;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
 * and 1 netherite scrap a day (24); and the sanctum's stone, 16 Brimstone or 8 Brimstone Bricks (1).</li>
 * <li>Buys, for 1 emerald: 20 quartz, 22 nether wart, 3 gold ingots, 2 crying obsidian.</li>
 * <li>Kin stock, to Tieflings only: Infernal Regalia ({@link KinStockOffers#brimstoneSanctum()}).</li>
 * <li>Class stock: a Warlock may buy one piece of the Wither set (20 emeralds; which piece is rolled per
 * Pactbroker), an Alchemist a Fast Brewing Stand (12).</li>
 * <li>Bargains, once per player ever: a Portable Hole at Honored or better ({@value #HOLE_PRICE} emeralds), and at
 * Exalted the Deck of Many Things ({@value #DECK_PRICE} emeralds and a netherite ingot, "a devil's bargain").</li>
 * </ul>
 */
public final class TieflingTrades {
    public static final int WITHER_PRICE = 20;
    public static final int BREWING_STAND_PRICE = 12;
    public static final int SCRAP_PRICE = 24;
    public static final int HOLE_PRICE = 32;
    public static final int DECK_PRICE = 48;
    /** Persistent player data: the once-ever bargains the player has struck. */
    public static final String BARGAINS_KEY = "dndPactBargains";

    private TieflingTrades() {
    }

    static TradeOfferList pactbroker() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(1, new ItemStack(Items.BLAZE_ROD, 2)));
        offers.add(sell(3, new ItemStack(Items.GHAST_TEAR)));
        offers.add(sell(2, new ItemStack(Items.MAGMA_CREAM, 4)));
        offers.add(sell(4, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.FIRE_RESISTANCE)));
        offers.add(NpcTrades.offer(new ItemStack(Items.EMERALD, SCRAP_PRICE), new ItemStack(Items.NETHERITE_SCRAP), 1,
                NpcTrades.MERCHANT_XP * 5));
        offers.add(buy(Items.QUARTZ, 20));
        offers.add(buy(Items.NETHER_WART, 22));
        offers.add(buy(Items.GOLD_INGOT, 3));
        offers.add(buy(Items.CRYING_OBSIDIAN, 2));
        offers.add(brimstone());
        offers.add(brimstoneBricks());
        return offers;
    }

    private static TradeOffer brimstone() {
        return sell(1, ModBlockFamilies.BRIMSTONE.base(), 16);
    }

    private static TradeOffer brimstoneBricks() {
        return sell(1, ModBlockFamilies.BRIMSTONE_BRICKS.base(), 8);
    }

    /** Adds the offers a saved Pactbroker from an older version doesn't have yet (the brimstone). */
    static void addMissing(TradeOfferList offers) {
        if (offers.stream().noneMatch(offer -> offer.getSellItem().isOf(ModBlockFamilies.BRIMSTONE.base().asItem()))) {
            offers.add(brimstone());
        }
        if (offers.stream().noneMatch(offer -> offer.getSellItem().isOf(ModBlockFamilies.BRIMSTONE_BRICKS.base().asItem()))) {
            offers.add(brimstoneBricks());
        }
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

    /**
     * The once-ever bargains this customer may strike: a Portable Hole at Honored or better, the Deck of Many
     * Things at Exalted. Each is offered until the player has bought it ({@link #traded}).
     */
    static java.util.List<TradeOffer> bargains(PlayerEntity customer, Entity npc) {
        java.util.List<TradeOffer> offers = new java.util.ArrayList<>();
        mattonfire.dnd.faction.ReputationTier tier = mattonfire.dnd.faction.TierEffects.tierWith(customer, npc);
        net.minecraft.nbt.NbtCompound struck = bargains(customer);
        if (tier.ordinal() >= mattonfire.dnd.faction.ReputationTier.HONORED.ordinal() && !struck.getBoolean("Hole")) {
            offers.add(NpcTrades.offer(new ItemStack(Items.EMERALD, HOLE_PRICE),
                    identified(mattonfire.dnd.magic.items.PlanarItems.PORTABLE_HOLE), 1, NpcTrades.MERCHANT_XP * 5));
        }
        if (tier == mattonfire.dnd.faction.ReputationTier.EXALTED && !struck.getBoolean("Deck")) {
            ItemStack deck = identified(mattonfire.dnd.magic.items.PlanarItems.DECK_OF_MANY_THINGS);
            offers.add(new TradeOffer(new ItemStack(Items.EMERALD, DECK_PRICE), new ItemStack(Items.NETHERITE_INGOT),
                    deck, 0, 1, NpcTrades.MERCHANT_XP * 10, NpcTrades.PRICE_MULTIPLIER));
        }
        return offers;
    }

    private static ItemStack identified(Item item) {
        return mattonfire.dnd.magic.MagicItemLootFunction.apply(new ItemStack(item), null, null, true);
    }

    private static net.minecraft.nbt.NbtCompound bargains(PlayerEntity player) {
        net.minecraft.nbt.NbtCompound data = ((mattonfire.dnd.classes.IEntityDataSaver) player).getPersistentData();
        if (!data.contains(BARGAINS_KEY))
            data.put(BARGAINS_KEY, new net.minecraft.nbt.NbtCompound());
        return data.getCompound(BARGAINS_KEY);
    }

    /** A bargain struck: never offered to this player again. */
    static void traded(net.minecraft.server.network.ServerPlayerEntity player, TradeOffer offer) {
        ItemStack sold = offer.getSellItem();
        if (sold.isOf(mattonfire.dnd.magic.items.PlanarItems.PORTABLE_HOLE)) {
            bargains(player).putBoolean("Hole", true);
        } else if (sold.isOf(mattonfire.dnd.magic.items.PlanarItems.DECK_OF_MANY_THINGS)) {
            bargains(player).putBoolean("Deck", true);
            player.sendMessage(net.minecraft.text.Text.translatable("entity.dndclasses.tiefling_pactbroker.devils_bargain")
                    .formatted(net.minecraft.util.Formatting.DARK_RED, net.minecraft.util.Formatting.ITALIC), false);
        } else {
            return;
        }
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Magic] {} struck a Pactbroker bargain: {}",
                player.getEntityName(), sold.getName().getString());
    }

    static boolean isWarlock(PlayerEntity player, Entity npc) {
        return Progression.classOf(player) == DndCharacter.WARLOCK;
    }

    static boolean isAlchemist(PlayerEntity player, Entity npc) {
        return Progression.classOf(player) == DndCharacter.ALCHEMIST;
    }
}
