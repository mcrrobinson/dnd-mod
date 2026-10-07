package mattonfire.dnd.classes.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import java.util.Optional;
import mattonfire.dnd.entity.raid.GoblinRaid;
import mattonfire.dnd.entity.raid.GoblinRaids;
import mattonfire.dnd.entity.raid.Settlement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * /goblinraid start        raid the hobbit village or dwarven fortress nearest you (within ~128 blocks),
 *                          or the spot you're standing on if there isn't one
 * /goblinraid start here   raid the spot you're standing on
 * /goblinraid stop         call off the raid nearest you
 * /goblinraid list         list the raids in this world
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
                .then(CommandManager.literal("list").executes(GoblinRaidCommand::list)));
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

    private static int list(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<GoblinRaid> raids = GoblinRaids.get(source.getWorld()).getRaids();
        if (raids.isEmpty()) {
            source.sendFeedback(Text.literal("No goblin raids"), false);
        }
        for (GoblinRaid raid : raids) {
            source.sendFeedback(Text.literal("Goblin raid #" + raid.getId() + " on ")
                    .append(Text.translatable(raid.getKind().nameKey()))
                    .append(" at " + raid.getRally().toShortString() + ": " + raid.getState()
                            + ", wave " + raid.getWave() + "/" + raid.getTotalWaves()
                            + ", " + raid.getRaidersLeft() + " raiders left"), false);
        }
        return raids.size();
    }
}
