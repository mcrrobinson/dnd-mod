package mattonfire.dnd.entity;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mattonfire.dnd.faction.FactionEvents;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.Nullable;
import mattonfire.dnd.world.GameTime;

/**
 * What every settlement merchant (innkeeper, Elf Speaker and Fletcher, Dragonborn Elder, Gnome Tinker, Orc
 * Quartermaster, Tiefling Pactbroker) shares, held by the NPC ({@link SettlementMerchant#merchant()}): the
 * customer, the offers (made on first use, {@code Offers} in NBT), the gated stocks shown after them
 * ({@link KinStock}), the morning restock ({@code LastRestockDay}) and the right-click flow.
 * <ul>
 * <li>Right-click: a name tag or spawn egg goes to the mob; a merchant that's fighting says no; one the player
 * is Hostile with (or that doesn't serve them, {@link #serves}) refuses with {@code <refusalKey>} on the
 * action bar; otherwise the trade screen opens.</li>
 * <li>Prices, per customer: the reputation tier's share of each base price, then the kin discount
 * ({@link KinPrices}); cleared again when the customer leaves.</li>
 * <li>A customer who dies or walks off more than {@link #MAX_CUSTOMER_DISTANCE} blocks stops trading.</li>
 * <li>Every {@link #RESTOCK_CHECK_TICKS} ticks: on a new day every offer gets its uses back.</li>
 * <li>A trade counts for the faction ({@link FactionEvents#traded}) and drops 3-6 XP when vanilla would.</li>
 * </ul>
 * The NPC calls {@link #tick()} from {@code mobTick}, {@link #interact} from {@code interactMob}, and
 * {@link #write} / {@link #read} from its NBT methods.
 */
public final class NpcMerchant {
    public static final double MAX_CUSTOMER_DISTANCE = 8.0D;
    public static final int RESTOCK_CHECK_TICKS = 100;

    private final PathAwareEntity npc;
    private final Merchant asMerchant;
    private final Function<ServerWorld, TradeOfferList> factory;
    private final String refusalKey;
    private List<KinStock> stocks = List.of();
    private Predicate<PlayerEntity> serves = player -> true;
    private Function<PlayerEntity, List<TradeOffer>> extraOffers = player -> List.of();
    private Runnable onNewDay = () -> {
    };
    private BiConsumer<ServerPlayerEntity, TradeOffer> onTraded = (player, offer) -> {
    };
    private SoundEvent yesSound = SoundEvents.ENTITY_VILLAGER_YES;
    private float tradePitch = 1.0F;
    private SoundEvent busySound = SoundEvents.ENTITY_VILLAGER_NO;
    private SoundEvent refuseSound = SoundEvents.ENTITY_VILLAGER_NO;

    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    /** The offers plus the stocks and extras the current customer may see, while they trade; null otherwise. */
    @Nullable
    private TradeOfferList shown;
    private long lastRestockDay = Long.MIN_VALUE;

    /**
     * @param factory    the normal offers, made on the server the first time they're needed
     * @param refusalKey the action-bar line ({@code %s} the NPC's name) for a player it won't serve
     */
    public <T extends PathAwareEntity & Merchant> NpcMerchant(T npc, Function<ServerWorld, TradeOfferList> factory,
                                                              String refusalKey) {
        this.npc = npc;
        this.asMerchant = npc;
        this.factory = factory;
        this.refusalKey = refusalKey;
    }

    // ---- Set-up ----

    /** Gated offers shown after the normal ones to their own audience (kin stock, class stock). */
    public NpcMerchant stocks(KinStock... stocks) {
        this.stocks = List.of(stocks);
        return this;
    }

    /** Players this merchant trades with at all (besides the Hostile refusal). Everyone by default. */
    public NpcMerchant serves(Predicate<PlayerEntity> serves) {
        this.serves = serves;
        return this;
    }

    /** Offers shown last, to some customers only (the innkeeper's Honored round). Made on the server. */
    public NpcMerchant extraOffers(Function<PlayerEntity, List<TradeOffer>> extraOffers) {
        this.extraOffers = extraOffers;
        return this;
    }

    /** Runs on each new day's restock check, after the offers got their uses back. */
    public NpcMerchant onNewDay(Runnable onNewDay) {
        this.onNewDay = onNewDay;
        return this;
    }

    /** Runs for each trade a player makes, after it counted for the faction. */
    public NpcMerchant onTraded(BiConsumer<ServerPlayerEntity, TradeOffer> onTraded) {
        this.onTraded = onTraded;
        return this;
    }

    /**
     * The merchant's voice: {@code yes} on a trade (at {@code tradePitch} times its pitch), {@code busy} when
     * it's fighting, {@code refuse} when it won't serve. A villager's by default.
     */
    public NpcMerchant sounds(SoundEvent yes, float tradePitch, SoundEvent busy, SoundEvent refuse) {
        this.yesSound = yes;
        this.tradePitch = tradePitch;
        this.busySound = busy;
        this.refuseSound = refuse;
        return this;
    }

    // ---- State ----

    @Nullable
    public PlayerEntity customer() {
        return this.customer;
    }

    /** The normal offers if they've been made (or loaded), else null. */
    @Nullable
    public TradeOfferList savedOffers() {
        return this.offers;
    }

    /** The normal offers, made on first use (empty on the client). */
    public TradeOfferList baseOffers() {
        if (this.offers == null) {
            this.offers = this.npc.getWorld() instanceof ServerWorld world ? this.factory.apply(world) : new TradeOfferList();
        }
        return this.offers;
    }

    /** What the current customer is shown: the normal offers, plus the stocks and extras meant for them. */
    public TradeOfferList offers() {
        return this.shown != null ? this.shown : this.baseOffers();
    }

    public SoundEvent yesSound() {
        return this.yesSound;
    }

    public boolean isClient() {
        return this.npc.getWorld().isClient;
    }

    // ---- Customers ----

    public void setCustomer(@Nullable PlayerEntity customer) {
        if (customer == null) {
            if (this.offers != null) {
                this.offers.forEach(TradeOffer::clearSpecialPrice);
            }
            this.stocks.forEach(KinStock::clearSpecialPrices);
            if (this.shown != null) {
                this.shown.forEach(TradeOffer::clearSpecialPrice);
            }
            this.shown = null;
        } else if (!this.isClient()) {
            TradeOfferList base = this.baseOffers();
            TradeOfferList all = null;
            for (KinStock stock : this.stocks) {
                if (stock.shownTo(customer, this.npc) && !stock.offers().isEmpty()) {
                    all = all != null ? all : copyOf(base);
                    all.addAll(stock.offers());
                }
            }
            List<TradeOffer> extra = this.extraOffers.apply(customer);
            if (!extra.isEmpty()) {
                all = all != null ? all : copyOf(base);
                all.addAll(extra);
            }
            this.shown = all;
        }
        this.customer = customer;
    }

    private static TradeOfferList copyOf(TradeOfferList offers) {
        TradeOfferList copy = new TradeOfferList();
        copy.addAll(offers);
        return copy;
    }

    /**
     * The usual right-click: {@code mobInteract} is the mob's own {@code interactMob}, for a name tag or a
     * spawn egg.
     */
    public ActionResult interact(PlayerEntity player, Hand hand, Supplier<ActionResult> mobInteract) {
        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) {
            return mobInteract.get();
        }
        if (hand != Hand.MAIN_HAND || !this.npc.isAlive() || this.customer != null) {
            return ActionResult.PASS;
        }
        if (this.npc.getTarget() != null) {
            this.npc.playSound(this.busySound, 1.0F, this.npc.getSoundPitch());
            return ActionResult.success(this.isClient());
        }
        if (!this.isClient()) {
            if (this.refuses(player)) {
                return ActionResult.CONSUME;
            }
            this.open(player);
        }
        return ActionResult.success(this.isClient());
    }

    /** Hostile with the merchant's faction, or not served here: says so and returns true. */
    public boolean refuses(PlayerEntity player) {
        if (!TierEffects.refusesService(TierEffects.tierWith(player, this.npc)) && this.serves.test(player)) {
            return false;
        }
        player.sendMessage(Text.translatable(this.refusalKey, this.npc.getDisplayName()).formatted(Formatting.RED), true);
        this.npc.playSound(this.refuseSound, 1.0F, this.npc.getSoundPitch());
        return true;
    }

    /** Opens the trade screen for {@code player} with their prices. */
    public void open(PlayerEntity player) {
        this.setCustomer(player);
        this.preparePrices(player);
        this.asMerchant.sendOffers(player, this.npc.getDisplayName(), 1);
    }

    /**
     * Sets this customer's special prices: cleared, then each price modifier adds its share of the base price,
     * so they stack (the reputation tier, then the kin discount).
     */
    public void preparePrices(PlayerEntity player) {
        TradeOfferList offers = this.offers();
        offers.forEach(TradeOffer::clearSpecialPrice);
        ReputationTier tier = TierEffects.tierWith(player, this.npc);
        for (TradeOffer offer : offers) {
            offer.increaseSpecialPrice(TierEffects.reputationPriceDelta(tier, offer.getOriginalFirstBuyItem().getCount()));
        }
        KinPrices.apply(this.npc, player, offers);
    }

    public void trade(TradeOffer offer) {
        offer.use();
        if (this.customer instanceof ServerPlayerEntity player) {
            FactionEvents.traded(player, this.npc);
            this.onTraded.accept(player, offer);
        }
        this.npc.playSound(this.yesSound, 1.0F, this.npc.getSoundPitch() * this.tradePitch);
        if (offer.shouldRewardPlayerExperience()) {
            this.npc.getWorld().spawnEntity(new ExperienceOrbEntity(this.npc.getWorld(), this.npc.getX(),
                    this.npc.getY() + 0.5D, this.npc.getZ(), 3 + this.npc.getRandom().nextInt(4)));
        }
    }

    // ---- Ticking and saving ----

    /** From the NPC's {@code mobTick}: keeps the customer in view and restocks each morning. */
    public void tick() {
        if (this.customer != null) {
            this.npc.getNavigation().stop();
            this.npc.getLookControl().lookAt(this.customer);
            if (!this.customer.isAlive()
                    || this.customer.squaredDistanceTo(this.npc) > MAX_CUSTOMER_DISTANCE * MAX_CUSTOMER_DISTANCE) {
                this.setCustomer(null);
            }
        }
        if (this.npc.age % RESTOCK_CHECK_TICKS == 0) {
            this.restockIfNewDay();
        }
    }

    private void restockIfNewDay() {
        long today = GameTime.day(this.npc.getWorld());
        if (today != this.lastRestockDay) {
            this.lastRestockDay = today;
            if (this.offers != null) {
                this.offers.forEach(TradeOffer::resetUses);
                this.stocks.forEach(KinStock::restock);
            }
            this.onNewDay.run();
        }
    }

    public void write(NbtCompound nbt) {
        nbt.putLong("LastRestockDay", this.lastRestockDay);
        if (this.offers != null) {
            nbt.put("Offers", this.offers.toNbt());
        }
        this.stocks.forEach(stock -> stock.write(nbt));
    }

    public void read(NbtCompound nbt) {
        if (nbt.contains("LastRestockDay")) {
            this.lastRestockDay = nbt.getLong("LastRestockDay");
        }
        if (nbt.contains("Offers")) {
            this.offers = new TradeOfferList(nbt.getCompound("Offers"));
        }
        this.stocks.forEach(stock -> stock.read(nbt));
    }
}
