package mattonfire.dnd.classes.Race;

import java.util.Map;
import java.util.function.Consumer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * The hook between races and ability scores ({@code classes/Abilities/}). Races hold their ability bonuses
 * as data in {@code race_info.json}; {@link #register()} adds them to the character sheet as the
 * {@code dndclasses:race} contributor and rebuilds the sheet whenever the race changes.
 */
public final class RaceAbilityBonuses {
    /** The contributor id area 4's design reserves for races. */
    public static final Identifier CONTRIBUTOR_ID = new Identifier(DnDClasses.MOD_ID, "race");

    /**
     * Called on the server whenever a player's race (or the dndRaces gamerule) changes, so a cached
     * character sheet can be rebuilt. {@link #register()} sets it to {@link AbilityScores#invalidate}.
     */
    public static Consumer<ServerPlayerEntity> onChange = player -> {
    };

    private RaceAbilityBonuses() {
    }

    /** Registers the race's bonuses with the character sheet. Called from {@link RaceLifecycle#register()}. */
    public static void register() {
        AbilityScores.register(CONTRIBUTOR_ID, (p, c) -> forPlayer(p)
                .forEach((a, n) -> c.add(Ability.valueOf(a), n, source(p))));
        onChange = AbilityScores::invalidate;
    }

    /**
     * Ability bonuses of the player's active race, keyed {@code STR DEX CON INT WIS CHA}
     * ({@link RaceInfo#ABILITIES}). Empty without a race or with {@code dndRaces} off.
     */
    public static Map<String, Integer> forPlayer(PlayerEntity player) {
        RaceInfo info = RaceInfo.get(RaceLifecycle.activeRaceOf(player));
        return info == null ? Map.of() : info.abilityBonuses();
    }

    /** Source text for the sheet's breakdown tooltip, e.g. "Elf". */
    public static String source(PlayerEntity player) {
        RaceInfo info = RaceInfo.get(RaceLifecycle.activeRaceOf(player));
        return info == null ? "Race" : info.name();
    }
}
