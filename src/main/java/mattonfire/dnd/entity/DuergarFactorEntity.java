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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Duergar Factor, a Duergar Hold's merchant (dialogue role {@code duergar_factor}): trades gold for Underdark
 * goods with anyone not Hostile with the Grey Clans, at their tier's prices ({@link DuergarTrades}). Higher tiers
 * see more stock (Friendly: a Potion of Invisibility; Honored: the Duergar War Pick). 30 health, 4 armour, 3
 * damage; he never Enlarges, but does vanish when hurt.
 */
public class DuergarFactorEntity extends DuergarEntity implements SettlementMerchant {
    public static final String ROLE_TAG = "dndclasses.role.duergar_factor";

    private final KinStock friendlyStock = new KinStock(DuergarTrades::friendly, DuergarTrades::showsFriendly,
            "FriendlyOffers");
    private final NpcMerchant merchant = new NpcMerchant(this, world -> DuergarTrades.factor(),
            "entity.dndclasses.duergar_factor.refuses")
            .stocks(this.friendlyStock,
                    new KinStock(DuergarTrades::honored, DuergarTrades::showsHonored, "HonoredOffers"))
            .sounds(SoundEvents.ENTITY_VINDICATOR_AMBIENT, 1.1F, SoundEvents.ENTITY_VINDICATOR_HURT,
                    SoundEvents.ENTITY_VILLAGER_NO);

    public DuergarFactorEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createFactorAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected boolean canEnlarge() {
        return false;
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
            this.setCustomName(Text.translatable("entity.dndclasses.duergar_factor.named", this.getCustomName()));
        }
        this.addCommandTag(ROLE_TAG);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLD_INGOT));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return data;
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
            DuergarTrades.addMissing(this.merchant.savedOffers());
        }
        if (nbt.contains("FriendlyOffers")) {
            DuergarTrades.addMissingFriendly(this.friendlyStock.offers());
        }
    }
}
