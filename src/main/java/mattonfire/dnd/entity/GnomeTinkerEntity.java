package mattonfire.dnd.entity;

import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Gnome Tinker of Cogsdeep: a Small (0.65) gnome in an apron with brass goggles pushed up on the forehead, who
 * tinkers at a workshop bench and trades ({@link TinkerTrades}): spyglasses, clocks, compasses and redstone
 * components, and to Gnomes only (kin stock) the Gnomish Repeater and Tinker's Goggles. Every Tinker keeps a shop;
 * wares restock every morning and Gnomes pay kin prices ({@link KinPrices}). Tinkers never fight: they flee
 * monsters, and striking one brings every Clockwork Sentinel nearby down on you. Quest-giver role
 * {@value #ROLE_TAG}.
 */
public class GnomeTinkerEntity extends PathAwareEntity implements Merchant {
    /** Skins: fair, tan, ruddy, brown, pale (see tools/cogsdeep_textures.py). */
    public static final int VARIANTS = 5;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(GnomeTinkerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final double MAX_CUSTOMER_DISTANCE = 8.0D;
    /** Sentinels this close to a struck gnome come to its defence. */
    static final double DEFENCE_RANGE = 16.0D;
    /** Command tag: the quest-giver role, matched by the faction file as {@code @dndclasses.role.gnome_tinker}. */
    public static final String ROLE_TAG = "dndclasses.role.gnome_tinker";

    private static final String[] NAMES = {
            "Alston", "Boddynock", "Brocc", "Burgell", "Dimble", "Eldon", "Erky", "Fonkin", "Frug", "Gerbo", "Gimble",
            "Glim", "Jebeddo", "Kellen", "Namfoodle", "Orryn", "Roondar", "Seebo", "Sindri", "Warryn", "Wrenn", "Zook",
            "Bimpnottin", "Breena", "Caramip", "Carlin", "Donella", "Duvamil", "Ella", "Ellyjobell", "Ellywick", "Lilli",
            "Loopmottin", "Lorilla", "Mardnab", "Nissa", "Nyx", "Oda", "Orla", "Roywyn", "Shamil", "Tana", "Waywocket",
            "Zanna"
    };
    private static final String[] CLANS = {
            "Beren", "Daergel", "Folkor", "Garrick", "Nackle", "Murnig", "Ningel", "Raulnor", "Scheppen", "Timbers",
            "Turen", "Cogwhistle", "Sprocketson", "Gearloose"
    };

    @Nullable
    private BlockPos home;
    private int range = 8;
    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    private long lastRestockDay = Long.MIN_VALUE;
    private final KinStock kinStock = new KinStock(KinStockOffers::cogsdeep);

    public GnomeTinkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
    }

    public static DefaultAttributeContainer.Builder createTinkerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3D));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, HostileEntity.class, 8.0F, 0.9D, 1.25D));
        // Players Hostile with the Tinkers' Guild are kept at a distance.
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0F, 0.9D, 1.2D,
                player -> TierEffects.fleesFrom(TierEffects.tierWith((PlayerEntity) player, this))));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Math.floorMod(variant, VARIANTS));
    }

    /** Ties the Tinker to a spot it keeps within {@code range} of. */
    public void setHome(BlockPos home, int range) {
        this.home = home.toImmutable();
        this.range = range;
        this.setPositionTarget(this.home, this.range);
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        String name = NAMES[random.nextInt(NAMES.length)] + " " + CLANS[random.nextInt(CLANS.length)];
        this.setCustomName(Text.translatable("entity.dndclasses.gnome_tinker.named", name));
        this.addCommandTag(ROLE_TAG);
        this.setPersistent();
        if (this.home == null) {
            this.setHome(this.getBlockPos(), this.range);
        }
        ItemStack tool = new ItemStack(random.nextBoolean() ? Items.CLOCK : random.nextBoolean() ? Items.COMPASS : Items.SPYGLASS);
        this.equipStack(EquipmentSlot.MAINHAND, tool);
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // The mod gives every mob a player-targeting goal; Tinkers never pick fights.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null) {
            super.setTarget(null);
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !this.world.isClient && source.getAttacker() instanceof PlayerEntity player
                && !player.isCreative() && !player.isSpectator()) {
            defend(this, player);
        }
        return hurt;
    }

    /** Every Sentinel near {@code npc} turns on {@code player}, who struck a gnome or a Sentinel. */
    static void defend(Entity npc, PlayerEntity player) {
        for (ClockworkSentinelEntity sentinel : npc.world.getEntitiesByClass(ClockworkSentinelEntity.class,
                new Box(npc.getBlockPos()).expand(DEFENCE_RANGE), LivingEntity::isAlive)) {
            sentinel.provoke(player);
        }
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
            if (this.home != null) {
                this.setPositionTarget(this.home, this.range);
            }
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
        if (held.isOf(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) {
            return super.interactMob(player, hand);
        }
        if (hand != Hand.MAIN_HAND || !this.isAlive() || this.customer != null) {
            return ActionResult.PASS;
        }
        if (!this.world.isClient) {
            if (TierEffects.refusesService(TierEffects.tierWith(player, this))) {
                player.sendMessage(Text.translatable("entity.dndclasses.gnome_tinker.refuses", this.getDisplayName())
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

    /** Clears the special prices, then adds the reputation tier's share and the kin discount. */
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
        if (!this.world.isClient) {
            if (customer == null && this.offers != null) {
                this.offers.forEach(TradeOffer::clearSpecialPrice);
            }
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
            this.offers = this.world.isClient ? new TradeOfferList() : TinkerTrades.tinker();
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

    // ---- Sounds: a villager's, higher ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.35F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.customer != null ? null : SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
        nbt.putInt("HomeRange", this.range);
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
        nbt.putLong("LastRestockDay", this.lastRestockDay);
        if (this.offers != null) {
            nbt.put("Offers", this.offers.toNbt());
        }
        this.kinStock.write(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setVariant(nbt.getInt("Variant"));
        if (nbt.contains("HomeRange")) {
            this.range = nbt.getInt("HomeRange");
        }
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")), this.range);
        }
        if (nbt.contains("LastRestockDay")) {
            this.lastRestockDay = nbt.getLong("LastRestockDay");
        }
        if (nbt.contains("Offers")) {
            this.offers = new TradeOfferList(nbt.getCompound("Offers"));
        }
        this.kinStock.read(nbt);
    }
}
