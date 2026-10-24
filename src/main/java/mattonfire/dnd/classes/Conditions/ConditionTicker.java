package mattonfire.dnd.classes.Conditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Runs the live conditions once a tick: expiry, repeat saves (and escalation), source-gone checks, the owner's
 * HUD refresh and the per-condition upkeep in {@link ConditionEffects#tick}. Only entities that have instances are
 * walked: they're tracked when one lands and when a saved entity loads.
 */
public final class ConditionTicker {
    /** Ticks an {@code endsWithSource} instance survives its source being gone. */
    public static final int SOURCE_GRACE_TICKS = 100;
    /** Further than this from its source counts as gone. */
    public static final double SOURCE_RANGE = 64.0D;
    /** How often the owner's detail packet is refreshed while they have any. */
    public static final int SYNC_EVERY = 20;

    private static final Set<LivingEntity> ACTIVE = new LinkedHashSet<>();
    /** "entity uuid|condition|source|key" to the world time its source went missing. */
    private static final Map<String, Long> GONE_SINCE = new HashMap<>();

    private ConditionTicker() {
    }

    static void track(LivingEntity entity) {
        ACTIVE.add(entity);
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ConditionTicker::tick);
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LivingEntity living
                    && ((IEntityDataSaver) living).getPersistentData().contains(Conditions.NBT_KEY,
                            NbtElement.LIST_TYPE)) {
                Conditions.ensureLoaded(living);
                if (!Conditions.all(living).isEmpty()) {
                    track(living);
                }
            }
        });
    }

    private static String goneKey(LivingEntity entity, ConditionInstance inst) {
        return entity.getUuidAsString() + "|" + inst.condition().id() + "|" + inst.source() + "|" + inst.key();
    }

    static void forgetGone(LivingEntity entity, ConditionInstance inst) {
        GONE_SINCE.remove(goneKey(entity, inst));
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (LivingEntity entity : new ArrayList<>(ACTIVE)) {
            if (entity.isRemoved()) {
                ACTIVE.remove(entity); // unloaded or dead; a saved entity is tracked again when it loads
                continue;
            }
            List<ConditionInstance> live = ((ConditionHolder) entity).dnd$conditions();
            if (live.isEmpty()) {
                ACTIVE.remove(entity);
                if (entity instanceof ServerPlayerEntity player) {
                    ConditionSync.send(player);
                }
                continue;
            }
            tickEntity(entity, server);
        }
    }

    private static void tickEntity(LivingEntity entity, MinecraftServer server) {
        long now = entity.getWorld().getTime();
        for (ConditionInstance inst : new ArrayList<>(((ConditionHolder) entity).dnd$conditions())) {
            if (!((ConditionHolder) entity).dnd$conditions().contains(inst)) {
                continue; // ended by an earlier one this tick (escalation)
            }
            if (inst.endsAt() >= 0 && now >= inst.endsAt()) {
                Conditions.end(entity, inst, ConditionEvents.EndReason.EXPIRED, null);
                continue;
            }
            if (inst.endsWithSource() && sourceGone(entity, inst, now)) {
                Conditions.end(entity, inst, ConditionEvents.EndReason.SOURCE_GONE, null);
                continue;
            }
            RepeatSave save = inst.save();
            if (save != null && now >= save.nextAt()) {
                repeatSave(entity, inst, save, now);
            }
        }
        if (!entity.isAlive()) {
            return;
        }
        ConditionEffects.tick(entity, now);
        if (entity instanceof ServerPlayerEntity player && server.getTicks() % SYNC_EVERY == 0) {
            ConditionSync.send(player);
        }
    }

    private static boolean sourceGone(LivingEntity entity, ConditionInstance inst, long now) {
        Entity source = Conditions.resolve(entity, inst.source());
        boolean gone = inst.source() != null && (source == null || source.isRemoved()
                || source instanceof LivingEntity living && !living.isAlive()
                || source.squaredDistanceTo(entity) > SOURCE_RANGE * SOURCE_RANGE);
        String key = goneKey(entity, inst);
        if (!gone) {
            GONE_SINCE.remove(key);
            return false;
        }
        // A hold whose holder is gone ends at once; anything else gets a grace period
        if (inst.transientInstance()) {
            return true;
        }
        long since = GONE_SINCE.computeIfAbsent(key, k -> now);
        return now - since >= SOURCE_GRACE_TICKS;
    }

    private static void repeatSave(LivingEntity entity, ConditionInstance inst, RepeatSave save, long now) {
        ConditionHolder holder = (ConditionHolder) entity;
        ConditionInstance next = inst.withSave(save.withNextAt(now + save.every()));
        if (entity instanceof PlayerEntity player && Downed.is(player)) {
            replace(holder, inst, next); // Downed players' repeat saves pause
            return;
        }
        Entity source = Conditions.resolve(entity, inst.source());
        SaveResult result = SavingThrow.of(entity, save.ability(), save.dc())
                .label(save.labelKey())
                .source(source)
                .tags(save.tags().toArray(new String[0]))
                .onSuccess(Text.translatable("condition.dndclasses.save.ends"))
                .onFailure(Text.translatable(save.escalateTo() != null
                        ? "condition.dndclasses.save.worsens" : "condition.dndclasses.save.lasts"))
                .roll();
        if (!holder.dnd$conditions().contains(inst)) {
            return; // a hook ended it while rolling
        }
        if (result.succeeded()) {
            Conditions.end(entity, inst, ConditionEvents.EndReason.SAVED, null);
        } else if (save.escalateTo() != null) {
            Conditions.escalate(entity, inst);
        } else {
            replace(holder, inst, next);
            if (entity instanceof ServerPlayerEntity player) {
                ConditionSync.send(player);
            }
        }
    }

    private static void replace(ConditionHolder holder, ConditionInstance old, ConditionInstance next) {
        List<ConditionInstance> list = holder.dnd$conditions();
        int i = list.indexOf(old);
        if (i >= 0) {
            list.set(i, next);
        }
        LivingEntity entity = (LivingEntity) holder;
        // Keep the saved copy's next-save time current
        Conditions.changed(entity);
        if (DnDClasses.LOGGER.isDebugEnabled()) {
            DnDClasses.LOGGER.debug("[Conditions] {} {} next save at {}", Conditions.name(entity),
                    old.condition().id(), next.save() == null ? -1 : next.save().nextAt());
        }
    }
}
