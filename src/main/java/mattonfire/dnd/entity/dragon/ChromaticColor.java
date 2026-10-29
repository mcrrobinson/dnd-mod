package mattonfire.dnd.entity.dragon;

import java.util.List;

import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

/**
 * The chromatic dragon colours an Ancient can be (design 10, 5.1). Each supplies what differs between them: its
 * boss bar, breath damage type, lair actions, texture, lair structure, unique drop and name syllables. Only Red
 * exists so far; Blue (lightning line, deserts), Green (poison cone, old forests), Black (acid line, swamps) and
 * White (cold cone, frozen peaks) would each be one value here plus one {@link AncientDragonEntity} subclass.
 */
public enum ChromaticColor {
    RED("red", BossBar.Color.RED, ModDamageTypes.ANCIENT_FIRE_BREATH,
            new String[] { "Khar", "Vyr", "Ash", "Ign", "Mor" },
            new String[] { "zan", "rak", "kel", "tor", "a" },
            new String[] { "thrax", "rath", "loth", "ion", "axis" });

    private final String id;
    private final BossBar.Color barColor;
    private final RegistryKey<DamageType> breathDamage;
    private final String[][] syllables;

    ChromaticColor(String id, BossBar.Color barColor, RegistryKey<DamageType> breathDamage, String[]... syllables) {
        this.id = id;
        this.barColor = barColor;
        this.breathDamage = breathDamage;
        this.syllables = syllables;
    }

    public String id() {
        return this.id;
    }

    public BossBar.Color barColor() {
        return this.barColor;
    }

    /** The damage type of its breath. */
    public RegistryKey<DamageType> breathDamage() {
        return this.breathDamage;
    }

    /** Name syllables, first to last ({@link AncientDragonNames}). */
    public String[][] syllables() {
        return this.syllables;
    }

    /** {@code textures/entity/ancient_dragon/<id>.png} (plus {@code <id>_glowmask.png}). */
    public Identifier texture() {
        return new Identifier(DnDClasses.MOD_ID, "textures/entity/ancient_dragon/" + this.id + ".png");
    }

    /**
     * Its lair structure, {@code dndclasses:ancient_<id>_lair}. Built by the lair ticket ("Ancient dragon lair: the
     * Red's caldera and hoard"); until then lookups find nothing and the dragon only gets a lair from its
     * {@code Lair} NBT.
     */
    public RegistryKey<Structure> lairStructure() {
        return RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "ancient_" + this.id + "_lair"));
    }

    /** What its lair can do on its turn ({@link AncientLairActions}). */
    public List<LairAction> lairActions() {
        return switch (this) {
            case RED -> AncientLairActions.RED;
        };
    }

    /** The unique item its kill always drops. */
    public Item uniqueDrop() {
        return switch (this) {
            case RED -> ModItems.RED_DRAGON_SCALE_MAIL;
        };
    }

    /** Its scale, the common drop. */
    public Item scale() {
        return switch (this) {
            case RED -> ModItems.ANCIENT_RED_SCALE;
        };
    }

    public EntityType<? extends AncientDragonEntity> entityType() {
        return switch (this) {
            case RED -> ModEntityTypes.ANCIENT_RED_DRAGON;
        };
    }
}
