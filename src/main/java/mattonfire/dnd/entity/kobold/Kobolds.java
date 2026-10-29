package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Blocks.KoboldSnareBlock;
import mattonfire.dnd.classes.Blocks.KoboldSnareBlockEntity;
import mattonfire.dnd.classes.Blocks.TributeBowlBlock;
import mattonfire.dnd.classes.Conditions.ConditionEffects;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.classes.SkillChecks.Study;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

/**
 * The kobolds and the Kobold Tribes (design 10, section 3; docs/mobs/kobolds.md): everything they register, in one
 * place so the tickets built alongside (warrens, the Ancient Red, feats, magic items) don't collide in the shared
 * registries. {@link #register()} is called once from {@code DnDClasses}, before the magic items.
 * <p>
 * Hooks left for the other tickets: the entity tags {@link #FACTION_TAG} and {@code #dndclasses:lair_guards/<role>},
 * {@link KoboldCourage#registerHome}, {@link KoboldHostility#isLairGuard} and {@link KoboldHostility#sparedByTraps}.
 */
public final class Kobolds {
    // ---- Entity types ----

    /** Hitbox of the three small kobolds: 0.5 x 0.95, eyes at 0.8, so they fit 1-block bolt-holes. */
    public static final float WIDTH = 0.5F;
    public static final float HEIGHT = 0.95F;
    /** The Dragonshield is drawn at 1.15x: 0.6 x 1.1. */
    public static final float DRAGONSHIELD_SCALE = 1.15F;

    public static final EntityType<KoboldWarriorEntity> KOBOLD = small("kobold", KoboldWarriorEntity::new);
    public static final EntityType<KoboldSlingerEntity> KOBOLD_SLINGER = small("kobold_slinger", KoboldSlingerEntity::new);
    public static final EntityType<KoboldTrapmakerEntity> KOBOLD_TRAPMAKER = small("kobold_trapmaker", KoboldTrapmakerEntity::new);
    public static final EntityType<KoboldDragonshieldEntity> KOBOLD_DRAGONSHIELD = Registry.register(Registries.ENTITY_TYPE,
            id("kobold_dragonshield"), FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, KoboldDragonshieldEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.1F)).build());

    // ---- Tags ----

    /** Every kobold: {@code #dndclasses:faction/kobolds} (the Kobold Tribes' members; D1's dragon calls them). */
    public static final TagKey<EntityType<?>> FACTION_TAG = TagKey.of(RegistryKeys.ENTITY_TYPE, id("faction/kobolds"));
    /** Killing one of these makes kobolds dread you for 3 days: {@code #dndclasses:kobold_dread}. */
    public static final TagKey<EntityType<?>> DREAD_TAG = TagKey.of(RegistryKeys.ENTITY_TYPE, id("kobold_dread"));
    /** Where kobolds spawn: {@code #dndclasses:habitat/kobold}. */
    public static final TagKey<Biome> HABITAT = TagKey.of(RegistryKeys.BIOME, id("habitat/kobold"));

    // ---- Blocks ----

    public static final KoboldSnareBlock KOBOLD_SNARE_BLOCK = new KoboldSnareBlock(FabricBlockSettings
            .of(Material.DECORATION, MapColor.BROWN).noCollision().breakInstantly().nonOpaque()
            .sounds(BlockSoundGroup.AZALEA_LEAVES));
    public static final BlockEntityType<KoboldSnareBlockEntity> KOBOLD_SNARE_ENTITY = FabricBlockEntityTypeBuilder
            .create(KoboldSnareBlockEntity::new, KOBOLD_SNARE_BLOCK).build(null);
    public static final TributeBowlBlock DRAGON_TRIBUTE_BOWL = new TributeBowlBlock(FabricBlockSettings
            .copyOf(Blocks.SMOOTH_BASALT).strength(1.5F, 6.0F).nonOpaque());

    // ---- Items ----

    public static Item KOBOLD_SLING;
    public static Item KOBOLD_SCALE;
    public static Item KOBOLD_TRINKET;
    public static Item KOBOLD_SNARE;
    public static Item DRAGON_TRIBUTE_BOWL_ITEM;
    public static Item KOBOLD_SPAWN_EGG;
    public static Item KOBOLD_SLINGER_SPAWN_EGG;
    public static Item KOBOLD_TRAPMAKER_SPAWN_EGG;
    public static Item KOBOLD_DRAGONSHIELD_SPAWN_EGG;

    // ---- Spawns (section 3.6) ----

    public static final int SPAWN_WEIGHT = 8;

    private Kobolds() {
    }

    public static void register() {
        Registry.register(Registries.BLOCK, id("kobold_snare"), KOBOLD_SNARE_BLOCK);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, id("kobold_snare"), KOBOLD_SNARE_ENTITY);
        Registry.register(Registries.BLOCK, id("dragon_tribute_bowl"), DRAGON_TRIBUTE_BOWL);

        KOBOLD_SLING = item("kobold_sling", new KoboldSlingItem(new FabricItemSettings().maxDamage(KoboldSlingItem.DURABILITY)));
        KOBOLD_SCALE = item("kobold_scale", new Item(new FabricItemSettings()));
        KOBOLD_TRINKET = item("kobold_trinket", new KoboldTrinketItem(new FabricItemSettings()));
        KOBOLD_SNARE = item("kobold_snare", new BlockItem(KOBOLD_SNARE_BLOCK, new FabricItemSettings()));
        DRAGON_TRIBUTE_BOWL_ITEM = item("dragon_tribute_bowl", new BlockItem(DRAGON_TRIBUTE_BOWL, new FabricItemSettings()));
        KOBOLD_SPAWN_EGG = item("kobold_spawn_egg", new SpawnEggItem(KOBOLD, 0x96402A, 0xCE9668, new FabricItemSettings()));
        KOBOLD_SLINGER_SPAWN_EGG = item("kobold_slinger_spawn_egg", new SpawnEggItem(KOBOLD_SLINGER, 0xB28032, 0x604028, new FabricItemSettings()));
        KOBOLD_TRAPMAKER_SPAWN_EGG = item("kobold_trapmaker_spawn_egg", new SpawnEggItem(KOBOLD_TRAPMAKER, 0x4E5A68, 0xB29C70, new FabricItemSettings()));
        KOBOLD_DRAGONSHIELD_SPAWN_EGG = item("kobold_dragonshield_spawn_egg", new SpawnEggItem(KOBOLD_DRAGONSHIELD, 0x96402A, 0xBA221E, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> {
            entries.add(KOBOLD_SLING);
            entries.add(KOBOLD_SCALE);
            entries.add(KOBOLD_TRINKET);
            entries.add(KOBOLD_SNARE);
            entries.add(DRAGON_TRIBUTE_BOWL_ITEM);
            entries.add(KOBOLD_SPAWN_EGG);
            entries.add(KOBOLD_SLINGER_SPAWN_EGG);
            entries.add(KOBOLD_TRAPMAKER_SPAWN_EGG);
            entries.add(KOBOLD_DRAGONSHIELD_SPAWN_EGG);
        });

        FabricDefaultAttributeRegistry.register(KOBOLD, KoboldWarriorEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KOBOLD_SLINGER, KoboldSlingerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KOBOLD_TRAPMAKER, KoboldTrapmakerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KOBOLD_DRAGONSHIELD, KoboldDragonshieldEntity.createAttributes());

        // Packs in the hills, badlands and dripstone caves (in the dark, like other monsters); initialize adds
        // the rest of the pack, like a pillager patrol.
        BiomeModifications.addSpawn(BiomeSelectors.tag(HABITAT), SpawnGroup.MONSTER, KOBOLD, SPAWN_WEIGHT, 1, 1);
        for (EntityType<? extends HostileEntity> type : new EntityType[] {KOBOLD, KOBOLD_SLINGER, KOBOLD_TRAPMAKER,
                KOBOLD_DRAGONSHIELD}) {
            restrict(type);
        }

        // Pack Tactics and Sunlight Sensitivity, through the mob attack-mode hook.
        ConditionEffects.registerMobAttackMode("pack tactics", KoboldCourage::packTactics);
        ConditionEffects.registerMobAttackMode("sunlight sensitivity", KoboldCourage::sunlightSensitivity);

        // Dragon-slayer dread: kobolds remember who killed a dragon (the reputation comes from the faction file).
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            ServerPlayerEntity player = killer instanceof ServerPlayerEntity p ? p
                    : killer instanceof Tameable tamed && tamed.getOwner() instanceof ServerPlayerEntity owner ? owner : null;
            if (player != null && killed.getType().isIn(DREAD_TAG)) {
                KoboldTribute.markDragonSlayer(player, killed);
            }
        });

        KoboldSaves.register();
        Text weakness = Text.translatable("study.dndclasses.weakness.kobold");
        for (EntityType<?> type : new EntityType[] {KOBOLD, KOBOLD_SLINGER, KOBOLD_TRAPMAKER, KOBOLD_DRAGONSHIELD}) {
            Study.registerWeakness(type, weakness);
        }
        Study.registerWeakness(KOBOLD_DRAGONSHIELD, Text.translatable("study.dndclasses.weakness.kobold_dragonshield"));
    }

    /** Whether {@code entity} is a kobold (any of the four, or anything else in {@code #dndclasses:faction/kobolds}). */
    public static boolean isKobold(Entity entity) {
        return entity instanceof KoboldEntity || entity.getType().isIn(FACTION_TAG);
    }

    private static <T extends HostileEntity> void restrict(EntityType<T> type) {
        SpawnRestriction.register(type, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                HostileEntity::canSpawnInDark);
    }

    private static <T extends KoboldEntity> EntityType<T> small(String name, EntityType.EntityFactory<T> factory) {
        return Registry.register(Registries.ENTITY_TYPE, id(name),
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, factory)
                        .dimensions(EntityDimensions.fixed(WIDTH, HEIGHT))
                        .build());
    }

    private static Item item(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }
}
