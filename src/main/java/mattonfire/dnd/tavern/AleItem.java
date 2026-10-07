package mattonfire.dnd.tavern;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;

/** A frothing mug of hobbit ale: filling, warming, and a little dizzying if you're unlucky. */
public class AleItem extends Item {
    public static final FoodComponent FOOD = new FoodComponent.Builder()
            .hunger(3)
            .saturationModifier(0.4F)
            .alwaysEdible()
            .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 6, 0), 1.0F)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 8, 0), 0.25F)
            .build();

    public AleItem(Settings settings) {
        super(settings.food(FOOD).maxCount(16));
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public SoundEvent getDrinkSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }
}
