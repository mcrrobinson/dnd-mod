package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.tavern.BountyNoticeItem;
import mattonfire.dnd.tavern.BountyRewards;
import mattonfire.dnd.tavern.RoomKeyItem;
import mattonfire.dnd.tavern.Tavern;
import mattonfire.dnd.tavern.TavernRooms;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import mattonfire.dnd.world.GameTime;

/**
 * The hobbit who keeps the village inn. Stays behind the bar, sells food and drink for emeralds
 * (and buys the village's produce), restocking every morning, and pays out finished bounty
 * notices from the tavern's bounty board. It also rents rooms: a Room Key for any bed inside its inn
 * ({@link #setInn}), good until the next dawn ({@link TavernRooms}).
 */
public class InnkeeperEntity extends HobbitEntity implements SettlementMerchant {
    /** Command tag that makes an NPC a quest giver with the innkeeper's dialogue (see {@code DialogueManager}). */
    public static final String ROLE_TAG = "dndclasses.role.innkeeper";

    /** The inn a Room Key from this innkeeper opens; null for one placed by hand (see {@link #innBox}). */
    @Nullable
    private BlockBox innBox;
    @Nullable
    private String innName;
    /** Real uses of the Room Key offer while it's shown sold out to this customer; -1 when it isn't. */
    private int roomKeyUsesHeld = -1;
    /** Honored with the hobbits: the extra trade (1 emerald: 3 ale, and a cake on the house). */
    @Nullable
    private TradeOffer honoredOffer;
    /** The Halfling Sling, for Halflings only; the Honored round last, for Honored customers only. */
    private final NpcMerchant merchant = new NpcMerchant(this, world -> InnkeeperTrades.create(),
            "entity.dndclasses.innkeeper.refuses")
            .stocks(new KinStock(KinStockOffers::halflingInnkeeper))
            .extraOffers(customer -> TierEffects.innkeeperFavours(TierEffects.tierWith(customer, this))
                    ? List.of(this.honoredOffer()) : List.of())
            .onNewDay(() -> {
                if (this.honoredOffer != null) {
                    this.honoredOffer.resetUses();
                }
            })
            .onTraded(this::traded);

    public InnkeeperEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public NpcMerchant merchant() {
        return this.merchant;
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
        this.merchant.tick();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient) {
            // Today's adventurers for hire (runs with NoAI too, so a parked innkeeper still keeps them).
            mattonfire.dnd.entity.hireling.HirelingRoster.tick(this);
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

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) {
            return super.interactMob(player, hand);
        }
        if (BountyNoticeItem.bounty(held) != null) {
            if (!this.world.isClient) {
                this.getLookControl().lookAt(player);
                if (this.merchant.refuses(player)) {
                    return ActionResult.CONSUME;
                }
                boolean paid = BountyRewards.claim((ServerPlayerEntity) player, held, this.getPos());
                this.playSound(paid ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            }
            return ActionResult.success(this.world.isClient);
        }
        if (hand != Hand.MAIN_HAND || !this.isAlive() || this.merchant.customer() != null) {
            return ActionResult.PASS;
        }
        // Identification for emeralds: sneak, emeralds in hand, the unidentified item in the offhand
        if (player.isSneaking() && held.isOf(Items.EMERALD)) {
            if (!this.world.isClient) {
                this.getLookControl().lookAt(player);
                if (this.merchant.refuses(player)) {
                    return ActionResult.CONSUME;
                }
                if (mattonfire.dnd.magic.Identify.innkeeperService((ServerPlayerEntity) player)) {
                    this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
                }
            }
            return ActionResult.success(this.world.isClient);
        }
        if (!this.world.isClient) {
            // Hostile with the hobbits: no trade, no bounty payout.
            if (this.merchant.refuses(player)) {
                return ActionResult.CONSUME;
            }
            this.merchant.setCustomer(player);
            this.merchant.preparePrices(player);
            this.prepareRoomFor(player);
            this.sendOffers(player, this.getDisplayName(), 1);
        }
        return ActionResult.success(this.world.isClient);
    }

    /**
     * The room for the night, once a customer's prices are set: Honored with the hobbits, a free one; already
     * rented today, the Room Key is shown sold out.
     */
    private void prepareRoomFor(PlayerEntity player) {
        if (!TavernRooms.rentedToday(player) && player instanceof ServerPlayerEntity serverPlayer
                && TierEffects.innkeeperFavours(TierEffects.tierWith(player, this))) {
            this.giveFreeRoom(serverPlayer);
        } else if (TavernRooms.rentedToday(player)) {
            this.holdRoomKeyOffer();
            player.sendMessage(Text.translatable("tavern.dndclasses.room.paid").formatted(Formatting.YELLOW), false);
        }
    }

    /**
     * Honored with the hobbits: a night's room on the house. The innkeeper hands over a Room Key stamped for
     * today (one a day, like a bought one), so the long rest at the inn costs nothing.
     */
    private void giveFreeRoom(ServerPlayerEntity player) {
        ItemStack key = new ItemStack(Tavern.ROOM_KEY);
        RoomKeyItem.stamp(key, this.innName(), this.innBox(), GameTime.day(this.world));
        TavernRooms.markRented(player);
        this.holdRoomKeyOffer();
        player.getInventory().offerOrDrop(key);
        player.sendMessage(Text.translatable("tavern.dndclasses.room.free", this.getDisplayName())
                .formatted(Formatting.GOLD), false);
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[Reputation] {} gave {} a free room (Honored with the hobbits)",
                this.getName().getString(), player.getEntityName());
    }

    private TradeOffer honoredOffer() {
        if (this.honoredOffer == null) {
            this.honoredOffer = InnkeeperTrades.honoredRound();
        }
        return this.honoredOffer;
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
        if (customer == null && this.merchant.savedOffers() != null) {
            this.releaseRoomKeyOffer();
        }
        this.merchant.setCustomer(customer);
    }

    /** The innkeeper's own part of a trade: the cake with an Honored round, and the room for a Room Key. */
    private void traded(ServerPlayerEntity player, TradeOffer offer) {
        if (offer == this.honoredOffer) {
            // The round comes with a cake, on the house.
            ItemStack cake = new ItemStack(Items.CAKE);
            player.getInventory().offerOrDrop(cake);
            player.sendMessage(Text.translatable("entity.dndclasses.innkeeper.honored_round").formatted(Formatting.GOLD), true);
        }
        if (InnkeeperTrades.isRoomKey(offer)) {
            TavernRooms.markRented(player);
            this.holdRoomKeyOffer();
            player.sendTradeOffers(player.currentScreenHandler.syncId, this.getOffers(), 1, this.getExperience(),
                    this.isLeveledMerchant(), this.canRefreshTrades());
        }
    }

    @Override
    public void onSellingItem(ItemStack stack) {
        // The key in the output slot is the one the customer takes: write the inn and today on it.
        if (!this.world.isClient && stack.isOf(Tavern.ROOM_KEY)) {
            RoomKeyItem.stamp(stack, this.innName(), this.innBox(), GameTime.day(this.world));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        // Not the per-customer "sold out" Room Key offer: save its real uses.
        boolean held = this.roomKeyUsesHeld >= 0;
        if (held) {
            this.releaseRoomKeyOffer();
        }
        this.merchant.write(nbt);
        if (held) {
            this.holdRoomKeyOffer();
        }
        if (this.honoredOffer != null) {
            nbt.put("HonoredOffer", this.honoredOffer.toNbt());
        }
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
        this.merchant.read(nbt);
        if (this.merchant.savedOffers() != null) {
            // Innkeepers from before rooms were for rent or thatch was for sale.
            InnkeeperTrades.addMissing(this.merchant.savedOffers());
        }
        if (nbt.contains("HonoredOffer")) {
            this.honoredOffer = new TradeOffer(nbt.getCompound("HonoredOffer"));
        }
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
