package mattonfire.dnd.classes;

import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class MonkStaff extends SwordItem {

    public MonkStaff(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, 9, -2.2F, settings); // Ensure toolMaterial is of type ToolMaterial
    }

    public ActionResult use(World world, ServerPlayerEntity player, Hand hand) {
        if (world.isClient) {
            // Client-side logic
            player.sendMessage(Text.of("You used the Monk Staff!"), true);
            player.playSound(SoundEvents.BLOCK_WOOL_BREAK, 1.0F, 1.0F);
        } else {
            // Server-side logic
            player.addExperience(10); // Example server-side effect
        }
        return ActionResult.SUCCESS;
    }
}
