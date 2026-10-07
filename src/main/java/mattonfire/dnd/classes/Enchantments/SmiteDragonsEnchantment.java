package mattonfire.dnd.classes.Enchantments;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Smite for dragons: extra melee damage against everything in #dndclasses:dragons (the same tag the
 * Dragon Slayer advancement uses). Vanilla only passes the target's EntityGroup to enchantments, so
 * the bonus is added by SmiteDragonsMixin in PlayerEntity.attack. Like Smite it goes on swords and
 * axes and can't be combined with Sharpness, Smite or Bane of Arthropods.
 */
public class SmiteDragonsEnchantment extends Enchantment {
    public static final TagKey<EntityType<?>> DRAGONS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "dragons"));
    // Smite's 2.5 per level
    public static final float DAMAGE_PER_LEVEL = 2.5F;

    public SmiteDragonsEnchantment(Rarity rarity, EnchantmentTarget target, EquipmentSlot... slots) {
        super(rarity, target, slots);
    }

    /** The extra damage the stack deals to the target, 0 if it isn't a dragon (or a dragon's part). */
    public static float getBonus(ItemStack stack, Entity target, Enchantment enchantment) {
        int level = EnchantmentHelper.getLevel(enchantment, stack);
        if (level <= 0 || target == null || !target.getType().isIn(DRAGONS)) {
            return 0.0F;
        }
        return level * DAMAGE_PER_LEVEL;
    }

    @Override
    public int getMinPower(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxPower(int level) {
        return this.getMinPower(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof AxeItem || super.isAcceptableItem(stack);
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        return !(other instanceof DamageEnchantment) && super.canAccept(other);
    }
}
