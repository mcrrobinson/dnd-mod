package mattonfire.dnd.classes.Obstacles;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.types.GuardianBeastType;
import mattonfire.dnd.classes.Obstacles.types.SingingDoorType;
import mattonfire.dnd.classes.Obstacles.types.ThornwallType;
import net.minecraft.block.Material;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * The nature and song obstacles, for Druids and Bards: the Guardian Beast, the Thornwall and the Singing Door.
 * Kept out of {@link ObstacleTypes} so other obstacle families can be added beside it; {@link ObstacleTypes}
 * loads this class before it builds the shared block entity type.
 */
public final class NatureSongObstacles {
    public static final GuardianBeastType GUARDIAN_BEAST = new GuardianBeastType(id("guardian_beast"));
    public static final ThornwallType THORNWALL = new ThornwallType(id("thornwall"));
    public static final SingingDoorType SINGING_DOOR = new SingingDoorType(id("singing_door"));

    public static final ObstacleBlock GUARDIAN_BEAST_BLOCK = ObstacleTypes.register(GUARDIAN_BEAST,
            ObstacleTypes.solidSettings(Material.WOOD, BlockSoundGroup.CHAIN));
    // Nether wood: not burnable, so vanilla fire and lava leave it to the Thornwall's own burn
    public static final ObstacleBlock THORNWALL_BLOCK = ObstacleTypes.register(THORNWALL,
            ObstacleTypes.solidSettings(Material.NETHER_WOOD, BlockSoundGroup.AZALEA_LEAVES));
    public static final ObstacleBlock SINGING_DOOR_BLOCK = ObstacleTypes.register(SINGING_DOOR,
            ObstacleTypes.glowSettings(Material.STONE, BlockSoundGroup.STONE, 3));

    private NatureSongObstacles() {
    }

    private static Identifier id(String name) {
        return new Identifier(DnDClasses.MOD_ID, name);
    }

    /** Loads the class, registering its types and blocks. */
    static void init() {
    }
}
