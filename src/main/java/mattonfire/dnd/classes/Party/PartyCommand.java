package mattonfire.dnd.classes.Party;

import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /party create
 * /party invite <player>
 * /party accept [inviter]
 * /party leave
 * /party list
 * /party kick <player> (leader only)
 *
 * Open to every player (no op level needed).
 */
public class PartyCommand {
    private static SimpleCommandExceptionType error(String message) {
        return new SimpleCommandExceptionType(Text.literal(message));
    }

    private static final SimpleCommandExceptionType ALREADY_IN_PARTY = error(
            "You're already in a party. Use /party leave first.");
    private static final SimpleCommandExceptionType NOT_IN_PARTY = error(
            "You're not in a party. Use /party create.");
    private static final SimpleCommandExceptionType NO_INVITES = error("You have no pending party invites.");
    private static final SimpleCommandExceptionType NO_INVITE_FROM = error("You have no invite from that player.");
    private static final SimpleCommandExceptionType PARTY_FULL = error(
            "That party is full (" + PartyManager.MAX_PARTY_SIZE + " players).");
    private static final SimpleCommandExceptionType NOT_LEADER = error("Only the party leader can do that.");
    private static final SimpleCommandExceptionType NOT_A_MEMBER = error("That player isn't in your party.");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("party")
                .then(CommandManager.literal("create").executes(PartyCommand::create))
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(PartyCommand::invite)))
                .then(CommandManager.literal("accept")
                        .executes(context -> accept(context, null))
                        .then(CommandManager.argument("inviter", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player == null) {
                                        return builder.buildFuture();
                                    }
                                    PartyManager manager = PartyManager.get(context.getSource().getServer());
                                    return CommandSource.suggestMatching(manager.pendingInvites(player).stream()
                                            .map(manager::getName), builder);
                                })
                                .executes(context -> accept(context,
                                        StringArgumentType.getString(context, "inviter")))))
                .then(CommandManager.literal("leave").executes(PartyCommand::leave))
                .then(CommandManager.literal("list").executes(PartyCommand::list))
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    PartyManager manager = PartyManager.get(context.getSource().getServer());
                                    Party party = player == null ? null : manager.getParty(player.getUuid());
                                    if (party == null) {
                                        return builder.buildFuture();
                                    }
                                    return CommandSource.suggestMatching(party.getMembers().stream()
                                            .filter(uuid -> !uuid.equals(player.getUuid()))
                                            .map(manager::getName), builder);
                                })
                                .executes(PartyCommand::kick))));
    }

    private static int create(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PartyManager manager = PartyManager.get(context.getSource().getServer());
        if (manager.getParty(player.getUuid()) != null) {
            throw ALREADY_IN_PARTY.create();
        }
        manager.create(player);
        context.getSource().sendFeedback(Text.literal("Party created. Invite players with /party invite <player>.")
                .formatted(Formatting.GREEN), false);
        PartyEvents.syncHud(context.getSource().getServer());
        return 1;
    }

    private static int invite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        PartyManager manager = PartyManager.get(context.getSource().getServer());

        if (target == player) {
            throw error("You can't invite yourself.").create();
        }
        Party party = manager.getParty(player.getUuid());
        if (party == null) {
            // Inviting without a party starts one, so two commands are enough.
            party = manager.create(player);
            context.getSource().sendFeedback(Text.literal("Party created.").formatted(Formatting.GREEN), false);
        }
        if (party.contains(target.getUuid())) {
            throw error(target.getEntityName() + " is already in your party.").create();
        }
        if (manager.getParty(target.getUuid()) != null) {
            throw error(target.getEntityName() + " is already in another party.").create();
        }
        if (party.size() >= PartyManager.MAX_PARTY_SIZE) {
            throw PARTY_FULL.create();
        }

        manager.invite(player, target);
        String command = "/party accept " + player.getEntityName();
        MutableText accept = Text.literal("[Accept]").styled(style -> style
                .withColor(Formatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
        target.sendMessage(Text.literal(player.getEntityName() + " invited you to their party. ")
                .formatted(Formatting.GOLD).append(accept));
        context.getSource().sendFeedback(Text.literal("Invited " + target.getEntityName()
                + " (expires in " + PartyManager.INVITE_TIMEOUT_TICKS / 20 + "s).").formatted(Formatting.GREEN),
                false);
        return 1;
    }

    private static int accept(CommandContext<ServerCommandSource> context, String inviterName)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PartyManager manager = PartyManager.get(context.getSource().getServer());
        if (manager.getParty(player.getUuid()) != null) {
            throw ALREADY_IN_PARTY.create();
        }

        List<UUID> pending = manager.pendingInvites(player);
        if (pending.isEmpty()) {
            throw NO_INVITES.create();
        }
        UUID inviter;
        if (inviterName == null) {
            // Most recent invite
            inviter = pending.get(pending.size() - 1);
        } else {
            inviter = pending.stream().filter(uuid -> manager.getName(uuid).equalsIgnoreCase(inviterName))
                    .findFirst().orElseThrow(NO_INVITE_FROM::create);
        }

        Party party = manager.getParty(inviter);
        if (party.size() >= PartyManager.MAX_PARTY_SIZE) {
            manager.removeInvite(player.getUuid(), inviter);
            throw PARTY_FULL.create();
        }
        broadcast(context.getSource().getServer(), party,
                Text.literal(player.getEntityName() + " joined the party.").formatted(Formatting.GREEN));
        manager.join(party, player);
        context.getSource().sendFeedback(Text.literal("You joined " + manager.getName(party.getLeader())
                + "'s party.").formatted(Formatting.GREEN), false);
        PartyEvents.syncHud(context.getSource().getServer());
        return 1;
    }

    private static int leave(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        MinecraftServer server = context.getSource().getServer();
        PartyManager manager = PartyManager.get(server);
        Party current = manager.getParty(player.getUuid());
        if (current == null) {
            throw NOT_IN_PARTY.create();
        }
        boolean wasLeader = current.isLeader(player.getUuid());
        Party party = manager.leave(player.getUuid());
        context.getSource().sendFeedback(Text.literal("You left the party.").formatted(Formatting.YELLOW), false);
        notifyRemoved(server, party, player.getEntityName() + " left the party.");
        if (wasLeader && party.size() > 0) {
            broadcast(server, party, Text.literal(manager.getName(party.getLeader()) + " is now the party leader.")
                    .formatted(Formatting.GOLD));
        }
        PartyEvents.clearHud(player);
        PartyEvents.syncHud(server);
        return 1;
    }

    private static int kick(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        MinecraftServer server = context.getSource().getServer();
        PartyManager manager = PartyManager.get(server);
        Party party = manager.getParty(player.getUuid());
        if (party == null) {
            throw NOT_IN_PARTY.create();
        }
        if (!party.isLeader(player.getUuid())) {
            throw NOT_LEADER.create();
        }
        String name = StringArgumentType.getString(context, "player");
        UUID target = party.getMembers().stream()
                .filter(uuid -> !uuid.equals(player.getUuid()) && manager.getName(uuid).equalsIgnoreCase(name))
                .findFirst().orElseThrow(NOT_A_MEMBER::create);
        String targetName = manager.getName(target);
        manager.leave(target);
        ServerPlayerEntity kicked = server.getPlayerManager().getPlayer(target);
        if (kicked != null) {
            kicked.sendMessage(Text.literal("You were removed from the party.").formatted(Formatting.YELLOW));
            PartyEvents.clearHud(kicked);
        }
        notifyRemoved(server, party, targetName + " was removed from the party.");
        PartyEvents.syncHud(server);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        MinecraftServer server = context.getSource().getServer();
        PartyManager manager = PartyManager.get(server);
        Party party = manager.getParty(player.getUuid());
        if (party == null) {
            List<UUID> pending = manager.pendingInvites(player);
            if (pending.isEmpty()) {
                throw NOT_IN_PARTY.create();
            }
            context.getSource().sendFeedback(Text.literal("You're not in a party. Pending invites from: "
                    + String.join(", ", pending.stream().map(manager::getName).toList())), false);
            return 0;
        }

        MutableText text = Text.literal("Party (" + party.size() + "/" + PartyManager.MAX_PARTY_SIZE + "):")
                .formatted(Formatting.GOLD);
        for (UUID uuid : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(uuid);
            MutableText line = Text.literal("\n " + (party.isLeader(uuid) ? "★ " : "- ") + manager.getName(uuid))
                    .formatted(member != null ? Formatting.WHITE : Formatting.GRAY);
            if (member != null) {
                line.append(Text.literal(String.format(" %.0f/%.0f HP", member.getHealth(), member.getMaxHealth()))
                        .formatted(Formatting.RED));
            } else {
                line.append(Text.literal(" (offline)").formatted(Formatting.DARK_GRAY));
            }
            text.append(line);
        }
        context.getSource().sendFeedback(text, false);
        return party.size();
    }

    private static void notifyRemoved(MinecraftServer server, Party party, String message) {
        broadcast(server, party, Text.literal(message).formatted(Formatting.YELLOW));
    }

    private static void broadcast(MinecraftServer server, Party party, Text text) {
        for (ServerPlayerEntity member : party.getOnlineMembers(server)) {
            member.sendMessage(text);
        }
    }
}
