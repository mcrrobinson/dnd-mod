package mattonfire.dnd.classes.Conditions;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.boss.Boss;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * The built-in immunities and the boss rule:
 * <ul>
 * <li>Creative and spectator players are immune to everything.</li>
 * <li>{@link Boss}es are immune to Petrified, Charmed and Frightened. Their Stunned, Paralyzed, Incapacitated,
 * Restrained and Prone are capped at {@value #BOSS_CAP_TICKS} ticks, after which that condition can't land on
 * them again for {@value #BOSS_SHRUG_TICKS} ticks ("shrugs it off").</li>
 * <li>Undead are immune to Poisoned; so is anything Petrified.</li>
 * <li>Entity type tags {@code #dndclasses:condition_immune/<id>} add immunities without code.</li>
 * </ul>
 */
public final class ConditionImmunities {
    public static final int BOSS_CAP_TICKS = 20;
    public static final int BOSS_SHRUG_TICKS = 200;

    private static final Map<Condition, TagKey<EntityType<?>>> TAGS = new EnumMap<>(Condition.class);
    /** Boss UUID + condition to the world time it can be controlled by that condition again. */
    private static final Map<String, Long> SHRUG_UNTIL = new HashMap<>();

    static {
        for (Condition c : Condition.values()) {
            TAGS.put(c, TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, "condition_immune/" + c.id())));
        }
    }

    private ConditionImmunities() {
    }

    public static TagKey<EntityType<?>> tag(Condition condition) {
        return TAGS.get(condition);
    }

    private static boolean bossControl(Condition c) {
        return c == Condition.STUNNED || c == Condition.PARALYZED || c == Condition.INCAPACITATED
                || c == Condition.RESTRAINED || c == Condition.PRONE;
    }

    private static String shrugKey(UUID boss, Condition c) {
        return boss + "|" + c.id();
    }

    static void register() {
        Conditions.registerImmunity((target, condition, source) -> {
            if (target instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
                return "condition.dndclasses.immune.creative";
            }
            if (target instanceof Boss) {
                if (condition == Condition.PETRIFIED || condition == Condition.CHARMED
                        || condition == Condition.FRIGHTENED) {
                    return "condition.dndclasses.immune.boss";
                }
                Long until = SHRUG_UNTIL.get(shrugKey(target.getUuid(), condition));
                if (until != null && target.getWorld().getTime() < until) {
                    return "condition.dndclasses.immune.shrugs_off";
                }
            }
            if (condition == Condition.POISONED) {
                if (target.getGroup() == EntityGroup.UNDEAD) {
                    return "condition.dndclasses.immune.undead";
                }
                if (Conditions.has(target, Condition.PETRIFIED)) {
                    return "condition.dndclasses.immune.petrified";
                }
            }
            if (target.getType().isIn(TAGS.get(condition))) {
                return "condition.dndclasses.immune.nature";
            }
            return null;
        });

        Conditions.registerApplyHook((target, condition, source, ticks) -> {
            if (!(target instanceof Boss) || !bossControl(condition)) {
                return ticks;
            }
            long now = target.getWorld().getTime();
            SHRUG_UNTIL.values().removeIf(t -> t < now);
            SHRUG_UNTIL.put(shrugKey(target.getUuid(), condition), now + BOSS_CAP_TICKS + BOSS_SHRUG_TICKS);
            return ticks < 0 || ticks > BOSS_CAP_TICKS ? BOSS_CAP_TICKS : ticks;
        });
    }
}
