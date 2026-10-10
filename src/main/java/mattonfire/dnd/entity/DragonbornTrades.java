package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.gen.structure.Structure;

/**
 * What the Dragonborn Elder sells and buys. Every trade has 12 uses and restocks each morning; Dragonborn pay
 * kin prices ({@link KinPrices}).
 * <ul>
 * <li>Sells: a Smite Dragons III book (18 emeralds), a long Potion of Fire Resistance (8 min, 4), and explorer
 * maps to the nearest dragon lair and frost lair (12 each).</li>
 * <li>Buys the wyvern and drake drops (from {@code loot_tables/entities/*wyvern*}, {@code frost_drake} and
 * {@code lightning_chaser}): phantom membrane, magma cream, blaze powder, blue ice, copper blocks and
 * netherite scrap, for 3-8 emeralds.</li>
 * <li>Kin stock, to Dragonborn only: Draconic Scale ({@link KinStockOffers#dragonTemple()}, shown through
 * {@link KinStock}).</li>
 * </ul>
 */
public final class DragonbornTrades {
    private static final int MAX_USES = 12;
    private static final int MERCHANT_XP = 2;
    private static final float PRICE_MULTIPLIER = 0.05F;
    /** The Elder's Smite Dragons book (of 5 levels). */
    public static final int SMITE_LEVEL = 3;
    public static final TagKey<Structure> DRAGON_LAIR_MAPS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "dragon_temple_maps/dragon_lair"));
    public static final TagKey<Structure> FROST_LAIR_MAPS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "dragon_temple_maps/frost_lair"));

    private DragonbornTrades() {
    }

    static TradeOfferList elder(ServerWorld world, BlockPos pos, Random random) {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(18, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModEnchantments.SMITE_DRAGONS_ENCHANTMENT,
                SMITE_LEVEL))));
        offers.add(sell(4, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.LONG_FIRE_RESISTANCE)));
        ItemStack dragonMap = lairMap(world, pos, DRAGON_LAIR_MAPS, "item.dndclasses.temple_dragon_lair_map");
        if (dragonMap != null) {
            offers.add(sell(12, dragonMap));
        }
        ItemStack frostMap = lairMap(world, pos, FROST_LAIR_MAPS, "item.dndclasses.temple_frost_lair_map");
        if (frostMap != null) {
            offers.add(sell(12, frostMap));
        }
        offers.add(buy(Items.PHANTOM_MEMBRANE, 4, 3));
        offers.add(buy(Items.MAGMA_CREAM, 6, 4));
        offers.add(buy(Items.BLAZE_POWDER, 8, 4));
        offers.add(buy(Items.BLUE_ICE, 2, 5));
        offers.add(buy(Items.COPPER_BLOCK, 2, 3));
        offers.add(buy(Items.NETHERITE_SCRAP, 1, 8));
        return offers;
    }

    /** A map to the nearest unexplored structure in {@code targets}, or null if there's none in range. */
    private static ItemStack lairMap(ServerWorld world, BlockPos pos, TagKey<Structure> targets, String name) {
        BlockPos target = world.locateStructure(targets, pos, 100, true);
        if (target == null) {
            return null;
        }
        ItemStack map = FilledMapItem.createMap(world, target.getX(), target.getZ(), (byte) 2, true, true);
        FilledMapItem.fillExplorationMap(world, map);
        MapState.addDecorationsNbt(map, target, "+", MapIcon.Type.RED_X);
        map.setCustomName(Text.translatable(name));
        return map;
    }

    private static TradeOffer sell(int emeralds, ItemStack stack) {
        return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), stack, MAX_USES, MERCHANT_XP, PRICE_MULTIPLIER);
    }

    private static TradeOffer buy(ItemConvertible item, int count, int emeralds) {
        return new TradeOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD, emeralds), MAX_USES, MERCHANT_XP,
                PRICE_MULTIPLIER);
    }
}
