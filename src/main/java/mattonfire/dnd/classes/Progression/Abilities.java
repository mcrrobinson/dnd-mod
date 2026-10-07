package mattonfire.dnd.classes.Progression;

import mattonfire.dnd.classes.Misc.PowerUpEffect;
import net.minecraft.server.network.ServerPlayerEntity;

/** Fires the active skills used with the power-up key. */
public final class Abilities {
    private Abilities() {
    }

    /**
     * Fires an active skill: the root is the class's original power-up in
     * {@link PowerUpEffect}, the rest are in the class's {@link ClassSkills}.
     *
     * @return false if nothing happened, so no mana is spent
     */
    public static boolean activate(ServerPlayerEntity player, SkillNode node) {
        if (node.isRoot()) {
            return PowerUpEffect.play(player.getServer(), player, Progression.classOf(player));
        }
        ClassSkills skills = ClassTrees.skills(Progression.classOf(player));
        return skills != null && ClassTrees.belongsTo(node, skills.dndClass()) && skills.activate(player, node);
    }
}
