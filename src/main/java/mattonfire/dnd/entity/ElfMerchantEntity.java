package mattonfire.dnd.entity;

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
import net.minecraft.util.Hand;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An elven merchant: the Speaker, who keeps to the Speaker's Hall up in the Heart Tree, or the Fletcher,
 * who keeps to the archery glade. Their wares ({@link ElfTrades}) restock every morning; Elves pay kin
 * prices ({@link KinPrices}).
 */
public class ElfMerchantEntity extends ElfEntity implements Merchant {
    private static final double MAX_CUSTOMER_DISTANCE = 8.0D;
    /** Command tag on the Speaker (faction files match it as {@code @dndclasses.role.elf_speaker}). */
    public static final String SPEAKER_TAG = "dndclasses.role.elf_speaker";

    @Nullable
    private PlayerEntity customer;
    @Nullable
    private TradeOfferList offers;
    private long lastRestockDay = Long.MIN_VALUE;

    public ElfMerchantEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSpeakerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    public static DefaultAttributeContainer.Builder createFletcherAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    public boolean isSpeaker() {
        return this.getType() == ModEntityTypes.ELF_SPEAKER;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            String key = this.isSpeaker() ? "entity.dndclasses.elf_speaker.named" : "entity.dndclasses.elf_fletcher.named";
            this.setCustomName(Text.translatable(key, this.getCustomName()));
        }
        if (this.isSpeaker()) {
            this.addCommandTag(SPEAKER_TAG);
        }
        this.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND,
                new ItemStack(this.isSpeaker() ? Items.BOOK : Items.ARROW));
        this.setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0.0F);
        return data;
    }

    // Merchants keep to their hall or glade.
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
                if (this.isSpeaker()) {
                    ElfTrades.newDailyBook(this.offers, this.random);
                }
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
            this.setCustomer(player);
            this.prepareOffersFor(player);
            this.sendOffers(player, this.getDisplayName(), 1);
        }
        return ActionResult.success(this.world.isClient);
    }

    /** Sets this customer's special prices: cleared, then each price modifier adds its share. */
    private void prepareOffersFor(PlayerEntity player) {
        TradeOfferList offers = this.getOffers();
        offers.forEach(TradeOffer::clearSpecialPrice);
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

    @Nullable
    @Override
    public PlayerEntity getCustomer() {
        return this.customer;
    }

    @Override
    public TradeOfferList getOffers() {
        if (this.offers == null) {
            this.offers = this.isSpeaker() && this.world instanceof ServerWorld serverWorld
                    ? ElfTrades.speaker(serverWorld, this.getBlockPos(), this.random)
                    : ElfTrades.fletcher(this.random);
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
    }
}
