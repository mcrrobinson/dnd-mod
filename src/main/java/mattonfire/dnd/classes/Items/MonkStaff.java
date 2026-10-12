package mattonfire.dnd.classes.Items;

import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;

public class MonkStaff extends SwordItem {

    public MonkStaff(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, 9, -2.2F, settings); // Ensure toolMaterial is of type ToolMaterial
    }
}
