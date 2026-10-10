package mattonfire.dnd.classes.Damages;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModDamageTypes {
    public static final Identifier BREWING_STAND_DAMAGE = new Identifier(DnDClasses.MOD_ID, "brewing_stand_explosion");
    public static final RegistryKey<DamageType> BREWING_STAND_DAMAGE_SOURCE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            BREWING_STAND_DAMAGE);

    public static final Identifier BLOOD_HUNTER_DAMAGE = new Identifier(DnDClasses.MOD_ID, "blood_hunter");
    public static final RegistryKey<DamageType> BLOOD_HUNTER_DAMAGE_SOURCE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            BLOOD_HUNTER_DAMAGE);

    public static final Identifier WIZARD_EXPLOSION_DAMAGE = new Identifier(DnDClasses.MOD_ID, "wizard_explosion");
    public static final RegistryKey<DamageType> WIZARD_EXPLOSION_DAMAGE_SOURCE = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            WIZARD_EXPLOSION_DAMAGE);

    public static final Identifier WARLOCK_WET_DAMAGE = new Identifier(DnDClasses.MOD_ID, "warlock_wet");
    public static final RegistryKey<DamageType> WARLOCK_WET_DAMAGE_SOURCE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            WARLOCK_WET_DAMAGE);

    /** A Gelatinous Cube's acid, dealt to whatever it has engulfed. */
    public static final RegistryKey<DamageType> GELATINOUS_CUBE_DAMAGE_SOURCE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "gelatinous_cube"));

    /** The Frost Drake's breath (tagged minecraft:is_freezing) and the hail of its hailstorm. */
    public static final RegistryKey<DamageType> FROST_BREATH = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "frost_breath"));
    public static final RegistryKey<DamageType> HAILSTONE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "hailstone"));

    /** Dungeon traps: flame vents and poison needles. */
    public static final RegistryKey<DamageType> TRAP = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "trap"));

    public static DamageSource of(World world, RegistryKey<DamageType> key, Entity attacker) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key), attacker);
    }

    public static DamageSource of(World world, RegistryKey<DamageType> key) {
        return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key));
    }
}
