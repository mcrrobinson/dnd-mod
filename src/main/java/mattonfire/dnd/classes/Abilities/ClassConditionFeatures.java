package mattonfire.dnd.classes.Abilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionEvents;
import mattonfire.dnd.classes.Conditions.ConditionInstance;
import mattonfire.dnd.classes.Conditions.ConditionTicker;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.BarbarianSkills;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SaveRules;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Class and race features that interact with {@link Conditions} (design 8, section 3). The numbers live here; the
 * features' own classes call in where they act:
 * <ul>
 * <li><b>Paladin (Conquest), Aura of Courage</b>: the Paladin and players within {@value #AURA_OF_COURAGE_RADIUS}
 * blocks can't be Frightened (an {@link Conditions.Immunity}), and a Frightened player who steps into the aura
 * stops being Frightened ({@link PaladinSkills#secondTick}, once a second).</li>
 * <li><b>Barbarian (Berserker), Mindless Rage</b>: while Rage is on, immune to Frightened and Charmed; starting Rage
 * ends them ({@link #onRage}).</li>
 * <li><b>Monk, Stillness of Mind</b> (level {@value #STILLNESS_OF_MIND_LEVEL}): Frightened and Charmed last half as
 * long (an {@link Conditions.ApplyHook}), and their repeat saves have advantage (on the sheet).</li>
 * <li><b>Monk, Diamond Soul</b> (level {@value ClassSaveFeatures#DIAMOND_SOUL_LEVEL}): the first failed repeat save
 * of each condition instance is rolled again (a {@link SavingThrow.Reroll} that only claims repeat saves).</li>
 * <li><b>Monk (Open Hand), Stunning Strike</b>: a CON save against {@link #stunningStrikeDc} or Stunned for
 * {@value #STUNNING_STRIKE_STUN_TICKS} ticks ({@code MonkSkills}).</li>
 * <li><b>Cleric, Cure Wounds</b>: also ends one condition on each healed creature, the first found of
 * {@link #CURE_WOUNDS_ORDER} ({@link #cureOne}, called from {@code ClericSkills}).</li>
 * <li><b>Bard, Countercharm</b>: for {@value #COUNTERCHARM_TICKS} ticks after a Bard plays the lute, players within
 * {@value #COUNTERCHARM_RADIUS} blocks roll repeat saves against Frightened and Charmed with advantage
 * ({@link #countercharm}, called from {@code InstrumentItem}).</li>
 * <li><b>Poison immunities</b> (Rogue, Devotion Paladin's Purity of Spirit, Alchemist's Iron Stomach, a potion of
 * immunity): immune to Poisoned, through {@link SaveRules#immune}.</li>
 * </ul>
 * The Elf's Fey Ancestry (advantage on saves against charm) is a race trait in {@code RacePassives}.
 */
public final class ClassConditionFeatures {
    public static final double AURA_OF_COURAGE_RADIUS = 8.0;
    public static final int STILLNESS_OF_MIND_LEVEL = 7;
    /** Stillness of Mind: Frightened and Charmed on the Monk last this fraction as long. */
    public static final float STILLNESS_OF_MIND_DURATION = 0.5F;
    public static final int STUNNING_STRIKE_STUN_TICKS = 40;
    public static final double COUNTERCHARM_RADIUS = 8.0;
    /** As long as the Bard's instrument cooldown, so a Bard playing the lute whenever it's ready keeps it up. */
    public static final int COUNTERCHARM_TICKS = 10 * 20;
    /** Cure Wounds ends the first of these it finds on each creature it heals. */
    public static final List<Condition> CURE_WOUNDS_ORDER = List.of(Condition.PETRIFIED, Condition.PARALYZED,
            Condition.BLINDED, Condition.POISONED, Condition.FRIGHTENED);

    public static final String AURA_OF_COURAGE_IMMUNE = "condition.dndclasses.immune.aura_of_courage";
    public static final String MINDLESS_RAGE_IMMUNE = "condition.dndclasses.immune.mindless_rage";
    public static final String POISON_IMMUNE = "condition.dndclasses.immune.poison";
    public static final String DIAMOND_SOUL_KEY = "feature.dndclasses.diamond_soul";

    /** Bard UUID to the world time their Countercharm ends. */
    private static final Map<UUID, Long> COUNTERCHARM_UNTIL = new HashMap<>();
    /** Condition instances (see {@link #instanceId}) whose one Diamond Soul reroll is spent. */
    private static final Set<String> DIAMOND_SOUL_USED = new HashSet<>();

    private ClassConditionFeatures() {
    }

    /** Registers the immunities, hooks and reroll; called from {@link ClassSaveFeatures#register}. */
    static void register() {
        Conditions.registerImmunity(ClassConditionFeatures::immunity);
        Conditions.registerApplyHook(ClassConditionFeatures::stillnessOfMind);
        SavingThrow.registerReroll(ClassConditionFeatures::diamondSoul);
        ConditionEvents.ENDED.register((target, inst, reason) -> DIAMOND_SOUL_USED.remove(instanceId(target, inst)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            COUNTERCHARM_UNTIL.clear();
            DIAMOND_SOUL_USED.clear();
        });
    }

    // ------------------------------------------------------------------ immunities and hooks

    @Nullable
    private static String immunity(LivingEntity target, Condition condition, @Nullable net.minecraft.entity.Entity source) {
        if (!(target instanceof PlayerEntity player)) {
            return null;
        }
        if (condition == Condition.FRIGHTENED && auraOfCourage(player) != null) {
            return AURA_OF_COURAGE_IMMUNE;
        }
        if ((condition == Condition.FRIGHTENED || condition == Condition.CHARMED) && hasMindlessRage(player)) {
            return MINDLESS_RAGE_IMMUNE;
        }
        if (condition == Condition.POISONED
                && SaveRules.immune(player, new StatusEffectInstance(StatusEffects.POISON, 20, 0))) {
            return POISON_IMMUNE;
        }
        return null;
    }

    /** Stillness of Mind: halves Frightened and Charmed on a level 7+ Monk (indefinite stays indefinite). */
    private static int stillnessOfMind(LivingEntity target, Condition condition,
            @Nullable net.minecraft.entity.Entity source, int ticks) {
        if (ticks <= 0 || condition != Condition.FRIGHTENED && condition != Condition.CHARMED
                || !(target instanceof PlayerEntity player) || !hasStillnessOfMind(player)) {
            return ticks;
        }
        return Math.max(1, Math.round(ticks * STILLNESS_OF_MIND_DURATION));
    }

    /** Diamond Soul: a Monk's first failed repeat save of each condition instance is rolled again. */
    @Nullable
    private static Text diamondSoul(ServerPlayerEntity player, Ability ability, D20.Roll failed) {
        ConditionInstance rolling = ConditionTicker.rollingRepeatSave();
        if (rolling == null || !ClassSaveFeatures.hasDiamondSoul(player)) {
            return null;
        }
        if (!DIAMOND_SOUL_USED.add(instanceId(player, rolling))) {
            return null;
        }
        return Text.translatable(DIAMOND_SOUL_KEY);
    }

    /** One instance, across the copies the ticker makes as its next save time moves on. */
    private static String instanceId(LivingEntity target, ConditionInstance inst) {
        return target.getUuidAsString() + "|" + inst.condition().id() + "|" + inst.source() + "|" + inst.key() + "|"
                + inst.appliedAt();
    }

    // ------------------------------------------------------------------ feature checks

    /** The standing Conquest Paladin whose Aura of Courage covers this player (themselves included), or null. */
    @Nullable
    public static PlayerEntity auraOfCourage(PlayerEntity player) {
        double r = AURA_OF_COURAGE_RADIUS;
        for (PlayerEntity paladin : player.getWorld().getEntitiesByClass(PlayerEntity.class,
                player.getBoundingBox().expand(r), p -> p.isAlive() && !p.isSpectator())) {
            if (Progression.classOf(paladin) == DndCharacter.PALADIN
                    && Progression.hasPassive(paladin, PaladinSkills.AURA_OF_COURAGE) && !Downed.is(paladin)) {
                return paladin;
            }
        }
        return null;
    }

    /** A Berserker Barbarian with Rage on. */
    public static boolean hasMindlessRage(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.BARBARIAN
                && Progression.current(player).hasSubclass(BarbarianSkills.BERSERKER)
                && BarbarianSkills.isRaging(player);
    }

    public static boolean hasStillnessOfMind(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.MONK
                && Progression.current(player).level() >= STILLNESS_OF_MIND_LEVEL;
    }

    /** Whether a Bard's Countercharm covers this player now. */
    public static boolean countercharmed(PlayerEntity player) {
        if (COUNTERCHARM_UNTIL.isEmpty() || player.getWorld().isClient) {
            return false;
        }
        long now = player.getWorld().getTime();
        COUNTERCHARM_UNTIL.values().removeIf(t -> t <= now);
        double r = COUNTERCHARM_RADIUS;
        for (PlayerEntity bard : player.getWorld().getEntitiesByClass(PlayerEntity.class,
                player.getBoundingBox().expand(r), p -> p.isAlive() && COUNTERCHARM_UNTIL.containsKey(p.getUuid()))) {
            if (bard.squaredDistanceTo(player) <= r * r) {
                return true;
            }
        }
        return false;
    }

    /** Repeat saves against Frightened and Charmed, which Stillness of Mind and Countercharm favour. */
    static RollFilter fearAndCharmRepeatSaves() {
        return RollFilter.allSaves().and(RollFilter.tag("condition"))
                .and(RollFilter.tag(Condition.FRIGHTENED.id()).or(RollFilter.tag(Condition.CHARMED.id())));
    }

    // ------------------------------------------------------------------ called from the class skills

    /** Mindless Rage: starting Rage ends Frightened and Charmed on a Berserker. From {@code BarbarianSkills.rage}. */
    public static void onRage(PlayerEntity player) {
        if (player.getWorld().isClient || !Progression.current(player).hasSubclass(BarbarianSkills.BERSERKER)) {
            return;
        }
        boolean ended = Conditions.remove(player, Condition.FRIGHTENED);
        ended |= Conditions.remove(player, Condition.CHARMED);
        if (ended) {
            DnDClasses.LOGGER.info("[Conditions] {} Mindless Rage ends fear and charm", player.getEntityName());
        }
    }

    /** Aura of Courage: ends Frightened on the players it covers. From the Paladin's once-a-second tick. */
    public static void auraOfCourageTick(PlayerEntity paladin, List<PlayerEntity> covered) {
        for (PlayerEntity ally : covered) {
            if (Conditions.has(ally, Condition.FRIGHTENED) && Conditions.remove(ally, Condition.FRIGHTENED)) {
                DnDClasses.LOGGER.info("[Conditions] {} Aura of Courage ({}) ends frightened", ally.getEntityName(),
                        paladin.getEntityName());
            }
        }
    }

    /** Stunning Strike's save DC: 8 + proficiency + WIS modifier. */
    public static int stunningStrikeDc(PlayerEntity monk) {
        CharacterSheet sheet = AbilityScores.sheet(monk);
        return 8 + sheet.proficiencyBonus() + sheet.modifier(Ability.WIS);
    }

    /**
     * Cure Wounds: ends the first condition of {@link #CURE_WOUNDS_ORDER} on the target. Call before healing, since
     * a Petrified creature can't be healed. Returns the condition it ended, or null.
     */
    @Nullable
    public static Condition cureOne(PlayerEntity cleric, LivingEntity target) {
        for (Condition condition : CURE_WOUNDS_ORDER) {
            if (Conditions.has(target, condition) && Conditions.remove(target, condition)) {
                DnDClasses.LOGGER.info("[Conditions] {} Cure Wounds ({}) ends {}",
                        target instanceof PlayerEntity ? target.getEntityName() : target.getName().getString(),
                        cleric.getEntityName(), condition.id());
                if (target instanceof PlayerEntity healed) {
                    healed.sendMessage(Text.translatable(condition == Condition.PETRIFIED
                            ? "condition.dndclasses.cured.petrified" : "condition.dndclasses.cured",
                            Text.translatable(condition.translationKey())), true);
                }
                return condition;
            }
        }
        return null;
    }

    /** Countercharm: a Bard played the lute. From {@code InstrumentItem.playAsBard}. */
    public static void countercharm(PlayerEntity bard) {
        if (bard.getWorld().isClient) {
            return;
        }
        COUNTERCHARM_UNTIL.put(bard.getUuid(), bard.getWorld().getTime() + COUNTERCHARM_TICKS);
        DnDClasses.LOGGER.info("[Conditions] {} Countercharm for {}t", bard.getEntityName(), COUNTERCHARM_TICKS);
        // Listeners' sheets show the advantage now, not at the next once-a-second refresh
        for (PlayerEntity listener : bard.getWorld().getEntitiesByClass(PlayerEntity.class,
                bard.getBoundingBox().expand(COUNTERCHARM_RADIUS), PlayerEntity::isAlive)) {
            AbilityScores.invalidate(listener);
        }
    }

    /** Sheet lines for the level-based features; from {@link ClassSaveFeatures}' level contributor. */
    static void levelFeatures(PlayerEntity player, Contribution c) {
        if (hasStillnessOfMind(player)) {
            c.advantage(fearAndCharmRepeatSaves(), "repeat saves against Frightened and Charmed",
                    "Stillness of Mind");
            c.feature("Frightened and Charmed last half as long", "Stillness of Mind");
        }
        if (ClassSaveFeatures.hasDiamondSoul(player)) {
            c.feature("reroll the first failed repeat save against each condition", "Diamond Soul");
        }
    }

    /** Sheet lines for the features that come and go; from {@link ClassSaveFeatures}' live contributor. */
    static void liveFeatures(PlayerEntity player, Contribution c) {
        PlayerEntity paladin = auraOfCourage(player);
        if (paladin != null) {
            c.feature("can't be Frightened", paladin == player ? "Aura of Courage"
                    : "Aura of Courage (" + paladin.getEntityName() + ")");
        }
        if (hasMindlessRage(player)) {
            c.feature("can't be Frightened or Charmed", "Mindless Rage");
        }
        if (countercharmed(player)) {
            c.advantage(fearAndCharmRepeatSaves(), "repeat saves against Frightened and Charmed", "Countercharm");
        }
    }
}
