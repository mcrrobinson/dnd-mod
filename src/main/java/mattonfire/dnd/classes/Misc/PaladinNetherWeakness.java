package mattonfire.dnd.classes.Misc;

import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Paladins are very weak in the Nether: while there they deal half damage, have
 * half armor and move 20% slower. Status effects can't be used because
 * Paladins are immune to them (LivingEntityMixin), so this uses temporary
 * attribute modifiers that are re-checked every tick and dropped as soon as the
 * player leaves the Nether or stops being a Paladin.
 */
public class PaladinNetherWeakness {
    private static final UUID ATTACK_UUID = UUID.fromString("5b3c1d2e-7a41-4f0e-9c6b-0a1d2e3f4a51");
    private static final UUID ARMOR_UUID = UUID.fromString("5b3c1d2e-7a41-4f0e-9c6b-0a1d2e3f4a52");
    private static final UUID SPEED_UUID = UUID.fromString("5b3c1d2e-7a41-4f0e-9c6b-0a1d2e3f4a53");
    private static final String NAME = "Paladin nether weakness";

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                update(player);
            }
        });
    }

    private static void update(ServerPlayerEntity player) {
        boolean weak = player instanceof PlayerEntityExt ext
                && ext.getDndClass() == DndCharacter.PALADIN
                && player.getWorld().getRegistryKey() == World.NETHER;

        EntityAttributeInstance attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        boolean applied = attack != null && attack.getModifier(ATTACK_UUID) != null;
        if (weak == applied) {
            return;
        }

        if (weak) {
            add(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_UUID, -0.5);
            add(player, EntityAttributes.GENERIC_ARMOR, ARMOR_UUID, -0.5);
            add(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_UUID, -0.2);
            player.sendMessage(Text.literal("The Nether saps your holy strength...").formatted(Formatting.DARK_RED),
                    true);
        } else {
            remove(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_UUID);
            remove(player, EntityAttributes.GENERIC_ARMOR, ARMOR_UUID);
            remove(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_UUID);
            if (player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.PALADIN) {
                player.sendMessage(Text.literal("Your holy strength returns.").formatted(Formatting.GOLD), true);
            }
        }
    }

    private static void add(ServerPlayerEntity player, EntityAttribute attribute, UUID uuid, double amount) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(uuid) == null) {
            instance.addTemporaryModifier(new EntityAttributeModifier(uuid, NAME, amount,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void remove(ServerPlayerEntity player, EntityAttribute attribute, UUID uuid) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }
}
