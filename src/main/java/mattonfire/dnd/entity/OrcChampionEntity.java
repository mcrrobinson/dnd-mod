package mattonfire.dnd.entity;

import java.util.UUID;
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
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Orc Champion who answers the war drum in the Trial of Strength ({@link OrcTrial}): 40 health, 6 armour,
 * 8 damage (an iron axe). It fights only its duelist, and while the duel lasts its health never drops below
 * {@link OrcTrial#FLOOR}: reaching it means it yields. It isn't a member of the horde for reputation, so the
 * duel's blows cost no standing.
 */
public class OrcChampionEntity extends OrcEntity {
    @Nullable
    private UUID duelist;

    public OrcChampionEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createOrcChampionAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getCustomName() != null) {
            this.setCustomName(Text.translatable("entity.dndclasses.orc_champion.named", this.getCustomName()));
        }
        // The iron axe's +8 is all of its 8 damage.
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.0F);
        }
        return data;
    }

    public void setDuelist(@Nullable UUID duelist) {
        this.duelist = duelist;
    }

    public boolean inDuel() {
        return this.duelist != null;
    }

    public boolean isDuelist(PlayerEntity player) {
        return this.duelist != null && this.duelist.equals(player.getUuid());
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        // A Champion whose duel is gone (the server restarted mid-duel) climbs back out of the pit.
        if (this.duelist != null && this.age % 20 == 0 && !OrcTrial.isChampion(this)) {
            this.discard();
        }
    }

    /** The non-lethal clamp: during the duel the Champion yields at {@link OrcTrial#FLOOR} instead of dying. */
    @Override
    public void setHealth(float health) {
        super.setHealth(this.duelist != null ? Math.max(health, OrcTrial.FLOOR) : health);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.duelist != null) {
            nbt.putUuid("Duelist", this.duelist);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.duelist = nbt.containsUuid("Duelist") ? nbt.getUuid("Duelist") : null;
    }
}
