package mattonfire.dnd.magic;

import java.util.UUID;

import net.fabricmc.fabric.api.item.v1.ModifyItemAttributeModifiersCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;

/**
 * Generic magic gear (+1 / +2 / +3), design section 3.4:
 * <ul>
 * <li>swords, axes, tridents: +N attack damage in the main hand</li>
 * <li>bows, crossbows: +10% arrow damage per +1</li>
 * <li>armor: +N armor in its slot, and +1 toughness at +3</li>
 * </ul>
 * The bonus is off while the item is dormant (unidentified), and it adds to the attack-roll modifier.
 */
public final class MagicGear {
    public enum Gear {
        NONE, MELEE, RANGED, ARMOR
    }

    public static final int MAX_PLUS = 3;
    public static final double ARROW_DAMAGE_PER_PLUS = 0.10;

    private static final UUID DAMAGE_ID = UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e01");
    // One id per armor slot, so a full +N set stacks
    private static final UUID[] ARMOR_IDS = {
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e10"), // feet
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e11"), // legs
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e12"), // chest
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e13"), // head
    };
    private static final UUID[] TOUGHNESS_IDS = {
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e20"),
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e21"),
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e22"),
            UUID.fromString("5b7d1e0a-6c3f-4c1e-9a51-2f6d0d1a7e23"),
    };

    private MagicGear() {
    }

    public static Gear gearOf(Item item) {
        if (item instanceof ArmorItem)
            return Gear.ARMOR;
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem)
            return Gear.MELEE;
        if (item instanceof BowItem || item instanceof CrossbowItem)
            return Gear.RANGED;
        return Gear.NONE;
    }

    public static boolean canHavePlus(Item item) {
        return gearOf(item) != Gear.NONE;
    }

    static void register() {
        ModifyItemAttributeModifiersCallback.EVENT.register((stack, slot, modifiers) -> {
            int plus = MagicData.activePlus(stack);
            if (plus <= 0)
                return;
            Item item = stack.getItem();
            switch (gearOf(item)) {
                case MELEE -> {
                    if (slot == EquipmentSlot.MAINHAND) {
                        modifiers.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(DAMAGE_ID,
                                "Magic weapon bonus", plus, EntityAttributeModifier.Operation.ADDITION));
                    }
                }
                case ARMOR -> {
                    if (((ArmorItem) item).getSlotType() == slot) {
                        int i = slot.getEntitySlotId();
                        modifiers.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(ARMOR_IDS[i],
                                "Magic armor bonus", plus, EntityAttributeModifier.Operation.ADDITION));
                        if (plus >= 3) {
                            modifiers.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(
                                    TOUGHNESS_IDS[i], "Magic armor bonus", 1.0, EntityAttributeModifier.Operation.ADDITION));
                        }
                    }
                }
                default -> {
                }
            }
        });
    }

    /** The bonus a melee weapon in hand adds to the attack roll's modifier. */
    public static int attackBonus(ItemStack weapon) {
        return gearOf(weapon.getItem()) == Gear.MELEE ? MagicData.activePlus(weapon) : 0;
    }

    /** Scales an arrow just shot from a +N bow or crossbow. */
    public static void applyArrowBonus(ItemStack launcher, PersistentProjectileEntity arrow) {
        if (gearOf(launcher.getItem()) != Gear.RANGED)
            return;
        int plus = MagicData.activePlus(launcher);
        if (plus > 0)
            arrow.setDamage(arrow.getDamage() * (1.0 + ARROW_DAMAGE_PER_PLUS * plus));
    }
}
