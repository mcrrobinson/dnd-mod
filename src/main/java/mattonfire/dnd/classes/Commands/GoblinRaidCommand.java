package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.entity.raid.GoblinRaid;
import mattonfire.dnd.entity.raid.GoblinRaids;
import mattonfire.dnd.entity.raid.Settlement;
import mattonfire.dnd.entity.raid.SettlementHealth;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * /goblinraid start        raid the hobbit village, dwarven fortress, elven enclave or dragon temple nearest you (within ~128 blocks),
 *                          or the spot you're standing on if there isn't one
 * /goblinraid start here   raid the spot you're standing on
 * /goblinraid stop         call off the raid nearest you
 * /goblinraid list         list the raids in this world, and the quiet zones round cleared goblin warrens
 * /goblinraid health <n>   set the settlement health of the raid nearest you (0 makes the settlement fall)
 *
 * Ignores the dndGoblinRaids gamerule and the per-settlement cooldown, for testing.
 */
public class GoblinRaidCommand {
    private static final int SEARCH_CHUNKS = 8;
    private static final SimpleCommandExceptionType ALREADY = new SimpleCommandExceptionType(
            Text.literal("Goblins are already raiding there"));
    private static final SimpleCommandExceptionType NONE = new SimpleCommandExceptionType(
            Text.literal("No goblin raid to stop"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("goblinraid")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("start")
                        .executes(context -> start(context, false))
                        .then(CommandManager.literal("here").executes(context -> start(context, true))))
                .then(CommandManager.literal("stop").executes(GoblinRaidCommand::stop))
                .then(CommandManager.literal("list").executes(GoblinRaidCommand::list))
                .then(CommandManager.literal("health")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0F, SettlementHealth.MAX))
                                .executes(GoblinRaidCommand::health))));
    }

    private static int start(CommandContext<ServerCommandSource> context, boolean here) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        Settlement settlement = here ? Settlement.wilds(pos)
                : Settlement.find(world, pos, SEARCH_CHUNKS, SEARCH_CHUNKS * 16).orElseGet(() -> Settlement.wilds(pos));
        GoblinRaid raid = GoblinRaids.get(world).start(world, settlement);
        if (raid == null) {
            throw ALREADY.create();
        }
        source.sendFeedback(Text.literal("Started goblin raid #" + raid.getId() + " on ")
                .append(Text.translatable(settlement.kind().nameKey()))
                .append(" at " + settlement.rally().toShortString() + " (" + raid.getTotalWaves() + " waves)"), true);
        return raid.getId();
    }

    private static int stop(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Optional<GoblinRaid> raid = GoblinRaids.get(source.getWorld())
                .stopNearest(source.getWorld(), BlockPos.ofFloored(source.getPosition()));
        if (raid.isEmpty()) {
            throw NONE.create();
        }
        source.sendFeedback(Text.literal("Called off goblin raid #" + raid.get().getId()), true);
        return 1;
    }

    private static int health(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        GoblinRaid raid = GoblinRaids.get(source.getWorld()).getRaids().stream()
                .filter(r -> !r.isOver())
                .min(Comparator.comparingDouble(r -> r.getRally().getSquaredDistance(pos)))
                .orElseThrow(NONE::create);
        float value = FloatArgumentType.getFloat(context, "value");
        raid.setHealth(value);
        source.sendFeedback(Text.literal("Set the settlement health of goblin raid #" + raid.getId() + " to "
                + raid.getHealth().shown() + "/" + (int) SettlementHealth.MAX), true);
        return raid.getHealth().shown();
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<GoblinRaid> raids = GoblinRaids.get(source.getWorld()).getRaids();
        if (raids.isEmpty()) {
            source.sendFeedback(Text.literal("No goblin raids"), false);
        }
        long now = source.getWorld().getTime();
        for (GoblinRaids.QuietZone zone : GoblinRaids.get(source.getWorld()).getQuietZones()) {
            if (now < zone.until()) {
                source.sendFeedback(Text.literal("No raids within " + zone.radius() + " blocks of " + zone.center().toShortString()
                        + " (a cleared goblin warren) for " + (zone.until() - now) / 1200L + " more minutes"), false);
            }
        }
        for (GoblinRaid raid : raids) {
            source.sendFeedback(Text.literal("Goblin raid #" + raid.getId() + " on ")
                    .append(Text.translatable(raid.getKind().nameKey()))
                    .append(" at " + raid.getRally().toShortString() + ": " + raid.getState()
                            + ", wave " + raid.getWave() + "/" + raid.getTotalWaves()
                            + ", " + raid.getRaidersLeft() + " raiders left, settlement health "
                            + raid.getHealth().shown() + "/" + (int) SettlementHealth.MAX
                            + " (" + raid.getHealth().folkKilled() + " folk killed, " + raid.getHealth().buildingsLost()
                            + "/" + raid.getHealth().sampleSize() + " building blocks lost)"), false);
        }
        return raids.size();
    }
}
