package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Debug log lines for mobs and bosses starting a special action (a charge, breath, spell, summon,
 * enrage), so tests can check how often and when they happen. They log at DEBUG level, which the
 * dev client writes to {@code run/logs/debug.log} (not {@code latest.log}). Each line reads
 * {@code [MobAction] <name> <action> -> <target> (<health>/<max> HP, t=<world time>)}.
 */
public final class MobActions {
    private MobActions() {
    }

    public static void log(LivingEntity mob, String action, @Nullable LivingEntity target) {
        if (!DnDClasses.LOGGER.isDebugEnabled()) {
            return;
        }
        DnDClasses.LOGGER.debug("[MobAction] {} {} -> {} ({}/{} HP, t={})", mob.getName().getString(), action,
                target == null ? "-" : target.getName().getString(), Math.round(mob.getHealth()),
                Math.round(mob.getMaxHealth()), mob.getWorld().getTime());
    }
}
