package mattonfire.dnd.classes;

import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

@SuppressWarnings("unused")
public class DisallowSwordServer {
    public static void onInitializeServer() {
        AttackEntityCallback.EVENT.register((player, world, hand, hitResult, entity) -> {
            if (((PlayerEntityExt) player).dndClassExist() == 8) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
