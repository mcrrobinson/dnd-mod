package mattonfire.dnd.classes.Damages;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.damage.DamageScaling;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModDamageTypes {
    public static final Identifier CUSTOM_DAMAGE_TYPE = new Identifier(DnDClasses.MOD_ID, "brewing_stand_explosion");
    public static final RegistryKey<DamageType> CUSTOM_DAMAGE_SOURCE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            CUSTOM_DAMAGE_TYPE);

    public static DamageSource of(World world, RegistryKey<DamageType> key) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key));
    }
}
