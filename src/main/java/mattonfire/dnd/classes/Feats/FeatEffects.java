package mattonfire.dnd.classes.Feats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import org.joml.Vector3f;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionEffects;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Rest.DndRules;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import mattonfire.dnd.classes.SkillChecks.AttackRolls;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.mixin.LivingEntityAccessor;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.magic.MagicMissiles;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

/**
 * The nine feats' effects and the hooks they're wired into: {@link FeatEffect}s per feat (dispatched from
 * {@code ProgressionEvents}), plus the D20 fumble range and reroll, the attack roll mode, the mob attack mode and
 * the rest hook. Numbers are in {@code docs/systems/feats.md}.
 */
public final class FeatEffects {
    public static final int FAST_TICKS = 5;

    /** Great Weapon Master: Power Attack's damage and fumble range, and the cooldown a Cleave sets (ready). */
    public static final float POWER_ATTACK_DAMAGE = 5.0F;
    public static final int POWER_ATTACK_FUMBLE = 4;
    public static final int CLEAVE_COOLDOWN_TICKS = 100;
    private static final java.util.Set<UUID> PENDING_CLEAVE = new java.util.HashSet<>();

    /** Mobile: how long a mob you hit has disadvantage against you. */
    public static final int HIT_AND_RUN_TICKS = 40;

    /** War Caster's opportunity bolt. */
    public static final double BOLT_RANGE = 3.0;
    public static final int BOLT_COOLDOWN_TICKS = 200;
    public static final float BOLT_DAMAGE = 2.0F;

    /** Alert's first to act. */
    public static final int ALERT_TICKS = 60;
    public static final int ALERT_COOLDOWN_TICKS = 600;
    public static final double ALERT_RANGE = 32.0;

    /** Sentinel. */
    public static final int SENTINEL_SLOW_TICKS = 20;
    public static final int SENTINEL_SLOW_AMPLIFIER = 2; // Slowness III
    public static final double GUARDIAN_RANGE = 4.0;
    public static final int GUARDIAN_TICKS = 60;
    public static final int GUARDIAN_RESTRAIN_TICKS = 20;

    private static final Map<Feat, FeatEffect> EFFECTS = new EnumMap<>(Feat.class);

    /** Server tick each player's bolt and Alert are ready again. */
    private static final Map<UUID, Integer> BOLT_READY = new HashMap<>();
    private static final Map<UUID, Integer> ALERT_READY = new HashMap<>();
    /** Hostile mobs that targeted each Alert player at the last fast tick, and whether they were in a boss fight. */
    private static final Map<UUID, Set<Integer>> TARGETED_BY = new HashMap<>();
    private static final Map<UUID, Boolean> IN_BOSS_FIGHT = new HashMap<>();
    /** Mobile: mob -> player it was hit by -> tick the disadvantage ends. */
    private static final Map<LivingEntity, Map<UUID, Integer>> HIT_AND_RUN = new WeakHashMap<>();
    /** Sentinel's Guardian: mob -> Sentinel who saw it hurt someone -> tick the mark ends. */
    private static final Map<LivingEntity, Map<UUID, Integer>> GUARDIAN_MARKS = new WeakHashMap<>();

    private static final DustParticleEffect GUARDIAN_DUST = new DustParticleEffect(new Vector3f(0.9F, 0.1F, 0.1F),
            1.0F);

    private static MinecraftServer server;

    private FeatEffects() {
    }

    static void register() {
        EFFECTS.put(Feat.GREAT_WEAPON_MASTER, new FeatEffect() {
            @Override
            public float modifyDealtDamage(ServerPlayerEntity player, LivingEntity target, DamageSource source,
                    float amount) {
                if (isMelee(player, source) && Feats.toggleOn(player) && Feats.isHeavy(player.getMainHandStack())
                        && AttackRolls.isFullStrength(player, target)) {
                    DnDClasses.LOGGER.info("[Feats] {} Power Attack +{} on {} ({} -> {})", player.getEntityName(),
                            POWER_ATTACK_DAMAGE, target.getName().getString(), amount, amount + POWER_ATTACK_DAMAGE);
                    return amount + POWER_ATTACK_DAMAGE;
                }
                return amount;
            }
        });
        EFFECTS.put(Feat.LUCKY, new FeatEffect() {
            @Override
            public void secondTick(ServerPlayerEntity player) {
                // With rests off, the points come back at each dawn
                if (!DndRules.rests(player.getWorld()) && Feats.day(player) != Feats.luckDay(player)
                        && Feats.refillLuck(player)) {
                    player.sendMessage(Text.literal("Lucky: your luck returns with the dawn ("
                            + Feats.LUCK_POINTS + " points).").formatted(Formatting.GOLD), true);
                }
            }
        });
        EFFECTS.put(Feat.MOBILE, new FeatEffect() {
            @Override
            public float modifyDealtDamage(ServerPlayerEntity player, LivingEntity target, DamageSource source,
                    float amount) {
                if (isMelee(player, source)) {
                    HIT_AND_RUN.computeIfAbsent(target, t -> new HashMap<>()).put(player.getUuid(),
                            now() + HIT_AND_RUN_TICKS);
                }
                return amount;
            }
        });
        EFFECTS.put(Feat.WAR_CASTER, new FeatEffect() {
            @Override
            public void fastTick(ServerPlayerEntity player) {
                opportunityBolt(player);
            }
        });
        EFFECTS.put(Feat.ALERT, new FeatEffect() {
            @Override
            public void fastTick(ServerPlayerEntity player) {
                firstToAct(player);
            }
        });
        EFFECTS.put(Feat.SENTINEL, new FeatEffect() {
            @Override
            public float modifyDealtDamage(ServerPlayerEntity player, LivingEntity target, DamageSource source,
                    float amount) {
                if (isMelee(player, source)) {
                    sentinelHit(player, target);
                }
                return amount;
            }
        });

        // Great Weapon Master's Power Attack: attack rolls with a heavy weapon fumble on 1-4
        D20.registerFumbleRange((roller, kind) -> kind == RollKind.ATTACK && roller instanceof PlayerEntity player
                && Feats.has(player, Feat.GREAT_WEAPON_MASTER) && Feats.toggleOn(player)
                && Feats.isHeavy(player.getMainHandStack()) ? POWER_ATTACK_FUMBLE : 1);

        // Lucky: registered after Dark One's Own Luck (Progression registers feats last), so that spends first
        D20.registerReroll((player, kind, outcome) -> {
            if (kind != RollKind.CHECK && kind != RollKind.SAVE && kind != RollKind.ATTACK)
                return null;
            if (!Feats.has(player, Feat.LUCKY) || !Feats.spendLuck(player))
                return null;
            int left = Feats.luckPoints(player);
            String reason = "Lucky (" + left + " left)";
            player.sendMessage(Text.literal("Lucky: you roll again (" + left + " left)").formatted(Formatting.GOLD),
                    true);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.PLAYERS, 0.8F, 1.6F);
            return reason;
        });
        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind == RestKind.LONG && Feats.has(player, Feat.LUCKY) && Feats.refillLuck(player)) {
                player.sendMessage(Text.literal("Lucky: your luck points are back (" + Feats.LUCK_POINTS + ").")
                        .formatted(Formatting.GOLD), false);
            }
        });

        // Sentinel's Guardian: advantage on the next melee hit on a mob that hurt someone else
        AttackRolls.registerMode((player, target) -> target instanceof LivingEntity living
                && guardianMarked(living, player) ? Advantage.ADVANTAGE : Advantage.NORMAL);

        // Mobile's hit and run: a mob you hit has disadvantage against you
        ConditionEffects.registerMobAttackMode("mobile", (attacker, target) -> {
            Map<UUID, Integer> marks = HIT_AND_RUN.get(attacker);
            Integer until = marks == null ? null : marks.get(target.getUuid());
            if (until == null || until <= now() || !(target instanceof PlayerEntity player)
                    || !Feats.has(player, Feat.MOBILE))
                return Advantage.NORMAL;
            DnDClasses.LOGGER.info("[Feats] {} Mobile hit and run: {} attacks with disadvantage",
                    player.getEntityName(), attacker.getName().getString());
            return Advantage.DISADVANTAGE;
        });

        // Alert: no blind spots
        ConditionEffects.registerNoBlindSpots(target -> target instanceof PlayerEntity player
                && Feats.has(player, Feat.ALERT));

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            BOLT_READY.clear();
            ALERT_READY.clear();
            TARGETED_BY.clear();
            IN_BOSS_FIGHT.clear();
            HIT_AND_RUN.clear();
            GUARDIAN_MARKS.clear();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
            UUID id = handler.player.getUuid();
            TARGETED_BY.remove(id);
            IN_BOSS_FIGHT.remove(id);
        });
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            server = s;
            if (!PENDING_CLEAVE.isEmpty()) {
                for (UUID id : PENDING_CLEAVE) {
                    ServerPlayerEntity player = s.getPlayerManager().getPlayer(id);
                    if (player != null)
                        ((LivingEntityAccessor) player).dnd$setLastAttackedTicks(CLEAVE_COOLDOWN_TICKS);
                }
                PENDING_CLEAVE.clear();
            }
            if (s.getTicks() % FAST_TICKS != 0)
                return;
            for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                for (FeatEffect effect : active(player)) {
                    effect.fastTick(player);
                }
            }
            tickGuardianMarks();
        });
    }

    private static int now() {
        return server == null ? 0 : server.getTicks();
    }

    /** A melee hit by the player itself (not a spell, a projectile or a sweep's source). */
    static boolean isMelee(PlayerEntity player, DamageSource source) {
        return source.getSource() == player && source.isOf(DamageTypes.PLAYER_ATTACK);
    }

    /** The effects of the player's current class's feats (Resilient's once). */
    private static List<FeatEffect> active(PlayerEntity player) {
        if (Progression.classOf(player) == DndCharacter.NONE)
            return List.of();
        List<FeatChoice> feats = Progression.current(player).feats;
        if (feats.isEmpty())
            return List.of();
        List<FeatEffect> out = new ArrayList<>(feats.size());
        Set<Feat> seen = new HashSet<>();
        for (FeatChoice choice : feats) {
            FeatEffect effect = EFFECTS.get(choice.feat());
            if (effect != null && seen.add(choice.feat())) {
                out.add(effect);
            }
        }
        return out;
    }

    // ---- Dispatch (ProgressionEvents) ----

    public static float modifyDealtDamage(PlayerEntity player, LivingEntity target, DamageSource source,
            float amount) {
        if (!(player instanceof ServerPlayerEntity serverPlayer))
            return amount;
        for (FeatEffect effect : active(player)) {
            amount = effect.modifyDealtDamage(serverPlayer, target, source, amount);
        }
        return amount;
    }

    public static float modifyTakenDamage(PlayerEntity player, DamageSource source, float amount) {
        for (FeatEffect effect : active(player)) {
            amount = effect.modifyTakenDamage(player, source, amount);
        }
        return amount;
    }

    public static void secondTick(ServerPlayerEntity player) {
        // Also catches a class change: the new class's feats' health and speed
        Feats.refresh(player);
        for (FeatEffect effect : active(player)) {
            effect.secondTick(player);
        }
    }

    public static void onKill(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        for (FeatEffect effect : active(player)) {
            effect.onKill(player, killed, source);
        }
    }

    /**
     * Great Weapon Master's Cleave, after {@code PlayerEntity.attack}: a critical hit or a kill with a heavy
     * weapon readies the next swing at once (the server's cooldown, and the client's through
     * {@link FeatPackets#S2C_CLEAVE}).
     */
    public static void afterAttack(PlayerEntity player, Entity target, boolean critical, boolean fullStrength) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !(target instanceof LivingEntity living))
            return;
        if (!Feats.has(player, Feat.GREAT_WEAPON_MASTER) || !Feats.isHeavy(player.getMainHandStack()))
            return;
        boolean kill = living.isDead() || !living.isAlive();
        if (!critical && !kill)
            return;
        ((LivingEntityAccessor) player).dnd$setLastAttackedTicks(CLEAVE_COOLDOWN_TICKS);
        // The client's hand-swing packet comes after the attack and resets the cooldown
        // (ServerPlayerEntity.swingHand), so set it again at the end of this tick
        PENDING_CLEAVE.add(player.getUuid());
        FeatPackets.sendCleave(serverPlayer);
        DnDClasses.LOGGER.info("[Feats] {} Cleave ({}) -> swing ready", player.getEntityName(),
                critical ? "critical" : "kill");
        serverPlayer.sendMessage(Text.literal("Cleave!").formatted(Formatting.GOLD), true);
    }

    // ---- War Caster ----

    private static void opportunityBolt(ServerPlayerEntity player) {
        Integer ready = BOLT_READY.get(player.getUuid());
        if (ready != null && now() < ready || !player.isAlive() || player.isSpectator())
            return;
        ServerWorld world = player.getWorld();
        MobEntity closest = null;
        double best = BOLT_RANGE * BOLT_RANGE;
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(BOLT_RANGE),
                m -> m instanceof Monster && m.isAlive() && m.getTarget() == player)) {
            double d = mob.squaredDistanceTo(player);
            if (d <= best) {
                best = d;
                closest = mob;
            }
        }
        if (closest == null)
            return;
        BOLT_READY.put(player.getUuid(), now() + BOLT_COOLDOWN_TICKS);
        DnDClasses.LOGGER.info("[Feats] {} War Caster opportunity bolt -> {} ({} damage)", player.getEntityName(),
                closest.getName().getString(), BOLT_DAMAGE);
        MagicMissiles.fire(world, player, closest, 1, BOLT_DAMAGE);
    }

    // ---- Alert ----

    private static void firstToAct(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Set<Integer> targeting = new HashSet<>();
        for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(ALERT_RANGE),
                m -> m instanceof Monster && m.isAlive() && m.getTarget() == player)) {
            targeting.add(mob.getId());
        }
        Set<Integer> before = TARGETED_BY.put(id, targeting);
        boolean newTarget = false;
        for (int mob : targeting) {
            if (before == null || !before.contains(mob)) {
                newTarget = true;
                break;
            }
        }
        boolean fight = BossFight.fightNear(player) != null;
        Boolean wasFight = IN_BOSS_FIGHT.put(id, fight);
        boolean fightStarted = fight && !Boolean.TRUE.equals(wasFight);
        if (!newTarget && !fightStarted)
            return;
        Integer ready = ALERT_READY.get(id);
        if (ready != null && now() < ready)
            return;
        ALERT_READY.put(id, now() + ALERT_COOLDOWN_TICKS);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ALERT_TICKS, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, ALERT_TICKS, 0));
        player.sendMessage(Text.literal("Alert: first to act!").formatted(Formatting.YELLOW), true);
        DnDClasses.LOGGER.info("[Feats] {} Alert: first to act ({})", player.getEntityName(),
                fightStarted ? "boss fight" : "targeted");
    }

    // ---- Sentinel ----

    private static void sentinelHit(ServerPlayerEntity player, LivingEntity target) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SENTINEL_SLOW_TICKS,
                SENTINEL_SLOW_AMPLIFIER), player);
        if (guardianMarked(target, player)) {
            GUARDIAN_MARKS.get(target).remove(player.getUuid());
            Conditions.of(target, Condition.RESTRAINED).source(player).ticks(GUARDIAN_RESTRAIN_TICKS).apply();
            DnDClasses.LOGGER.info("[Feats] {} Sentinel guardian strike: {} restrained", player.getEntityName(),
                    target.getName().getString());
        }
    }

    private static boolean guardianMarked(LivingEntity mob, PlayerEntity sentinel) {
        Map<UUID, Integer> marks = GUARDIAN_MARKS.get(mob);
        Integer until = marks == null ? null : marks.get(sentinel.getUuid());
        return until != null && until > now();
    }

    /**
     * Called for every hit before armor ({@code ProgressionEvents.modifyDamage}): a hostile mob hurting someone
     * marks it for each Sentinel within {@value #GUARDIAN_RANGE} blocks of it (other than the victim).
     */
    public static void onDamage(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0 || !(source.getAttacker() instanceof MobEntity mob) || !(mob instanceof Monster)
                || mob == victim || !(mob.getWorld() instanceof ServerWorld world) || victim instanceof Monster)
            return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player == victim || !player.isAlive() || player.isSpectator()
                    || player.squaredDistanceTo(mob) > GUARDIAN_RANGE * GUARDIAN_RANGE
                    || !Feats.has(player, Feat.SENTINEL))
                continue;
            Integer old = GUARDIAN_MARKS.computeIfAbsent(mob, m -> new HashMap<>()).put(player.getUuid(),
                    now() + GUARDIAN_TICKS);
            if (old == null || old <= now()) {
                DnDClasses.LOGGER.info("[Feats] {} Sentinel: {} hurt {}, marked", player.getEntityName(),
                        mob.getName().getString(), victim.getName().getString());
            }
        }
    }

    /** Red dust around guardian-marked mobs; drops expired marks (and Mobile's). */
    private static void tickGuardianMarks() {
        int now = now();
        GUARDIAN_MARKS.entrySet().removeIf(e -> {
            e.getValue().values().removeIf(until -> until <= now);
            LivingEntity mob = e.getKey();
            if (e.getValue().isEmpty() || !mob.isAlive())
                return true;
            if (mob.getWorld() instanceof ServerWorld world) {
                Box box = mob.getBoundingBox();
                for (int i = 0; i < 8; i++) {
                    double x = (i & 1) == 0 ? box.minX : box.maxX;
                    double z = (i & 2) == 0 ? box.minZ : box.maxZ;
                    double y = (i & 4) == 0 ? box.minY + 0.1 : box.maxY;
                    world.spawnParticles(GUARDIAN_DUST, x, y, z, 1, 0, 0, 0, 0);
                }
            }
            return false;
        });
        HIT_AND_RUN.entrySet().removeIf(e -> {
            e.getValue().values().removeIf(until -> until <= now);
            return e.getValue().isEmpty() || !e.getKey().isAlive();
        });
    }
}
