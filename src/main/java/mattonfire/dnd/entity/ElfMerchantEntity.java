package mattonfire.dnd.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
public class ElfMerchantEntity extends ElfEntity implements SettlementMerchant {
    /** Command tag on the Speaker (faction files match it as {@code @dndclasses.role.elf_speaker}). */
    public static final String SPEAKER_TAG = "dndclasses.role.elf_speaker";

    /** The Speaker's Elven Chain, for Elves only. The Fletcher has none. */
    private final KinStock kinStock = new KinStock(
            () -> this.isSpeaker() ? KinStockOffers.elvenSpeaker() : new TradeOfferList());
    private final NpcMerchant merchant = new NpcMerchant(this,
            world -> this.isSpeaker() ? ElfTrades.speaker(world, this.getBlockPos(), this.random) : ElfTrades.fletcher(this.random),
            "entity.dndclasses.elf_merchant.refuses")
            .stocks(this.kinStock)
            .onNewDay(this::newDailyBook);

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
    public NpcMerchant merchant() {
        return this.merchant;
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
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(this.isSpeaker() ? Items.BOOK : Items.ARROW));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
        this.merchant.tick();
    }

    /** Each morning the Speaker has a new enchanted book. */
    private void newDailyBook() {
        TradeOfferList offers = this.merchant.savedOffers();
        if (offers != null && this.isSpeaker()) {
            ElfTrades.newDailyBook(offers, this.random);
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        return this.merchant.interact(player, hand, () -> super.interactMob(player, hand));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.merchant.write(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.merchant.read(nbt);
        if (this.merchant.savedOffers() != null) {
            ElfTrades.addMissing(this.merchant.savedOffers(), this.isSpeaker());
        }
    }
}
