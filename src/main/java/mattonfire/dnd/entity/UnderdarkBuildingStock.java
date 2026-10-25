package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.faction.ReputationTier;
import net.minecraft.village.TradeOffer;

/**
 * The Underdark building blocks' merchant stock (design 9 T6), for the Duergar Factor's offer table
 * ({@code DuergarTrades.factor}, T5). The factor calls {@link #duergarFactor(ReputationTier)} when it builds its
 * offers, and from its {@code addMissing}, so factors that already exist get them too:
 * <ul>
 * <li>Forge Bricks x16 for 4 emeralds, Duergar Steel Ingot for 8: always.</li>
 * <li>Greysteel Plate x2 for 12 emeralds: at Friendly and above.</li>
 * </ul>
 * The ingot is the T6 material the design's factor table lists; the factor shouldn't add its own offer for it.
 */
public final class UnderdarkBuildingStock {
    public static final int FORGE_BRICKS_COUNT = 16;
    public static final int FORGE_BRICKS_PRICE = 4;
    public static final int STEEL_INGOT_PRICE = 8;
    public static final int GREYSTEEL_PLATE_COUNT = 2;
    public static final int GREYSTEEL_PLATE_PRICE = 12;
    /** The Greysteel Plate offer's tier. */
    public static final ReputationTier GREYSTEEL_TIER = ReputationTier.FRIENDLY;

    private UnderdarkBuildingStock() {
    }

    /** The factor's building stock for a player at {@code tier} with the Grey Clans. */
    public static List<TradeOffer> duergarFactor(ReputationTier tier) {
        List<TradeOffer> offers = new ArrayList<>();
        offers.add(NpcTrades.sell(FORGE_BRICKS_PRICE, ModBlockFamilies.BASALT_FORGE_BRICKS.base(), FORGE_BRICKS_COUNT));
        offers.add(NpcTrades.sell(STEEL_INGOT_PRICE, UnderdarkBlocks.DUERGAR_STEEL_INGOT));
        if (tier.atLeast(GREYSTEEL_TIER)) {
            offers.add(NpcTrades.sell(GREYSTEEL_PLATE_PRICE, ModBlockFamilies.GREYSTEEL_PLATE.base(),
                    GREYSTEEL_PLATE_COUNT));
        }
        return offers;
    }
}
