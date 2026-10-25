package mattonfire.dnd.entity.drow;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A Drow Warrior: 26 health, 4 armour. A hand crossbow at 6-14 blocks (a bolt every 50 ticks, 3 damage and Drow
 * Poison), a scimitar (5) within 4 blocks. One in five carries a shield.
 */
public class DrowWarriorEntity extends DrowEntity {
    public static final float SHIELD_CHANCE = 0.2F;

    public DrowWarriorEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createDrowWarriorAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new HandCrossbowGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.15D, true));
    }

    @Override
    protected boolean hasHandCrossbow() {
        return true;
    }

    @Override
    protected void equip(Random random) {
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        CrossbowItem.setCharged(crossbow, true);
        this.equipStack(EquipmentSlot.MAINHAND, crossbow);
        if (random.nextFloat() < SHIELD_CHANCE) {
            this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
    }
}
