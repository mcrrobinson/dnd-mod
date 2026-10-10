package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.world.gen.enclave.Moonwell;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
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
 * What the enclave's merchants sell and buy. Every trade has 12 uses and restocks each morning; Elves pay
 * kin prices. The Elven Longbow and Elven Chain (racial gear) join these lists in a later ticket.
 */
public final class ElfTrades {
    private static final int MAX_USES = 12;
    private static final int MERCHANT_XP = 2;
    private static final float PRICE_MULTIPLIER = 0.05F;
    /** Where the Speaker's explorer map can lead: dragon and beholder lairs. */
    public static final TagKey<Structure> MAP_TARGETS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "elf_speaker_maps"));
    /** The Speaker's daily enchanted book is offer number 1. */
    private static final int BOOK_OFFER = 1;

    private ElfTrades() {
    }

    /** The Fletcher: arrows, tipped arrows, enchanted bows and Power III; buys sticks, feathers and string. */
    static TradeOfferList fletcher(Random random) {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(1, new ItemStack(Items.ARROW, 16)));
        offers.add(sell(2, PotionUtil.setPotion(new ItemStack(Items.TIPPED_ARROW, 4), Potions.SLOWNESS)));
        offers.add(sell(2, PotionUtil.setPotion(new ItemStack(Items.TIPPED_ARROW, 4), Potions.POISON)));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.POWER, 1 + random.nextInt(2));
        offers.add(sell(6, bow));
        offers.add(sell(14, EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.POWER, 3))));
        offers.add(buy(Items.STICK, 32));
        offers.add(buy(Items.FEATHER, 12));
        offers.add(buy(Items.STRING, 14));
        return offers;
    }

    /** The Speaker: Moonwater, a different enchanted book each day, an explorer map to a lair, glow berries. */
    static TradeOfferList speaker(ServerWorld world, BlockPos pos, Random random) {
        TradeOfferList offers = new TradeOfferList();
        offers.add(sell(3, Moonwell.moonwater()));
        offers.add(dailyBook(random));
        offers.add(sell(1, new ItemStack(Items.GLOW_BERRIES)));
        ItemStack map = explorerMap(world, pos);
        if (map != null) {
            offers.add(sell(14, map));
        }
        return offers;
    }

    /** Swaps the Speaker's book for a new one (each morning). */
    static void newDailyBook(TradeOfferList offers, Random random) {
        if (offers.size() > BOOK_OFFER && offers.get(BOOK_OFFER).getSellItem().isOf(Items.ENCHANTED_BOOK)) {
            offers.set(BOOK_OFFER, dailyBook(random));
        }
    }

    /** One random enchanted book (any treasure-free enchantment, any level) for 12-20 emeralds. */
    private static TradeOffer dailyBook(Random random) {
        java.util.List<Enchantment> pool = Registries.ENCHANTMENT.stream()
                .filter(e -> !e.isTreasure() && e.isAvailableForRandomSelection()).toList();
        Enchantment enchantment = pool.get(random.nextInt(pool.size()));
        int level = 1 + random.nextInt(enchantment.getMaxLevel());
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, level));
        int price = Math.min(20, 12 + random.nextInt(5) + level * 2);
        return sell(price, book);
    }

    /** A map to the nearest unexplored dragon or beholder lair, or null if there's none in range. */
    private static ItemStack explorerMap(ServerWorld world, BlockPos pos) {
        BlockPos target = world.locateStructure(MAP_TARGETS, pos, 100, true);
        if (target == null) {
            return null;
        }
        ItemStack map = FilledMapItem.createMap(world, target.getX(), target.getZ(), (byte) 2, true, true);
        FilledMapItem.fillExplorationMap(world, map);
        MapState.addDecorationsNbt(map, target, "+", MapIcon.Type.RED_X);
        map.setCustomName(Text.translatable("item.dndclasses.elf_lair_map"));
        return map;
    }

    private static TradeOffer sell(int emeralds, ItemStack stack) {
        return new TradeOffer(new ItemStack(Items.EMERALD, emeralds), stack, MAX_USES, MERCHANT_XP, PRICE_MULTIPLIER);
    }

    private static TradeOffer buy(ItemConvertible item, int count) {
        return new TradeOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD), MAX_USES, MERCHANT_XP,
                PRICE_MULTIPLIER);
    }
}
