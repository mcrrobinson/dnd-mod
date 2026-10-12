package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleTypes;
import net.minecraft.block.Material;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The craft and element obstacles, for Artificers, Alchemists and Warlocks: the Clockwork Door, the
 * Alchemical Seal, the Miasma Vent and the Fire Curtain with its brazier. Kept out of {@link ObstacleTypes}
 * so each obstacle family registers from its own file; {@link ObstacleTypes} calls {@link #init()} from its
 * static initialiser, before it builds the shared block entity type, so nothing else may load this class first.
 */
public final class CraftElementObstacles {
    public static final ClockworkDoorType CLOCKWORK_DOOR = new ClockworkDoorType(id("clockwork_door"));
    public static final AlchemicalSealType ALCHEMICAL_SEAL = new AlchemicalSealType(id("alchemical_seal"));
    public static final MiasmaVentType MIASMA_VENT = new MiasmaVentType(id("miasma_vent"));
    public static final FireCurtainType FIRE_CURTAIN = new FireCurtainType(id("fire_curtain"));
    public static final FireBrazierType FIRE_CURTAIN_BRAZIER = new FireBrazierType(id("fire_curtain_brazier"),
            FIRE_CURTAIN);

    public static final ObstacleBlock CLOCKWORK_DOOR_BLOCK = ObstacleTypes.register(CLOCKWORK_DOOR,
            ObstacleTypes.solidSettings(Material.METAL, BlockSoundGroup.COPPER));
    /** Amber resin: see-through, faintly glowing. */
    public static final ObstacleBlock ALCHEMICAL_SEAL_BLOCK = ObstacleTypes.register(ALCHEMICAL_SEAL,
            ObstacleTypes.glowSettings(Material.STONE, BlockSoundGroup.HONEY, 3));
    public static final ObstacleBlock MIASMA_VENT_BLOCK = ObstacleTypes.register(MIASMA_VENT,
            ObstacleTypes.glowSettings(Material.METAL, BlockSoundGroup.METAL, 4));
    /**
     * Soul fire that isn't a fire block: a non-replaceable material, so water can't flow into or wash it away,
     * and it isn't in the fire tag, so splash water doesn't put it out.
     */
    public static final ObstacleBlock FIRE_CURTAIN_BLOCK = ObstacleTypes.register(FIRE_CURTAIN,
            ObstacleTypes.glowSettings(Material.STONE, BlockSoundGroup.WOOL, 10));
    public static final ObstacleBlock FIRE_CURTAIN_BRAZIER_BLOCK = ObstacleTypes.register(FIRE_CURTAIN_BRAZIER,
            ObstacleTypes.glowSettings(Material.METAL, BlockSoundGroup.LANTERN, 12));

    private CraftElementObstacles() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, which registers everything above. Called by {@link ObstacleTypes} only. */
    public static void init() {
    }
}
