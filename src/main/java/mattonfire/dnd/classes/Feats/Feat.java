package mattonfire.dnd.classes.Feats;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * The feats every class can take (see {@code docs/systems/feats.md}). The single source of truth for names and
 * text: the picker, the Character tab, the guidebook page and {@code /dndclass feat} all read it. {@link #icon} is
 * the cell in {@code textures/gui/feats.png} ({@code tools/feat_icons.py}).
 */
public enum Feat {
    GREAT_WEAPON_MASTER("great_weapon_master", "Great Weapon Master",
            "Heavy weapons: +5 dmg,",
            "fumble 1-4 (N). Cleave.",
            "Power Attack (toggle with N): with a heavy weapon (axe, trident, greataxe, waraxe or any melee weapon"
                    + " with attack speed 1.0 or less) your attack rolls fumble on 1-4 and full-strength hits deal +5"
                    + " damage. Cleave: a critical hit or a kill with a heavy weapon readies your next swing at once."),
    SHARPSHOOTER("sharpshooter", "Sharpshooter",
            "Steady Aim (N): x1.4. Long",
            "Shot x1.25. Past shields.",
            "Steady Aim (toggle with N): fully drawn bow shots and crossbow shots deal x1.4 damage but spread 2.5"
                    + " times wider. Long Shot: your arrows deal x1.25 to anything hit 24 or more blocks from where"
                    + " they were loosed. Past cover: shields don't stop your arrows."),
    LUCKY("lucky", "Lucky",
            "3 luck points reroll",
            "your failed d20s.",
            "You have 3 luck points. A failed check, a failed save or a fumbled attack of yours spends one to roll"
                    + " the d20 again; the second roll stands. They come back on a long rest (at dawn when rests are"
                    + " off)."),
    TOUGH("tough", "Tough",
            "+1 max health per",
            "class level.",
            "Your maximum health goes up by 1 for each class level you have (+6 at level 6, +10 at level 10)."),
    MOBILE("mobile", "Mobile",
            "+10% speed. Sprint past",
            "webs. Hit and run.",
            "+10% movement speed. While sprinting, cobwebs, berry bushes, powder snow, soul sand and honey don't slow"
                    + " you. A mob you hit in melee has disadvantage on its melee attacks against you for 2 s."),
    WAR_CASTER("war_caster", "War Caster",
            "Advantage on CON saves.",
            "Bolt mobs that close in.",
            "Advantage on CON saves. Somatic hands: a Warlock's fireballs also work with a weapon or shield in hand."
                    + " Opportunity bolt: a hostile mob that targets you and comes within 3 blocks is hit by a free"
                    + " Magic Missile dart (2 force damage), once every 10 s."),
    ALERT("alert", "Alert",
            "Speed and Haste when a",
            "fight starts. Keen watch.",
            "First to act: when a hostile mob first targets you or a boss fight you're in starts, you get Speed I and"
                    + " Haste I for 3 s (once per 30 s). Your camp watch rolls have advantage. Attacks against you"
                    + " never get advantage from your being Blinded."),
    SENTINEL("sentinel", "Sentinel",
            "Hits slow mobs. Punish",
            "foes that hurt friends.",
            "Hold the line: mobs you hit in melee get Slowness III for 1 s. Guardian: when a hostile mob within 4"
                    + " blocks of you hurts someone else, your next melee hit on it within 3 s has advantage and"
                    + " Restrains it for 1 s."),
    RESILIENT("resilient", "Resilient",
            "+1 to an ability and",
            "proficiency in its saves.",
            "+1 to one ability (up to 20) and proficiency in its saving throws. You can take it once for each"
                    + " ability.");

    private final String id;
    private final String displayName;
    private final String summary1;
    private final String summary2;
    private final String description;

    Feat(String id, String displayName, String summary1, String summary2, String description) {
        this.id = id;
        this.displayName = displayName;
        this.summary1 = summary1;
        this.summary2 = summary2;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Two short lines for the picker card. */
    public String summary1() {
        return summary1;
    }

    public String summary2() {
        return summary2;
    }

    /** The full rules text (tooltips, guidebook, docs). */
    public String description() {
        return description;
    }

    /** Cell index in {@code textures/gui/feats.png}. */
    public int icon() {
        return ordinal();
    }

    /** Resilient: taken once per ability, so it needs one. */
    public boolean perAbility() {
        return this == RESILIENT;
    }

    /** Whether N (Power Attack / Steady Aim) does anything for it. */
    public boolean hasToggle() {
        return this == GREAT_WEAPON_MASTER || this == SHARPSHOOTER;
    }

    @Nullable
    public static Feat byId(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (Feat feat : values()) {
            if (feat.id.equals(lower) || feat.name().equalsIgnoreCase(lower)) {
                return feat;
            }
        }
        return null;
    }
}
