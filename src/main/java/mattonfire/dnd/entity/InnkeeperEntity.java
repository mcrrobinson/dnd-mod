package mattonfire.dnd.entity;

import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.tavern.BountyNoticeItem;
import mattonfire.dnd.tavern.BountyRewards;
import mattonfire.dnd.tavern.RoomKeyItem;
import mattonfire.dnd.tavern.Tavern;
import mattonfire.dnd.tavern.TavernRooms;
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
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
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
 * notices from the tavern's bounty board. It also rents rooms: a Room Key for any bed inside its inn
 * ({@link #setInn}), good until the next dawn ({@link TavernRooms}).
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
    /** The inn a Room Key from this innkeeper opens; null for one placed by hand (see {@link #innBox}). */
    @Nullable
    private BlockBox innBox;
    @Nullable
    private String innName;
    /** Real uses of the Room Key offer while it's shown sold out to this customer; -1 when it isn't. */
    private int roomKeyUsesHeld = -1;
    /** The Halfling Sling, for Halflings only. */
    private final KinStock kinStock = new KinStock(KinStockOffers::halflingInnkeeper);

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

    /**
     * Sets the inn whose beds this innkeeper's Room Keys open, and its name for the key ("<name> Room
     * Key"). Structures that place an innkeeper call this (the hobbit village's Green Dragon does).
     */
    public void setInn(BlockBox box, String name) {
        this.innBox = box;
        this.innName = name;
    }

    /**
     * The inn a Room Key opens: the one set by {@link #setInn}, or for an innkeeper placed by hand,
     * the room around its home (8 blocks across each way, 3 down and 8 up).
     */
    public BlockBox innBox() {
        if (this.innBox != null) {
            return this.innBox;
        }
        BlockPos home = this.hasPositionTarget() ? this.getPositionTarget() : this.getBlockPos();
        return new BlockBox(home.getX() - 8, home.getY() - 3, home.getZ() - 8, home.getX() + 8, home.getY() + 8,
                home.getZ() + 8);
    }

    public String innName() {
        return this.innName != null ? this.innName : "Tavern";
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
        // Identification for emeralds: sneak, emeralds in hand, the unidentified item in the offhand
        if (player.isSneaking() && held.isOf(Items.EMERALD)) {
            if (!this.world.isClient) {
                this.getLookControl().lookAt(player);
                if (this.refusesHostile(player)) {
                    return ActionResult.CONSUME;
                }
                if (mattonfire.dnd.magic.Identify.innkeeperService((ServerPlayerEntity) player)) {
                    this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
                }
            }
            return ActionResult.success(this.world.isClient);
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
        if (TavernRooms.rentedToday(player)) {
            this.holdRoomKeyOffer();
            player.sendMessage(Text.translatable("tavern.dndclasses.room.paid").formatted(Formatting.YELLOW), false);
        }
    }

    @Nullable
    private TradeOffer roomKeyOffer() {
        for (TradeOffer offer : this.getOffers()) {
            if (InnkeeperTrades.isRoomKey(offer)) {
                return offer;
            }
        }
        return null;
    }

    /** One room a day per player: shows the Room Key sold out to this customer. */
    private void holdRoomKeyOffer() {
        TradeOffer offer = this.roomKeyOffer();
        if (offer != null && this.roomKeyUsesHeld < 0) {
            this.roomKeyUsesHeld = offer.getUses();
            offer.disable();
        }
    }

    /** Puts the Room Key offer back as it was, when the customer leaves. */
    private void releaseRoomKeyOffer() {
        TradeOffer offer = this.roomKeyOffer();
        if (offer != null && this.roomKeyUsesHeld >= 0) {
            offer.resetUses();
            for (int i = 0; i < this.roomKeyUsesHeld; i++) {
                offer.use();
            }
        }
        this.roomKeyUsesHeld = -1;
    }

    // ---- Merchant ----

    @Override
    public void setCustomer(@Nullable PlayerEntity customer) {
        if (customer == null && this.offers != null) {
            this.offers.forEach(TradeOffer::clearSpecialPrice);
            this.releaseRoomKeyOffer();
        }
        if (!this.world.isClient) {
            this.kinStock.setCustomer(this, customer, this.baseOffers());
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

    /** What the innkeeper shows the current customer: the normal offers, plus kin stock for Halflings. */
    @Override
    public TradeOfferList getOffers() {
        return this.kinStock.view(this.baseOffers());
    }

    private TradeOfferList baseOffers() {
        if (this.offers == null) {
            this.offers = InnkeeperTrades.create();
        } else {
            // Innkeepers from before rooms were for rent.
            InnkeeperTrades.addMissing(this.offers);
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
            if (InnkeeperTrades.isRoomKey(offer)) {
                TavernRooms.markRented(player);
                this.holdRoomKeyOffer();
                player.sendTradeOffers(player.currentScreenHandler.syncId, this.getOffers(), 1, this.getExperience(),
                        this.isLeveledMerchant(), this.canRefreshTrades());
            }
        }
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
        if (offer.shouldRewardPlayerExperience()) {
            this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.getX(), this.getY() + 0.5D, this.getZ(),
                    3 + this.random.nextInt(4)));
        }
    }

    @Override
    public void onSellingItem(ItemStack stack) {
        // The key in the output slot is the one the customer takes: write the inn and today on it.
        if (!this.world.isClient && stack.isOf(Tavern.ROOM_KEY)) {
            RoomKeyItem.stamp(stack, this.innName(), this.innBox(), this.world.getTimeOfDay() / 24000L);
        }
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
            // Not the per-customer "sold out" Room Key offer: save its real uses.
            int held = this.roomKeyUsesHeld;
            this.releaseRoomKeyOffer();
            nbt.put("Offers", this.offers.toNbt());
            if (held >= 0) {
                this.holdRoomKeyOffer();
            }
        }
        this.kinStock.write(nbt);
        if (this.innBox != null) {
            nbt.putIntArray("InnBox", new int[]{this.innBox.getMinX(), this.innBox.getMinY(), this.innBox.getMinZ(),
                    this.innBox.getMaxX(), this.innBox.getMaxY(), this.innBox.getMaxZ()});
        }
        if (this.innName != null) {
            nbt.putString("InnName", this.innName);
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
        this.kinStock.read(nbt);
        int[] box = nbt.getIntArray("InnBox");
        if (box.length == 6) {
            this.innBox = new BlockBox(box[0], box[1], box[2], box[3], box[4], box[5]);
        }
        if (nbt.contains("InnName")) {
            this.innName = nbt.getString("InnName");
        }
        // Innkeepers spawned before roles existed (command tags are read before this).
        this.addCommandTag(ROLE_TAG);
    }
}
