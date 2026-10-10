package mattonfire.dnd.magic;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.item.Item;

/**
 * A registered magic item: its rarity, category, whether it needs attunement, which classes may use it,
 * a built-in curse (filled in by the curses ticket) and the type shown while it's unidentified.
 *
 * @param typeName the unidentified type id, shown as "Unidentified &lt;type&gt;" through the lang key
 *                 {@code magic.dndclasses.type.<typeName>} (sword, staff, helmet, ring, wondrous_item, ...)
 */
public record MagicItemDef(Item item, MagicTier tier, MagicKind kind, boolean attunement,
        Set<DndCharacter> classes, @Nullable String fixedCurse, String typeName) {

    public MagicItemDef {
        classes = Set.copyOf(classes);
    }

    public static MagicItemDef of(Item item, MagicTier tier, MagicKind kind, String typeName) {
        return new MagicItemDef(item, tier, kind, false, Set.of(), null, typeName);
    }

    /** Usable only by these classes (no attunement). */
    public MagicItemDef onlyFor(DndCharacter... characters) {
        return new MagicItemDef(item, tier, kind, attunement, Set.of(characters), fixedCurse, typeName);
    }

    /** A built-in curse ({@link Curse} id), applied whenever the item is rolled as loot. */
    public MagicItemDef withCurse(Curse curse) {
        return new MagicItemDef(item, tier, kind, attunement, classes, curse.id, typeName);
    }

    public MagicItemDef withAttunement() {
        return new MagicItemDef(item, tier, kind, true, classes, fixedCurse, typeName);
    }
}
