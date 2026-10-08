package mattonfire.dnd.classes.Items.lib;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import mattonfire.dnd.classes.Config.FAConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;


public abstract class FAArmorItem extends ArmorItem implements GeoItem, SetBonusArmor {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private final Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;

    protected FAArmorItem(Type type, FAArmorAttributes armorAttributes) {
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
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (FAConfig.getValues().showDescriptions()) {
            super.appendTooltip(stack, world, tooltip, context);

            ArmorTooltips.appendDescription(this.getTranslationKey(), tooltip);
        }
    }


    @Environment(EnvType.CLIENT)
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private GeoArmorRenderer<? extends FAArmorItem> renderer;

            @SuppressWarnings("unchecked")
            @Override
            public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
                if (renderer == null) {
                    renderer = createArmorRenderer();
                }

                renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    @Environment(EnvType.CLIENT)
    protected abstract GeoArmorRenderer<? extends FAArmorItem> createArmorRenderer();

    @Override
    public abstract List<StatusEffectInstance> getFullSetEffects();
}