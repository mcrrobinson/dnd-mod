package mattonfire.dnd.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An Orc Warrior of the hold: 30 health, 4 armour, 7 damage plus its stone or iron axe, an iron helmet 40%
 * of the time. Roams inside the palisade.
 */
public class OrcWarriorEntity extends OrcEntity {
    public OrcWarriorEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createOrcWarriorAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.2D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(this.random.nextBoolean() ? Items.IRON_AXE : Items.STONE_AXE));
        if (this.random.nextFloat() < 0.4F) {
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setEquipmentDropChance(slot, 0.05F);
        }
        return data;
    }
}
