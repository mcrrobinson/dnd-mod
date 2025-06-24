package mattonfire.dnd.classes.mixin;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.util.math.random.Random;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {
    private static final UUID ARTIFICER_MINING_SPEED_UUID = UUID.fromString("b1a7e6e2-8e2f-4e5e-9e5e-123456789abc");
    private static final String ARTIFICER_MINING_SPEED_KEY = "ArtificerMiningSpeed";
    private static final double MINING_SPEED_BONUS = 0.5; // 50% faster

    @Inject(method = "onTakeItem", at = @At("RETURN"))
    private void dnd$addArtificerMiningBoost(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() instanceof PickaxeItem && player instanceof PlayerEntityExt ext) {
            if (ext.getDndClass() == DndCharacter.ARTIFICER && Random.create().nextFloat() < 0.5f) {
                // Add attribute modifier for mining speed
                stack.addAttributeModifier(
                        EntityAttributes.GENERIC_ATTACK_SPEED, // Not ideal, but vanilla doesn't have a mining speed
                                                               // attribute. See below.
                        new EntityAttributeModifier(
                                ARTIFICER_MINING_SPEED_UUID,
                                ARTIFICER_MINING_SPEED_KEY,
                                MINING_SPEED_BONUS,
                                EntityAttributeModifier.Operation.MULTIPLY_TOTAL),
                        net.minecraft.entity.EquipmentSlot.MAINHAND);
                // Optionally, add a custom NBT tag for identification
                stack.getOrCreateNbt().putBoolean("artificer_mining_boost", true);
            }
        }
    }
}