package mattonfire.dnd.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * The kin discount: a merchant of a racial home (see {@link RacialHomes#homeOfMember}) charges players of
 * that home's race 25% less. It's its own special-price modifier, so it stacks with any other (reputation
 * tiers, vanilla gossip): the merchant clears the special prices, then each modifier adds its share.
 * Barterers (dwarves) can't discount, so for kin they roll their table twice and hand over the rarer roll.
 */
public final class KinPrices {
    /** Kin pay this much less (of the first item's count; rounds down, never below 1). */
    public static final float DISCOUNT = 0.25F;

    private KinPrices() {
    }

    /** Whether {@code player} is kin to {@code npc}'s settlement, for prices. */
    public static boolean isKin(PlayerEntity player, Entity npc) {
        RacialHomes.Home home = RacialHomes.homeOfMember(npc);
        return home != null && home.givesKinPrices(RaceLifecycle.activeRaceOf(player));
    }

    /** The kin modifier for one offer: how much to add to its special price (0 or negative). */
    public static int discount(TradeOffer offer) {
        int base = offer.getOriginalFirstBuyItem().getCount();
        int price = Math.max(1, (int) Math.floor(base * (1.0F - DISCOUNT)));
        return Math.min(0, price - base);
    }

    /** Adds the kin discount to every offer if {@code player} is kin to {@code npc}. Clear the special prices first. */
    public static void apply(Entity npc, PlayerEntity player, TradeOfferList offers) {
        if (!isKin(player, npc)) {
            return;
        }
        for (TradeOffer offer : offers) {
            offer.increaseSpecialPrice(discount(offer));
        }
    }

    /**
     * A barter roll: for kin, {@code roll} runs twice and the rarer result is kept, rarer meaning a lower
     * entry weight in {@code table} (the loot table id, e.g. {@code dndclasses:gameplay/dwarf_barter}).
     */
    public static List<ItemStack> barter(ServerWorld world, PlayerEntity player, Entity npc, Identifier table,
                                         Supplier<List<ItemStack>> roll) {
        List<ItemStack> first = roll.get();
        if (!isKin(player, npc)) {
            return first;
        }
        List<ItemStack> second = roll.get();
        Map<Identifier, Integer> weights = weights(world, table);
        int a = weight(first, weights);
        int b = weight(second, weights);
        List<ItemStack> kept = b < a ? second : first;
        DnDClasses.LOGGER.info("[KinPrices] Kin barter for {}: {} (weight {}) vs {} (weight {}), kept {}",
                player.getEntityName(), describe(first), a, describe(second), b, describe(kept));
        return kept;
    }

    /** The lowest weight among the stacks' items (unknown items count as common). */
    private static int weight(List<ItemStack> stacks, Map<Identifier, Integer> weights) {
        int best = Integer.MAX_VALUE;
        for (ItemStack stack : stacks) {
            best = Math.min(best, weights.getOrDefault(Registries.ITEM.getId(stack.getItem()), Integer.MAX_VALUE));
        }
        return best;
    }

    /** Item id to entry weight, read from the loot table's JSON (the loaded table doesn't expose its weights). */
    private static Map<Identifier, Integer> weights(ServerWorld world, Identifier table) {
        Map<Identifier, Integer> weights = new HashMap<>();
        Identifier file = new Identifier(table.getNamespace(), "loot_tables/" + table.getPath() + ".json");
        Optional<Resource> resource = world.getServer().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return weights;
        }
        try (Reader reader = resource.get().getReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement pool : json.getAsJsonArray("pools")) {
                for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    JsonObject object = entry.getAsJsonObject();
                    if (object.has("name")) {
                        int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
                        weights.merge(new Identifier(object.get("name").getAsString()), weight, Math::min);
                    }
                }
            }
        } catch (Exception e) {
            DnDClasses.LOGGER.warn("[KinPrices] Couldn't read weights from {}: {}", file, e.getMessage());
        }
        return weights;
    }

    private static String describe(List<ItemStack> stacks) {
        return stacks.stream().map(stack -> stack.getCount() + " " + stack.getItem()).toList().toString();
    }
}
