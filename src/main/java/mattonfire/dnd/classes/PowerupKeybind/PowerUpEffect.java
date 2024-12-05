package mattonfire.dnd.classes.PowerupKeybind;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World.ExplosionSourceType;

public class PowerUpEffect {
    public static void play(PlayerEntity player, int classID) {
        System.out.println("Starting powerup on: " + Integer.toString(classID));
        switch (classID) {
            case 8:
                // Make the bow shoot faster
                break;
            case 12:
                player.getEntityWorld().createExplosion(null, player.getX(), player.getY(), player.getZ(), 10.F, true,
                        ExplosionSourceType.TNT);
                break;
            case 11:
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
