package mattonfire.dnd.classes.PowerupKeybind;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion.DestructionType;

public class PowerUpEffect {
    public static void play(PlayerEntity player) {
        play(player, player);
    }

    public static void play(PlayerEntity localPlayer, PlayerEntity effectPlayer) {
        World world = localPlayer.getEntityWorld();
        world.createExplosion( null, localPlayer.getX(), localPlayer.getY(), localPlayer.getZ(), 5.F, DestructionType.DESTROY);
    }
}
