package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.faction.ReputationTier;
import net.minecraft.village.TradeOffer;

/**
 * The Underdark building blocks' merchant stock (design 9 T6), for the Duergar Factor's offer table
 * ({@link DuergarTrades}). The factor's base offers take {@link #always()} and its Friendly stock
 * {@link #friendly()}, when it builds them and from its {@code addMissing}, so factors that already exist get them
 * too:
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
        List<TradeOffer> offers = new ArrayList<>(always());
        if (tier.atLeast(GREYSTEEL_TIER)) {
            offers.addAll(friendly());
        }
        return offers;
    }

    /** Offered to every customer: the forge bricks and the steel ingot. */
    public static List<TradeOffer> always() {
        return List.of(
                NpcTrades.sell(FORGE_BRICKS_PRICE, ModBlockFamilies.BASALT_FORGE_BRICKS.base(), FORGE_BRICKS_COUNT),
                NpcTrades.sell(STEEL_INGOT_PRICE, UnderdarkBlocks.DUERGAR_STEEL_INGOT));
    }

    /** Offered from {@link #GREYSTEEL_TIER}: the Greysteel Plate. */
    public static List<TradeOffer> friendly() {
        return List.of(NpcTrades.sell(GREYSTEEL_PLATE_PRICE, ModBlockFamilies.GREYSTEEL_PLATE.base(),
                GREYSTEEL_PLATE_COUNT));
    }
}
