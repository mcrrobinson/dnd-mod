package mattonfire.dnd.classes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class DisallowSwordClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, hitResult, entity) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof SwordItem) {
                player.sendMessage(Text.of("You're a ranger, you can't use swords."), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}