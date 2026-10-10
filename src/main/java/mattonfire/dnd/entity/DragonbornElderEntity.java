package mattonfire.dnd.entity;

import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
 * The Dragonborn Elder of a Dragon Temple: merchant and quest giver (role {@code dragonborn_elder}), who keeps
 * to the sanctum. The wares ({@link DragonbornTrades}) restock every morning; Dragonborn pay kin prices
 * ({@link KinPrices}).
 */
public class DragonbornElderEntity extends DragonbornEntity implements Merchant {
    private static final double MAX_CUSTOMER_DISTANCE = 8.0D;
    /**
     * Command tag on the Elder: the quest-giver role for dialogue and quests, and the faction file matches
     * it as {@code @dndclasses.role.dragonborn_elder}.
     */
    public static final String ELDER_TAG = "dndclasses.role.dragonborn_elder";

    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    private long lastRestockDay = Long.MIN_VALUE;
    /** Draconic Scale, for Dragonborn only. */
    private final KinStock kinStock = new KinStock(KinStockOffers::dragonTemple);

    public DragonbornElderEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createElderAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.dragonborn_elder.named", this.getCustomName()));
        }
        this.addCommandTag(ELDER_TAG);
        this.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
        this.setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0.0F);
        return data;
    }

    // The Elder keeps to the sanctum.
    @Override
    protected int dayRange() {
        return 4;
    }

    @Override
    protected int nightRange() {
        return 4;
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
                this.kinStock.restock();
            }
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(Items.NAME_TAG) || held.getItem() instanceof net.minecraft.item.SpawnEggItem) {
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
                player.sendMessage(Text.translatable("entity.dndclasses.dragonborn_elder.refuses", this.getDisplayName())
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

    /**
     * Sets this customer's special prices: cleared, then each price modifier adds its share of the base price
     * (the reputation tier, then the kin discount), as at the hobbit innkeeper.
     */
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

    /** What the Elder shows the current customer: the normal offers, plus Draconic Scale for Dragonborn. */
    @Override
    public TradeOfferList getOffers() {
        return this.kinStock.view(this.baseOffers());
    }

    private TradeOfferList baseOffers() {
        if (this.offers == null) {
            this.offers = this.world instanceof ServerWorld serverWorld
                    ? DragonbornTrades.elder(serverWorld, this.getBlockPos(), this.random)
                    : new TradeOfferList();
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
