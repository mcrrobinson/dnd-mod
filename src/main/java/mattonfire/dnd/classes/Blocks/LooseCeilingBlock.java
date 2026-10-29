package mattonfire.dnd.classes.Blocks;

import net.minecraft.block.Block;

/**
 * Cracked, loosened rock in a tunnel roof (design 10, 4.1): the 3 x 3 over a {@link TrapKind#ROCKFALL} trap. It stays
 * put; when the trap goes off, rocks fall from under it ({@link TrapTriggerBlockEntity}). A sharp eye spots the
 * cracks. Unbreakable, like the other trap blocks.
 */
public class LooseCeilingBlock extends Block {
    public LooseCeilingBlock(Settings settings) {
        super(settings);
    }
}
