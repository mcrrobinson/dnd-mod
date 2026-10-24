package mattonfire.dnd.entity.hireling;

import java.util.Locale;

import mattonfire.dnd.classes.PartyRole;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * A hireling's job: its party role, title, wage, health, melee damage and how far behind its owner it
 * walks. The numbers are the ones in {@code docs/systems/hirelings.md}.
 */
public enum HirelingRole {
    TANK(PartyRole.TANK, "shieldbearer", 18, 40.0F, 6.0F, 2.0D),
    HEALER(PartyRole.HEALER, "acolyte", 27, 28.0F, 3.0F, 4.0D),
    DAMAGE(PartyRole.DAMAGE, "sellsword", 18, 30.0F, 6.0F, 3.0D),
    SUPPORT(PartyRole.SUPPORT, "minstrel", 18, 26.0F, 3.0F, 3.0D),
    UTILITY(PartyRole.UTILITY, "guide", 9, 30.0F, 5.0F, 3.0D);

    /** The hire fee is this many days' wages, paid into the purse up front. */
    public static final int FEE_DAYS = 3;

    public final PartyRole partyRole;
    public final String titleKey;
    /** Gold nuggets a day, before reputation. */
    public final int wage;
    public final float health;
    public final float melee;
    /** How close it keeps to its owner when following (the Healer hangs back). */
    public final double keepDistance;

    HirelingRole(PartyRole partyRole, String title, int wage, float health, float melee, double keepDistance) {
        this.partyRole = partyRole;
        this.titleKey = "hireling.dndclasses.title." + title;
        this.wage = wage;
        this.health = health;
        this.melee = melee;
        this.keepDistance = keepDistance;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public MutableText title() {
        return Text.translatable(this.titleKey);
    }

    /** What it does, for the hire card. */
    public MutableText blurb() {
        return Text.translatable("hireling.dndclasses.blurb." + this.id());
    }

    @Nullable
    public static HirelingRole byId(String id) {
        for (HirelingRole role : values()) {
            if (role.id().equalsIgnoreCase(id)) {
                return role;
            }
        }
        return null;
    }

    public static HirelingRole byOrdinal(int ordinal) {
        HirelingRole[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
