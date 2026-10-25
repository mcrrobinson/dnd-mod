package mattonfire.dnd.classes.Conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * One server-side status layer for 5e conditions: a {@link Condition} on a creature, with a source, a duration and
 * an optional repeat save to shake it off. Conditions aren't vanilla status effects: they don't show in the
 * inventory, milk ends only Poisoned, and every client sees them through a tracked mask.
 *
 * <pre>{@code
 * if (SavingThrow.of(victim, MIND_BLAST).source(this).roll().failed()) {
 *     Conditions.of(victim, Condition.STUNNED).source(this).ticks(60)
 *             .repeatSave(MIND_BLAST, 40).apply();
 * }
 * }</pre>
 *
 * Rules: the same condition, source and key refreshes (never stacks); initial saves are the caller's job, repeat
 * saves are rolled here; creative and spectator players are immune; bosses shrug off control after 20 ticks; death
 * clears everything and going Downed clears everything but Petrified. See {@code docs/systems/conditions.md}.
 */
public final class Conditions {
    public static final int VETO = Integer.MIN_VALUE;
    public static final int INDEFINITE = -1;
    /** Persistent-data key of the saved (non-transient) instances. */
    public static final String NBT_KEY = "dndConditions";
    /** Key of sourceless instances that didn't name one. */
    public static final String NO_KEY = "none";

    @FunctionalInterface
    public interface Immunity {
        /** A reason key ("condition.dndclasses.immune.undead") when immune, else null. */
        @Nullable
        String immune(LivingEntity target, Condition condition, @Nullable Entity source);
    }

    @FunctionalInterface
    public interface ApplyHook {
        /**
         * Called before an instance lands. Return the ticks to use (-1 = indefinite), or {@link Conditions#VETO}
         * to stop it.
         */
        int adjust(LivingEntity target, Condition condition, @Nullable Entity source, int ticks);
    }

    private static final List<Immunity> IMMUNITIES = new CopyOnWriteArrayList<>();
    private static final List<ApplyHook> APPLY_HOOKS = new CopyOnWriteArrayList<>();

    private Conditions() {
    }

    /** Wires the system in; called once from the mod initializer. */
    public static void bootstrap() {
        ConditionImmunities.register();
        ConditionEffects.register();
        ConditionTicker.register();
        ConditionSync.register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ConditionCommand.register(dispatcher));
    }

    // ------------------------------------------------------------------ queries

    /** Start building a condition on target. Server side only. */
    public static Builder of(LivingEntity target, Condition condition) {
        return new Builder(target, condition);
    }

    /**
     * Both sides (reads the synced mask on the client). INCAPACITATED is also true when any incapacitating
     * condition is present.
     */
    public static boolean has(LivingEntity entity, Condition condition) {
        return (mask(entity) & condition.bit()) != 0;
    }

    /** Both sides: the raw mask (derived INCAPACITATED bit included). */
    public static int mask(LivingEntity entity) {
        if (!(entity instanceof ConditionHolder holder)) {
            return 0;
        }
        if (entity.getWorld() != null && !entity.getWorld().isClient) {
            ensureLoaded(entity);
        }
        return holder.dnd$getConditionMask();
    }

    /** Both sides: !has(INCAPACITATED). Mob specials, player attacks, item use and the power-up key check it. */
    public static boolean canAct(LivingEntity entity) {
        return !has(entity, Condition.INCAPACITATED);
    }

    /** Both sides: false while RESTRAINED, STUNNED, PARALYZED or PETRIFIED. */
    public static boolean canMove(LivingEntity entity) {
        int mask = mask(entity);
        return (mask & (Condition.RESTRAINED.bit() | Condition.STUNNED.bit() | Condition.PARALYZED.bit()
                | Condition.PETRIFIED.bit())) == 0;
    }

    /** Server: every live instance (a condition can have several, one per source/key). */
    public static List<ConditionInstance> all(LivingEntity entity) {
        if (!(entity instanceof ConditionHolder holder) || entity.getWorld().isClient) {
            return List.of();
        }
        ensureLoaded(entity);
        return List.copyOf(holder.dnd$conditions());
    }

    /** Server: the instance of that condition that ends last (indefinite wins), or null. */
    @Nullable
    public static ConditionInstance get(LivingEntity entity, Condition condition) {
        ConditionInstance best = null;
        for (ConditionInstance inst : all(entity)) {
            if (inst.condition() == condition && (best == null || inst.endsAfter(best))) {
                best = inst;
            }
        }
        return best;
    }

    /** Server: the source entity of the latest-ending instance, if loaded in the same world. */
    @Nullable
    public static Entity sourceOf(LivingEntity entity, Condition condition) {
        ConditionInstance inst = get(entity, condition);
        return inst == null ? null : resolve(entity, inst.source());
    }

    @Nullable
    static Entity resolve(LivingEntity entity, @Nullable UUID source) {
        if (source == null || !(entity.getWorld() instanceof ServerWorld world)) {
            return null;
        }
        return world.getEntity(source);
    }

    // ------------------------------------------------------------------ removal

    /** Server: end every instance of condition (reason REMOVED). Returns whether anything ended. */
    public static boolean remove(LivingEntity entity, Condition condition) {
        return endMatching(entity, inst -> inst.condition() == condition, ConditionEvents.EndReason.REMOVED, null);
    }

    /** Server: end the instances from that source (null = sourceless ones). */
    public static boolean remove(LivingEntity entity, Condition condition, @Nullable Entity source) {
        UUID id = source == null ? null : source.getUuid();
        return endMatching(entity, inst -> inst.condition() == condition
                && (id == null ? inst.source() == null : id.equals(inst.source())),
                ConditionEvents.EndReason.REMOVED, null);
    }

    /** Server: end the instance with that key from any source. */
    public static boolean removeKey(LivingEntity entity, Condition condition, String key) {
        return endMatching(entity, inst -> inst.condition() == condition && inst.key().equals(key),
                ConditionEvents.EndReason.REMOVED, null);
    }

    /** Server: end everything (death, Downed, /dndcondition remove all). */
    public static void clear(LivingEntity entity) {
        endMatching(entity, inst -> true, ConditionEvents.EndReason.REMOVED, null);
    }

    /** Server: end everything matching, with a reason (death, Downed). */
    static boolean endMatching(LivingEntity entity, java.util.function.Predicate<ConditionInstance> which,
            ConditionEvents.EndReason reason, @Nullable String logNote) {
        if (!(entity instanceof ConditionHolder holder) || entity.getWorld().isClient) {
            return false;
        }
        ensureLoaded(entity);
        List<ConditionInstance> ended = new ArrayList<>();
        for (ConditionInstance inst : holder.dnd$conditions()) {
            if (which.test(inst)) {
                ended.add(inst);
            }
        }
        if (ended.isEmpty()) {
            return false;
        }
        holder.dnd$conditions().removeAll(ended);
        changed(entity);
        for (ConditionInstance inst : ended) {
            afterEnd(entity, inst, reason, logNote);
        }
        return true;
    }

    // ------------------------------------------------------------------ immunity and hooks

    /** Server: would condition land on target from source (immunities, boss rules, hooks)? */
    public static boolean isImmune(LivingEntity target, Condition condition, @Nullable Entity source) {
        return immuneReason(target, condition, source) != null;
    }

    /** Server: the reason key when immune, else null. */
    @Nullable
    public static String immuneReason(LivingEntity target, Condition condition, @Nullable Entity source) {
        for (Immunity immunity : IMMUNITIES) {
            String reason = immunity.immune(target, condition, source);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    public static void registerImmunity(Immunity immunity) {
        IMMUNITIES.add(immunity);
    }

    public static void registerApplyHook(ApplyHook hook) {
        APPLY_HOOKS.add(hook);
    }

    /**
     * Worldgen: mark an entity that isn't in a world yet (structure statues) with an indefinite, sourceless
     * condition, written straight into its persistent NBT. No events fire.
     */
    public static void preset(LivingEntity entity, Condition condition) {
        NbtCompound data = ((IEntityDataSaver) entity).getPersistentData();
        NbtList list = data.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
        ConditionInstance inst = new ConditionInstance(condition, "preset", null, null, 0L, -1L, null, false, false);
        list.add(inst.toNbt());
        data.put(NBT_KEY, list);
        if (entity instanceof ConditionHolder holder) {
            holder.dnd$conditions().clear();
            holder.dnd$setConditionsLoaded(false);
        }
    }

    // ------------------------------------------------------------------ internals

    /** Reads the saved instances into the live list the first time the server touches the entity. */
    static void ensureLoaded(LivingEntity entity) {
        if (!(entity instanceof ConditionHolder holder) || holder.dnd$conditionsLoaded()) {
            return;
        }
        holder.dnd$setConditionsLoaded(true);
        holder.dnd$conditions().removeIf(inst -> !inst.transientInstance());
        NbtCompound data = ((IEntityDataSaver) entity).getPersistentData();
        if (data.contains(NBT_KEY, NbtElement.LIST_TYPE)) {
            NbtList list = data.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                ConditionInstance inst = ConditionInstance.fromNbt(list.getCompound(i));
                if (inst != null) {
                    holder.dnd$conditions().add(inst);
                }
            }
        }
        if (!holder.dnd$conditions().isEmpty()) {
            holder.dnd$setConditionMask(computeMask(holder.dnd$conditions()));
            ConditionTicker.track(entity);
            ConditionEffects.onLoaded(entity);
        }
    }

    /** Called when the entity's NBT is read again: the saved list is the truth. */
    public static void onNbtRead(Entity entity) {
        if (entity instanceof ConditionHolder holder) {
            holder.dnd$setConditionsLoaded(false);
        }
    }

    static int computeMask(List<ConditionInstance> list) {
        int mask = 0;
        for (ConditionInstance inst : list) {
            mask |= inst.condition().bit();
            if (inst.condition().incapacitates()) {
                mask |= Condition.INCAPACITATED.bit();
            }
        }
        return mask;
    }

    /** The live set changed: mask, NBT, attributes, sheet and the owner's HUD. */
    static void changed(LivingEntity entity) {
        ConditionHolder holder = (ConditionHolder) entity;
        int before = holder.dnd$getConditionMask();
        int mask = computeMask(holder.dnd$conditions());
        holder.dnd$setConditionMask(mask);
        persist(entity);
        ConditionEffects.onMaskChanged(entity, before, mask);
        if (entity instanceof ServerPlayerEntity player) {
            ConditionSync.send(player);
        }
    }

    private static void persist(LivingEntity entity) {
        NbtCompound data = ((IEntityDataSaver) entity).getPersistentData();
        NbtList list = new NbtList();
        for (ConditionInstance inst : ((ConditionHolder) entity).dnd$conditions()) {
            if (!inst.transientInstance()) {
                list.add(inst.toNbt());
            }
        }
        if (list.isEmpty()) {
            data.remove(NBT_KEY);
        } else {
            data.put(NBT_KEY, list);
        }
    }

    static String name(Entity entity) {
        return entity instanceof PlayerEntity ? entity.getEntityName() : entity.getName().getString();
    }

    static String reasonWord(String reasonKey) {
        int dot = reasonKey.lastIndexOf('.');
        return dot < 0 ? reasonKey : reasonKey.substring(dot + 1);
    }

    private static void afterEnd(LivingEntity entity, ConditionInstance inst, ConditionEvents.EndReason reason,
            @Nullable String logNote) {
        DnDClasses.LOGGER.info("[Conditions] {} -{} ({})", name(entity), inst.condition().id(),
                logNote != null ? logNote : reason.name().toLowerCase(java.util.Locale.ROOT));
        ConditionTicker.forgetGone(entity, inst);
        ConditionEffects.onEnded(entity, inst, reason);
        ConditionEvents.ENDED.invoker().ended(entity, inst, reason);
    }

    /** Ends one instance (ticker: expiry, saves, source gone, escalation). */
    static void end(LivingEntity entity, ConditionInstance inst, ConditionEvents.EndReason reason,
            @Nullable String logNote) {
        ConditionHolder holder = (ConditionHolder) entity;
        if (!holder.dnd$conditions().remove(inst)) {
            return;
        }
        changed(entity);
        afterEnd(entity, inst, reason, logNote);
    }

    /** Swaps a failed repeat save's instance for its escalation, same source and key. */
    static void escalate(LivingEntity entity, ConditionInstance inst) {
        RepeatSave save = inst.save();
        if (save == null || save.escalateTo() == null) {
            return;
        }
        Condition next = save.escalateTo();
        DnDClasses.LOGGER.info("[Conditions] {} {} -> {}", name(entity), inst.condition().id(), next.id());
        end(entity, inst, ConditionEvents.EndReason.ESCALATED, "escalated to " + next.id());
        Entity source = resolve(entity, inst.source());
        Builder b = of(entity, next).key(inst.key()).ticks(save.escalateTicks());
        b.sourceUuid = inst.source();
        b.sourceName = inst.sourceName();
        b.sourceEntity = source;
        if (inst.endsWithSource()) {
            b.endsWithSource();
        }
        b.apply();
    }

    public static final class Builder {
        private final LivingEntity target;
        private final Condition condition;
        @Nullable
        private Entity sourceEntity;
        @Nullable
        private UUID sourceUuid;
        @Nullable
        private Text sourceName;
        @Nullable
        private String key;
        private int ticks;
        @Nullable
        private Ability saveAbility;
        private int saveDc;
        private int saveEvery;
        @Nullable
        private String saveLabel;
        private final List<String> saveTags = new ArrayList<>();
        @Nullable
        private Condition escalateTo;
        private int escalateTicks;
        private boolean endsWithSource;
        private boolean transientInstance;
        private boolean silent;

        private Builder(LivingEntity target, Condition condition) {
            this.target = target;
            this.condition = condition;
            this.ticks = INDEFINITE;
        }

        /** UUID + display name; default key "<source uuid>". */
        public Builder source(@Nullable Entity source) {
            this.sourceEntity = source;
            this.sourceUuid = source == null ? null : source.getUuid();
            if (source != null) {
                this.sourceName = source.getDisplayName();
            }
            return this;
        }

        /** For sourceless instances (a trap, an item). */
        public Builder sourceName(Text name) {
            this.sourceName = name;
            return this;
        }

        /** Same condition + source + key refreshes, never stacks. */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /** Default {@link Conditions#INDEFINITE}. */
        public Builder ticks(int ticks) {
            this.ticks = ticks < 0 ? INDEFINITE : ticks;
            return this;
        }

        public Builder repeatSave(Ability ability, int dc, int everyTicks) {
            this.saveAbility = ability;
            this.saveDc = dc;
            this.saveEvery = Math.max(1, everyTicks);
            return this;
        }

        /** Ability, DC and label from the entry. */
        public Builder repeatSave(MobSaveInfo.Entry entry, int everyTicks) {
            repeatSave(entry.ability(), entry.dc(), everyTicks);
            if (this.saveLabel == null) {
                this.saveLabel = entry.labelKey();
            }
            return this;
        }

        /** Default "condition.dndclasses.<id>.save". */
        public Builder saveLabel(String translationKey) {
            this.saveLabel = translationKey;
            return this;
        }

        /** Added to "condition" and the condition id. */
        public Builder saveTags(String... tags) {
            Collections.addAll(this.saveTags, tags);
            return this;
        }

        /** A failed repeat save swaps to next. */
        public Builder escalateOnFailure(Condition next, int nextTicks) {
            this.escalateTo = next;
            this.escalateTicks = nextTicks;
            return this;
        }

        /** Ends when the source dies/unloads (100 ticks grace). */
        public Builder endsWithSource() {
            this.endsWithSource = true;
            return this;
        }

        /** Not written to NBT (holds owned by live code). */
        public Builder transientInstance() {
            this.transientInstance = true;
            return this;
        }

        /** No action-bar message. */
        public Builder silent() {
            this.silent = true;
            return this;
        }

        /** Applies it. Returns the instance, or null if immune or vetoed (logged). */
        @Nullable
        public ConditionInstance apply() {
            if (target.getWorld().isClient || !(target instanceof ConditionHolder holder) || !target.isAlive()) {
                return null;
            }
            ensureLoaded(target);
            String reason = immuneReason(target, condition, sourceEntity);
            if (reason != null) {
                DnDClasses.LOGGER.info("[Conditions] {} immune to {} ({})", name(target), condition.id(),
                        reasonWord(reason));
                return null;
            }
            int finalTicks = ticks;
            for (ApplyHook hook : APPLY_HOOKS) {
                finalTicks = hook.adjust(target, condition, sourceEntity, finalTicks);
                if (finalTicks == VETO) {
                    DnDClasses.LOGGER.info("[Conditions] {} {} vetoed", name(target), condition.id());
                    return null;
                }
                if (finalTicks < 0) {
                    finalTicks = INDEFINITE;
                }
            }
            if (finalTicks == 0) {
                return null;
            }
            long now = target.getWorld().getTime();
            String slotKey = key != null ? key : sourceUuid != null ? sourceUuid.toString() : NO_KEY;
            RepeatSave save = null;
            if (saveAbility != null) {
                List<String> tags = new ArrayList<>();
                tags.add("condition");
                tags.add(condition.id());
                tags.addAll(saveTags);
                save = new RepeatSave(saveAbility, saveDc, saveEvery,
                        saveLabel != null ? saveLabel : condition.translationKey() + ".save", List.copyOf(tags),
                        escalateTo, escalateTicks, now + saveEvery);
            }
            ConditionInstance inst = new ConditionInstance(condition, slotKey, sourceUuid, sourceName, now,
                    finalTicks < 0 ? -1L : now + finalTicks, save, endsWithSource, transientInstance);

            ConditionInstance existing = null;
            for (ConditionInstance other : holder.dnd$conditions()) {
                if (other.sameSlot(condition, sourceUuid, slotKey)) {
                    existing = other;
                    break;
                }
            }
            if (existing != null && !inst.endsAfter(existing)) {
                return existing; // refresh, not stack: the old one already lasts at least as long
            }
            boolean freshCondition = !has(target, condition);
            if (existing != null) {
                holder.dnd$conditions().remove(existing);
            }
            holder.dnd$conditions().add(inst);
            ConditionTicker.track(target);
            changed(target);
            DnDClasses.LOGGER.info("[Conditions] {} +{} ({}, {}{})", name(target), condition.id(),
                    sourceName != null ? sourceName.getString() : "no source",
                    finalTicks < 0 ? "indefinite" : finalTicks + "t",
                    save == null ? "" : ", save " + save.ability().shortKey() + " " + save.dc() + "/" + save.every()
                            + "t" + (save.escalateTo() == null ? "" : " -> " + save.escalateTo().id()));
            ConditionEffects.onApplied(target, inst, freshCondition, silent);
            ConditionEvents.APPLIED.invoker().applied(target, inst, existing == null);
            return inst;
        }
    }
}
