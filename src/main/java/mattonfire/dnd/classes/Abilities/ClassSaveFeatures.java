package mattonfire.dnd.classes.Abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.BarbarianSkills;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Class features that change saving throws, by class level (5e's, scaled to levels 1-10):
 * <ul>
 * <li><b>Paladin, Aura of Protection</b> (the tree passive): players within 8 blocks add the Paladin's CHA modifier
 * (at least +1) to every save; the best Paladin counts ({@link PaladinSkills#auraSave}).</li>
 * <li><b>Rogue and Monk, Evasion</b> (level {@value #EVASION_LEVEL}): a half-damage DEX save takes no damage on a
 * success and half on a failure. A {@link SavingThrow.ResultHook}, so it covers every caller of
 * {@link SaveResult#damage} at once (dragon breath and storms, Dragonborn breath, traps, puzzle vents).</li>
 * <li><b>Barbarian, Danger Sense</b> (level {@value #DANGER_SENSE_LEVEL}): advantage on DEX saves unless blinded
 * or in Darkness.</li>
 * <li><b>Barbarian, Rage</b> (while raging): advantage on STR saves and Athletics checks.</li>
 * <li><b>Fighter, Indomitable</b> (level {@value #INDOMITABLE_LEVEL}): reroll a failed save once every
 * {@value #INDOMITABLE_COOLDOWN_TICKS} ticks (5 minutes). A {@link SavingThrow.Reroll}: the failed die shows on the
 * save lane as "Indomitable", then the second roll.</li>
 * <li><b>Bard, Jack of All Trades</b> (level {@value #JACK_OF_ALL_TRADES_LEVEL}): half proficiency on every check
 * the Bard isn't proficient in.</li>
 * <li><b>Monk, Diamond Soul</b> (level {@value #DIAMOND_SOUL_LEVEL}): proficient in every save.</li>
 * </ul>
 * The features that work through conditions (Aura of Courage, Mindless Rage, Stillness of Mind, Diamond Soul's
 * repeat-save reroll, Stunning Strike, Cure Wounds, Countercharm, poison immunity) are in
 * {@link ClassConditionFeatures}.
 * Each one adds a line to the sheet's breakdown ({@code /dndclass sheet}).
 */
public final class ClassSaveFeatures {
    public static final Identifier LEVEL_FEATURES = new Identifier(DnDClasses.MOD_ID, "class/save_features");
    /** Rage and the Paladin's aura change from second to second, so they're a dynamic contributor. */
    public static final Identifier LIVE_FEATURES = new Identifier(DnDClasses.MOD_ID, "class/save_features_live");

    public static final int EVASION_LEVEL = 7;
    public static final int DANGER_SENSE_LEVEL = 2;
    public static final int INDOMITABLE_LEVEL = 9;
    public static final int INDOMITABLE_COOLDOWN_TICKS = 5 * 60 * 20;
    public static final int JACK_OF_ALL_TRADES_LEVEL = 2;
    public static final int DIAMOND_SOUL_LEVEL = 10;
    /** Evasion's damage multipliers on a half-damage DEX save. */
    public static final float EVASION_SUCCESS_DAMAGE = 0.0F;
    public static final float EVASION_FAILURE_DAMAGE = 0.5F;

    public static final String EVASION_KEY = "feature.dndclasses.evasion";
    public static final String INDOMITABLE_KEY = "feature.dndclasses.indomitable";

    /** Fighter UUID to the server tick Indomitable is ready again. Kept across relogs, so logging out doesn't reset it. */
    private static final Map<UUID, Integer> INDOMITABLE_READY = new HashMap<>();

    private ClassSaveFeatures() {
    }

    /** Registers the contributors and save hooks; called from {@link AbilityScores#bootstrap}. */
    static void register() {
        AbilityScores.register(LEVEL_FEATURES, ClassSaveFeatures::levelFeatures);
        AbilityScores.register(LIVE_FEATURES, ClassSaveFeatures::liveFeatures, true);
        SavingThrow.registerResultHook(ClassSaveFeatures::evasion);
        SavingThrow.registerReroll(ClassSaveFeatures::indomitable);
        ClassConditionFeatures.register();
        // Server ticks restart with the server: don't carry one world's cooldowns into the next
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> INDOMITABLE_READY.clear());
    }

    private static int level(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.NONE ? 0 : Progression.current(player).level();
    }

    /** Whether the player has the feature unlocked by reaching this level in one of these classes. */
    private static boolean has(PlayerEntity player, int level, DndCharacter... classes) {
        DndCharacter dndClass = Progression.classOf(player);
        for (DndCharacter c : classes) {
            if (c == dndClass) {
                return level(player) >= level;
            }
        }
        return false;
    }

    public static boolean hasEvasion(PlayerEntity player) {
        return has(player, EVASION_LEVEL, DndCharacter.ROGUE, DndCharacter.MONK);
    }

    public static boolean hasIndomitable(PlayerEntity player) {
        return has(player, INDOMITABLE_LEVEL, DndCharacter.FIGHTER);
    }

    public static boolean hasDiamondSoul(PlayerEntity player) {
        return has(player, DIAMOND_SOUL_LEVEL, DndCharacter.MONK);
    }

    private static void levelFeatures(PlayerEntity player, Contribution c) {
        if (has(player, JACK_OF_ALL_TRADES_LEVEL, DndCharacter.BARD)) {
            c.checkProficiencyFloor(Proficiency.HALF, "Jack of All Trades");
        }
        if (hasDiamondSoul(player)) {
            for (Ability a : Ability.values()) {
                c.saveProficiency(a, Proficiency.PROFICIENT, "Diamond Soul");
            }
        }
        if (has(player, DANGER_SENSE_LEVEL, DndCharacter.BARBARIAN)) {
            // Checked at roll time: no advantage while you can't see it coming
            c.advantage(RollFilter.save(Ability.DEX).and(q -> !player.hasStatusEffect(StatusEffects.BLINDNESS)
                    && !player.hasStatusEffect(StatusEffects.DARKNESS)),
                    "DEX saves, unless blinded or in Darkness", "Danger Sense");
        }
        if (hasEvasion(player)) {
            c.feature("half-damage DEX saves: no damage on a success, half on a failure", "Evasion");
        }
        if (hasIndomitable(player)) {
            c.feature("reroll a failed save, once every 5 minutes", "Indomitable");
        }
        ClassConditionFeatures.levelFeatures(player, c);
    }

    private static void liveFeatures(PlayerEntity player, Contribution c) {
        if (Progression.classOf(player) == DndCharacter.BARBARIAN && BarbarianSkills.isRaging(player)) {
            c.advantage(RollFilter.save(Ability.STR).or(RollFilter.check(Skill.ATHLETICS)),
                    "STR saves and Athletics", "Rage");
        }
        PaladinSkills.AuraSave aura = PaladinSkills.auraSave(player);
        if (aura != null) {
            c.saveBonus(null, aura.bonus(), aura.paladin() == player ? "Aura of Protection"
                    : "Aura of Protection (" + aura.paladin().getEntityName() + ")");
        }
        ClassConditionFeatures.liveFeatures(player, c);
    }

    /** Evasion: a Rogue's or Monk's half-damage DEX save takes nothing on a success and half on a failure. */
    private static SaveResult evasion(LivingEntity target, Ability ability, SaveResult result) {
        if (ability != Ability.DEX || !result.halvesDamage() || !(target instanceof PlayerEntity player)
                || !hasEvasion(player)) {
            return result;
        }
        return result.withDamageMultipliers(EVASION_SUCCESS_DAMAGE, EVASION_FAILURE_DAMAGE,
                Text.translatable(EVASION_KEY));
    }

    /** Indomitable: a Fighter's failed save is rolled again, once every 5 minutes. */
    @Nullable
    private static Text indomitable(ServerPlayerEntity player, Ability ability, D20.Roll failed) {
        if (!hasIndomitable(player) || player.getServer() == null) {
            return null;
        }
        int now = player.getServer().getTicks();
        Integer ready = INDOMITABLE_READY.get(player.getUuid());
        if (ready != null && now < ready) {
            return null;
        }
        INDOMITABLE_READY.put(player.getUuid(), now + INDOMITABLE_COOLDOWN_TICKS);
        return Text.translatable(INDOMITABLE_KEY);
    }
}
