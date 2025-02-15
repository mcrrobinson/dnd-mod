package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.passive.TameableEntity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "onRemoved", at = @At("RETURN"))
    private void onRemoved(CallbackInfo callbackInfo) {
        Entity entity = (Entity) (Object) this;

        // Check if the entity is a tameable entity
        if (!(entity instanceof TameableEntity))
            return;

        TameableEntity tameableEntity = (TameableEntity) entity;

        // Check if it has an owner and if the owner is a player
        if (tameableEntity.getOwner() instanceof PlayerEntity) {
            PlayerEntity owner = (PlayerEntity) tameableEntity.getOwner();
            if (owner instanceof PlayerEntityExt) {
                PlayerEntityExt playerEntity = (PlayerEntityExt) owner;

                // Ensure the entity is actually tamed before proceeding
                if (tameableEntity.isTamed()) {

                    // TODO: THIS ISNT UPDATING THE HEATLH
                    if (playerEntity.getDndClass() == DndCharacter.BARD) {
                        // If the owner is type bard reduce the max health
                        // Get the player's max health attribute
                        EntityAttributeInstance attribute = owner
                                .getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);

                        if (attribute != null) {
                            double newHealth = Math.max(1.0, attribute.getBaseValue() + 1);
                            attribute.setBaseValue(newHealth);

                            // If the current health is higher than the new max, adjust it
                            if (owner.getHealth() > newHealth) {
                                owner.setHealth((float) newHealth);
                            }
                        }
                    }

                    System.out.println("Tameable entity owned by " + owner.getName().getString() + " removed");
                    return;
                }
            }

        }
    }
}
