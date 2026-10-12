package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * DM roll calls ({@code /dm roll}): each target gets a prompt on the d20 HUD and rolls with the roll key (Y), or
 * the server rolls for them after {@link #TIMEOUT_TICKS}. The client only says "now" ({@link #C2S_ANSWER} has no
 * payload): the server rolls, so results can't be forged. Each result goes to the DM as it comes in, then a
 * summary with [Passed] / [Failed] buttons that fill in {@code /dm apply}.
 */
public final class DmRollCalls {
    /** S2C: label text, caller name, DC (0 if secret), advantage mode, ticks left (0 clears the prompt). */
    public static final Identifier S2C_PROMPT = new Identifier(DnDClasses.MOD_ID, "roll_prompt");
    /** C2S, empty: "roll my pending DM call now". */
    public static final Identifier C2S_ANSWER = new Identifier(DnDClasses.MOD_ID, "roll_answer");
    /** 20 s, then the server rolls for the player. */
    public static final int TIMEOUT_TICKS = 400;

    /** What a call rolls: a saving throw, a skill check, or a flat d20. */
    public record Check(String id, @Nullable Ability save, @Nullable Skill skill) {
        public Text label() {
            if (save != null) {
                return Text.translatable(save.saveTranslationKey());
            }
            if (skill != null) {
                return Text.translatable(skill.translationKey());
            }
            return Text.translatable("skill.dndclasses.flat");
        }

        /** {@code str..cha} (saves), any skill id, or {@code flat}; null if unknown. */
        @Nullable
        public static Check parse(String text) {
            String id = text.toLowerCase(Locale.ROOT);
            if (id.equals("flat")) {
                return new Check(id, null, null);
            }
            Ability ability = Ability.byId(id);
            if (ability != null) {
                return new Check(ability.id(), ability, null);
            }
            Skill skill = Skill.byId(id);
            return skill == null ? null : new Check(skill.id(), null, skill);
        }
    }

    /** One {@code /dm roll}: who was asked, and what each of them rolled so far. */
    public static final class Call {
        final int number;
        final ServerCommandSource caller;
        final Check check;
        final int dc;
        final Advantage mode;
        final boolean secret;
        final long deadline;
        /** Everyone asked, in order, with their names (for players who leave before rolling). */
        final Map<UUID, String> targets = new LinkedHashMap<>();
        final Map<UUID, D20.Roll> results = new LinkedHashMap<>();

        Call(int number, ServerCommandSource caller, Check check, int dc, Advantage mode, boolean secret,
                long deadline) {
            this.number = number;
            this.caller = caller;
            this.check = check;
            this.dc = dc;
            this.mode = mode;
            this.secret = secret;
            this.deadline = deadline;
        }

        boolean done() {
            return results.size() >= targets.size();
        }

        /** "Dexterity Save DC 14" */
        String title() {
            return check.label().getString() + " DC " + dc;
        }
    }

    /** Pending prompts, by player. A player has at most one. */
    private static final Map<UUID, Call> PENDING = new HashMap<>();
    @Nullable
    private static Call last;
    private static int counter;

    private DmRollCalls() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_ANSWER,
                (server, player, handler, buf, sender) -> server.execute(() -> answer(player)));
        ServerTickEvents.END_SERVER_TICK.register(DmRollCalls::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING.clear();
            last = null;
        });
    }

    /** The last call, for {@code /dm apply}; null if none since the server started. */
    @Nullable
    public static Call last() {
        return last;
    }

    /**
     * Starts a call. With {@code auto} everyone rolls at once; otherwise each target gets a prompt. A player who
     * still has a prompt from an earlier call rolls that one first.
     */
    public static Call start(ServerCommandSource caller, List<ServerPlayerEntity> players, Check check, int dc,
            Advantage mode, boolean secret, boolean auto) {
        MinecraftServer server = caller.getServer();
        Call call = new Call(++counter, caller, check, dc, mode, secret, server.getTicks() + TIMEOUT_TICKS);
        for (ServerPlayerEntity player : players) {
            call.targets.put(player.getUuid(), player.getEntityName());
        }
        last = call;
        DnDClasses.LOGGER.info("[DmRolls] call #{} {} for {}{}{}{}", call.number, call.title(),
                String.join(", ", call.targets.values()), mode == Advantage.NORMAL ? "" : " " + mode,
                secret ? " secret" : "", auto ? " auto" : "");
        for (ServerPlayerEntity player : players) {
            Call earlier = PENDING.remove(player.getUuid());
            if (earlier != null) {
                resolve(player, earlier, true);
            }
            if (auto) {
                resolve(player, call, false);
            } else {
                PENDING.put(player.getUuid(), call);
                sendPrompt(player, call, TIMEOUT_TICKS);
            }
        }
        return call;
    }

    private static void answer(ServerPlayerEntity player) {
        Call call = PENDING.remove(player.getUuid());
        if (call != null) {
            resolve(player, call, false);
        }
    }

    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        long now = server.getTicks();
        List<Map.Entry<UUID, Call>> due = new ArrayList<>();
        for (Iterator<Map.Entry<UUID, Call>> it = PENDING.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Call> entry = it.next();
            if (now >= entry.getValue().deadline) {
                due.add(entry);
                it.remove();
            }
        }
        for (Map.Entry<UUID, Call> entry : due) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                resolve(player, entry.getValue(), true);
            } else {
                absent(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Rolls for one player of a call, shows it to them and reports it to the DM. */
    private static void resolve(ServerPlayerEntity player, Call call, boolean timedOut) {
        if (call.results.containsKey(player.getUuid())) {
            return;
        }
        sendPrompt(player, call, 0);
        Text detail = Text.translatable(timedOut ? "dm.dndclasses.roll.timed_out" : "dm.dndclasses.roll.called",
                call.caller.getName());
        D20.Roll roll;
        Check check = call.check;
        if (check.save() != null) {
            SavingThrow.Builder b = SavingThrow.of(player, check.save(), call.dc).source(call.caller.getEntity())
                    .tags("dm").mode(call.mode).mainPanel().onSuccess(detail).onFailure(detail);
            roll = (call.secret ? b.secret() : b).roll().roll();
        } else {
            D20.Builder b = check.skill() != null ? SkillCheck.builder(player, check.skill(), call.dc, "dm")
                    : SkillCheck.flat(player, call.dc);
            b.mode(call.mode).display(D20.Display.MAIN);
            if (call.secret) {
                b.secret();
            }
            roll = b.roll();
            D20.show(player, roll, detail);
        }
        call.results.put(player.getUuid(), roll);
        call.caller.sendFeedback(Text.literal("#" + call.number + " ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(player.getEntityName() + " ").formatted(Formatting.WHITE))
                .append(entry(roll)).append(timedOut ? Text.literal(" (timed out)").formatted(Formatting.GRAY)
                        : Text.empty()), false);
        if (call.done()) {
            summary(call);
        }
    }

    /** A target left before rolling: counted as no result. */
    private static void absent(UUID uuid, Call call) {
        call.results.put(uuid, null);
        call.caller.sendFeedback(Text.literal("#" + call.number + " " + call.targets.get(uuid) + " left, no roll")
                .formatted(Formatting.GRAY), false);
        if (call.done()) {
            summary(call);
        }
    }

    /** "19 (nat 16 +3) PASS", "21 (nat 18 adv, 7 dropped, +3) PASS", or "... NAT 20" / "... NAT 1". */
    private static MutableText entry(D20.Roll roll) {
        StringBuilder dice = new StringBuilder("nat ").append(roll.natural());
        if (roll.natural2() > 0) {
            dice.append(roll.mode() == Advantage.ADVANTAGE ? " adv, " : " dis, ").append(roll.natural2())
                    .append(" dropped,");
        }
        dice.append(' ').append(String.format("%+d", roll.modifier()));
        MutableText text = Text.literal(roll.total() + " (" + dice + ") ").formatted(Formatting.GRAY);
        String word = switch (roll.outcome()) {
            case CRITICAL -> "NAT 20";
            case FUMBLE -> "NAT 1";
            case SUCCESS -> "PASS";
            case FAILURE -> "FAIL";
        };
        return text.append(Text.literal(word).formatted(roll.outcome().succeeded() ? Formatting.GREEN
                : Formatting.RED));
    }

    private static void summary(Call call) {
        MutableText line = Text.literal(call.title() + ": ").formatted(Formatting.GOLD);
        int failed = 0;
        int rolled = 0;
        boolean first = true;
        for (Map.Entry<UUID, String> target : call.targets.entrySet()) {
            D20.Roll roll = call.results.get(target.getKey());
            if (!first) {
                line.append(Text.literal(" · ").formatted(Formatting.DARK_GRAY));
            }
            first = false;
            line.append(Text.literal(target.getValue() + " ").formatted(Formatting.WHITE));
            if (roll == null) {
                line.append(Text.literal("absent").formatted(Formatting.GRAY));
                continue;
            }
            rolled++;
            if (!roll.outcome().succeeded()) {
                failed++;
            }
            line.append(entry(roll));
        }
        line.append(Text.literal(" · " + failed + " of " + rolled + " failed ").formatted(Formatting.GOLD));
        line.append(button("Passed", "pass", Formatting.GREEN)).append(" ")
                .append(button("Failed", "fail", Formatting.RED));
        call.caller.sendFeedback(line, false);
        DnDClasses.LOGGER.info("[DmRolls] call #{} done: {} of {} failed", call.number, failed, rolled);
    }

    private static Text button(String label, String which, Formatting color) {
        String command = "/dm apply " + which + " ";
        return Text.literal("[" + label + "]").styled(style -> style.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal(command + "<command>: run a command as each player who "
                                + (which.equals("pass") ? "passed" : "failed")))));
    }

    private static void sendPrompt(ServerPlayerEntity player, Call call, int ticks) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeText(call.check.label());
        buf.writeText(call.caller.getDisplayName());
        buf.writeVarInt(call.secret ? 0 : call.dc);
        buf.writeEnumConstant(call.mode);
        buf.writeVarInt(ticks);
        ServerPlayNetworking.send(player, S2C_PROMPT, buf);
    }

    /** Players of the last call whose roll passed ({@code pass}), failed ({@code fail}) or any ({@code all}). */
    public static List<UUID> playersOf(Call call, String which) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, D20.Roll> entry : call.results.entrySet()) {
            D20.Roll roll = entry.getValue();
            if (roll == null) {
                continue;
            }
            boolean passed = roll.outcome().succeeded();
            if (which.equals("all") || which.equals("pass") == passed) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
