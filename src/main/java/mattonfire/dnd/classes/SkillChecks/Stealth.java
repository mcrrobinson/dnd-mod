package mattonfire.dnd.classes.SkillChecks;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.Abilities.Skill;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Passive Stealth: a sneaking player is noticed from closer the better their passive Stealth. Vanilla already
 * scales a mob's detection range by 0.8 for a sneaking target ({@code getAttackDistanceScalingFactor});
 * {@code StealthMixin} multiplies that by {@link #factor}. Nothing is rolled.
 *
 * A chestplate in {@link #HEAVY_ARMOR} gives disadvantage on Stealth (-5 passive), through a sheet contributor.
 */
public final class Stealth {
    public static final TagKey<Item> HEAVY_ARMOR = TagKey.of(RegistryKeys.ITEM,
            new Identifier(DnDClasses.MOD_ID, "heavy_armor"));
    public static final Identifier HEAVY_ARMOR_CONTRIBUTOR = new Identifier(DnDClasses.MOD_ID, "heavy_armor");
    /** Each point of passive Stealth over 10 shrinks the detection range by this much. */
    public static final double PER_POINT = 0.04;
    /** The factor never goes below this (on top of vanilla's 0.8 for sneaking). */
    public static final double MIN_FACTOR = 0.35;

    private Stealth() {
    }

    /** The detection-range factor for a passive Stealth score: clamp(1 - 0.04 x (passive - 10), 0.35, 1). */
    public static double factor(int passiveStealth) {
        return MathHelper.clamp(1.0 - PER_POINT * (passiveStealth - 10), MIN_FACTOR, 1.0);
    }

    /** The player's factor (1 when not sneaking). */
    public static double factor(PlayerEntity player) {
        return player.isSneaking() ? factor(AbilityScores.passive(player, Skill.STEALTH)) : 1.0;
    }

    public static boolean wearsHeavyArmor(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isIn(HEAVY_ARMOR);
    }

    static void register() {
        // Dynamic: armour changes without anyone invalidating the sheet
        AbilityScores.register(HEAVY_ARMOR_CONTRIBUTOR, (player, c) -> {
            if (wearsHeavyArmor(player)) {
                c.disadvantage(RollFilter.check(Skill.STEALTH), "Stealth", "Heavy armour");
            }
        }, true);
    }
}
