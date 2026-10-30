package mattonfire.dnd.entity.kobold;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** A Kobold (id {@code kobold}): 8 health, 2 armour, a spear (3). Pack Tactics and cowardice, nothing more. */
public class KoboldWarriorEntity extends KoboldEntity {
    public KoboldWarriorEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return koboldAttributes(8.0D, 2.0D, 0.33D, 3.0D);
    }
}
