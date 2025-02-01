package mattonfire.dnd.classes.Misc;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World.ExplosionSourceType;

public class PowerUpEffect {
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
                // Vec3d aim = player.getVelocity();
                // FireballEntity fireball = new FireballEntity(player.world, player, 1, 1, 1);
                // fireball.refreshPositionAndAngles(player.getX() + aim.x * 1.50, player.getY()
                // + aim.y * 1.50, player.getZ() + aim.z * 1.50, 0.F, 0.F);
                // player.world.spawnEntity(fireball);

            default:
                break;
        }
    }
}
