package mattonfire.dnd.entity.hireling;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import mattonfire.dnd.entity.HirelingEntity;
import mattonfire.dnd.entity.InnkeeperEntity;
import mattonfire.dnd.world.GameTime;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /hireling}: list, hire, pay, follow, wait and dismiss for everyone; {@code /hireling admin ...} (op 2)
 * for tests and server admins. Hirelings are named by first name, by UUID (the chat card's buttons) or
 * {@code all}.
 */
public final class HirelingCommand {
    private HirelingCommand() {
    }

    private static final SuggestionProvider<ServerCommandSource> OWN_NAMES = (context, builder) -> {
        List<String> names = new ArrayList<>();
        names.add("all");
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player != null) {
            for (HirelingRegistry.Record record : HirelingRegistry.get(context.getSource().getServer()).of(player.getUuid())) {
                names.add(firstName(record.name));
            }
        }
        return CommandSource.suggestMatching(names, builder);
    };

    private static final SuggestionProvider<ServerCommandSource> ROLES = (context, builder) ->
            CommandSource.suggestMatching(java.util.Arrays.stream(HirelingRole.values()).map(HirelingRole::id), builder);
    private static final SuggestionProvider<ServerCommandSource> RACES = (context, builder) ->
            CommandSource.suggestMatching(java.util.Arrays.stream(HirelingRace.values()).map(HirelingRace::id), builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("hireling")
                .then(CommandManager.literal("list").executes(HirelingCommand::list))
                .then(CommandManager.literal("hire")
                        .then(CommandManager.argument("who", StringArgumentType.word())
                                .suggests((c, b) -> CommandSource.suggestMatching(List.of("nearest"), b))
                                .executes(HirelingCommand::hire)))
                .then(CommandManager.literal("pay")
                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(OWN_NAMES)
                                .then(CommandManager.argument("days", IntegerArgumentType.integer(1, HirelingWages.MAX_PURSE_DAYS))
                                        .executes(HirelingCommand::pay))))
                .then(CommandManager.literal("follow")
                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(OWN_NAMES)
                                .executes(context -> mode(context, false))))
                .then(CommandManager.literal("wait")
                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(OWN_NAMES)
                                .executes(context -> mode(context, true))))
                .then(CommandManager.literal("dismiss")
                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(OWN_NAMES)
                                .executes(HirelingCommand::dismiss)))
                .then(CommandManager.literal("admin").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("spawn")
                                .then(CommandManager.argument("owner", EntityArgumentType.player())
                                        .then(CommandManager.argument("role", StringArgumentType.word()).suggests(ROLES)
                                                .executes(context -> spawn(context, null, null))
                                                .then(CommandManager.argument("race", StringArgumentType.word()).suggests(RACES)
                                                        .executes(context -> spawn(context, StringArgumentType.getString(context, "race"), null))
                                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                                .executes(context -> spawn(context, StringArgumentType.getString(context, "race"),
                                                                        StringArgumentType.getString(context, "name"))))))))
                        .then(CommandManager.literal("loyalty")
                                .then(CommandManager.argument("hireling", EntityArgumentType.entity())
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(HirelingCommand::setLoyalty))))
                        .then(CommandManager.literal("purse")
                                .then(CommandManager.argument("hireling", EntityArgumentType.entity())
                                        .then(CommandManager.argument("nuggets", IntegerArgumentType.integer(0, 10000))
                                                .executes(HirelingCommand::setPurse))))
                        .then(CommandManager.literal("newday")
                                .then(CommandManager.argument("owner", EntityArgumentType.player())
                                        .executes(HirelingCommand::newDay)))
                        .then(CommandManager.literal("roster")
                                .then(CommandManager.argument("innkeeper", EntityArgumentType.entity())
                                        .then(CommandManager.literal("reroll").executes(HirelingCommand::reroll))))
                        .then(CommandManager.literal("knockout")
                                .then(CommandManager.argument("hireling", EntityArgumentType.entity())
                                        .executes(HirelingCommand::knockout)))
                        .then(CommandManager.literal("list")
                                .executes(context -> adminList(context, null))
                                .then(CommandManager.argument("owner", EntityArgumentType.player())
                                        .executes(context -> adminList(context, EntityArgumentType.getPlayer(context, "owner")))))));
    }

    private static String firstName(String name) {
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }

    /** The player's loaded hirelings matching a first name, a UUID or {@code all}. */
    private static List<HirelingEntity> resolve(ServerPlayerEntity player, String name) {
        List<HirelingEntity> matches = new ArrayList<>();
        for (HirelingEntity hireling : Hirelings.hirelingsOf(player)) {
            if (name.equalsIgnoreCase("all") || hireling.getUuidAsString().equalsIgnoreCase(name)
                    || hireling.firstName().toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                matches.add(hireling);
            }
        }
        if (matches.isEmpty()) {
            player.sendMessage(Text.translatable("hireling.dndclasses.command.none", name).formatted(Formatting.RED), false);
        }
        return matches;
    }

    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        MinecraftServer server = context.getSource().getServer();
        List<HirelingRegistry.Record> records = HirelingRegistry.get(server).of(player.getUuid());
        if (records.isEmpty()) {
            context.getSource().sendFeedback(Text.translatable("hireling.dndclasses.command.list_empty").formatted(Formatting.GRAY), false);
            return 0;
        }
        context.getSource().sendFeedback(Text.translatable("hireling.dndclasses.command.list_header", records.size(),
                Hirelings.MAX_PER_PLAYER).formatted(Formatting.GOLD), false);
        for (HirelingRegistry.Record record : records) {
            HirelingEntity hireling = Hirelings.loaded(server, record);
            if (hireling != null) {
                String mode = hireling.isKnockedOut() ? "knocked out" : hireling.isWaiting() ? "waiting" : "following";
                context.getSource().sendFeedback(Text.literal(" - " + hireling.hirelingName() + ", ")
                        .append(hireling.role().title())
                        .append(String.format(Locale.ROOT, ": %d/%d HP, loyalty %d, purse %d (%d days), %s, at %d %d %d (%.0f blocks)",
                                (int) Math.ceil(hireling.getHealth()), (int) hireling.getMaxHealth(), hireling.loyalty(),
                                hireling.purse(), hireling.daysPaid(), mode, hireling.getBlockX(), hireling.getBlockY(),
                                hireling.getBlockZ(), hireling.world == player.world ? hireling.distanceTo(player) : -1.0F)), false);
            } else {
                context.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT, " - %s: not nearby (%s, last at %d %d %d in %s)",
                        record.name, record.waiting ? "waiting" : "following", record.pos.getX(), record.pos.getY(), record.pos.getZ(),
                        record.dimension.getValue())).formatted(Formatting.GRAY), false);
            }
        }
        return records.size();
    }

    private static int hire(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String who = StringArgumentType.getString(context, "who");
        HirelingEntity candidate = null;
        if (who.equalsIgnoreCase("nearest")) {
            candidate = Hirelings.nearestCandidate(player);
        } else {
            try {
                Entity entity = player.getWorld().getEntity(UUID.fromString(who));
                candidate = entity instanceof HirelingEntity hireling ? hireling : null;
            } catch (IllegalArgumentException ignored) {
                // not a UUID
            }
        }
        if (candidate == null) {
            context.getSource().sendError(Text.translatable("hireling.dndclasses.command.no_candidate"));
            return 0;
        }
        return Hirelings.hire(player, candidate) ? 1 : 0;
    }

    private static int pay(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        int days = IntegerArgumentType.getInteger(context, "days");
        int paid = 0;
        for (HirelingEntity hireling : resolve(player, StringArgumentType.getString(context, "name"))) {
            if (Hirelings.payDays(player, hireling, days)) {
                paid++;
            }
        }
        return paid;
    }

    private static int mode(CommandContext<ServerCommandSource> context, boolean waiting) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        List<HirelingEntity> hirelings = resolve(player, StringArgumentType.getString(context, "name"));
        for (HirelingEntity hireling : hirelings) {
            hireling.setWaiting(waiting);
            context.getSource().sendFeedback(Text.translatable(waiting ? "hireling.dndclasses.waits" : "hireling.dndclasses.follows",
                    hireling.firstName()).formatted(Formatting.YELLOW), false);
        }
        return hirelings.size();
    }

    private static int dismiss(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "name");
        MinecraftServer server = context.getSource().getServer();
        HirelingRegistry registry = HirelingRegistry.get(server);
        int count = 0;
        // Ones that aren't loaded are struck off the list: if they ever load again, they're discarded.
        for (HirelingRegistry.Record record : List.copyOf(registry.of(player.getUuid()))) {
            if (Hirelings.loaded(server, record) == null && (name.equalsIgnoreCase("all")
                    || record.uuid.toString().equalsIgnoreCase(name)
                    || firstName(record.name).toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT)))) {
                registry.remove(player.getUuid(), record.uuid);
                player.sendMessage(Text.translatable("hireling.dndclasses.dismissed", firstName(record.name))
                        .formatted(Formatting.YELLOW), false);
                count++;
            }
        }
        List<HirelingEntity> hirelings = count > 0 ? Hirelings.hirelingsOf(player) : resolve(player, name);
        for (HirelingEntity hireling : hirelings) {
            if (name.equalsIgnoreCase("all") || hireling.getUuidAsString().equalsIgnoreCase(name)
                    || hireling.firstName().toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
                hireling.leave(false);
                count++;
            }
        }
        return count;
    }

    // ---- Admin ----

    private static HirelingEntity hireling(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "hireling");
        if (!(entity instanceof HirelingEntity hireling)) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Text.translatable("hireling.dndclasses.command.not_hireling")).create();
        }
        return hireling;
    }

    private static int spawn(CommandContext<ServerCommandSource> context, String raceName, String name) throws CommandSyntaxException {
        ServerPlayerEntity owner = EntityArgumentType.getPlayer(context, "owner");
        HirelingRole role = HirelingRole.byId(StringArgumentType.getString(context, "role"));
        if (role == null) {
            context.getSource().sendError(Text.literal("Unknown role. Use tank, healer, damage, support or utility."));
            return 0;
        }
        HirelingRace race = raceName == null ? HirelingRace.pick(owner.getRandom()) : HirelingRace.byId(raceName);
        if (race == null) {
            context.getSource().sendError(Text.literal("Unknown race. Use human, elf, half_orc, tiefling, dragonborn or gnome."));
            return 0;
        }
        HirelingEntity hireling = Hirelings.spawnHired(owner, role, race, name);
        context.getSource().sendFeedback(Text.literal("Spawned ").append(hireling.getDisplayName())
                .append(" for " + owner.getEntityName() + " (" + race.id() + ", purse " + hireling.purse() + ")"), true);
        return 1;
    }

    private static int setLoyalty(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        HirelingEntity hireling = hireling(context);
        int value = IntegerArgumentType.getInteger(context, "value");
        hireling.changeLoyalty(value - hireling.loyalty(), "admin");
        context.getSource().sendFeedback(Text.literal(hireling.firstName() + " loyalty: " + hireling.loyalty()), true);
        return hireling.loyalty();
    }

    private static int setPurse(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        HirelingEntity hireling = hireling(context);
        hireling.setPurse(IntegerArgumentType.getInteger(context, "nuggets"));
        Hirelings.refreshRecord(hireling);
        context.getSource().sendFeedback(Text.literal(hireling.firstName() + " purse: " + hireling.purse()), true);
        return hireling.purse();
    }

    private static int newDay(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity owner = EntityArgumentType.getPlayer(context, "owner");
        List<HirelingEntity> hirelings = Hirelings.hirelingsOf(owner);
        for (HirelingEntity hireling : hirelings) {
            hireling.payDay(owner);
            context.getSource().sendFeedback(Text.literal(hireling.firstName() + ": loyalty " + hireling.loyalty()
                    + ", purse " + hireling.purse() + ", unpaid days " + hireling.unpaidStreak()
                    + (hireling.isHired() ? "" : ", left")), true);
        }
        return hirelings.size();
    }

    private static int reroll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "innkeeper");
        if (!(entity instanceof InnkeeperEntity innkeeper)) {
            context.getSource().sendError(Text.literal("That isn't an innkeeper."));
            return 0;
        }
        List<HirelingEntity> spawned = HirelingRoster.reroll(innkeeper, GameTime.day(innkeeper.world));
        for (HirelingEntity hireling : spawned) {
            context.getSource().sendFeedback(Text.literal("For hire: ").append(hireling.getDisplayName())
                    .append(" (" + hireling.race().id() + ") at " + hireling.getBlockX() + " " + hireling.getBlockY() + " "
                            + hireling.getBlockZ()), true);
        }
        return spawned.size();
    }

    private static int knockout(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        HirelingEntity hireling = hireling(context);
        if (!hireling.isHired()) {
            context.getSource().sendError(Text.literal(hireling.firstName() + " isn't hired."));
            return 0;
        }
        hireling.knockOut();
        context.getSource().sendFeedback(Text.literal(hireling.firstName() + " is knocked out."), true);
        return 1;
    }

    private static int adminList(CommandContext<ServerCommandSource> context, ServerPlayerEntity only) {
        MinecraftServer server = context.getSource().getServer();
        int total = 0;
        for (Map.Entry<UUID, List<HirelingRegistry.Record>> entry : HirelingRegistry.get(server).all().entrySet()) {
            if (only != null && !only.getUuid().equals(entry.getKey())) {
                continue;
            }
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(entry.getKey());
            context.getSource().sendFeedback(Text.literal((owner != null ? owner.getEntityName() : entry.getKey().toString())
                    + ":").formatted(Formatting.GOLD), false);
            for (HirelingRegistry.Record record : entry.getValue()) {
                boolean loaded = Hirelings.loaded(server, record) != null;
                context.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT, " - %s %s gen %d%s%s at %d %d %d %s",
                        record.name, record.uuid, record.generation, record.waiting ? " waiting" : "",
                        record.stashed ? " stashed" : loaded ? " loaded" : " unloaded",
                        record.pos.getX(), record.pos.getY(), record.pos.getZ(), record.dimension.getValue())), false);
                total++;
            }
        }
        if (total == 0) {
            context.getSource().sendFeedback(Text.literal("No hirelings."), false);
        }
        return total;
    }
}
