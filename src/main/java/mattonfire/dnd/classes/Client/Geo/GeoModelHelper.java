package mattonfire.dnd.classes.Client.Geo;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.util.Identifier;

/** The class armour model and texture for each class. The ids are constants: GeckoLib asks for them every frame. */
public class GeoModelHelper {
    public static final Identifier WIZARD_MODEL = new Identifier("dndclasses", "geo/wizard_armor.geo.json");
    public static final Identifier WIZARD_TEXTURE = new Identifier("dndclasses", "textures/models/armor/wizard_armor.png");
    private static final Identifier ROGUE_MODEL = new Identifier("dndclasses", "geo/rogue_armor.geo.json");
    private static final Identifier ROGUE_TEXTURE = new Identifier("dndclasses", "textures/models/armor/rogue_armor.png");
    private static final Identifier BLOOD_HUNTER_MODEL = new Identifier("dndclasses", "geo/blood_hunter_armor.geo.json");
    private static final Identifier BLOOD_HUNTER_TEXTURE = new Identifier("dndclasses", "textures/models/armor/blood_hunter_armor.png");
    private static final Identifier CLERIC_MODEL = new Identifier("dndclasses", "geo/cleric_armor.geo.json");
    private static final Identifier CLERIC_TEXTURE = new Identifier("dndclasses", "textures/models/armor/cleric_armor.png");
    private static final Identifier KNIGHT_MODEL = new Identifier("dndclasses", "geo/knight_armor.geo.json");
    private static final Identifier KNIGHT_TEXTURE = new Identifier("dndclasses", "textures/models/armor/knight_armor.png");
    private static final Identifier GOLDEN_HORNS_MODEL = new Identifier("dndclasses", "geo/golden_horns_armor.geo.json");
    private static final Identifier GOLDEN_HORNS_TEXTURE = new Identifier("dndclasses", "textures/models/armor/golden_horns_armor.png");

    public static Identifier getModelLocation(DndCharacter character) {
        return switch (character) {
            case ROGUE -> ROGUE_MODEL;
            case BLOODHUNTER -> BLOOD_HUNTER_MODEL;
            case CLERIC -> CLERIC_MODEL;
            case PALADIN -> KNIGHT_MODEL;
            case BARBARIAN -> GOLDEN_HORNS_MODEL;
            default -> WIZARD_MODEL; // Wizard, Necromancer, Warlock and the rest
        };
    }

    public static Identifier getTextureLocation(DndCharacter character) {
        return switch (character) {
            case ROGUE -> ROGUE_TEXTURE;
            case BLOODHUNTER -> BLOOD_HUNTER_TEXTURE;
            case CLERIC -> CLERIC_TEXTURE;
            case PALADIN -> KNIGHT_TEXTURE;
            case BARBARIAN -> GOLDEN_HORNS_TEXTURE;
            default -> WIZARD_TEXTURE;
        };
    }
}
