package mattonfire.dnd.classes.Items.racial;

import java.util.List;
import java.util.Set;

import mattonfire.dnd.classes.Items.lib.DndArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;
import mattonfire.dnd.classes.Items.lib.ModArmorMaterials;
import mattonfire.dnd.classes.Race.DndRace;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;

/**
 * Tinker's Goggles (Gnome kin stock at Cogsdeep): a brass helmet of lenses. Whoever wears them sees what's inside
 * a chest, barrel, hopper, furnace (or any other container) they look at within {@link #RANGE} blocks: the
 * contents show beside the crosshair ({@code Client/Hud/GogglesHud}, filled in by {@link TinkersGoggles}). A
 * single piece, so it has no full-set bonus.
 */
public class TinkersGogglesItem extends DndArmorItem {
    /** How far the lenses see: a little past arm's reach. */
    public static final double RANGE = 6.0D;
    /** Armor points. */
    public static final int PROTECTION = 1;

    public TinkersGogglesItem(FAArmorAttributes armorAttributes) {
        super(ModArmorMaterials.TINKERS_GOGGLES, ArmorItem.Type.HELMET, armorAttributes);
    }

    /** Whether {@code player} has the goggles on. */
    public static boolean wears(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof TinkersGogglesItem;
    }

    /** Lenses, not plate: as light as a leather cap (so a Wizard can wear them), whatever the material says. */
    @Override
    public int getProtection() {
        return PROTECTION;
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of();
    }

    @Override
    public Set<DndRace> getMatchingRaces() {
        return Set.of(DndRace.GNOME);
    }
}
