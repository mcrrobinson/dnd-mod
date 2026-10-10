package mattonfire.dnd.classes.Progression;

import java.util.UUID;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;

/**
 * A passive that is just an attribute modifier, kept up to date once a second.
 * {@code skill} is a passive's node id, or a {@link Subclass} id for a subclass
 * feature (on whenever that subclass is chosen).
 */
public record AttributeBonus(String skill, EntityAttribute attribute, double amount,
        EntityAttributeModifier.Operation operation) {
    public UUID uuid() {
        return UUID.nameUUIDFromBytes(("dndclasses:" + skill).getBytes());
    }
}
