package mattonfire.dnd.classes.Progression.Classes;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;

/** Wizard skill tree: not designed yet. */
public class WizardSkills extends ClassSkills {
    @Override
    public DndCharacter dndClass() {
        return DndCharacter.WIZARD;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of();
    }
}
