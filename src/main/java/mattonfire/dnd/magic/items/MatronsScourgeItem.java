package mattonfire.dnd.magic.items;

import java.util.UUID;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ToolMaterials;

/**
 * The Matron's Scourge (Very Rare weapon, attunement; design 9, 9.4): the Matron Mother's whip of barbed chains. A
 * sword's stats at 7 damage, and {@value #REACH} block more reach in the main hand. Its hits poison and frighten
 * ({@link DrowItems}).
 */
public class MatronsScourgeItem extends NamedWeaponItem {
    /** Extra attack reach, in blocks. */
    public static final double REACH = 1.0;
    private static final UUID REACH_ID = UUID.fromString("3b7e2c51-9d4a-4f6e-8a02-6c5d1e9f2b47");

    private final Multimap<EntityAttribute, EntityAttributeModifier> mainHand;

    public MatronsScourgeItem(Settings settings) {
        super(ToolMaterials.IRON, 4, -2.4F, "item.dndclasses.matrons_scourge.tooltip", true, settings);
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(super.getAttributeModifiers(EquipmentSlot.MAINHAND));
        builder.put(ReachEntityAttributes.ATTACK_RANGE, new EntityAttributeModifier(REACH_ID, "Scourge reach", REACH,
                EntityAttributeModifier.Operation.ADDITION));
        this.mainHand = builder.build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.mainHand : super.getAttributeModifiers(slot);
    }
}
