package mattonfire.dnd.classes;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The attribute base values each class sets. Applying them only sets base values,
 * so it is safe to repeat on every join and respawn: it never touches effects or
 * health. Server-safe (no client classes).
 *
 * Behaviour that isn't an attribute lives elsewhere: Cleric haste in ClericHandler,
 * Monk weapon rules in MonkHandler, Paladin potion/crafting bans in mixins, Ranger
 * zoom and sword ban in RangerBowZoomMixin/RangerSwordPickupMixin, Warlock fire in
 * Warlock, Artificer damage in ArtificerDamage, Druid hearts in Druid.
 */
public final class ClassStats {
    /**
     * @param pickHealth health set when the class is picked (or switched to), or
     *                   null to keep the current health (capped at the new max)
     */
    private record Stats(double attackDamage, double movementSpeed, double maxHealth, double attackSpeed,
            double luck, @Nullable Float pickHealth) {
        Stats damage(double v) {
            return new Stats(v, movementSpeed, maxHealth, attackSpeed, luck, pickHealth);
        }

        Stats speed(double v) {
            return new Stats(attackDamage, v, maxHealth, attackSpeed, luck, pickHealth);
        }

        Stats health(double v) {
            return new Stats(attackDamage, movementSpeed, v, attackSpeed, luck, pickHealth);
        }

        Stats attackSpeed(double v) {
            return new Stats(attackDamage, movementSpeed, maxHealth, v, luck, pickHealth);
        }

        Stats luck(double v) {
            return new Stats(attackDamage, movementSpeed, maxHealth, attackSpeed, v, pickHealth);
        }

        Stats pickHealth(float v) {
            return new Stats(attackDamage, movementSpeed, maxHealth, attackSpeed, luck, v);
        }
    }

    /** Vanilla player defaults. */
    private static final Stats DEFAULT = new Stats(1.0, 0.10000000149011612, 20.0, 4.0, 0.0, null);

    private static final Map<DndCharacter, Stats> STATS = new EnumMap<>(DndCharacter.class);

    static {
        for (DndCharacter c : DndCharacter.values()) {
            STATS.put(c, DEFAULT);
        }
        STATS.put(DndCharacter.BARBARIAN, DEFAULT.damage(6).speed(0.08).health(40).pickHealth(25));
        STATS.put(DndCharacter.BARD, DEFAULT.speed(0.12).health(15).pickHealth(15));
        STATS.put(DndCharacter.CLERIC, DEFAULT.damage(0.67)); // -33%
        STATS.put(DndCharacter.FIGHTER, DEFAULT.damage(6).health(26).pickHealth(25));
        STATS.put(DndCharacter.MONK, DEFAULT.speed(0.12).attackSpeed(6.0));
        STATS.put(DndCharacter.PALADIN, DEFAULT.health(26).pickHealth(25));
        STATS.put(DndCharacter.RANGER, DEFAULT.luck(5));
        STATS.put(DndCharacter.ROGUE, DEFAULT.health(14));
        // Necromancers start fragile: 5 HP, only when the class is picked.
        STATS.put(DndCharacter.NECROMANCER, DEFAULT.damage(0.5).pickHealth(5));
        STATS.put(DndCharacter.WARLOCK, DEFAULT.damage(0.5));
        STATS.put(DndCharacter.WIZARD, DEFAULT.health(10).pickHealth(10));
        STATS.put(DndCharacter.ARTIFICER, DEFAULT.speed(0.12));
    }

    private ClassStats() {
    }

    /** Sets the class's attribute base values (vanilla defaults for anything it doesn't change). */
    public static void apply(PlayerEntity player, DndCharacter dndClass) {
        Stats stats = STATS.getOrDefault(dndClass == null ? DndCharacter.NONE : dndClass, DEFAULT);
        setBase(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, stats.attackDamage());
        setBase(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, stats.movementSpeed());
        setBase(player, EntityAttributes.GENERIC_MAX_HEALTH, stats.maxHealth());
        setBase(player, EntityAttributes.GENERIC_ATTACK_SPEED, stats.attackSpeed());
        setBase(player, EntityAttributes.GENERIC_LUCK, stats.luck());
        if (dndClass == DndCharacter.DRUID && player instanceof ServerPlayerEntity serverPlayer) {
            // Extra hearts from tamed animals; kept up to date by Druid.serverTick.
            Druid.updateAnimalHearts(serverPlayer);
        }
        capHealth(player);
    }

    /** The health a player is set to when they pick this class, or null to keep theirs. */
    @Nullable
    public static Float pickHealth(DndCharacter dndClass) {
        return STATS.getOrDefault(dndClass, DEFAULT).pickHealth();
    }

    /** A lower max health doesn't lower current health on its own. */
    public static void capHealth(PlayerEntity player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setBase(PlayerEntity player, EntityAttribute attribute, double value) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }
}
