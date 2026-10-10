package mattonfire.dnd.classes;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * A class's job in a party. Each class has a primary and a secondary role in
 * {@code class_info.json} ({@link ClassInfo#role()}, {@link ClassInfo#secondaryRole()}).
 * The class picker, the guidebook and the party HUD show them; role synergies
 * build on them later.
 */
public enum PartyRole {
    TANK(0x5B8DEF),
    HEALER(0x55D17A),
    DAMAGE(0xE85D4A),
    SUPPORT(0xE8C14A),
    UTILITY(0xB07CE8);

    /** Role icons in {@code textures/gui/roles.png}: 7x7 each, side by side in enum order, 8 pixels apart. */
    public static final String ICON_TEXTURE = "textures/gui/roles.png";
    public static final int ICON_SIZE = 7;
    public static final int ICON_STRIDE = 8;
    public static final int ICON_TEXTURE_WIDTH = 64;
    public static final int ICON_TEXTURE_HEIGHT = 8;

    private final int color;

    PartyRole(int color) {
        this.color = color;
    }

    /** RGB colour, no alpha. */
    public int color() {
        return color;
    }

    public String translationKey() {
        return "role." + DnDClasses.MOD_ID + "." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The role's name in its colour. */
    public MutableText text() {
        return Text.translatable(translationKey()).styled(s -> s.withColor(TextColor.fromRgb(color)));
    }

    /** Left edge of this role's icon in {@link #ICON_TEXTURE}. */
    public int iconU() {
        return ordinal() * ICON_STRIDE;
    }
}
