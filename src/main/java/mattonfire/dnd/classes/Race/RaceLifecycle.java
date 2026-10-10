package mattonfire.dnd.classes.Race;

import java.util.List;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.ClassLifecycle;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

/**
 * The one server-side path for a player's race, next to {@link ClassLifecycle} (which calls the join,
 * copy and respawn hooks here so the race is handled in the same order as the class):
 * <ul>
 * <li>{@link #change} picks or switches the race (the picker packet and {@code /dndrace set}).</li>
 * <li>Join and respawn re-apply {@link RaceStats} and tell the client its race, and whether to open the
 * race picker.</li>
 * <li>The {@code dndRaces} gamerule turns races off: nobody is prompted and no race modifiers apply.</li>
 * </ul>
 * Race packets carry the race id, the ancestry id and a "prompt" flag (true when the client should open
 * the picker for a race of NONE).
 */
public final class RaceLifecycle {
    public static final Identifier C2S_RACE_PICK = new Identifier(DnDClasses.MOD_ID, "race_pick");
    public static final Identifier S2C_RACE_QUERY = new Identifier(DnDClasses.MOD_ID, "race_query");
    public static final Identifier S2C_APPROVE_RACE_PICK = new Identifier(DnDClasses.MOD_ID, "approve_race_pick");

    public static final GameRules.Key<GameRules.BooleanRule> RACES_ENABLED = GameRuleRegistry.register(
            "dndRaces", GameRules.Category.PLAYER,
            GameRuleFactory.createBooleanRule(true, (server, rule) -> onRuleChanged(server)));

    private RaceLifecycle() {
    }

    public static void register() {
        RaceAbilityBonuses.register();
        BreathWeapon.register();
        ServerPlayNetworking.registerGlobalReceiver(C2S_RACE_PICK, (server, player, handler, buf, sender) -> {
            int race = buf.readVarInt();
            int ancestry = buf.readVarInt();
            server.execute(() -> onPick(player, race, ancestry));
        });
    }

    /** The saved race, whatever the gamerule says. */
    public static DndRace raceOf(PlayerEntity player) {
        DndRace race = player instanceof PlayerEntityExt ext ? ext.getDndRace() : null;
        return race == null ? DndRace.NONE : race;
    }

    public static DragonAncestry ancestryOf(PlayerEntity player) {
        DragonAncestry ancestry = player instanceof PlayerEntityExt ext ? ext.getDragonAncestry() : null;
        return ancestry == null ? DragonAncestry.NONE : ancestry;
    }

    /** The race whose traits apply: NONE while {@code dndRaces} is off. Use this for every trait. */
    public static DndRace activeRaceOf(PlayerEntity player) {
        return enabled(player) ? raceOf(player) : DndRace.NONE;
    }

    public static boolean enabled(PlayerEntity player) {
        // Client worlds have default game rules, so this is only meaningful on the server.
        return player.getWorld().getGameRules().getBoolean(RACES_ENABLED);
    }

    // --- ClassLifecycle hooks ---

    /** On join, before the class query, so the client opens the race picker first. */
    public static void onJoin(ServerPlayerEntity player) {
        RaceStats.apply(player);
        RaceAbilityBonuses.onChange.accept(player);
        boolean prompt = needsPick(player);
        if (prompt && ClassLifecycle.classOf(player) != DndCharacter.NONE) {
            player.sendMessage(Text.translatable("message.dndclasses.race.choose_heritage")
                    .formatted(Formatting.GOLD), false);
        }
        send(player, S2C_RACE_QUERY);
        BreathWeapon.sync(player);
    }

    /** A new player entity (death, End exit) gets the race of the old one. */
    public static void copy(PlayerEntity oldPlayer, PlayerEntity newPlayer) {
        if (newPlayer instanceof PlayerEntityExt ext) {
            ext.setDndRace(raceOf(oldPlayer), ancestryOf(oldPlayer));
        }
    }

    /** After respawn or End exit, before the class's health is set, so the race's max health counts. */
    public static void afterRespawn(ServerPlayerEntity newPlayer) {
        RaceStats.apply(newPlayer);
        RaceAbilityBonuses.onChange.accept(newPlayer);
        send(newPlayer, S2C_RACE_QUERY);
        BreathWeapon.sync(newPlayer);
    }

    // --- picking ---

    private static boolean needsPick(ServerPlayerEntity player) {
        return enabled(player) && raceOf(player) == DndRace.NONE;
    }

    /** The race picker. Only a player without a race may pick; changing afterwards is {@code /dndrace set}. */
    private static void onPick(ServerPlayerEntity player, int raceId, int ancestryId) {
        DndRace race;
        DragonAncestry ancestry;
        try {
            race = DndRace.fromValue(raceId);
            ancestry = DragonAncestry.fromValue(ancestryId);
        } catch (IllegalArgumentException e) {
            DnDClasses.LOGGER.warn("{} sent an unknown race {} / ancestry {}", player.getEntityName(), raceId,
                    ancestryId);
            return;
        }
        if (race == DndRace.NONE || !needsPick(player) || race.hasAncestry() != (ancestry != DragonAncestry.NONE)) {
            DnDClasses.LOGGER.info("Ignored race pick {} {} from {} (race {})", race.id(), ancestry.id(),
                    player.getEntityName(), raceOf(player).id());
            // Tell the client its real race again so it stops showing the picker.
            send(player, S2C_APPROVE_RACE_PICK);
            return;
        }
        change(player, race, ancestry);
    }

    /**
     * Sets a player's race (first pick or admin change): swaps the race modifiers, prints the intro and
     * tells the client. NONE clears the race, and the client reopens the race picker.
     */
    public static void change(ServerPlayerEntity player, DndRace race, DragonAncestry ancestry) {
        if (!race.hasAncestry()) {
            ancestry = DragonAncestry.NONE;
        }
        if (player instanceof PlayerEntityExt ext) {
            ext.setDndRace(race, ancestry);
        }
        RaceStats.apply(player);
        RaceAbilityBonuses.onChange.accept(player);
        RaceInfo info = RaceInfo.get(race);
        if (info != null) {
            sendIntro(player, info, ancestry);
        }
        DnDClasses.LOGGER.info("{} is now race {} {}", player.getEntityName(), race.id(), ancestry.id());
        send(player, S2C_APPROVE_RACE_PICK);
        BreathWeapon.sync(player);
    }

    private static void onRuleChanged(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            RaceStats.apply(player);
            RaceAbilityBonuses.onChange.accept(player);
            send(player, S2C_RACE_QUERY);
            BreathWeapon.sync(player);
        }
    }

    private static void send(ServerPlayerEntity player, Identifier packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(raceOf(player).getValue());
        buf.writeVarInt(ancestryOf(player).getValue());
        buf.writeBoolean(needsPick(player));
        ServerPlayNetworking.send(player, packet, buf);
    }

    /** The race name with its ancestry, e.g. "Dragonborn (Frost)". */
    public static String displayName(RaceInfo info, DragonAncestry ancestry) {
        return ancestry == DragonAncestry.NONE ? info.name() : info.name() + " (" + ancestry.displayName() + ")";
    }

    private static void sendIntro(PlayerEntity player, RaceInfo info, DragonAncestry ancestry) {
        player.sendMessage(Text.literal("The " + displayName(info, ancestry) + "\n")
                .formatted(Formatting.UNDERLINE, Formatting.GOLD), false);
        player.sendMessage(Text.literal(info.summary() + ".").formatted(Formatting.YELLOW), false);
        List<String> stats = info.stats().describe();
        if (!stats.isEmpty()) {
            player.sendMessage(Text.literal("Body: " + String.join(", ", stats) + ".").formatted(Formatting.GREEN),
                    false);
        }
        player.sendMessage(Text.literal("Ability scores: " + info.abilityText() + ".")
                .formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("Traits (coming soon): " + ClassInfo.joinForGame(info.traits()))
                .formatted(Formatting.DARK_PURPLE), false);
        player.sendMessage(Text.literal("Your heritage is in your Class Guidebook.").formatted(Formatting.GRAY,
                Formatting.ITALIC), false);
    }
}
