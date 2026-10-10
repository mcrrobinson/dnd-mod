package mattonfire.dnd.classes.Downed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;

/**
 * Helping a Downed player by holding right-click on them. Use-entity has no "held" state, so the client sends
 * {@link #C2S_HELP_HOLD} with the target's entity id while use is held on a Downed player (and -1 when it stops);
 * the server checks distance, line of sight and the helper's hand every tick and counts {@value #HOLD_TICKS}.
 * <ul>
 * <li>On an unstable player: they're <b>stable</b>, and the helper rolls Medicine (DC {@value #MEDICINE_DC}).
 * A success means they stand up in {@value #MEDICINE_STABLE_TICKS} ticks instead of 30 s.</li>
 * <li>On a stable player: <b>help up</b>, they stand up with {@value Revives#APPLE_HP} HP.</li>
 * </ul>
 * Interrupted if the helper lets go, looks away, moves out of range, takes damage or is Downed. The player who
 * downed you (PvP) can't stabilise you.
 */
public final class Stabilise {
    public static final Identifier C2S_HELP_HOLD = new Identifier(DnDClasses.MOD_ID, "downed_help_hold");
    public static final int HOLD_TICKS = 60;
    public static final double RANGE = 3.5D;
    public static final int MEDICINE_DC = 15;
    /** A stable player's stand-up timer after a Medicine success. */
    public static final int MEDICINE_STABLE_TICKS = 200;

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    /** The target each helper just finished with: they let go before helping them again (stable -> up). */
    private static final Map<UUID, UUID> DONE = new HashMap<>();

    private static final class Hold {
        final UUID target;
        int ticks;

        Hold(UUID target) {
            this.target = target;
        }
    }

    private Stabilise() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_HELP_HOLD, (server, player, handler, buf, sender) -> {
            int targetId = buf.readVarInt();
            server.execute(() -> setHolding(player, targetId));
        });
        ServerTickEvents.END_SERVER_TICK.register(Stabilise::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            HOLDS.remove(handler.getPlayer().getUuid());
            DONE.remove(handler.getPlayer().getUuid());
        });
    }

    /** The helper's progress on their current target, 0 to 1, or -1 if they aren't helping anyone. */
    public static float progress(ServerPlayerEntity helper) {
        Hold hold = HOLDS.get(helper.getUuid());
        return hold == null ? -1.0F : (float) hold.ticks / HOLD_TICKS;
    }

    static void setHolding(ServerPlayerEntity helper, int targetId) {
        Hold hold = HOLDS.get(helper.getUuid());
        Entity entity = targetId < 0 ? null : helper.world.getEntityById(targetId);
        if (!(entity instanceof ServerPlayerEntity target)) {
            DONE.remove(helper.getUuid());
            if (hold != null) {
                interrupt(helper, hold, "You let go.");
            }
            return;
        }
        if (hold != null && hold.target.equals(target.getUuid())) {
            return;
        }
        if (target.getUuid().equals(DONE.get(helper.getUuid()))) {
            return;
        }
        DONE.remove(helper.getUuid());
        if (hold != null) {
            interrupt(helper, hold, null);
        }
        String refusal = refusal(helper, target);
        if (refusal != null) {
            helper.sendMessage(Text.literal(refusal).formatted(Formatting.GRAY), true);
            return;
        }
        HOLDS.put(helper.getUuid(), new Hold(target.getUuid()));
        DnDClasses.LOGGER.info("[Downed] {} starts helping {}", helper.getEntityName(), target.getEntityName());
    }

    /** Why the helper can't help the target right now, or null if they can. */
    @Nullable
    static String refusal(ServerPlayerEntity helper, ServerPlayerEntity target) {
        if (target == helper || !Downed.is(target)) {
            return "";
        }
        if (Downed.is(helper) || !helper.isAlive() || helper.isSpectator()) {
            return "You can't help anyone right now.";
        }
        Downed.State state = Downed.state(target);
        if (state != null && helper.getUuid().equals(state.attacker)) {
            return "You downed " + target.getEntityName() + ": you won't help them up.";
        }
        if (helper.getMainHandStack().getUseAction() != UseAction.NONE) {
            return "Empty your hand to help " + target.getEntityName() + ".";
        }
        if (helper.squaredDistanceTo(target) > RANGE * RANGE || !helper.canSee(target)) {
            return "Too far to help " + target.getEntityName() + ".";
        }
        return null;
    }

    private static void tick(MinecraftServer server) {
        if (HOLDS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Hold> entry : new ArrayList<>(HOLDS.entrySet())) {
            ServerPlayerEntity helper = server.getPlayerManager().getPlayer(entry.getKey());
            Hold hold = entry.getValue();
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(hold.target);
            if (helper == null || target == null || target.world != helper.world) {
                HOLDS.remove(entry.getKey());
                continue;
            }
            if (!Downed.is(target)) {
                HOLDS.remove(entry.getKey());
                continue;
            }
            if (helper.hurtTime > 0) {
                interrupt(helper, hold, "Interrupted: you were hit.");
                continue;
            }
            String refusal = refusal(helper, target);
            if (refusal != null) {
                interrupt(helper, hold, refusal.isEmpty() ? null : "Interrupted. " + refusal);
                continue;
            }
            hold.ticks++;
            boolean stable = Downed.isStable(target);
            if (hold.ticks >= HOLD_TICKS) {
                HOLDS.remove(entry.getKey());
                DONE.put(helper.getUuid(), target.getUuid());
                complete(helper, target, stable);
                continue;
            }
            if (hold.ticks % 10 == 1) {
                String verb = stable ? "Helping " + target.getEntityName() + " up" : "Stabilising " + target.getEntityName();
                Text bar = progressText(verb, hold.ticks);
                helper.sendMessage(bar, true);
                target.sendMessage(progressText(helper.getEntityName() + (stable ? " is helping you up" : " is stabilising you"),
                        hold.ticks), true);
            }
        }
    }

    private static Text progressText(String verb, int ticks) {
        int filled = Math.min(10, ticks * 10 / HOLD_TICKS);
        return Text.literal(verb + "  ").formatted(Formatting.YELLOW)
                .append(Text.literal("■".repeat(filled)).formatted(Formatting.GREEN))
                .append(Text.literal("■".repeat(10 - filled)).formatted(Formatting.DARK_GRAY));
    }

    private static void complete(ServerPlayerEntity helper, ServerPlayerEntity target, boolean stable) {
        helper.world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (stable) {
            DnDClasses.LOGGER.info("[Downed] {} helped {} up", helper.getEntityName(), target.getEntityName());
            Downed.revive(target, Revives.APPLE_HP);
            helper.sendMessage(Text.literal("You help " + target.getEntityName() + " up.").formatted(Formatting.GREEN), true);
            return;
        }
        D20.Roll roll = SkillCheck.builder(helper, Skill.MEDICINE, MEDICINE_DC, "stabilise").roll();
        boolean quick = roll.outcome().succeeded();
        D20.show(helper, roll, Text.literal(quick
                ? target.getEntityName() + " stands up in " + MEDICINE_STABLE_TICKS / 20 + " s"
                : target.getEntityName() + " is stable"));
        DnDClasses.LOGGER.info("[Downed] {} stabilised {} (Medicine {})", helper.getEntityName(),
                target.getEntityName(), roll.outcome());
        Downed.stabilise(target, quick ? MEDICINE_STABLE_TICKS : Downed.STABLE_TICKS);
    }

    private static void interrupt(ServerPlayerEntity helper, Hold hold, @Nullable String message) {
        HOLDS.remove(helper.getUuid());
        if (hold.ticks > 0 && message != null) {
            helper.sendMessage(Text.literal(message).formatted(Formatting.GRAY), true);
        }
        DnDClasses.LOGGER.info("[Downed] {} stopped helping after {} ticks{}", helper.getEntityName(), hold.ticks,
                message == null ? "" : ": " + message);
    }
}
