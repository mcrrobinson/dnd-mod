package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Pactbroker of a Brimstone Sanctum: merchant and quest giver (role {@code tiefling_pactbroker}), who keeps
 * to the Pact Hall. The wares ({@link TieflingTrades}) restock every morning; Tieflings pay kin prices
 * ({@link KinPrices}). Besides the normal offers it keeps three gated stocks, each shown after them only to its
 * own customers: Infernal Regalia for Tieflings, a Wither-set piece for Warlocks, a Fast Brewing Stand for
 * Alchemists.
 */
public class TieflingPactbrokerEntity extends TieflingEntity implements Merchant {
    private static final double MAX_CUSTOMER_DISTANCE = 8.0D;
    /** Command tag: the quest-giver role, matched by the faction file as {@code @dndclasses.role.tiefling_pactbroker}. */
    public static final String ROLE_TAG = "dndclasses.role.tiefling_pactbroker";

    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    /** Normal offers plus the gated ones the current customer may see, while they trade. */
    @Nullable
    private TradeOfferList shown;
    private long lastRestockDay = Long.MIN_VALUE;
    private final KinStock kinStock = new KinStock(KinStockOffers::brimstoneSanctum);
    private final KinStock warlockStock = new KinStock(() -> TieflingTrades.warlock(this.random),
            TieflingTrades::isWarlock, "WarlockOffers");
    private final KinStock alchemistStock = new KinStock(TieflingTrades::alchemist, TieflingTrades::isAlchemist,
            "AlchemistOffers");

    public TieflingPactbrokerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createPactbrokerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    private List<KinStock> stocks() {
        return List.of(this.kinStock, this.warlockStock, this.alchemistStock);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.tiefling_pactbroker.named", this.getCustomName()));
        }
        this.addCommandTag(ROLE_TAG);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return data;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.customer != null) {
            this.getNavigation().stop();
            this.getLookControl().lookAt(this.customer);
            if (!this.customer.isAlive() || this.customer.squaredDistanceTo(this) > MAX_CUSTOMER_DISTANCE * MAX_CUSTOMER_DISTANCE) {
                this.setCustomer(null);
            }
        }
        if (this.age % 100 == 0) {
            this.restockIfNewDay();
        }
    }

    private void restockIfNewDay() {
        long today = this.world.getTimeOfDay() / 24000L;
        if (today != this.lastRestockDay) {
            this.lastRestockDay = today;
            if (this.offers != null) {
                this.offers.forEach(TradeOffer::resetUses);
                this.stocks().forEach(KinStock::restock);
            }
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) {
            return super.interactMob(player, hand);
        }
        if (hand != Hand.MAIN_HAND || !this.isAlive() || this.customer != null) {
            return ActionResult.PASS;
        }
        if (this.getTarget() != null) {
            this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            return ActionResult.success(this.world.isClient);
        }
        if (!this.world.isClient) {
            if (TierEffects.refusesService(TierEffects.tierWith(player, this))) {
                player.sendMessage(Text.translatable("entity.dndclasses.tiefling_pactbroker.refuses", this.getDisplayName())
                        .formatted(Formatting.RED), true);
                this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
                return ActionResult.CONSUME;
            }
            this.setCustomer(player);
            this.prepareOffersFor(player);
            this.sendOffers(player, this.getDisplayName(), 1);
        }
        return ActionResult.success(this.world.isClient);
    }

    /** Clears the special prices, then adds the reputation tier's share and the kin discount, as at the Elder. */
    private void prepareOffersFor(PlayerEntity player) {
        TradeOfferList offers = this.getOffers();
        offers.forEach(TradeOffer::clearSpecialPrice);
        ReputationTier tier = TierEffects.tierWith(player, this);
        for (TradeOffer offer : offers) {
            offer.increaseSpecialPrice(TierEffects.reputationPriceDelta(tier, offer.getOriginalFirstBuyItem().getCount()));
        }
        KinPrices.apply(this, player, offers);
    }

    // ---- Merchant ----

    @Override
    public void setCustomer(@Nullable PlayerEntity customer) {
        if (customer == null) {
            if (this.offers != null) {
                this.offers.forEach(TradeOffer::clearSpecialPrice);
            }
            for (KinStock stock : this.stocks()) {
                stock.offers().forEach(TradeOffer::clearSpecialPrice);
            }
            this.shown = null;
        } else if (!this.world.isClient) {
            TradeOfferList all = new TradeOfferList();
            all.addAll(this.baseOffers());
            for (KinStock stock : this.stocks()) {
                if (stock.shownTo(customer, this)) {
                    all.addAll(stock.offers());
                }
            }
            this.shown = all;
        }
        this.customer = customer;
    }

    @Nullable
    @Override
    public PlayerEntity getCustomer() {
        return this.customer;
    }

    @Override
    public TradeOfferList getOffers() {
        return this.shown != null ? this.shown : this.baseOffers();
    }

    private TradeOfferList baseOffers() {
        if (this.offers == null) {
            this.offers = this.world.isClient ? new TradeOfferList() : TieflingTrades.pactbroker();
        }
        return this.offers;
    }

    @Override
    public void setOffersFromServer(TradeOfferList offers) {
    }

    @Override
    public void trade(TradeOffer offer) {
        offer.use();
        if (this.getCustomer() instanceof ServerPlayerEntity player) {
            mattonfire.dnd.faction.FactionEvents.traded(player, this);
        }
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
        if (offer.shouldRewardPlayerExperience()) {
            this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.getX(), this.getY() + 0.5D, this.getZ(),
                    3 + this.random.nextInt(4)));
        }
    }

    @Override
    public void onSellingItem(ItemStack stack) {
    }

    @Override
    public int getExperience() {
        return 0;
    }

    @Override
    public void setExperienceFromServer(int experience) {
    }

    @Override
    public boolean isLeveledMerchant() {
        return false;
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.ENTITY_VILLAGER_YES;
    }

    @Override
    public boolean isClient() {
        return this.world.isClient;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putLong("LastRestockDay", this.lastRestockDay);
        if (this.offers != null) {
            nbt.put("Offers", this.offers.toNbt());
        }
        this.stocks().forEach(stock -> stock.write(nbt));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("LastRestockDay")) {
            this.lastRestockDay = nbt.getLong("LastRestockDay");
        }
        if (nbt.contains("Offers")) {
            this.offers = new TradeOfferList(nbt.getCompound("Offers"));
        }
        this.stocks().forEach(stock -> stock.read(nbt));
    }
}
