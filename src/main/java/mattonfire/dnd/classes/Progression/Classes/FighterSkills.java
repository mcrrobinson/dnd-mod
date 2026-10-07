package mattonfire.dnd.classes.Progression.Classes;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;

/** Fighter skill tree: not designed yet. */
public class FighterSkills extends ClassSkills {
    @Override
    public DndCharacter dndClass() {
        return DndCharacter.FIGHTER;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of();
    }
}
