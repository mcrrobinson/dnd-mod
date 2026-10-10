package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.FactionEvents;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.stronghold.BloodSworn;
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
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Orc Quartermaster of a hold's smithy: merchant and quest giver (role {@code orc_quartermaster}). Trades
 * only with Half-Orcs and with players Blood-Sworn at his hold; everyone else is waved off. Wares in
 * {@link OrcTrades}; Half-Orcs pay kin prices and see the Orcish Warplate kin stock. 30 health, 4 armour,
 * 7 damage; he only fights those who wrong the hold.
 */
public class OrcQuartermasterEntity extends OrcEntity implements Merchant {
    private static final double MAX_CUSTOMER_DISTANCE = 8.0D;
    public static final String ROLE_TAG = "dndclasses.role.orc_quartermaster";

    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    private long lastRestockDay = Long.MIN_VALUE;
    private final KinStock kinStock = new KinStock(KinStockOffers::orcStronghold);

    public OrcQuartermasterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createOrcQuartermasterAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.orc_quartermaster.named", this.getCustomName()));
        }
        this.addCommandTag(ROLE_TAG);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return data;
    }

    /** Whether {@code player} may trade here: a Half-Orc, or Blood-Sworn at this hold. */
    public boolean servesPlayer(PlayerEntity player) {
        if (RaceLifecycle.activeRaceOf(player) == DndRace.HALFORC) {
            return true;
        }
        return this.world instanceof ServerWorld world && this.getHold() != NO_HOLD
                && BloodSworn.get(world).isSworn(this.getHold(), player.getUuid());
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
            long today = this.world.getTimeOfDay() / 24000L;
            if (today != this.lastRestockDay) {
                this.lastRestockDay = today;
                if (this.offers != null) {
                    this.offers.forEach(TradeOffer::resetUses);
                    this.kinStock.restock();
                }
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
            this.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, 1.0F, this.getSoundPitch());
            return ActionResult.success(this.world.isClient);
        }
        if (!this.world.isClient) {
            if (TierEffects.refusesService(TierEffects.tierWith(player, this)) || !this.servesPlayer(player)) {
                player.sendMessage(Text.translatable("entity.dndclasses.orc_quartermaster.refuses", this.getDisplayName())
                        .formatted(Formatting.RED), true);
                this.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT, 1.0F, this.getSoundPitch());
                return ActionResult.CONSUME;
            }
            this.setCustomer(player);
            this.prepareOffersFor(player);
            this.sendOffers(player, this.getDisplayName(), 1);
        }
        return ActionResult.success(this.world.isClient);
    }

    /** Special prices: cleared, then the reputation tier's share, then the kin discount. */
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
        if (customer == null && this.offers != null) {
            this.offers.forEach(TradeOffer::clearSpecialPrice);
        }
        if (!this.world.isClient) {
            this.kinStock.setCustomer(this, customer, this.baseOffers());
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
        return this.kinStock.view(this.baseOffers());
    }

    private TradeOfferList baseOffers() {
        if (this.offers == null) {
            this.offers = this.world.isClient ? new TradeOfferList() : OrcTrades.quartermaster(this.random);
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
            FactionEvents.traded(player, this);
        }
        this.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT, 1.0F, this.getSoundPitch() * 1.2F);
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
        return SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT;
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
        this.kinStock.write(nbt);
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
        this.kinStock.read(nbt);
    }
}
