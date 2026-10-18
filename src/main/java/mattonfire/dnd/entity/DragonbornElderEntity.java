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
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Dragonborn Elder of a Dragon Temple: merchant and quest giver (role {@code dragonborn_elder}), who keeps
 * to the sanctum. The wares ({@link DragonbornTrades}) restock every morning; Dragonborn pay kin prices
 * ({@link KinPrices}).
 */
public class DragonbornElderEntity extends DragonbornEntity implements SettlementMerchant {
    /**
     * Command tag on the Elder: the quest-giver role for dialogue and quests, and the faction file matches
     * it as {@code @dndclasses.role.dragonborn_elder}.
     */
    public static final String ELDER_TAG = "dndclasses.role.dragonborn_elder";

    /** Draconic Scale, for Dragonborn only. */
    private final NpcMerchant merchant = new NpcMerchant(this,
            world -> DragonbornTrades.elder(world, this.getBlockPos(), this.random),
            "entity.dndclasses.dragonborn_elder.refuses")
            .stocks(new KinStock(KinStockOffers::dragonTemple));

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
    public NpcMerchant merchant() {
        return this.merchant;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.dragonborn_elder.named", this.getCustomName()));
        }
        this.addCommandTag(ELDER_TAG);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
        this.merchant.tick();
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
            DragonbornTrades.addMissing(this.merchant.savedOffers());
        }
    }
}
