package mattonfire.dnd.entity.drow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SpiderEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Who stands with the drow: other drow and spiders (cave spiders too). Drow never target or hurt them, their
 * Darkness doesn't blind them, and spiders never target drow ({@code SpiderTargetMixin}).
 */
public final class DrowAllies {
    private DrowAllies() {
    }

    public static boolean isDrow(@Nullable Entity entity) {
        return entity instanceof DrowEntity;
    }

    /** A drow or a spider. */
    public static boolean isAlly(@Nullable Entity entity) {
        return entity instanceof DrowEntity || entity instanceof SpiderEntity;
    }

    /** Whether a spider must leave {@code target} alone. */
    public static boolean spiderSpares(Entity spider, @Nullable Entity target) {
        return spider instanceof SpiderEntity && isDrow(target);
    }
}
