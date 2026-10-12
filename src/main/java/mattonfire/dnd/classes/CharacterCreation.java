package mattonfire.dnd.classes;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceInfo;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The combined character-creation pick: race, ancestry and class in one packet from the client's
 * character-creation screen, applied race first (so the class's starting health sees the race's max health),
 * with one chat intro for both. Each half follows its own one-pick rule: the race only applies while the
 * player needs a race ({@link RaceLifecycle#needsPick}), the class only while it is NONE. The separate race and
 * class pick packets still work on their own.
 */
public final class CharacterCreation {
    public static final Identifier C2S_CHARACTER_PICK = new Identifier(DnDClasses.MOD_ID, "character_pick");

    private CharacterCreation() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_CHARACTER_PICK, (server, player, handler, buf, sender) -> {
            int race = buf.readVarInt();
            int ancestry = buf.readVarInt();
            int dndClass = buf.readVarInt();
            server.execute(() -> onPick(player, race, ancestry, dndClass));
        });
    }

    private static void onPick(ServerPlayerEntity player, int raceId, int ancestryId, int classId) {
        DndRace race;
        DragonAncestry ancestry;
        DndCharacter picked;
        try {
            race = DndRace.fromValue(raceId);
            ancestry = DragonAncestry.fromValue(ancestryId);
            picked = DndCharacter.fromValue(classId);
        } catch (IllegalArgumentException e) {
            DnDClasses.LOGGER.warn("{} sent an unknown character pick {} / {} / {}", player.getEntityName(), raceId,
                    ancestryId, classId);
            reject(player);
            return;
        }
        boolean needRace = RaceLifecycle.needsPick(player);
        boolean needClass = ClassLifecycle.classOf(player) == DndCharacter.NONE;
        if (!needRace && !needClass || needRace && !RaceLifecycle.validPick(race, ancestry)
                || needClass && picked == DndCharacter.NONE) {
            DnDClasses.LOGGER.info("Ignored character pick {} {} {} from {} (race {}, class {})", race.id(),
                    ancestry.id(), picked, player.getEntityName(), RaceLifecycle.raceOf(player).id(),
                    ClassLifecycle.classOf(player));
            reject(player);
            return;
        }
        // Every half answers with its approval, so the client knows both are settled.
        if (needRace) {
            RaceLifecycle.change(player, race, ancestry, false);
        } else {
            RaceLifecycle.resend(player);
        }
        if (needClass) {
            ClassLifecycle.change(player, picked, false);
        } else {
            ClassLifecycle.resend(player);
        }
        sendIntro(player, needRace ? RaceInfo.get(race) : null, ancestry,
                needClass ? ClassInfo.get(picked) : null);
        if (needClass) {
            DnDClasses.sendRespawnHint(player);
        }
        DnDClasses.LOGGER.info("{} created {} {} {}", player.getEntityName(), RaceLifecycle.raceOf(player).id(),
                RaceLifecycle.ancestryOf(player).id(), ClassLifecycle.classOf(player));
    }

    /** Tells the client its real race and class again, so it stops waiting on the pick. */
    private static void reject(ServerPlayerEntity player) {
        RaceLifecycle.resend(player);
        ClassLifecycle.resend(player);
    }

    /**
     * The chat intro for a race, a class or both, from the same data as the guidebook and README: "The Dwarf
     * Barbarian", the race's lines, the class's lines and a pointer to the guidebook. {@link RaceLifecycle} and
     * {@link ClassLifecycle} use it for a single pick too.
     */
    public static void sendIntro(PlayerEntity player, @Nullable RaceInfo race, DragonAncestry ancestry,
            @Nullable ClassInfo dndClass) {
        if (race == null && dndClass == null) {
            return;
        }
        String name = race == null ? dndClass.name()
                : dndClass == null ? RaceLifecycle.displayName(race, ancestry)
                        : RaceLifecycle.displayName(race, ancestry) + " " + dndClass.name();
        player.sendMessage(Text.literal("The " + name + "\n").formatted(Formatting.UNDERLINE, Formatting.GOLD), false);
        if (race != null) {
            player.sendMessage(Text.literal(race.summary() + ".").formatted(Formatting.YELLOW), false);
            List<String> stats = race.stats().describe();
            if (!stats.isEmpty()) {
                player.sendMessage(Text.literal("Body: " + String.join(", ", stats) + ".")
                        .formatted(Formatting.GREEN), false);
            }
            player.sendMessage(Text.literal("Ability scores: " + race.abilityText() + ".")
                    .formatted(Formatting.AQUA), false);
            player.sendMessage(Text.literal("Traits: " + ClassInfo.joinForGame(race.traits()))
                    .formatted(Formatting.DARK_PURPLE), false);
        }
        if (dndClass != null) {
            player.sendMessage(Text.literal("Pros: " + ClassInfo.joinForGame(dndClass.pros()))
                    .formatted(Formatting.GREEN), false);
            player.sendMessage(Text.literal("Cons: " + ClassInfo.joinForGame(dndClass.cons()))
                    .formatted(Formatting.RED), false);
            player.sendMessage(Text.literal("Special: " + ClassInfo.joinForGame(dndClass.special()))
                    .formatted(Formatting.DARK_PURPLE), false);
        }
        String footer = dndClass == null ? "Your heritage is in your Class Guidebook."
                : race == null ? "Read your Class Guidebook for the details."
                        : "Your heritage and class are in your Class Guidebook.";
        player.sendMessage(Text.literal(footer).formatted(Formatting.GRAY, Formatting.ITALIC), false);
    }
}
