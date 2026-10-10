package mattonfire.dnd.entity;

import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.tavern.BountyNoticeItem;
import mattonfire.dnd.tavern.BountyRewards;
import mattonfire.dnd.tavern.Tavern;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
 * The hobbit who keeps the village inn. Stays behind the bar, sells food and drink for emeralds
 * (and buys the village's produce), restocking every morning, and pays out finished bounty
 * notices from the tavern's bounty board.
 */
public class InnkeeperEntity extends HobbitEntity implements Merchant {
    /** Command tag that makes an NPC a quest giver with the innkeeper's dialogue (see {@code DialogueManager}). */
    public static final String ROLE_TAG = "dndclasses.role.innkeeper";
    private static final double MAX_CUSTOMER_DISTANCE = 8.0D;

    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    private long lastRestockDay = Long.MIN_VALUE;

    public InnkeeperEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.innkeeper.named", this.getCustomName()));
        }
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Tavern.ALE));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.addCommandTag(ROLE_TAG);
        return data;
    }

    // The innkeeper keeps to the bar.
    @Override
    protected int dayRange() {
        return 4;
    }

    @Override
    protected int nightRange() {
        return 3;
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
            }
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(Items.NAME_TAG) || held.getItem() instanceof net.minecraft.item.SpawnEggItem) {
            return super.interactMob(player, hand);
        }
        if (BountyNoticeItem.bounty(held) != null) {
            if (!this.world.isClient) {
                this.getLookControl().lookAt(player);
                if (this.refusesHostile(player)) {
                    return ActionResult.CONSUME;
                }
                boolean paid = BountyRewards.claim((ServerPlayerEntity) player, held, this.getPos());
                this.playSound(paid ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            }
            return ActionResult.success(this.world.isClient);
        }
        if (hand != Hand.MAIN_HAND || !this.isAlive() || this.customer != null) {
            return ActionResult.PASS;
        }
        if (!this.world.isClient) {
            if (this.refusesHostile(player)) {
                return ActionResult.CONSUME;
            }
            this.setCustomer(player);
            this.prepareOffersFor(player);
            this.sendOffers(player, this.getDisplayName(), 1);
        }
        return ActionResult.success(this.world.isClient);
    }

    /** Hostile with the hobbits: no trade, no bounty payout. */
    private boolean refusesHostile(PlayerEntity player) {
        if (!TierEffects.refusesService(TierEffects.tierWith(player, this))) {
            return false;
        }
        player.sendMessage(Text.translatable("entity.dndclasses.innkeeper.refuses", this.getDisplayName())
                .formatted(Formatting.RED), true);
        this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
        return true;
    }

    /**
     * Sets this customer's special prices: cleared, then each price modifier adds its share of the base
     * price, so they stack (reputation tier, then the kin discount).
     */
    private void prepareOffersFor(PlayerEntity player) {
        TradeOfferList offers = this.getOffers();
        offers.forEach(TradeOffer::clearSpecialPrice);
        this.applyReputationPrices(player, offers);
        KinPrices.apply(this, player, offers);
    }

    // ---- Merchant ----

    @Override
    public void setCustomer(@Nullable PlayerEntity customer) {
        if (customer == null && this.offers != null) {
            this.offers.forEach(TradeOffer::clearSpecialPrice);
        }
        this.customer = customer;
    }

    /**
     * Reputation price modifier: each customer sees prices for their own standing with the hobbits
     * (Unfriendly +50%, Friendly -10%, Honored -25%, Exalted -40%), added to the offers' special price
     * the way vanilla villagers apply gossip. {@link #prepareOffersFor} clears the prices first, and they're
     * cleared again when the customer leaves.
     */
    private void applyReputationPrices(PlayerEntity customer, TradeOfferList offers) {
        ReputationTier tier = TierEffects.tierWith(customer, this);
        for (TradeOffer offer : offers) {
            offer.increaseSpecialPrice(TierEffects.reputationPriceDelta(tier, offer.getOriginalFirstBuyItem().getCount()));
        }
    }

    @Nullable
    @Override
    public PlayerEntity getCustomer() {
        return this.customer;
    }

    @Override
    public TradeOfferList getOffers() {
        if (this.offers == null) {
            this.offers = InnkeeperTrades.create();
        }
        return this.offers;
    }

    @Override
    public void setOffersFromServer(TradeOfferList offers) {
    }

    @Override
    public void trade(TradeOffer offer) {
        offer.use();
        if (this.getCustomer() instanceof net.minecraft.server.network.ServerPlayerEntity player) {
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
        // Innkeepers spawned before roles existed (command tags are read before this).
        this.addCommandTag(ROLE_TAG);
    }
}
