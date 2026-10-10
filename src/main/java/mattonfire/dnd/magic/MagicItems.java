package mattonfire.dnd.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Registry.ModPotions;
import mattonfire.dnd.tavern.Tavern;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * The magic item table (Java, v1). Registered items get a fixed tier; potions are tiered by strength;
 * anything else is magic only if its stack carries a tier in {@link MagicData} (the +N vanilla gear).
 */
public final class MagicItems {
    private static final Map<Item, MagicItemDef> DEFS = new HashMap<>();

    /** Everything known about a stack's magic, resolved from its def, its NBT and its item class. */
    public record Info(MagicTier tier, MagicKind kind, boolean attunement, Set<DndCharacter> classes,
            String typeName, @Nullable MagicItemDef def) {
    }

    private MagicItems() {
    }

    public static void register(MagicItemDef def) {
        DEFS.put(def.item(), def);
    }

    public static @Nullable MagicItemDef def(Item item) {
        return DEFS.get(item);
    }

    /** The resolved magic info, or null for a mundane stack. */
    public static @Nullable Info info(ItemStack stack) {
        if (stack.isEmpty())
            return null;
        Item item = stack.getItem();
        MagicItemDef def = DEFS.get(item);
        MagicTier stored = MagicData.storedTier(stack);
        if (def != null) {
            return new Info(stored != null ? stored : def.tier(), def.kind(), def.attunement(), def.classes(),
                    def.typeName(), def);
        }
        MagicTier tier = stored != null ? stored : potionTier(stack);
        if (tier == null)
            return null;
        return new Info(tier, kindOf(item), false, Set.of(), typeOf(item), null);
    }

    /** Common for base potions, Uncommon for II / extended ones; the mod's potions per the design table. */
    private static @Nullable MagicTier potionTier(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem))
            return null;
        Potion potion = PotionUtil.getPotion(stack);
        if (potion == ModPotions.INVULNERABILITY_POTION.value())
            return MagicTier.RARE;
        if (potion == ModPotions.FREEZE_POTION.value() || potion == ModPotions.ARROW_STORM_POTION.value())
            return MagicTier.UNCOMMON;
        if (potion.getEffects().isEmpty())
            return null; // water, awkward, mundane, thick
        String path = Registries.POTION.getId(potion).getPath();
        return path.startsWith("strong_") || path.startsWith("long_") ? MagicTier.UNCOMMON : MagicTier.COMMON;
    }

    public static MagicKind kindOf(Item item) {
        if (item instanceof ArmorItem)
            return MagicKind.ARMOR;
        if (item instanceof PotionItem)
            return MagicKind.POTION;
        if (MagicGear.gearOf(item) != MagicGear.Gear.NONE)
            return MagicKind.WEAPON;
        return MagicKind.WONDROUS;
    }

    /** The unidentified type id for an item without a def. */
    public static String typeOf(Item item) {
        if (item instanceof ArmorItem armor) {
            return switch (armor.getType()) {
                case HELMET -> "helmet";
                case CHESTPLATE -> "chestplate";
                case LEGGINGS -> "leggings";
                case BOOTS -> "boots";
            };
        }
        if (item instanceof SwordItem)
            return "sword";
        if (item instanceof AxeItem)
            return "axe";
        if (item instanceof TridentItem)
            return "trident";
        if (item instanceof BowItem)
            return "bow";
        if (item instanceof CrossbowItem)
            return "crossbow";
        if (item instanceof PotionItem)
            return "potion";
        return "wondrous_item";
    }

    /** Tiers for the mod's existing items (design section 3.2). Called once the items are registered. */
    public static void registerDefaults() {
        for (Item staff : new Item[] { ModItems.STAFF_OF_ICE, ModItems.STAFF_OF_FIRE, ModItems.STAFF_OF_LIGHTNING }) {
            // No attunement, so a Wizard can carry all three; shown as Wizard only (ExtendedSwordItem.canWield)
            register(MagicItemDef.of(staff, MagicTier.RARE, MagicKind.WEAPON, "staff").onlyFor(DndCharacter.WIZARD));
        }
        register(MagicItemDef.of(ModItems.MONK_STAFF, MagicTier.COMMON, MagicKind.WEAPON, "staff"));
        for (Item instrument : new Item[] { ModItems.LUTE, ModItems.DRUM, ModItems.FLUTE }) {
            register(MagicItemDef.of(instrument, MagicTier.COMMON, MagicKind.WONDROUS, "instrument"));
        }
        register(MagicItemDef.of(Tavern.ALE, MagicTier.COMMON, MagicKind.CONSUMABLE, "drink"));
        // The 14 class armor sets: every ArmorItem the mod registers
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (id.getNamespace().equals(DnDClasses.MOD_ID) && item instanceof ArmorItem && !DEFS.containsKey(item)) {
                register(MagicItemDef.of(item, MagicTier.UNCOMMON, MagicKind.ARMOR, typeOf(item)));
            }
        }
    }
}
