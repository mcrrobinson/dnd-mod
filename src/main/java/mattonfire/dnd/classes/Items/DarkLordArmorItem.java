package mattonfire.dnd.classes.Items;
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


public class DarkLordArmorItem extends FAArmorItem {
    public DarkLordArmorItem(Type type, FAArmorAttributes armorAttributes) {
        super(type, armorAttributes);
    }

    @Override
    public List<StatusEffectInstance> getFullSetEffects() {
        return List.of(
            new StatusEffectInstance(StatusEffects.JUMP_BOOST, 239)
            );
    }


    @Override
    @Environment(EnvType.CLIENT)
    protected GeoArmorRenderer<? extends FAArmorItem> createArmorRenderer() {
        return new FAArmorRenderer<>(new FAArmorModel<>(
                "geo/dark_lord_armor.geo.json",
                "textures/models/armor/dark_lord_armor.png"
        ));
    }
}