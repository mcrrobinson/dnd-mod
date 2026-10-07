package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.util.Identifier;

/** The loot table a container rolls when first opened (null once rolled); see {@code Lockpicking}. */
@Mixin(LootableContainerBlockEntity.class)
public interface LootableContainerBlockEntityAccessor {
    @Accessor("lootTableId")
    Identifier dndclasses$getLootTableId();
}
