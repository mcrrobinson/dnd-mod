package mattonfire.dnd.classes.SkillChecks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Abilities.Skill;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Grabs and holds (a Mimic's jaws, an Owlbear's hug) and the jump-to-struggle escape.
 * <p>
 * A creature that holds a player calls {@link #hold} when the hold starts and {@link #release} when it ends.
 * The player's client is told ({@link #S2C_HELD}), and while held each jump keypress sends {@link #C2S_STRUGGLE}:
 * an escape check with the better of Athletics (STR) or Acrobatics (DEX) against the grab's DC, at most once per
 * {@value #STRUGGLE_COOLDOWN} ticks. A success calls the holder's {@link Holder#breakFree}.
 * <p>
 * Holds are checked every tick against {@link Holder#isHolding}, so a holder that dies, is removed or lets go
 * by itself never leaves a player marked as held.
 */
public final class Grapple {
    public static final Identifier C2S_STRUGGLE = new Identifier(DnDClasses.MOD_ID, "struggle");
    /** Boolean: whether the receiving player is now held. */
    public static final Identifier S2C_HELD = new Identifier(DnDClasses.MOD_ID, "held");

    /** Minimum ticks between two escape checks by one player. */
    public static final int STRUGGLE_COOLDOWN = 10;
    /** Advantage-filter tag on grab saves and escape checks ("advantage on grapple" features). */
    public static final String TAG = "grapple";

    /** Something holding a creature. */
    public interface Holder {
        /** Whether it still holds {@code victim} (alive, not removed, hold not over). */
        boolean isHolding(LivingEntity victim);

        /** The victim won an escape check: let go. */
        void breakFree(LivingEntity victim);
    }

    private record Hold(Holder holder, int dc, Text name) {
    }

    /** Key of the Restrained condition a hold applies. */
    public static final String CONDITION_KEY = "grapple";

    private record Restraint(Holder holder, String key) {
    }

    /** Every held creature (players and mobs) to who restrains it. */
    private static final Map<LivingEntity, Restraint> RESTRAINED = new java.util.IdentityHashMap<>();

    /** Held player UUID to their hold. */
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    /** Player UUID to the server tick of their last escape check. */
    private static final Map<UUID, Integer> LAST_STRUGGLE = new HashMap<>();

    private Grapple() {
    }

    /**
     * {@code holder} now holds {@code victim}; a player can struggle against {@code dc}. {@code nameKey} names the
     * hold in the escape check's result ("Mimic's jaws"). Non-players are ignored (they can't struggle).
     */
    public static synchronized void hold(LivingEntity victim, Holder holder, int dc, String nameKey) {
        hold(victim, holder, dc, nameKey, CONDITION_KEY);
    }

    /**
     * As {@link #hold(LivingEntity, Holder, int, String)}, with the key of the Restrained condition it applies
     * ({@value #CONDITION_KEY} by default; the Gelatinous Cube uses "engulf").
     */
    public static synchronized void hold(LivingEntity victim, Holder holder, int dc, String nameKey,
            String conditionKey) {
        restrain(victim, holder, conditionKey);
        if (!(victim instanceof ServerPlayerEntity player)) {
            return;
        }
        boolean wasHeld = HOLDS.containsKey(player.getUuid());
        HOLDS.put(player.getUuid(), new Hold(holder, dc, Text.translatable(nameKey)));
        if (!wasHeld) {
            sendHeld(player, true);
        }
    }

    /** The hold on {@code victim} is over (if {@code holder} is the one holding it). */
    public static synchronized void release(LivingEntity victim, Holder holder) {
        unrestrain(victim, holder);
        Hold hold = HOLDS.get(victim.getUuid());
        if (hold != null && hold.holder() == holder) {
            HOLDS.remove(victim.getUuid());
            if (victim instanceof ServerPlayerEntity player) {
                sendHeld(player, false);
            }
        }
    }

    /** A held creature is Restrained (transient, source the holder when it's an entity) while the hold lasts. */
    private static void restrain(LivingEntity victim, Holder holder, String conditionKey) {
        if (victim.getWorld().isClient) {
            return;
        }
        Conditions.Builder b = Conditions.of(victim, Condition.RESTRAINED).key(conditionKey).transientInstance();
        if (holder instanceof Entity entity) {
            b.source(entity);
        }
        if (b.apply() != null) {
            RESTRAINED.put(victim, new Restraint(holder, conditionKey));
        }
    }

    private static void unrestrain(LivingEntity victim, Holder holder) {
        Restraint r = RESTRAINED.get(victim);
        if (r != null && r.holder() == holder) {
            RESTRAINED.remove(victim);
            if (holder instanceof Entity entity) {
                Conditions.remove(victim, Condition.RESTRAINED, entity);
            } else {
                Conditions.removeKey(victim, Condition.RESTRAINED, r.key());
            }
        }
    }

    public static synchronized boolean isHeld(LivingEntity victim) {
        return HOLDS.containsKey(victim.getUuid());
    }

    /** A held player pressed jump: an escape check, if the cooldown allows. */
    static void struggle(ServerPlayerEntity player) {
        Hold hold;
        synchronized (Grapple.class) {
            hold = HOLDS.get(player.getUuid());
            if (hold == null || !hold.holder().isHolding(player)) {
                return;
            }
            int now = player.server.getTicks();
            Integer last = LAST_STRUGGLE.get(player.getUuid());
            if (last != null && now - last < STRUGGLE_COOLDOWN) {
                return;
            }
            LAST_STRUGGLE.put(player.getUuid(), now);
        }
        Skill skill = escapeSkill(player);
        D20.Roll roll = SkillCheck.builder(player, skill, hold.dc(), TAG).roll();
        boolean free = roll.outcome().succeeded();
        DnDClasses.LOGGER.info("[Grapple] {} struggles against {} ({} vs DC {}): {}", player.getEntityName(),
                hold.name().getString(), skill, hold.dc(), free ? "breaks free" : "still held");
        D20.show(player, roll, Text.translatable(free ? "grapple.dndclasses.escaped" : "grapple.dndclasses.still_held",
                hold.name()));
        if (free) {
            hold.holder().breakFree(player);
            release(player, hold.holder());
        }
    }

    /** The better of the player's Athletics and Acrobatics (Athletics on a tie). */
    public static Skill escapeSkill(ServerPlayerEntity player) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        return sheet.check(Skill.ACROBATICS) > sheet.check(Skill.ATHLETICS) ? Skill.ACROBATICS : Skill.ATHLETICS;
    }

    private static void sendHeld(ServerPlayerEntity player, boolean held) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBoolean(held);
        ServerPlayNetworking.send(player, S2C_HELD, buf);
    }

    private static synchronized void tick(MinecraftServer server) {
        // Holders that let go without calling release (died, removed) free their Restrained victims
        for (Map.Entry<LivingEntity, Restraint> e : new java.util.ArrayList<>(RESTRAINED.entrySet())) {
            LivingEntity victim = e.getKey();
            if (victim.isRemoved() || !e.getValue().holder().isHolding(victim)) {
                unrestrain(victim, e.getValue().holder());
                RESTRAINED.remove(victim);
            }
        }
        Iterator<Map.Entry<UUID, Hold>> it = HOLDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Hold> e = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) {
                it.remove();
            } else if (!e.getValue().holder().isHolding(player)) {
                it.remove();
                sendHeld(player, false);
            }
        }
        LAST_STRUGGLE.values().removeIf(t -> server.getTicks() - t > STRUGGLE_COOLDOWN);
    }

    /** Drops a player's hold and cooldown; called on disconnect. */
    public static synchronized void forget(UUID player) {
        HOLDS.remove(player);
        LAST_STRUGGLE.remove(player);
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(Grapple::tick);
        ServerPlayNetworking.registerGlobalReceiver(C2S_STRUGGLE,
                (server, player, handler, buf, sender) -> server.execute(() -> struggle(player)));
    }
}
