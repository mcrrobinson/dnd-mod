package mattonfire.dnd.classes.Conditions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Conditions.ai.ConditionFleeGoal;
import mattonfire.dnd.classes.Downed.DownedEvents;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.SkillChecks.AttackRolls;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

/**
 * What each condition does: movement, blocked actions, saves and checks, advantage on attacks against it, the
 * statue rule, charm, blindness and the visuals the server drives. See {@code docs/systems/conditions.md} for the
 * table.
 * <p>
 * Advantage against a target means a player's melee attack roll keeps the higher of 2d20, and a mob's melee hit
 * does x{@value #MOB_ADVANTAGE}; disadvantage on the attacker is the lower die, or x{@value #MOB_DISADVANTAGE}
 * (x{@value #MOB_POISONED} when Poisoned is the only reason).
 */
public final class ConditionEffects {
    public static final float MOB_ADVANTAGE = 1.25F;
    public static final float MOB_DISADVANTAGE = 0.75F;
    public static final float MOB_POISONED = 0.8F;
    /** Melee this close to a Prone target has advantage; further (and projectiles) don't. */
    public static final double PRONE_MELEE_RANGE = 3.0D;
    public static final float PRONE_PROJECTILE = 0.75F;
    /** Melee this close to a Paralyzed target is a critical hit. */
    public static final double PARALYZED_CRIT_RANGE = 2.5D;
    public static final float PETRIFIED_DAMAGE = 0.5F;
    public static final float FRIGHTENED_DAMAGE = 0.5F;
    /** A Frightened creature's source counts as "in sight" this close (and in line of sight). */
    public static final double FRIGHTENED_SIGHT = 32.0D;
    /** Blinded mobs can't target or keep a target further than this. */
    public static final double BLINDED_TARGET_RANGE = 4.0D;
    /** Charmed mobs stay this close to a mob charmer. */
    public static final double THRALL_FOLLOW = 12.0D;

    static final UUID ROOTED_ID = UUID.fromString("6a3c1f0e-2b7d-4e59-9c11-0d5a7e3b9f21");
    static final UUID PRONE_ID = UUID.fromString("6a3c1f0e-2b7d-4e59-9c11-0d5a7e3b9f22");
    static final UUID STONE_ID = UUID.fromString("6a3c1f0e-2b7d-4e59-9c11-0d5a7e3b9f23");
    /** Persistent-data flag: this mob was silenced as a statue (so it's unsilenced when the stone falls away). */
    static final String STATUE_KEY = "dndStatue";

    private static final Identifier PHASE = new Identifier(DnDClasses.MOD_ID, "conditions");
    private static final Map<UUID, Integer> LAST_BLOCK_MESSAGE = new HashMap<>();

    private ConditionEffects() {
    }

    // ------------------------------------------------------------------ registration

    static void register() {
        // Saves: STR and DEX auto-fail while Stunned, Paralyzed or Petrified; DEX at disadvantage while Restrained
        SavingThrow.registerPreRoll((target, ability, tags) -> {
            if (ability == Ability.STR || ability == Ability.DEX) {
                for (Condition c : new Condition[] { Condition.STUNNED, Condition.PARALYZED, Condition.PETRIFIED }) {
                    if (Conditions.has(target, c)) {
                        return SavingThrow.PreRoll.Outcome.autoFail(Text.translatable(c.translationKey()));
                    }
                }
            }
            if (ability == Ability.DEX && Conditions.has(target, Condition.RESTRAINED)) {
                return SavingThrow.PreRoll.Outcome.mode(Advantage.DISADVANTAGE,
                        Text.translatable(Condition.RESTRAINED.translationKey()));
            }
            return null;
        });

        // Player attack rolls
        AttackRolls.registerMode(ConditionEffects::attackMode);
        AttackRolls.registerAutoCrit((player, target) -> target instanceof LivingEntity living
                && Conditions.has(living, Condition.PARALYZED)
                && player.distanceTo(target) <= PARALYZED_CRIT_RANGE ? "Paralyzed" : null);

        // The sheet: check disadvantages
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "conditions"), (player, c) -> {
            if (Conditions.has(player, Condition.FRIGHTENED) && frightenedSourceInSight(player)) {
                c.disadvantage(RollFilter.kind(RollKind.CHECK), "ability checks (Frightened)", "Frightened");
            }
            if (Conditions.has(player, Condition.POISONED)) {
                c.disadvantage(RollFilter.kind(RollKind.CHECK), "ability checks (Poisoned)", "Poisoned");
            }
            if (Conditions.has(player, Condition.BLINDED)) {
                c.disadvantage(RollFilter.check(Skill.PERCEPTION).or(RollFilter.tag("search")),
                        "Perception and Search (Blinded)", "Blinded");
            }
        }, true);

        // Incapacitated players can't attack, use or break anything (before attack rolls and lockpicking see it)
        AttackEntityCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        AttackBlockCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseItemCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        PlayerBlockBreakEvents.BEFORE.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        AttackEntityCallback.EVENT.register(PHASE, (player, world, hand, entity, hit) -> blocked(player, "attack"));
        AttackBlockCallback.EVENT.register(PHASE, (player, world, hand, pos, dir) -> blocked(player, "break"));
        UseBlockCallback.EVENT.register(PHASE, (player, world, hand, hit) -> blocked(player, "use block"));
        UseEntityCallback.EVENT.register(PHASE, (player, world, hand, entity, hit) -> blocked(player, "use entity"));
        UseItemCallback.EVENT.register(PHASE, (player, world, hand) -> blocked(player, "use item") == ActionResult.FAIL
                ? TypedActionResult.fail(player.getStackInHand(hand))
                : TypedActionResult.pass(player.getStackInHand(hand)));
        PlayerBlockBreakEvents.BEFORE.register(PHASE,
                (world, player, pos, state, blockEntity) -> blocked(player, "break") != ActionResult.FAIL);
        RestEvents.ALLOW_REST.register((player, kind) -> Conditions.canAct(player) ? null
                : Text.translatable("condition.dndclasses.blocked", incapacitatingName(player)));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(ConditionEffects::allowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> Conditions.endMatching(entity,
                inst -> true, ConditionEvents.EndReason.DEATH, null));
        DownedEvents.AFTER_DOWNED.register((player, source) -> Conditions.endMatching(player,
                inst -> inst.condition() != Condition.PETRIFIED, ConditionEvents.EndReason.DOWNED, null));

        // Every path mob can flee whatever frightens it
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PathAwareEntity mob) {
                attachFleeGoal(mob);
            }
        });
    }

    static void attachFleeGoal(PathAwareEntity mob) {
        var goals = ((MobEntityAccessor) mob).getGoalSelector();
        for (PrioritizedGoal goal : goals.getGoals()) {
            if (goal.getGoal() instanceof ConditionFleeGoal) {
                return;
            }
        }
        goals.add(1, new ConditionFleeGoal(mob, 16.0F, 1.0D, 1.3D));
    }

    // ------------------------------------------------------------------ queries

    /** The first incapacitating condition's name, for messages. */
    static Text incapacitatingName(LivingEntity entity) {
        for (Condition c : new Condition[] { Condition.PETRIFIED, Condition.PARALYZED, Condition.STUNNED,
                Condition.INCAPACITATED }) {
            if (Conditions.has(entity, c)) {
                return Text.translatable(c.translationKey());
            }
        }
        return Text.translatable(Condition.INCAPACITATED.translationKey());
    }

    /**
     * Whether a Frightened creature's fear is "in sight": a sourceless fear always is; otherwise any source within
     * {@value #FRIGHTENED_SIGHT} blocks and in line of sight. Server only (the client clamp uses its own copy).
     */
    public static boolean frightenedSourceInSight(LivingEntity entity) {
        if (entity.getWorld().isClient) {
            return Conditions.has(entity, Condition.FRIGHTENED);
        }
        for (ConditionInstance inst : Conditions.all(entity)) {
            if (inst.condition() != Condition.FRIGHTENED) {
                continue;
            }
            if (inst.source() == null) {
                return true;
            }
            Entity source = Conditions.resolve(entity, inst.source());
            if (source != null && source.squaredDistanceTo(entity) <= FRIGHTENED_SIGHT * FRIGHTENED_SIGHT
                    && entity.canSee(source)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code entity} has an instance of {@code condition} from {@code source}. */
    public static boolean hasFrom(LivingEntity entity, Condition condition, @Nullable UUID source) {
        if (source == null || !Conditions.has(entity, condition)) {
            return false;
        }
        for (ConditionInstance inst : Conditions.all(entity)) {
            if (inst.condition() == condition && source.equals(inst.source())) {
                return true;
            }
        }
        return false;
    }

    /** An indefinitely Petrified mob: a statue. */
    public static boolean isStatue(LivingEntity entity) {
        if (!Conditions.has(entity, Condition.PETRIFIED) || entity.getWorld().isClient) {
            return false;
        }
        ConditionInstance inst = Conditions.get(entity, Condition.PETRIFIED);
        return inst != null && inst.indefinite();
    }

    /** Sources of advantage the target's conditions give an attacker at this distance (melee). */
    private static int advantagesAgainst(LivingEntity target, @Nullable Entity attacker) {
        int n = 0;
        if (Conditions.has(target, Condition.PRONE) && attacker != null
                && attacker.distanceTo(target) <= PRONE_MELEE_RANGE) {
            n++;
        }
        for (Condition c : new Condition[] { Condition.RESTRAINED, Condition.BLINDED, Condition.STUNNED,
                Condition.PETRIFIED, Condition.PARALYZED }) {
            if (Conditions.has(target, c)) {
                n++;
            }
        }
        return n;
    }

    /** Sources of disadvantage the attacker's own conditions give it. */
    private static int disadvantagesOf(LivingEntity attacker) {
        int n = 0;
        for (Condition c : new Condition[] { Condition.PRONE, Condition.RESTRAINED, Condition.BLINDED,
                Condition.POISONED }) {
            if (Conditions.has(attacker, c)) {
                n++;
            }
        }
        if (Conditions.has(attacker, Condition.FRIGHTENED) && frightenedSourceInSight(attacker)) {
            n++;
        }
        return n;
    }

    /** {@link AttackRolls} mode: a player's melee attack roll against this target. */
    static Advantage attackMode(PlayerEntity player, Entity target) {
        int adv = target instanceof LivingEntity living ? advantagesAgainst(living, player) : 0;
        int dis = disadvantagesOf(player);
        return Advantage.resolve(adv, dis);
    }

    // ------------------------------------------------------------------ actions

    private static ActionResult blocked(PlayerEntity player, String what) {
        if (Conditions.canAct(player)) {
            return ActionResult.PASS;
        }
        Integer last = LAST_BLOCK_MESSAGE.get(player.getUuid());
        if (last == null || Math.abs(player.age - last) >= 10) {
            LAST_BLOCK_MESSAGE.put(player.getUuid(), player.age);
            DnDClasses.LOGGER.info("[Conditions] {} blocked: {} ({}, {})", player.getEntityName(), what,
                    incapacitatingName(player).getString(), player.getWorld().isClient ? "client" : "server");
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.translatable("condition.dndclasses.blocked", incapacitatingName(player))
                        .formatted(Formatting.RED), true);
            }
        }
        return ActionResult.FAIL;
    }

    // ------------------------------------------------------------------ damage

    private static boolean shatters(LivingEntity target, DamageSource source) {
        return source.getAttacker() instanceof PlayerEntity player && source.getSource() == player
                && player.getMainHandStack().getItem() instanceof PickaxeItem;
    }

    /** Server ALLOW_DAMAGE: statues, and charmed creatures that won't hurt their charmer. */
    private static boolean allowDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target instanceof PlayerEntity) && isStatue(target)
                && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            boolean shatters = shatters(target, source);
            DnDClasses.LOGGER.info("[Conditions] statue {} hit by {} ({}, {}): {}", Conditions.name(target),
                    source.getAttacker() == null ? "nothing" : Conditions.name(source.getAttacker()), source.getName(),
                    source.getAttacker() instanceof LivingEntity a ? a.getMainHandStack().getItem() : "-",
                    shatters ? "shatters" : "no effect");
            if (!shatters) {
                return false; // only a pickaxe breaks a statue
            }
        }
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity living && hasFrom(living, Condition.CHARMED, target.getUuid())) {
            if (living instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("condition.dndclasses.charmed.refuse", target.getDisplayName())
                        .formatted(Formatting.LIGHT_PURPLE), true);
            }
            return false;
        }
        // Charm breaks when the charmer (or one of its thralls) hurts the charmed creature
        if (attacker != null && Conditions.has(target, Condition.CHARMED)) {
            for (ConditionInstance inst : Conditions.all(target)) {
                if (inst.condition() == Condition.CHARMED && inst.source() != null
                        && (inst.source().equals(attacker.getUuid()) || attacker instanceof LivingEntity a
                                && hasFrom(a, Condition.CHARMED, inst.source()))) {
                    Conditions.endMatching(target, i -> i == inst, ConditionEvents.EndReason.REMOVED,
                            "charm broken");
                }
            }
        }
        return true;
    }

    /**
     * Damage multipliers from conditions, after the class and item modifiers ({@code ProgressionEvents.modifyDamage}).
     * Server only.
     */
    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (target.getWorld().isClient || amount <= 0.0F) {
            return amount;
        }
        Entity attacker = source.getAttacker();
        boolean melee = attacker instanceof LivingEntity && source.getSource() == attacker;
        if (Conditions.has(target, Condition.PETRIFIED)) {
            if (shatters(target, source) && isStatue(target)) {
                return Math.max(amount, target.getMaxHealth() * 10.0F);
            }
            amount *= PETRIFIED_DAMAGE;
        }
        if (Conditions.has(target, Condition.PRONE) && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            amount *= PRONE_PROJECTILE;
        }
        if (melee) {
            LivingEntity living = (LivingEntity) attacker;
            if (Conditions.has(living, Condition.FRIGHTENED)) {
                amount *= FRIGHTENED_DAMAGE;
            }
            if (!(living instanceof PlayerEntity)) {
                if (Conditions.has(target, Condition.PARALYZED)
                        && living.distanceTo(target) <= PARALYZED_CRIT_RANGE) {
                    return amount * 2.0F; // an automatic critical
                }
                int adv = advantagesAgainst(target, living);
                int dis = disadvantagesOf(living);
                Advantage mode = Advantage.resolve(adv, dis);
                if (mode == Advantage.ADVANTAGE) {
                    amount *= MOB_ADVANTAGE;
                } else if (mode == Advantage.DISADVANTAGE) {
                    boolean onlyPoison = dis == 1 && Conditions.has(living, Condition.POISONED);
                    amount *= onlyPoison ? MOB_POISONED : MOB_DISADVANTAGE;
                }
            }
        }
        return amount;
    }

    // ------------------------------------------------------------------ targeting

    /**
     * Whether a mob may not target {@code target}: Petrified creatures are ignored, Blinded mobs can't see past
     * {@value #BLINDED_TARGET_RANGE} blocks, and Charmed mobs won't go for their charmer or its other thralls.
     * Called from {@code DownedTargetMixin} on {@code LivingEntity.canTarget}.
     */
    public static boolean blocksTarget(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof PlayerEntity || attacker.getWorld().isClient) {
            return false;
        }
        if (Conditions.has(target, Condition.PETRIFIED)) {
            return true;
        }
        if (Conditions.has(attacker, Condition.BLINDED)
                && attacker.squaredDistanceTo(target) > BLINDED_TARGET_RANGE * BLINDED_TARGET_RANGE) {
            return true;
        }
        if (Conditions.has(attacker, Condition.CHARMED)) {
            for (ConditionInstance inst : Conditions.all(attacker)) {
                if (inst.condition() == Condition.CHARMED && inst.source() != null
                        && (inst.source().equals(target.getUuid())
                                || hasFrom(target, Condition.CHARMED, inst.source()))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ lifecycle

    static void onApplied(LivingEntity target, ConditionInstance inst, boolean freshCondition, boolean silent) {
        Condition c = inst.condition();
        if (target instanceof ServerPlayerEntity player && !silent && freshCondition) {
            player.sendMessage(appliedMessage(inst).formatted(Formatting.GOLD), true);
        }
        if (c.incapacitates() || c.roots()) {
            target.stopUsingItem(); // a raised shield drops
            if (target instanceof MobEntity mob) {
                mob.getNavigation().stop();
            }
        }
        if (c == Condition.PETRIFIED) {
            Conditions.remove(target, Condition.POISONED);
            target.removeStatusEffect(StatusEffects.POISON);
            setupStatue(target);
        }
        if (c == Condition.BLINDED && target instanceof PlayerEntity) {
            hiddenBlindness(target);
        }
    }

    private static net.minecraft.text.MutableText appliedMessage(ConditionInstance inst) {
        Text name = Text.translatable(inst.condition().translationKey());
        RepeatSave save = inst.save();
        if (inst.sourceName() != null) {
            if (save != null) {
                return Text.translatable("condition.dndclasses.applied_save", name, inst.sourceName(),
                        Text.translatable(save.ability().shortTranslationKey()), seconds(save.every()));
            }
            return Text.translatable("condition.dndclasses.applied", name, inst.sourceName());
        }
        if (save != null) {
            return Text.translatable("condition.dndclasses.applied_self_save", name,
                    Text.translatable(save.ability().shortTranslationKey()), seconds(save.every()));
        }
        return Text.translatable("condition.dndclasses.applied_self", name);
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    static void onEnded(LivingEntity entity, ConditionInstance inst, ConditionEvents.EndReason reason) {
        Condition c = inst.condition();
        boolean stillHas = Conditions.has(entity, c);
        if (entity instanceof ServerPlayerEntity player && !stillHas && reason != ConditionEvents.EndReason.DEATH
                && reason != ConditionEvents.EndReason.ESCALATED) {
            player.sendMessage(Text.translatable("condition.dndclasses.ended", Text.translatable(c.translationKey()))
                    .formatted(Formatting.GREEN), true);
        }
        if (c == Condition.BLINDED && !stillHas) {
            StatusEffectInstance blind = entity.getStatusEffect(StatusEffects.BLINDNESS);
            if (blind != null && !blind.shouldShowIcon()) {
                entity.removeStatusEffect(StatusEffects.BLINDNESS);
            }
        }
        if (c == Condition.PETRIFIED && !stillHas) {
            var data = ((IEntityDataSaver) entity).getPersistentData();
            if (data.getBoolean(STATUE_KEY)) {
                data.remove(STATUE_KEY);
                entity.setSilent(false);
            }
        }
    }

    static void onLoaded(LivingEntity entity) {
        updateAttributes(entity);
        if (Conditions.has(entity, Condition.PETRIFIED)) {
            setupStatue(entity);
        }
    }

    private static void setupStatue(LivingEntity entity) {
        if (entity instanceof MobEntity mob && isStatue(mob)) {
            mob.setPersistent();
            if (!mob.isSilent()) {
                mob.setSilent(true);
                ((IEntityDataSaver) mob).getPersistentData().putBoolean(STATUE_KEY, true);
            }
            mob.setTarget(null);
        }
    }

    static void onMaskChanged(LivingEntity entity, int before, int after) {
        updateAttributes(entity);
        if (before != after && entity instanceof PlayerEntity player) {
            AbilityScores.invalidate(player);
        }
        if ((after & Condition.INCAPACITATED.bit()) != 0 && entity instanceof MobEntity mob) {
            mob.getNavigation().stop();
        }
    }

    private static void updateAttributes(LivingEntity entity) {
        boolean rooted = !Conditions.canMove(entity);
        boolean prone = Conditions.has(entity, Condition.PRONE);
        setModifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, ROOTED_ID, "Condition: rooted", -1.0D,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL, rooted);
        setModifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, PRONE_ID, "Condition: prone", -0.5D,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL, prone && !rooted);
        setModifier(entity, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, STONE_ID, "Condition: petrified", 1.0D,
                EntityAttributeModifier.Operation.ADDITION, Conditions.has(entity, Condition.PETRIFIED));
        if (prone && entity.isSprinting()) {
            entity.setSprinting(false);
        }
    }

    private static void setModifier(LivingEntity entity, EntityAttribute attribute, UUID id, String name,
            double amount, EntityAttributeModifier.Operation op, boolean wanted) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        boolean has = instance.getModifier(id) != null;
        if (wanted && !has) {
            instance.addTemporaryModifier(new EntityAttributeModifier(id, name, amount, op));
        } else if (!wanted && has) {
            instance.removeModifier(id);
        }
    }

    private static void hiddenBlindness(LivingEntity entity) {
        StatusEffectInstance current = entity.getStatusEffect(StatusEffects.BLINDNESS);
        if (current == null || !current.shouldShowIcon() && current.getDuration() < 30) {
            // No icon, no particles: the condition's HUD icon says it
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
        }
    }

    // ------------------------------------------------------------------ per tick

    /** Upkeep for an entity with live conditions, every tick (server). */
    static void tick(LivingEntity entity, long now) {
        ServerWorld world = (ServerWorld) entity.getWorld();
        int mask = Conditions.mask(entity);
        if ((mask & (Condition.STUNNED.bit() | Condition.PARALYZED.bit())) != 0 && now % 4 == 0) {
            // Stars circling over the head
            double angle = now * 0.35D;
            double y = entity.getY() + entity.getHeight() + 0.25D;
            for (int i = 0; i < 2; i++) {
                double a = angle + i * Math.PI;
                world.spawnParticles(ParticleTypes.WAX_OFF, entity.getX() + Math.cos(a) * 0.45D, y,
                        entity.getZ() + Math.sin(a) * 0.45D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if ((mask & Condition.PRONE.bit()) != 0 && !(entity instanceof PlayerEntity) && now % 10 == 0) {
            BlockState below = world.getBlockState(entity.getBlockPos().down());
            if (!below.isAir()) {
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, below), entity.getX(),
                        entity.getY() + 0.1D, entity.getZ(), 4, entity.getWidth() * 0.3D, 0.05D,
                        entity.getWidth() * 0.3D, 0.0D);
            }
        }
        if ((mask & Condition.CHARMED.bit()) != 0 && now % 20 == 0) {
            world.spawnParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getHeight() + 0.3D,
                    entity.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
        if ((mask & Condition.BLINDED.bit()) != 0 && entity instanceof PlayerEntity && now % 10 == 0) {
            hiddenBlindness(entity);
        }
        if (now % 10 != 0) {
            return;
        }
        if ((mask & Condition.PETRIFIED.bit()) != 0) {
            dropTargetsOn(entity, world);
        }
        if (entity instanceof MobEntity mob) {
            LivingEntity target = mob.getTarget();
            if ((mask & Condition.BLINDED.bit()) != 0 && target != null
                    && mob.squaredDistanceTo(target) > BLINDED_TARGET_RANGE * BLINDED_TARGET_RANGE) {
                mob.setTarget(null);
            }
            if ((mask & Condition.CHARMED.bit()) != 0 && Conditions.canAct(mob)) {
                thrall(mob);
            }
        }
    }

    /** Mobs attacking a statue lose interest. */
    private static void dropTargetsOn(LivingEntity statue, ServerWorld world) {
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, statue.getBoundingBox().expand(48.0D),
                m -> m.getTarget() == statue)) {
            mob.setTarget(null);
            DnDClasses.LOGGER.info("[Conditions] {} stops targeting petrified {}", mob.getName().getString(),
                    Conditions.name(statue));
        }
    }

    /** What the mob that has Charmed {@code mob} is fighting, or null (not charmed by a living mob, or no target). */
    @org.jetbrains.annotations.Nullable
    public static LivingEntity charmerTarget(MobEntity mob) {
        for (ConditionInstance inst : Conditions.all(mob)) {
            if (inst.condition() == Condition.CHARMED
                    && Conditions.resolve(mob, inst.source()) instanceof MobEntity master && master.isAlive()) {
                return master.getTarget();
            }
        }
        return null;
    }

    /** A charmed mob follows a mob charmer and fights what it fights. */
    private static void thrall(MobEntity mob) {
        for (ConditionInstance inst : Conditions.all(mob)) {
            if (inst.condition() != Condition.CHARMED) {
                continue;
            }
            Entity charmer = Conditions.resolve(mob, inst.source());
            if (!(charmer instanceof MobEntity master) || !master.isAlive()) {
                continue;
            }
            if (mob.getTarget() == master) {
                mob.setTarget(null);
            }
            LivingEntity wanted = master.getTarget();
            if (wanted != null && wanted != mob && wanted.isAlive() && !blocksTarget(mob, wanted)) {
                mob.setTarget(wanted);
            } else if (mob.squaredDistanceTo(master) > THRALL_FOLLOW * THRALL_FOLLOW) {
                mob.getNavigation().startMovingTo(master, 1.1D);
            }
            return;
        }
    }
}
