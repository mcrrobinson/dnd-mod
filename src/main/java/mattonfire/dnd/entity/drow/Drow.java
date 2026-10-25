package mattonfire.dnd.entity.drow;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.classes.SkillChecks.Study;
import mattonfire.dnd.world.underdark.Underdark;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

/**
 * The drow and the Spider Houses (design 9, section 4; docs/mobs/drow.md): everything they register, in one place
 * so the Underdark tickets built alongside (the duergar, the building blocks) don't collide in the shared registries.
 * {@link #register()} is called once from {@code DnDClasses}, after the effects and potions.
 */
public final class Drow {
    // ---- Entity types ----

    public static final EntityType<DrowWarriorEntity> DROW_WARRIOR = humanoid("drow_warrior", DrowWarriorEntity::new);
    public static final EntityType<DrowMageEntity> DROW_MAGE = humanoid("drow_mage", DrowMageEntity::new);
    public static final EntityType<DrowPriestessEntity> DROW_PRIESTESS = humanoid("drow_priestess", DrowPriestessEntity::new);
    public static final EntityType<DrowEliteEntity> DROW_ELITE = humanoid("drow_elite", DrowEliteEntity::new);
    public static final EntityType<DrowBoltEntity> DROW_BOLT = Registry.register(Registries.ENTITY_TYPE, id("drow_bolt"),
            FabricEntityTypeBuilder.<DrowBoltEntity>create(SpawnGroup.MISC, DrowBoltEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
                    .trackRangeBlocks(4).trackedUpdateRate(20)
                    .build());

    /** Every drow (the Spider Houses' members): {@code #dndclasses:faction/drow}. */
    public static final TagKey<EntityType<?>> FACTION_TAG = TagKey.of(RegistryKeys.ENTITY_TYPE, id("faction/drow"));
    /** Underdark folk a Mind Flayer can enthral: {@code #dndclasses:underdark/thralls} (the drow; the duergar later). */
    public static final TagKey<EntityType<?>> THRALLS_TAG = TagKey.of(RegistryKeys.ENTITY_TYPE, id("underdark/thralls"));

    // ---- Effects and the potion ----

    public static StatusEffect DROW_POISON;
    public static StatusEffect FAERIE_FIRE;
    /** The Drow Poison Vial is a splash potion of this. */
    public static Potion DROW_POISON_POTION;

    // ---- Items ----

    /** Spidersilk: drow loot; the building-blocks ticket weaves it into Spidersilk blocks, the duergar buy it. */
    public static Item SPIDERSILK;
    public static Item DROW_WARRIOR_SPAWN_EGG;
    public static Item DROW_MAGE_SPAWN_EGG;
    public static Item DROW_PRIESTESS_SPAWN_EGG;
    public static Item DROW_ELITE_SPAWN_EGG;

    // ---- Spawns (design 9, section 4.5) ----

    public static final int WARRIOR_WEIGHT = 20;
    public static final int MAGE_WEIGHT = 5;
    public static final int PRIESTESS_WEIGHT = 2;

    private Drow() {
    }

    public static void register() {
        DROW_POISON = Registry.register(Registries.STATUS_EFFECT, id("drow_poison"), new DrowPoison.Effect());
        FAERIE_FIRE = Registry.register(Registries.STATUS_EFFECT, id("faerie_fire"), new FaerieFire.Effect());
        DROW_POISON_POTION = Registry.register(Registries.POTION, id("drow_poison"),
                new Potion("drow_poison", new StatusEffectInstance(DROW_POISON, DrowPoison.FAIL_TICKS)));

        SPIDERSILK = item("spidersilk", new Item(new FabricItemSettings()));
        DROW_WARRIOR_SPAWN_EGG = item("drow_warrior_spawn_egg", new SpawnEggItem(DROW_WARRIOR, 0x2B2235, 0xC8C8D8, new FabricItemSettings()));
        DROW_MAGE_SPAWN_EGG = item("drow_mage_spawn_egg", new SpawnEggItem(DROW_MAGE, 0x2B2235, 0x8A4FD0, new FabricItemSettings()));
        DROW_PRIESTESS_SPAWN_EGG = item("drow_priestess_spawn_egg", new SpawnEggItem(DROW_PRIESTESS, 0x2B2235, 0xA02040, new FabricItemSettings()));
        DROW_ELITE_SPAWN_EGG = item("drow_elite_spawn_egg", new SpawnEggItem(DROW_ELITE, 0x17121D, 0xE8E8F4, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(SPIDERSILK);
            entries.add(DROW_WARRIOR_SPAWN_EGG);
            entries.add(DROW_MAGE_SPAWN_EGG);
            entries.add(DROW_PRIESTESS_SPAWN_EGG);
            entries.add(DROW_ELITE_SPAWN_EGG);
        });

        FabricDefaultAttributeRegistry.register(DROW_WARRIOR, DrowWarriorEntity.createDrowWarriorAttributes());
        FabricDefaultAttributeRegistry.register(DROW_MAGE, DrowMageEntity.createDrowMageAttributes());
        FabricDefaultAttributeRegistry.register(DROW_PRIESTESS, DrowPriestessEntity.createDrowPriestessAttributes());
        FabricDefaultAttributeRegistry.register(DROW_ELITE, DrowEliteEntity.createDrowEliteAttributes());

        // Patrols in the Fungal Forest and the Faerzress Hollows (#dndclasses:habitat/drow), in the dark. Elites only
        // come with the Drow Spire.
        BiomeModifications.addSpawn(BiomeSelectors.tag(Underdark.HABITAT_DROW), SpawnGroup.MONSTER, DROW_WARRIOR,
                WARRIOR_WEIGHT, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(Underdark.HABITAT_DROW), SpawnGroup.MONSTER, DROW_MAGE,
                MAGE_WEIGHT, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(Underdark.HABITAT_DROW), SpawnGroup.MONSTER, DROW_PRIESTESS,
                PRIESTESS_WEIGHT, 1, 1);
        for (EntityType<? extends HostileEntity> type : new EntityType[] {DROW_WARRIOR, DROW_MAGE, DROW_PRIESTESS, DROW_ELITE}) {
            restrict(type);
        }

        DrowSaves.register();
        DrowPoison.register();
        DrowDarkness.register();

        Text weakness = Text.translatable("study.dndclasses.weakness.drow_sunlight");
        Study.registerWeakness(DROW_WARRIOR, weakness);
        Study.registerWeakness(DROW_MAGE, weakness);
        Study.registerWeakness(DROW_PRIESTESS, weakness);
        Study.registerWeakness(DROW_PRIESTESS, Text.translatable("study.dndclasses.weakness.drow_priestess"));
        Study.registerWeakness(DROW_ELITE, weakness);
    }

    @SuppressWarnings("unchecked")
    private static <T extends HostileEntity> void restrict(EntityType<T> type) {
        SpawnRestriction.register(type, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                HostileEntity::canSpawnInDark);
    }

    private static <T extends DrowEntity> EntityType<T> humanoid(String name, EntityType.EntityFactory<T> factory) {
        return Registry.register(Registries.ENTITY_TYPE, id(name),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, factory)
                        .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                        .build());
    }

    private static Item item(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }
}
