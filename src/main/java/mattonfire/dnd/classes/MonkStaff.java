package mattonfire.dnd.classes;

import mattonfire.dnd.classes.gui.GUI;
import mattonfire.dnd.classes.gui.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MonkStaff extends SwordItem {
    protected MonkStaff(ToolMaterial toolMaterial){
        super(toolMaterial, 9, -2.2F, new Item.Settings().group(ItemGroup.COMBAT));
    }

    // Override player sound to wool break. Should be custom in future.
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand){
        if(world.isClient){
            MinecraftClient.getInstance().openScreen(new Screen(new GUI()));
        }
        return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, playerEntity.getStackInHand(hand));
    }
}
