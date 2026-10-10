package mattonfire.dnd.classes.Abilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.network.PacketByteBuf;

/**
 * A player's final ability scores, proficiencies and roll bonuses, built by {@link AbilityScores} from
 * every registered {@link AbilityContributor}. The server computes it; the client gets a copy
 * ({@link #client}) for display, without the advantage filters (only their {@link #lines}).
 *
 * @param lines where each number comes from, for tooltips and {@code /dndclass sheet}
 */
public record CharacterSheet(DndCharacter dndClass, int level, int[] scores, int proficiencyBonus,
        Proficiency[] saveProficiencies, int[] saveFlat, Proficiency[] skillProficiencies, int[] skillFlat,
        int[] checkFlat, Proficiency checkProficiencyFloor, int critRange, boolean rerollNaturalOnes,
        List<AdvantageSource> advantages, List<Line> lines) {

    /** One entry of the breakdown: who added what to which number ("STR", "save:DEX", "skill:STEALTH"...). */
    public record Line(String source, String target, String detail) {
    }

    /** An advantage (or disadvantage) and the rolls it applies to. Server-side only. */
    public record AdvantageSource(RollFilter filter, boolean advantage, String description, String source) {
    }

    /** The sheet the client last received for its own player. */
    public static CharacterSheet client = classless();

    /** Everything 10, no proficiencies, proficiency bonus +2. */
    public static CharacterSheet classless() {
        return new Contribution().build(DndCharacter.NONE, 0, AbilityScores.proficiencyBonus(0));
    }

    public int score(Ability ability) {
        return scores[ability.ordinal()];
    }

    public int modifier(Ability ability) {
        return Ability.modifier(score(ability));
    }

    public Proficiency saveProficiency(Ability ability) {
        return saveProficiencies[ability.ordinal()];
    }

    /** Saving throw bonus: modifier + proficiency + flat save bonuses. */
    public int save(Ability ability) {
        return modifier(ability) + saveProficiency(ability).bonus(proficiencyBonus) + saveFlat[ability.ordinal()];
    }

    /** Proficiency in a skill, including the check floor (Jack of All Trades). */
    public Proficiency skillProficiency(Skill skill) {
        return skillProficiencies[skill.ordinal()].max(checkProficiencyFloor);
    }

    /** Check bonus with a skill: ability modifier + proficiency + flat bonuses. */
    public int check(Skill skill) {
        Ability ability = skill.ability();
        return modifier(ability) + skillProficiency(skill).bonus(proficiencyBonus) + skillFlat[skill.ordinal()]
                + checkFlat[ability.ordinal()];
    }

    /** Plain ability check (no skill): modifier + the check floor + flat check bonuses. */
    public int abilityCheck(Ability ability) {
        return modifier(ability) + checkProficiencyFloor.bonus(proficiencyBonus) + checkFlat[ability.ordinal()];
    }

    /** Advantage, disadvantage or neither for a roll, from the sheet's sources (they cancel out). */
    public Advantage advantage(RollQuery query) {
        int adv = 0;
        int dis = 0;
        for (AdvantageSource source : advantages) {
            if (source.filter().test(query)) {
                if (source.advantage()) {
                    adv++;
                } else {
                    dis++;
                }
            }
        }
        return Advantage.resolve(adv, dis);
    }

    /** Passive score: 10 + check bonus, +5 with advantage, -5 with disadvantage. */
    public int passive(Skill skill) {
        int base = 10 + check(skill);
        return switch (advantage(RollQuery.check(skill, "passive"))) {
            case ADVANTAGE -> base + 5;
            case DISADVANTAGE -> base - 5;
            case NORMAL -> base;
        };
    }

    /** Melee attack bonus: the better of STR and DEX modifier, plus proficiency. */
    public int attackBonus() {
        return Math.max(modifier(Ability.STR), modifier(Ability.DEX)) + proficiencyBonus;
    }

    /** Breakdown lines for one number (target as in {@link Line}). */
    public List<Line> linesFor(String target) {
        List<Line> out = new ArrayList<>();
        for (Line line : lines) {
            if (line.target().equals(target)) {
                out.add(line);
            }
        }
        return out;
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(dndClass);
        buf.writeVarInt(level);
        buf.writeIntArray(scores);
        buf.writeVarInt(proficiencyBonus);
        writeProfs(buf, saveProficiencies);
        buf.writeIntArray(saveFlat);
        writeProfs(buf, skillProficiencies);
        buf.writeIntArray(skillFlat);
        buf.writeIntArray(checkFlat);
        buf.writeEnumConstant(checkProficiencyFloor);
        buf.writeVarInt(critRange);
        buf.writeBoolean(rerollNaturalOnes);
        buf.writeCollection(lines, (b, line) -> {
            b.writeString(line.source());
            b.writeString(line.target());
            b.writeString(line.detail());
        });
    }

    public static CharacterSheet read(PacketByteBuf buf) {
        DndCharacter dndClass = buf.readEnumConstant(DndCharacter.class);
        int level = buf.readVarInt();
        int[] scores = buf.readIntArray();
        int prof = buf.readVarInt();
        Proficiency[] saveProf = readProfs(buf);
        int[] saveFlat = buf.readIntArray();
        Proficiency[] skillProf = readProfs(buf);
        int[] skillFlat = buf.readIntArray();
        int[] checkFlat = buf.readIntArray();
        Proficiency floor = buf.readEnumConstant(Proficiency.class);
        int crit = buf.readVarInt();
        boolean reroll = buf.readBoolean();
        List<Line> lines = buf.readList(b -> new Line(b.readString(), b.readString(), b.readString()));
        return new CharacterSheet(dndClass, level, scores, prof, saveProf, saveFlat, skillProf, skillFlat, checkFlat,
                floor, crit, reroll, List.of(), lines);
    }

    private static void writeProfs(PacketByteBuf buf, Proficiency[] profs) {
        buf.writeVarInt(profs.length);
        for (Proficiency p : profs) {
            buf.writeEnumConstant(p);
        }
    }

    private static Proficiency[] readProfs(PacketByteBuf buf) {
        Proficiency[] profs = new Proficiency[buf.readVarInt()];
        for (int i = 0; i < profs.length; i++) {
            profs[i] = buf.readEnumConstant(Proficiency.class);
        }
        return profs;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CharacterSheet s && dndClass == s.dndClass && level == s.level
                && Arrays.equals(scores, s.scores) && proficiencyBonus == s.proficiencyBonus
                && Arrays.equals(saveProficiencies, s.saveProficiencies) && Arrays.equals(saveFlat, s.saveFlat)
                && Arrays.equals(skillProficiencies, s.skillProficiencies) && Arrays.equals(skillFlat, s.skillFlat)
                && Arrays.equals(checkFlat, s.checkFlat) && checkProficiencyFloor == s.checkProficiencyFloor
                && critRange == s.critRange && rerollNaturalOnes == s.rerollNaturalOnes && lines.equals(s.lines);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(scores) * 31 + lines.hashCode();
    }
}
