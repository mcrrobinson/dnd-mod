package mattonfire.dnd.classes.Race;

import java.util.Map;
import java.util.function.Consumer;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * The hook between races and ability scores (area 4, {@code classes/Abilities/}). Races only hold their
 * ability bonuses as data in {@code race_info.json}; ability scores own the contributor API and read them
 * from here. To hook it up, area 4 adds (in its own register code):
 *
 * <pre>
 * AbilityScores.register(RaceAbilityBonuses.CONTRIBUTOR_ID, (p, c) -&gt; RaceAbilityBonuses.forPlayer(p)
 *         .forEach((a, n) -&gt; c.add(Ability.valueOf(a), n, RaceAbilityBonuses.source(p))));
 * RaceAbilityBonuses.onChange = AbilityScores::invalidate;
 * </pre>
 *
 * Until then this class changes nothing in game.
 */
public final class RaceAbilityBonuses {
    /** The contributor id area 4's design reserves for races. */
    public static final Identifier CONTRIBUTOR_ID = new Identifier(DnDClasses.MOD_ID, "race");

    /**
     * Called on the server whenever a player's race (or the dndRaces gamerule) changes, so a cached
     * character sheet can be rebuilt. A no-op until ability scores replace it.
     */
    public static Consumer<ServerPlayerEntity> onChange = player -> {
    };

    private RaceAbilityBonuses() {
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
