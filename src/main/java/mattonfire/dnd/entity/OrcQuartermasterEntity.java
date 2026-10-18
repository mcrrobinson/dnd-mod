package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.stronghold.BloodSworn;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
public class OrcQuartermasterEntity extends OrcEntity implements SettlementMerchant {
    public static final String ROLE_TAG = "dndclasses.role.orc_quartermaster";

    private final NpcMerchant merchant = new NpcMerchant(this, world -> OrcTrades.quartermaster(this.random),
            "entity.dndclasses.orc_quartermaster.refuses")
            .stocks(new KinStock(KinStockOffers::orcStronghold))
            .serves(this::servesPlayer)
            .sounds(SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT, 1.2F, SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY,
                    SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT);

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
    public NpcMerchant merchant() {
        return this.merchant;
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
            // Quartermasters from before Tanned Hide.
            OrcTrades.addMissing(this.merchant.savedOffers());
        }
    }
}
