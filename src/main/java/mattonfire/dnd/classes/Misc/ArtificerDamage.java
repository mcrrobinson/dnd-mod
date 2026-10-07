package mattonfire.dnd.classes.Misc;

import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Artificer con: deals 25% less damage. Applied as a temporary (not saved)
 * attack damage modifier that is checked every tick, so it is re-added after a
 * relog without duplicating and removed as soon as the class changes.
 */
public class ArtificerDamage {
    private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("c4e2a9b1-6f3d-4b7e-8a15-2d9f0e6b7c38");
    private static final String DAMAGE_MODIFIER_NAME = "Artificer damage penalty";
    public static final double DAMAGE_REDUCTION = -0.25; // MULTIPLY_TOTAL: x0.75

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                update(player);
            }
        });
    }

    private static void update(ServerPlayerEntity player) {
        EntityAttributeInstance attackDamage = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        boolean isArtificer = player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.ARTIFICER;
        boolean hasModifier = attackDamage.getModifier(DAMAGE_MODIFIER_ID) != null;
        if (isArtificer && !hasModifier) {
            attackDamage.addTemporaryModifier(new EntityAttributeModifier(DAMAGE_MODIFIER_ID, DAMAGE_MODIFIER_NAME,
                    DAMAGE_REDUCTION, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!isArtificer && hasModifier) {
            attackDamage.removeModifier(DAMAGE_MODIFIER_ID);
        }
    }
}
