package mattonfire.dnd.entity;

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
 * Use: the merchant's {@code getOffers()} returns {@link #view(TradeOfferList)} of its normal offers,
 * {@code setCustomer} calls {@link #setCustomer}, the morning restock calls {@link #restock()}, and the NBT
 * methods call {@link #write} / {@link #read}.
 */
public final class KinStock {
    private static final String NBT_KEY = "KinOffers";

    private final Supplier<TradeOfferList> factory;
    @Nullable
    private TradeOfferList offers;
    /** Normal + kin offers, while a kin customer trades. The same offer objects, so uses are shared. */
    @Nullable
    private TradeOfferList shown;

    public KinStock(Supplier<TradeOfferList> factory) {
        this.factory = factory;
    }

    /**
     * Whether {@code player} is of the home race of {@code npc}'s settlement. Kin stock is for the home race
     * only: interim kin (Gnomes at the dwarven fortresses) get kin prices but not the racial gear.
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

    /** What the merchant shows: {@code base}, plus the kin offers while a kin customer trades. */
    public TradeOfferList view(TradeOfferList base) {
        return this.shown != null ? this.shown : base;
    }

    /** The merchant's customer changed. Call before preparing the customer's prices. */
    public void setCustomer(Entity npc, @Nullable PlayerEntity customer, TradeOfferList base) {
        if (customer != null && isKin(customer, npc) && !this.offers().isEmpty()) {
            TradeOfferList all = new TradeOfferList();
            all.addAll(base);
            all.addAll(this.offers());
            this.shown = all;
            return;
        }
        if (customer == null && this.offers != null) {
            this.offers.forEach(TradeOffer::clearSpecialPrice);
        }
        this.shown = null;
    }

    public void restock() {
        if (this.offers != null) {
            this.offers.forEach(TradeOffer::resetUses);
        }
    }

    public void write(NbtCompound nbt) {
        if (this.offers != null) {
            nbt.put(NBT_KEY, this.offers.toNbt());
        }
    }

    public void read(NbtCompound nbt) {
        if (nbt.contains(NBT_KEY)) {
            this.offers = new TradeOfferList(nbt.getCompound(NBT_KEY));
        }
    }
}
