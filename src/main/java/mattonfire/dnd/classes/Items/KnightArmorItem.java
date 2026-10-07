package mattonfire.dnd.classes.Items;
import mattonfire.dnd.classes.DndCharacter;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import mattonfire.dnd.classes.Items.lib.FAArmorItem;
import mattonfire.dnd.classes.Items.lib.FAArmorModel;
import mattonfire.dnd.classes.Items.lib.FAArmorRenderer;
import mattonfire.dnd.classes.Items.lib.FAArmorAttributes;


public class KnightArmorItem extends FAArmorItem {
    public KnightArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(
            new StatusEffectInstance(StatusEffects.JUMP_BOOST, 239)
            );
    }

    @Override
    public Set<DndCharacter> getMatchingClasses() {
        return Set.of(DndCharacter.FIGHTER);
    }

    @Override
    public List<StatusEffectInstance> getMatchingClassEffects() {
        return List.of(new StatusEffectInstance(StatusEffects.RESISTANCE, 239));
    }


    @Override
    @Environment(EnvType.CLIENT)
    protected GeoArmorRenderer<? extends FAArmorItem> createArmorRenderer() {
        return new FAArmorRenderer<>(new FAArmorModel<>(
                "geo/knight_armor.geo.json",
                "textures/models/armor/knight_armor.png"
        ));
    }
}