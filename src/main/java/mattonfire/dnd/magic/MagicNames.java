package mattonfire.dnd.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

/**
 * Item names and tooltip lines for magic items. {@link #decorateName} runs from the
 * {@code ItemStack#getName} mixin, so the colour shows wherever the name does.
 */
public final class MagicNames {
    private MagicNames() {
    }

    /** The coloured (and maybe renamed) name, or null to keep the vanilla one. */
    public static @Nullable Text decorateName(ItemStack stack, Text original) {
        MagicItems.Info info = MagicItems.info(stack);
        if (info == null)
            return null;
        MutableText name;
        if (!MagicData.isIdentified(stack)) {
            name = Text.translatable("magic.dndclasses.unidentified", typeName(info.typeName()));
        } else if (stack.hasCustomName()) {
            name = Text.empty().append(original);
        } else {
            name = Text.empty();
            int plus = MagicData.displayPlus(stack);
            if (plus > 0)
                name.append("+" + plus + " ");
            name.append(potionRename(stack, original));
        }
        return name.styled(style -> style.withColor(TextColor.fromRgb(info.tier().color)));
    }

    /** Healing II reads "Potion of Greater Healing", as in D&D. */
    private static Text potionRename(ItemStack stack, Text original) {
        if (PotionUtil.getPotion(stack) == Potions.STRONG_HEALING) {
            if (stack.isOf(Items.POTION))
                return Text.translatable("magic.dndclasses.potion.greater_healing");
            if (stack.isOf(Items.SPLASH_POTION))
                return Text.translatable("magic.dndclasses.splash_potion.greater_healing");
            if (stack.isOf(Items.LINGERING_POTION))
                return Text.translatable("magic.dndclasses.lingering_potion.greater_healing");
        }
        return original;
    }

    public static Text typeName(String typeName) {
        return Text.translatable("magic.dndclasses.type." + typeName);
    }

    /**
     * Tooltip line 2, grey italic: "Rare weapon (Wizard only)", "Uncommon armor",
     * "Very Rare wondrous item (requires attunement by a Cleric or Paladin)". Unidentified items get
     * "Unidentified. Its magic sleeps." instead.
     */
    public static @Nullable Text tooltipLine(ItemStack stack) {
        MagicItems.Info info = MagicItems.info(stack);
        if (info == null)
            return null;
        if (!MagicData.isIdentified(stack))
            return Text.translatable("magic.dndclasses.tooltip.unidentified").formatted(Formatting.GRAY,
                    Formatting.ITALIC);
        MutableText line = Text.translatable("magic.dndclasses.tooltip.line", info.tier().displayName(),
                info.kind().displayName());
        Set<DndCharacter> classes = info.classes();
        if (info.attunement() && !classes.isEmpty()) {
            line.append(" ").append(Text.translatable("magic.dndclasses.tooltip.attune_by", withArticle(classList(classes))));
        } else if (info.attunement()) {
            line.append(" ").append(Text.translatable("magic.dndclasses.tooltip.attune"));
        } else if (!classes.isEmpty()) {
            line.append(" ").append(Text.translatable("magic.dndclasses.tooltip.only", classList(classes)));
        }
        return line.formatted(Formatting.GRAY, Formatting.ITALIC);
    }

    /**
     * The lines after tooltip line 2: "Attuned" (aqua) for the viewer's own bond, "Attuned to &lt;player&gt;"
     * (grey) for someone else's, and a Forge blessing.
     */
    public static List<Text> tooltipExtras(ItemStack stack, @Nullable java.util.UUID viewer) {
        List<Text> lines = new ArrayList<>();
        java.util.UUID owner = MagicData.attunedTo(stack);
        if (owner != null) {
            if (owner.equals(viewer)) {
                lines.add(Text.translatable("magic.dndclasses.tooltip.attuned").formatted(Formatting.AQUA));
            } else {
                String name = MagicData.attunedName(stack);
                lines.add(Text.translatable("magic.dndclasses.tooltip.attuned_to", name.isEmpty() ? "?" : name)
                        .formatted(Formatting.GRAY));
            }
        }
        if (MagicData.isForgeBlessed(stack))
            lines.add(Text.translatable("magic.dndclasses.tooltip.forge_blessing").formatted(Formatting.GOLD));
        return lines;
    }

    /** "Wizard", "Cleric or Paladin", "Wizard, Necromancer or Warlock". */
    private static String classList(Set<DndCharacter> classes) {
        List<String> names = new ArrayList<>();
        for (DndCharacter c : DndCharacter.values()) {
            if (classes.contains(c))
                names.add(className(c));
        }
        if (names.size() == 1)
            return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
    }

    private static String withArticle(String noun) {
        return ("AEIOU".indexOf(noun.charAt(0)) >= 0 ? "an " : "a ") + noun;
    }

    public static String className(DndCharacter c) {
        if (c == DndCharacter.BLOODHUNTER)
            return "Blood Hunter";
        String n = c.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
