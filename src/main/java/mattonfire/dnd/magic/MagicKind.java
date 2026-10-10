package mattonfire.dnd.magic;

import net.minecraft.text.Text;

/** The D&D item category shown in the tooltip: "Rare <i>weapon</i>", "Uncommon <i>wondrous item</i>". */
public enum MagicKind {
    WEAPON("weapon"),
    ARMOR("armor"),
    POTION("potion"),
    RING("ring"),
    ROD("rod"),
    SCROLL("scroll"),
    STAFF("staff"),
    WAND("wand"),
    WONDROUS("wondrous_item"),
    CONSUMABLE("consumable");

    public final String id;

    MagicKind(String id) {
        this.id = id;
    }

    public Text displayName() {
        return Text.translatable("magic.dndclasses.kind." + id);
    }
}
