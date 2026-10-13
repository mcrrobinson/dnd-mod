package mattonfire.dnd.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Class rules that keep a temporary attribute modifier in step with the player's class (Monk armour
 * penalty, Artificer damage, Paladin Nether weakness). Each rule is idempotent: it adds its modifier
 * when it should apply and removes it otherwise.
 * <p>
 * Rules run for every player on a class change ({@link ClassLifecycle#change}), on join, respawn and
 * world change, and every {@value #INTERVAL_TICKS} ticks. A rule that has to follow something faster
 * (the Monk's worn armour) also runs every tick, but only for the players its filter picks.
 */
public final class ClassAttributeRules {
    /** How often every rule is re-checked for every player. */
    public static final int INTERVAL_TICKS = 20;

    private record Rule(Consumer<ServerPlayerEntity> apply, Predicate<ServerPlayerEntity> everyTick) {
    }

    private static final List<Rule> RULES = new ArrayList<>();

    private ClassAttributeRules() {
    }

    /** Adds a rule that runs on class change, join, respawn, world change and every {@value #INTERVAL_TICKS} ticks. */
    public static void add(Consumer<ServerPlayerEntity> rule) {
        RULES.add(new Rule(rule, player -> false));
    }

    /** As {@link #add}, and also every tick for the players {@code everyTick} picks. */
    public static void add(Consumer<ServerPlayerEntity> rule, Predicate<ServerPlayerEntity> everyTick) {
        RULES.add(new Rule(rule, everyTick));
    }

    /** Brings every rule's modifiers up to date for the player. */
    public static void apply(ServerPlayerEntity player) {
        for (Rule rule : RULES) {
            rule.apply().accept(player);
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean sweep = server.getTicks() % INTERVAL_TICKS == 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                for (Rule rule : RULES) {
                    if (sweep || rule.everyTick().test(player)) {
                        rule.apply().accept(player);
                    }
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply(handler.getPlayer()));
        // A respawned player is a new entity without the temporary modifiers.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> apply(newPlayer));
        // The Paladin's Nether weakness starts and ends with the dimension.
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> apply(player));
    }
}
