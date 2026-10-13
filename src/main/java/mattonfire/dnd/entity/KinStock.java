package mattonfire.dnd.entity;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.Nullable;

/**
 * A merchant's kin-only offers (the racial gear, {@link KinStockOffers}). They're kept in their own list and
 * shown after the merchant's normal offers only while a customer of the settlement's home race trades
 * ({@link #isKin}); everyone else never sees them. Kin prices apply to them like any other offer.
 * <p>
 * A merchant lists its stocks in its {@link NpcMerchant}, which shows them, restocks them and saves them.
 */
public final class KinStock {
    private static final String NBT_KEY = "KinOffers";

    private final Supplier<TradeOfferList> factory;
    /** Who sees these offers: kin of the merchant's settlement, or (for gated stock) some other rule. */
    private final BiPredicate<PlayerEntity, Entity> audience;
    private final String nbtKey;
    @Nullable
    private TradeOfferList offers;

    public KinStock(Supplier<TradeOfferList> factory) {
        this(factory, KinStock::isKin, NBT_KEY);
    }

    /**
     * Offers only {@code audience} sees, saved under {@code nbtKey}: e.g. the Pactbroker's class-gated stock
     * for Warlocks and Alchemists.
     */
    public KinStock(Supplier<TradeOfferList> factory, BiPredicate<PlayerEntity, Entity> audience, String nbtKey) {
        this.factory = factory;
        this.audience = audience;
        this.nbtKey = nbtKey;
    }

    /** Whether {@code player} sees these offers at {@code npc}. */
    public boolean shownTo(PlayerEntity player, Entity npc) {
        return this.audience.test(player, npc);
    }

    /**
     * Whether {@code player} is of the home race of {@code npc}'s settlement. Kin stock is for the home race
     * only: interim kin ({@link RacialHomes.Home#priceKin}) would get kin prices but not the racial gear.
     */
    public static boolean isKin(PlayerEntity player, Entity npc) {
        RacialHomes.Home home = RacialHomes.homeOfMember(npc);
        return home != null && home.isHomeOf(RaceLifecycle.activeRaceOf(player));
    }

    /** The kin-only offers, made on first use (also for merchants saved before kin stock existed). */
    public TradeOfferList offers() {
        if (this.offers == null) {
            this.offers = this.factory.get();
        }
        return this.offers;
    }

    /** The customer left: their prices come off these offers. */
    public void clearSpecialPrices() {
        if (this.offers != null) {
            this.offers.forEach(TradeOffer::clearSpecialPrice);
        }
    }

    public void restock() {
        if (this.offers != null) {
            this.offers.forEach(TradeOffer::resetUses);
        }
    }

    public void write(NbtCompound nbt) {
        if (this.offers != null) {
            nbt.put(this.nbtKey, this.offers.toNbt());
        }
    }

    public void read(NbtCompound nbt) {
        if (nbt.contains(this.nbtKey)) {
            this.offers = new TradeOfferList(nbt.getCompound(this.nbtKey));
        }
    }
}
