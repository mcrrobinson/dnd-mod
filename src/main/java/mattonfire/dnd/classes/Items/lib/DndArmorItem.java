package mattonfire.dnd.classes.Items.lib;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import mattonfire.dnd.classes.Config.FAConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public abstract class DndArmorItem extends ArmorItem {

    private final Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;

    protected DndArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(ArmorMaterials.NETHERITE, type, new Item.Settings().maxCount(1));
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();

        if (armorAttributes.armor() > 0) {
            builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(UUID.randomUUID(), "Armor", armorAttributes.armor(), EntityAttributeModifier.Operation.ADDITION));
        }

        if (armorAttributes.armorToughness() > 0) {
            builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(UUID.randomUUID(), "Armor toughness", armorAttributes.armorToughness(), EntityAttributeModifier.Operation.ADDITION));
        }

        if (armorAttributes.knockbackResistance() > 0) {
            builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(UUID.randomUUID(), "Armor knockback resistance", armorAttributes.knockbackResistance(), EntityAttributeModifier.Operation.ADDITION));
        }

        if (armorAttributes.movementSpeed() > 0) {
            builder.put(EntityAttributes.GENERIC_MOVEMENT_SPEED, new EntityAttributeModifier(UUID.randomUUID(), "Armor movement speed", armorAttributes.movementSpeed(), EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (armorAttributes.maxHealth() > 0) {
            builder.put(EntityAttributes.GENERIC_MAX_HEALTH, new EntityAttributeModifier(UUID.randomUUID(), "Armor health gain", armorAttributes.maxHealth(), EntityAttributeModifier.Operation.ADDITION));
        }

        if (armorAttributes.attackDamage() > 0) {
            builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(UUID.randomUUID(), "Armor attack damage", armorAttributes.attackDamage(), EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (armorAttributes.attackSpeed() > 0) {
            builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(UUID.randomUUID(), "Armor attack speed", armorAttributes.attackSpeed(), EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (armorAttributes.luck() > 0) {
            builder.put(EntityAttributes.GENERIC_LUCK, new EntityAttributeModifier(UUID.randomUUID(), "Armor luck", armorAttributes.luck(), EntityAttributeModifier.Operation.ADDITION));
        }

        attributeModifiers = builder.build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
        Multimap<EntityAttribute, EntityAttributeModifier> modifiers = super.getAttributeModifiers(stack, slot);

        if (slot == this.type.getEquipmentSlot()) {
            ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
            if (!FAConfig.getValues().applyModifiers()) {
                builder.putAll(modifiers);
            }
            builder.putAll(this.attributeModifiers);
            return builder.build();
        }

        return modifiers;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (FAConfig.getValues().showDescriptions()) {
            super.appendTooltip(stack, world, tooltip, context);

            String translationKey = this.getTranslationKey() + ".tooltip";
            String translatedText = Text.translatable(translationKey).getString();

            int maxWidth = FAConfig.getValues().descrtiptionsLength();
            if (maxWidth < 20 || maxWidth > 1000) maxWidth = 250;

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            String[] lines = translatedText.split("\n");

            for (String line : lines) {
                StringBuilder currentLine = new StringBuilder("§7");
                String[] words = line.split(" ");

                for (String word : words) {
                    if (textRenderer.getWidth(currentLine + word) > maxWidth) {
                        tooltip.add(Text.literal(currentLine.toString()));
                        currentLine = new StringBuilder("§7" + word);
                    } else {
                        if (currentLine.length() > 2) currentLine.append(" ");
                        currentLine.append(word);
                    }
                }

                if (currentLine.length() > 0) {
                    tooltip.add(Text.literal(currentLine.toString()));
                }
            }
        }
    }

    // Implement this in subclasses
    public abstract List<StatusEffectInstance> getFullSetEffects();
}
