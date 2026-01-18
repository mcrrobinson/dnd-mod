package mattonfire.dnd.classes.Client.Geo;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.util.Identifier;

public class GeoModelHelper {

    public static Identifier getModelLocation(DndCharacter character) {
        String path = "geo/wizard_armor.geo.json";
        switch (character) {
            case ROGUE:
                path = "geo/rogue_armor.geo.json";
                break;
            case BLOODHUNTER:
                path = "geo/blood_hunter_armor.geo.json";
                break;
            case CLERIC:
                path = "geo/cleric_armor.geo.json";
                break;
            case PALADIN:
                path = "geo/knight_armor.geo.json";
                break;
            case BARBARIAN:
                path = "geo/golden_horns_armor.geo.json";
                break;
            case WIZARD:
            case NECROMANCER:
            case WARLOCK:
            default:
                path = "geo/wizard_armor.geo.json";
                break;
        }
        return new Identifier("dndclasses", path);
    }

    public static Identifier getTextureLocation(DndCharacter character) {
        String path = "textures/models/armor/wizard_armor.png";
        switch (character) {
            case ROGUE:
                path = "textures/models/armor/rogue_armor.png";
                break;
            case BLOODHUNTER:
                path = "textures/models/armor/blood_hunter_armor.png";
                break;
            case CLERIC:
                path = "textures/models/armor/cleric_armor.png";
                break;
            case PALADIN:
                path = "textures/models/armor/knight_armor.png";
                break;
            case BARBARIAN:
                path = "textures/models/armor/golden_horns_armor.png";
                break;
            case WIZARD:
            case NECROMANCER:
            case WARLOCK:
            default:
                path = "textures/models/armor/wizard_armor.png";
                break;
        }
        return new Identifier("dndclasses", path);
    }
}
