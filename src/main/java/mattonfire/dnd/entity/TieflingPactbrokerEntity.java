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
 * The Pactbroker of a Brimstone Sanctum: merchant and quest giver (role {@code tiefling_pactbroker}), who keeps
 * to the Pact Hall. The wares ({@link TieflingTrades}) restock every morning; Tieflings pay kin prices
 * ({@link KinPrices}). Besides the normal offers it keeps three gated stocks, each shown after them only to its
 * own customers: Infernal Regalia for Tieflings, a Wither-set piece for Warlocks, a Fast Brewing Stand for
 * Alchemists.
 */
public class TieflingPactbrokerEntity extends TieflingEntity implements SettlementMerchant {
    /** Command tag: the quest-giver role, matched by the faction file as {@code @dndclasses.role.tiefling_pactbroker}. */
    public static final String ROLE_TAG = "dndclasses.role.tiefling_pactbroker";

    private final NpcMerchant merchant = new NpcMerchant(this, world -> TieflingTrades.pactbroker(),
            "entity.dndclasses.tiefling_pactbroker.refuses")
            .stocks(new KinStock(KinStockOffers::brimstoneSanctum),
                    new KinStock(() -> TieflingTrades.warlock(this.random), TieflingTrades::isWarlock, "WarlockOffers"),
                    new KinStock(TieflingTrades::alchemist, TieflingTrades::isAlchemist, "AlchemistOffers"))
            .extraOffers(customer -> TieflingTrades.bargains(customer, this))
            .onTraded(TieflingTrades::traded);

    public TieflingPactbrokerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createPactbrokerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
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
            this.setCustomName(Text.translatable("entity.dndclasses.tiefling_pactbroker.named", this.getCustomName()));
        }
        this.addCommandTag(ROLE_TAG);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
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
            TieflingTrades.addMissing(this.merchant.savedOffers());
        }
    }
}
