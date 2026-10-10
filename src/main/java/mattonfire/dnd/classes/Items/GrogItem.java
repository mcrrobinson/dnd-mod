package mattonfire.dnd.classes.Items;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;

/** Orcish grog from the stronghold Quartermaster: Strength I for 60 s, and 5 s of the spins. */
public class GrogItem extends Item {
    public static final FoodComponent FOOD = new FoodComponent.Builder()
            .hunger(2)
            .saturationModifier(0.2F)
            .alwaysEdible()
            .statusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60, 0), 1.0F)
            .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 5, 0), 1.0F)
            .build();

    public GrogItem(Settings settings) {
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
