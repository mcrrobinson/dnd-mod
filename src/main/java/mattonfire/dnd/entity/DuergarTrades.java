package mattonfire.dnd.entity;

import static mattonfire.dnd.entity.NpcTrades.buy;
import static mattonfire.dnd.entity.NpcTrades.sell;

import java.util.Optional;
import java.util.function.Function;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * What the Duergar Factor sells and buys (design 9 section 5.3). Every trade has 12 uses and restocks each
 * morning. Prices follow the customer's standing with the Grey Clans; there are no kin prices here.
 * <ul>
 * <li>Sells: 16 Zurkhwood Planks (3 emeralds), 8 Faerzress Stone (4).</li>
 * <li>Buys: 4 gold ingots or 12 raw iron for 1 emerald.</li>
 * <li>Friendly and up ({@link #FRIENDLY_STOCK}): a Potion of Invisibility (18).</li>
 * <li>Honored and up ({@link #HONORED_STOCK}): the Duergar War Pick (20).</li>
 * </ul>
 * <b>Stock from other Underdark tickets</b> is offered only once its item is registered ({@link #LATER_STOCK}, by
 * id, so this class needs none of them to compile): Darklake Pearls (T3), Spidersilk (T4), Duergar Steel Ingots and
 * Basalt Forge Bricks (T6), and Greysteel Plate at Friendly (T6). A factor saved before such an item existed gets
 * its trade on load ({@link #addMissing}). The maps to the Rift and to a Drow Outpost wait for T2/T3 and T7.
 */
public final class DuergarTrades {
    /** Shown to Friendly and better customers. */
    public static final ReputationTier FRIENDLY_STOCK = ReputationTier.FRIENDLY;
    /** Shown to Honored and better customers. */
    public static final ReputationTier HONORED_STOCK = ReputationTier.HONORED;

    /** Optional stock: the item's id and how its trade is made. */
    private record Later(String id, Function<Item, TradeOffer> offer) {
    }

    /** Trades for items other Underdark tickets add; each shows up once its item is registered. */
    private static final Later[] LATER_STOCK = {
            new Later("darklake_pearl", item -> buy(item, 1, 8)),
            new Later("spidersilk", item -> buy(item, 8, 2)),
            new Later("duergar_steel_ingot", item -> sell(8, item)),
            new Later("basalt_forge_bricks", item -> sell(4, item, 16)),
    };
    /** Friendly stock from other tickets (T6's Greysteel Plate). */
    private static final Later[] LATER_FRIENDLY_STOCK = {
            new Later("greysteel_plate", item -> sell(6, item, 4)),
    };

    private DuergarTrades() {
    }

    static TradeOfferList factor() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(3, ModBlockFamilies.ZURKHWOOD_PLANKS.base(), 16));
        offers.add(sell(4, UnderdarkBlocks.FAERZRESS_STONE, 8));
        offers.add(buy(Items.GOLD_INGOT, 4, 1));
        offers.add(buy(Items.RAW_IRON, 12, 1));
        addMissing(offers);
        return offers;
    }

    /** Friendly stock: the Potion of Invisibility (and T6's Greysteel Plate once it exists). */
    static TradeOfferList friendly() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(18, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY)));
        addLater(offers, LATER_FRIENDLY_STOCK);
        return offers;
    }

    /** Honored stock: the Duergar War Pick. */
    static TradeOfferList honored() {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(20, warPick()));
        return offers;
    }

    /** An iron pickaxe with Efficiency II and Sharpness I, named. */
    public static ItemStack warPick() {
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.addEnchantment(Enchantments.EFFICIENCY, 2);
        pick.addEnchantment(Enchantments.SHARPNESS, 1);
        pick.setCustomName(Text.translatable("item.dndclasses.duergar_war_pick").styled(style -> style.withItalic(false)
                .withColor(Formatting.GRAY)));
        return pick;
    }

    static boolean showsFriendly(PlayerEntity player, Entity npc) {
        return TierEffects.tierWith(player, npc).atLeast(FRIENDLY_STOCK);
    }

    static boolean showsHonored(PlayerEntity player, Entity npc) {
        return TierEffects.tierWith(player, npc).atLeast(HONORED_STOCK);
    }

    /** Adds the optional trades whose items exist now and aren't offered yet (also for saved factors). */
    static void addMissing(TradeOfferList offers) {
        addLater(offers, LATER_STOCK);
    }

    static void addMissingFriendly(TradeOfferList offers) {
        addLater(offers, LATER_FRIENDLY_STOCK);
    }

    private static void addLater(TradeOfferList offers, Later[] stock) {
        for (Later later : stock) {
            item(later.id()).ifPresent(item -> {
                boolean offered = offers.stream().anyMatch(offer -> offer.getSellItem().isOf(item)
                        || offer.getOriginalFirstBuyItem().isOf(item));
                if (!offered) {
                    offers.add(later.offer().apply(item));
                }
            });
        }
    }

    /** {@code dndclasses:<path>} if some ticket registered it. */
    static Optional<Item> item(String path) {
        Identifier id = new Identifier(DnDClasses.MOD_ID, path);
        return Registries.ITEM.containsId(id) ? Optional.of(Registries.ITEM.get(id)) : Optional.empty();
    }
}
