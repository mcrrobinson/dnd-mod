package mattonfire.dnd.classes.SkillChecks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.RollKind;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Saving throws against monsters, traps and items: d20 + the target's save bonus against a DC, shown on the
 * compact save lane beside the crosshair.
 *
 * <pre>{@code
 * SaveResult r = SavingThrow.of(victim, Ability.DEX, 14)
 *         .label("save.dndclasses.dragon_breath")   // shown on the save lane (default "DEX save")
 *         .source(dragon)                           // the attacker
 *         .exposure(dragon, "breath", 60)           // one roll per breath, reused for 60 ticks
 *         .halvesDamage()
 *         .roll();
 * victim.damage(source, r.damage(4.0F));
 * if (r.failed()) { applyFreeze(victim); }
 * }</pre>
 *
 * Anti-spam rules:
 * <ul>
 * <li><b>Exposures:</b> with {@link Builder#exposure}, a save is rolled once per exposure (one breath, one ray,
 * one grab), not per damage tick. Later calls inside the window return the cached result without rolling.</li>
 * <li><b>Throttle:</b> at most one save packet per player per {@link #THROTTLE_TICKS}. Saves in between are
 * still applied and arrive in the next packet as a count, which the client shows as "x N" on one row.</li>
 * <li>Non-players (pets, villagers, mobs) roll silently with a flat {@link #NON_PLAYER_BONUS}; creative and
 * spectator players roll but aren't shown anything.</li>
 * </ul>
 */
public final class SavingThrow {
    /** Minimum ticks between two save-lane packets to one player. */
    public static final int THROTTLE_TICKS = 10;
    /** Flat save bonus for anything without a character sheet. */
    public static final int NON_PLAYER_BONUS = 1;
    private static final int PRUNE_EVERY_TICKS = 200;

    private record Cached(SaveResult result, long expiresAt) {
    }

    /** Target UUID to exposure key (attacker UUID + ":" + tag) to its result. */
    private static final Map<UUID, Map<String, Cached>> EXPOSURES = new HashMap<>();

    private static final class Pending {
        D20.Roll roll;
        Text detail;
        int count;

        Pending(D20.Roll roll, Text detail) {
            this.roll = roll;
            this.detail = detail;
        }
    }

    private static final class Lane {
        int lastSent = Integer.MIN_VALUE / 2;
        final LinkedHashMap<String, Pending> pending = new LinkedHashMap<>();
    }

    /** Player UUID to their throttle state. */
    private static final Map<UUID, Lane> LANES = new HashMap<>();

    private SavingThrow() {
    }

    /** A saving throw by {@code target} with an ability against a DC. */
    public static Builder of(LivingEntity target, Ability ability, int dc) {
        return new Builder(target, ability, dc);
    }

    /** A saving throw from a creature's registered save (label, ability, DC and half damage from the entry). */
    public static Builder of(LivingEntity target, MobSaveInfo.Entry entry) {
        Builder b = new Builder(target, entry.ability(), entry.dc()).label(entry.labelKey());
        return entry.halvesDamage() ? b.halvesDamage() : b;
    }

    public static final class Builder {
        private final LivingEntity target;
        private final Ability ability;
        private int dc;
        @Nullable
        private Text label;
        @Nullable
        private Entity source;
        @Nullable
        private String exposureKey;
        private int exposureTicks;
        private boolean halvesDamage;
        private String[] tags = new String[0];
        private int advantages;
        private int disadvantages;
        private boolean rerollNaturalOnes;
        private final List<D20.Bonus> bonuses = new ArrayList<>();
        @Nullable
        private Text onSuccess;
        @Nullable
        private Text onFailure;
        private boolean silent;
        private boolean secret;

        private Builder(LivingEntity target, Ability ability, int dc) {
            this.target = target;
            this.ability = ability;
            this.dc = dc;
        }

        /** Replaces the DC (e.g. +1 when a boss is enraged). */
        public Builder dc(int dc) {
            this.dc = dc;
            return this;
        }

        /** What the save is against, as a translation key ("Fire breath"); the lane adds the ability. */
        public Builder label(String translationKey) {
            return label(Text.translatable(translationKey));
        }

        public Builder label(Text label) {
            this.label = label;
            return this;
        }

        /** Who or what forced the save (for logs, and the exposure key when none is given). */
        public Builder source(@Nullable Entity source) {
            this.source = source;
            return this;
        }

        /**
         * One roll per exposure: calls with the same attacker and tag within {@code windowTicks} of the first
         * roll reuse its result (no roll, no packet).
         */
        public Builder exposure(@Nullable Entity attacker, String tag, int windowTicks) {
            return exposure(attacker == null ? "none" : attacker.getUuidAsString(), tag, windowTicks);
        }

        /** As {@link #exposure(Entity, String, int)} for sources that aren't entities (a trap's block pos). */
        public Builder exposure(String sourceKey, String tag, int windowTicks) {
            this.exposureKey = sourceKey + ":" + tag;
            this.exposureTicks = windowTicks;
            return this;
        }

        /** A success halves the damage ({@link SaveResult#damage}). */
        public Builder halvesDamage() {
            this.halvesDamage = true;
            return this;
        }

        /** Keywords for advantage filters on the sheet ("poison", "fear", "grapple", "trap"). */
        public Builder tags(String... tags) {
            this.tags = tags;
            return this;
        }

        public Builder advantage() {
            advantages++;
            return this;
        }

        public Builder disadvantage() {
            disadvantages++;
            return this;
        }

        public Builder advantageIf(boolean condition) {
            return condition ? advantage() : this;
        }

        public Builder disadvantageIf(boolean condition) {
            return condition ? disadvantage() : this;
        }

        public Builder mode(Advantage mode) {
            return switch (mode) {
                case ADVANTAGE -> advantage();
                case DISADVANTAGE -> disadvantage();
                case NORMAL -> this;
            };
        }

        /** A named bonus on this save only ("+2 aura"). */
        public Builder bonus(Text label, int amount) {
            bonuses.add(new D20.Bonus(label, amount));
            return this;
        }

        /** Reroll a natural 1 once (on top of the sheet's Halfling Lucky). */
        public Builder rerollNaturalOnes() {
            this.rerollNaturalOnes = true;
            return this;
        }

        /** The words after the tick or cross on the lane ("half damage", "not frightened"). */
        public Builder onSuccess(Text text) {
            this.onSuccess = text;
            return this;
        }

        public Builder onFailure(Text text) {
            this.onFailure = text;
            return this;
        }

        /** Roll and log, but show nothing (passive and background saves). */
        public Builder silent() {
            this.silent = true;
            return this;
        }

        /** Show the total but not the DC or outcome (DM secret rolls). */
        public Builder secret() {
            this.secret = true;
            return this;
        }

        /** Rolls (or reuses the exposure's result), shows it, and returns the result. */
        public SaveResult roll() {
            long now = target.getWorld().getTime();
            if (exposureKey != null) {
                Map<String, Cached> byTarget = EXPOSURES.get(target.getUuid());
                Cached cached = byTarget == null ? null : byTarget.get(exposureKey);
                if (cached != null && cached.expiresAt() > now) {
                    return cached.result().cached();
                }
            }
            D20.Roll roll = rollDie();
            SaveResult result = new SaveResult(roll, halvesDamage, true);
            if (exposureKey != null) {
                EXPOSURES.computeIfAbsent(target.getUuid(), k -> new HashMap<>()).put(exposureKey,
                        new Cached(result, now + exposureTicks));
            }
            if (target instanceof PlayerEntity player) {
                D20.show(player, roll, detail(result));
            }
            return result;
        }

        private D20.Roll rollDie() {
            D20.Builder b;
            boolean hidden = silent;
            if (target instanceof PlayerEntity player) {
                b = SkillCheck.save(player, ability, dc, tags);
                hidden |= player.isCreative() || player.isSpectator();
            } else {
                b = D20.roll(target).label(ability.saveTranslationKey()).ability(ability).kind(RollKind.SAVE)
                        .modifier(NON_PLAYER_BONUS).dc(dc);
                hidden = true;
            }
            if (label != null) {
                b.label(label);
            }
            for (int i = 0; i < advantages; i++) {
                b.advantage();
            }
            for (int i = 0; i < disadvantages; i++) {
                b.disadvantage();
            }
            for (D20.Bonus bonus : bonuses) {
                b.bonus(bonus.label(), bonus.amount());
            }
            if (rerollNaturalOnes) {
                b.rerollNaturalOnes(true);
            }
            if (secret) {
                b.secret();
            }
            return b.display(hidden ? D20.Display.SILENT : D20.Display.SAVE_LANE).roll();
        }

        private Text detail(SaveResult result) {
            if (result.succeeded()) {
                return onSuccess != null ? onSuccess
                        : halvesDamage ? Text.translatable("save.dndclasses.effect.half_damage") : Text.empty();
            }
            return onFailure != null ? onFailure
                    : halvesDamage ? Text.translatable("save.dndclasses.effect.full_damage") : Text.empty();
        }
    }

    /**
     * Sends a save-lane roll to its player, throttled: straight away if the last save packet was at least
     * {@link #THROTTLE_TICKS} ago, otherwise merged with any waiting save of the same label and outcome and sent
     * when the window opens. {@link D20#show} calls this for every SAVE_LANE roll.
     */
    static synchronized void send(ServerPlayerEntity player, D20.Roll roll, Text detail) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        int now = server.getTicks();
        Lane lane = LANES.computeIfAbsent(player.getUuid(), k -> new Lane());
        if (lane.pending.isEmpty() && now - lane.lastSent >= THROTTLE_TICKS) {
            lane.lastSent = now;
            sendNow(player, roll, detail, 1);
            return;
        }
        String key = mergeKey(roll);
        Pending pending = lane.pending.computeIfAbsent(key, k -> new Pending(roll, detail));
        pending.roll = roll;
        pending.detail = detail;
        pending.count++;
    }

    /** Rows merge when they're the same save with the same outcome. */
    static String mergeKey(D20.Roll roll) {
        return roll.label().getString() + "|" + roll.ability() + "|" + roll.outcome();
    }

    private static void sendNow(ServerPlayerEntity player, D20.Roll roll, Text detail, int count) {
        DnDClasses.LOGGER.info("[D20] save lane -> " + player.getEntityName() + ": " + roll.label().getString()
                + " " + roll.outcome() + " x" + count);
        D20.send(player, roll, detail, count);
    }

    private static synchronized void tick(MinecraftServer server) {
        int now = server.getTicks();
        Iterator<Map.Entry<UUID, Lane>> lanes = LANES.entrySet().iterator();
        while (lanes.hasNext()) {
            Map.Entry<UUID, Lane> e = lanes.next();
            Lane lane = e.getValue();
            if (lane.pending.isEmpty()) {
                if (now - lane.lastSent > 20 * 60) {
                    lanes.remove();
                }
                continue;
            }
            if (now - lane.lastSent < THROTTLE_TICKS) {
                continue;
            }
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) {
                lanes.remove();
                continue;
            }
            Iterator<Pending> it = lane.pending.values().iterator();
            Pending next = it.next();
            it.remove();
            lane.lastSent = now;
            sendNow(player, next.roll, next.detail, next.count);
        }
        if (now % PRUNE_EVERY_TICKS == 0) {
            long time = server.getOverworld().getTime();
            EXPOSURES.values().forEach(m -> m.values().removeIf(c -> c.expiresAt() <= time));
            EXPOSURES.values().removeIf(Map::isEmpty);
        }
    }

    /** Drops a player's cached exposures and waiting packets; called on disconnect. */
    public static synchronized void forget(UUID player) {
        EXPOSURES.remove(player);
        LANES.remove(player);
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SavingThrow::tick);
    }
}
