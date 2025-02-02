package mattonfire.dnd.classes.Misc;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World.ExplosionSourceType;

public class PowerUpEffect {

    public static void bardEffect(PlayerEntity player) {
        List<LivingEntity> nearbyEntities = player.getEntityWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(10), // 10-block radius
                entity -> entity instanceof PassiveEntity && !(entity instanceof PlayerEntity));

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof PassiveEntity passiveMob) {

                // If it's a tameable entity (e.g., wolf, cat, etc.), make it follow the player
                if (passiveMob instanceof TameableEntity tameable) {
                    if (!tameable.isTamed()) {
                        tameable.setOwner(player);
                    }
                }

                // Modify AI Goals using Mixin Accessor
                if (passiveMob instanceof MobEntity) {
                    MobEntityAccessor accessor = (MobEntityAccessor) passiveMob;
                    accessor.getTargetSelector().add(1, new ActiveTargetGoal<>(
                            passiveMob, HostileEntity.class, true));
                }
            }
        }

        // Make player unseen by hostile mobs
        List<HostileEntity> hostileEntities = player.getEntityWorld().getEntitiesByClass(
                HostileEntity.class,
                player.getBoundingBox().expand(10), // 10-block radius
                entity -> true);
    }

    public static void play(PlayerEntity player, DndCharacter character) {
        System.out.println("Starting powerup on: " + character.toString());
        switch (character) {
            case RANGER:
                // Make the bow shoot faster
                break;
            case WIZARD:
                player.getEntityWorld().createExplosion(null, player.getX(), player.getY(), player.getZ(), 10.F, true,
                        ExplosionSourceType.TNT);
                break;
            case BARBARIAN:
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 2));
                break;
            case BARD:
                bardEffect(player);
                break;
            case CLERIC:

            default:
                break;
        }
    }
}
