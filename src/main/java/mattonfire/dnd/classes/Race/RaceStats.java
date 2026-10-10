package mattonfire.dnd.classes.Race;

import java.util.UUID;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;

import mattonfire.dnd.classes.ClassStats;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;

/**
 * The race's attribute modifiers, on top of the class's base values ({@link ClassStats} only sets base
 * values, so the two never overwrite each other). Each modifier has a fixed UUID and is removed before
 * being added again, so applying on every join, respawn and race change never stacks. Modifiers are
 * persistent so a Dwarf's extra heart is still there when the saved health is read back on rejoin.
 * It also sets the synced body race, which {@link RaceSize} reads for the hitbox and model size.
 */
public final class RaceStats {
    public static final UUID SPEED_ID = UUID.fromString("5a1d7c2e-3b0f-4e11-9a6e-7d2c1f0e5b01");
    public static final UUID MAX_HEALTH_ID = UUID.fromString("5a1d7c2e-3b0f-4e11-9a6e-7d2c1f0e5b02");
    public static final UUID KNOCKBACK_ID = UUID.fromString("5a1d7c2e-3b0f-4e11-9a6e-7d2c1f0e5b03");
    public static final UUID ATTACK_RANGE_ID = UUID.fromString("5a1d7c2e-3b0f-4e11-9a6e-7d2c1f0e5b04");

    private RaceStats() {
    }

    /** Replaces the player's race modifiers with those of their active race (none if races are off). */
    public static void apply(PlayerEntity player) {
        DndRace active = RaceLifecycle.activeRaceOf(player);
        if (player instanceof PlayerEntityExt ext) {
            ext.setBodyRace(active);
        }
        RaceInfo info = RaceInfo.get(active);
        RaceInfo.Stats stats = info == null ? RaceInfo.Stats.NONE : info.stats();
        set(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, "Race speed", stats.speed(),
                EntityAttributeModifier.Operation.MULTIPLY_BASE);
        set(player, EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH_ID, "Race health", stats.maxHealth(),
                EntityAttributeModifier.Operation.ADDITION);
        set(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Race knockback resistance",
                stats.knockbackResistance(), EntityAttributeModifier.Operation.ADDITION);
        set(player, ReachEntityAttributes.ATTACK_RANGE, ATTACK_RANGE_ID, "Race attack range", stats.attackRange(),
                EntityAttributeModifier.Operation.ADDITION);
        ClassStats.capHealth(player);
    }

    private static void set(PlayerEntity player, EntityAttribute attribute, UUID id, String name, double value,
            EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (value != 0) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, name, value, operation));
        }
    }
}
