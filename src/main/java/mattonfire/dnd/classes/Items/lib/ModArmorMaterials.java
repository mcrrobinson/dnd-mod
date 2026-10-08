package mattonfire.dnd.classes.Items.lib;

import java.util.function.Supplier;

import net.minecraft.item.ArmorItem.Type;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Materials for the armor sets that render with vanilla-style layered textures
 * ({@code Client/Render/LayeredArmorRenderer}). Durability, protection, toughness,
 * knockback resistance and enchantability match netherite, like the GeckoLib sets
 * ({@code FAArmorItem} uses {@code ArmorMaterials.NETHERITE}); only the repair item
 * and equip sound differ per set.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    PRISMARINE("prismarine", "prismarine", SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            () -> Ingredient.ofItems(Items.PRISMARINE_SHARD)),
    HOLY("holy_armor", "holy_armor", SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
            () -> Ingredient.ofItems(Items.GOLD_INGOT)),
    ROBE("robe", "robe", SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            () -> Ingredient.fromTag(ItemTags.WOOL)),
    STEAMPUNK("steam_punk", "steampunk", SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            () -> Ingredient.ofItems(Items.COPPER_INGOT)),
    WARRIOR("warrior", "warrior", SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            () -> Ingredient.ofItems(Items.IRON_INGOT)),
    WITHER("wither", "wither", SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.ofItems(Items.NETHERITE_SCRAP)),
    WOODEN("wooden", "wooden", SoundEvents.ITEM_ARMOR_EQUIP_GENERIC,
            () -> Ingredient.fromTag(ItemTags.LOGS));

    // Same numbers as vanilla ArmorMaterials.NETHERITE.
    private static final int[] BASE_DURABILITY = { 13, 15, 16, 11 }; // boots, leggings, chestplate, helmet
    private static final int DURABILITY_MULTIPLIER = 37;
    private static final int[] PROTECTION = { 3, 6, 8, 3 }; // boots, leggings, chestplate, helmet
    private static final int ENCHANTABILITY = 15;
    private static final float TOUGHNESS = 3.0f;
    private static final float KNOCKBACK_RESISTANCE = 0.1f;

    private final String name;
    private final String textureFolder;
    private final SoundEvent equipSound;
    private final Supplier<Ingredient> repairIngredient;
    private Ingredient repairIngredientCache;

    ModArmorMaterials(String name, String textureFolder, SoundEvent equipSound, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.textureFolder = textureFolder;
        this.equipSound = equipSound;
        this.repairIngredient = repairIngredient;
    }

    /** Folder under {@code assets/dndclasses/textures/models/armor/} that holds this set's layers. */
    public String getTextureFolder() {
        return textureFolder;
    }

    private static int index(Type type) {
        return type.getEquipmentSlot().getEntitySlotId();
    }

    @Override
    public int getDurability(Type type) {
        return BASE_DURABILITY[index(type)] * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getProtection(Type type) {
        return PROTECTION[index(type)];
    }

    @Override
    public int getEnchantability() {
        return ENCHANTABILITY;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        if (repairIngredientCache == null) {
            repairIngredientCache = repairIngredient.get();
        }
        return repairIngredientCache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return TOUGHNESS;
    }

    @Override
    public float getKnockbackResistance() {
        return KNOCKBACK_RESISTANCE;
    }
}
