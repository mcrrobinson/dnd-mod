package mattonfire.dnd.classes.Progression;

import mattonfire.dnd.entity.boss.BossMinions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Hands out kill XP and passes game events to the player's class's
 * {@link ClassSkills}.
 */
public final class ProgressionEvents {
    /** Every class gets this for killing a hostile mob. */
    public static final int HOSTILE_KILL_XP = 2;

    private ProgressionEvents() {
    }

    static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ProgressionEvents::onKill);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0)
                return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                secondTick(player);
            }
        });

        SkillHelpers.register();
        for (ClassSkills skills : ClassTrees.all()) {
            skills.register();
        }
    }

    private static void onKill(LivingEntity entity, DamageSource source) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity player) || entity == player)
            return;

        int xp = entity instanceof Monster ? HOSTILE_KILL_XP : 0;
        ClassSkills skills = ClassTrees.skills(Progression.classOf(player));
        if (skills != null) {
            xp += skills.killXp(player, entity, source);
        }
        // Boss minions are endless; they give no class XP.
        if (!BossMinions.isMinion(entity)) {
            Progression.addXp(player, xp);
        }

        if (skills != null) {
            skills.onKill(player, Progression.current(player), entity, source);
        }
    }

    private static void secondTick(ServerPlayerEntity player) {
        ClassProgress progress = Progression.current(player);

        // Check every class's bonuses so they come off when the class changes.
        for (ClassSkills classSkills : ClassTrees.all()) {
            for (AttributeBonus bonus : classSkills.attributeBonuses()) {
                EntityAttributeInstance instance = player.getAttributeInstance(bonus.attribute());
                if (instance == null)
                    continue;
                boolean wanted = progress.hasPassive(bonus.skill());
                boolean has = instance.getModifier(bonus.uuid()) != null;
                if (wanted && !has) {
                    // Temporary so it isn't saved; it's re-added here after a relog.
                    instance.addTemporaryModifier(new EntityAttributeModifier(bonus.uuid(), bonus.skill(),
                            bonus.amount(), bonus.operation()));
                } else if (!wanted && has) {
                    instance.removeModifier(bonus.uuid());
                }
            }
        }

        ClassSkills skills = ClassTrees.skills(progress.dndClass);
        if (skills != null) {
            skills.secondTick(player, progress);
        }
    }

    /**
     * Applies damage passives. Called from {@code LivingEntityMixin} with the
     * amount about to be dealt (before armor).
     */
    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (target.getWorld().isClient)
            return amount;

        if (source.getAttacker() instanceof PlayerEntity player && player != target) {
            ClassProgress progress = Progression.current(player);
            ClassSkills skills = ClassTrees.skills(progress.dndClass);
            if (skills != null) {
                amount = skills.modifyDealtDamage(player, progress, target, source, amount);
            }
        }

        if (target instanceof PlayerEntity player) {
            ClassProgress progress = Progression.current(player);
            ClassSkills skills = ClassTrees.skills(progress.dndClass);
            if (skills != null) {
                amount = skills.modifyTakenDamage(player, progress, source, amount);
            }
        }
        return amount;
    }
}
