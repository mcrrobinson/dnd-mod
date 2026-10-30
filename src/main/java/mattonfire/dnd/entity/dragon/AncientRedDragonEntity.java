package mattonfire.dnd.entity.dragon;

import mattonfire.dnd.entity.DragonPart;
import mattonfire.dnd.entity.FireBreath;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * The Ancient Red Dragon (design 10, section 5): fire breath ({@link AncientFireBreath}) and the volcanic lair
 * actions ({@link AncientLairActions#RED}). Immune to fire and lava (its type is {@code fireImmune}).
 */
public class AncientRedDragonEntity extends AncientDragonEntity {
    public AncientRedDragonEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world, ChromaticColor.RED);
    }

    @Override
    protected FireBreath createBreath(DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey) {
        return new AncientFireBreath(this, head, ticksKey, aimKey);
    }
}
