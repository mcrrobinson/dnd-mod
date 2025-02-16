package mattonfire.dnd.classes.mixin;

import java.util.Random;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mattonfire.dnd.classes.BrewingStandAccess;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;
import net.minecraft.block.entity.BrewingStandBlockEntity;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin implements BrewingStandAccess {

    @Unique
    private DndCharacter lastPlayer;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void onBrewComplete(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity,
            CallbackInfo ci) {
        if (!world.isClient) { // Ensure this runs only on the server
            BrewingStandAccessor accessor = (BrewingStandAccessor) blockEntity; // Cast to our Accessor
            BrewingStandAccess instance = (BrewingStandAccess) blockEntity;

            if (accessor.getBrewTime() == 1) { // Use the accessor method
                if (instance.getLastPlayer() != null) {
                    System.out.println(instance.getLastPlayer().toString());
                } else {
                    System.out.println("lastPlayer is null");
                }

                DndCharacter lastPlayer = instance.getLastPlayer();
                if (lastPlayer != null) {
                    if (lastPlayer == DndCharacter.ALCHEMIST) {
                        return;
                    }
                }

                // Define a custom damage source
                DamageSource customExplosionSource = ModDamageTypes.of(world, ModDamageTypes.CUSTOM_DAMAGE_SOURCE);

                // Create an explosion using the custom damage source
                world.createExplosion(null, customExplosionSource, null,
                        blockEntity.getPos().toCenterPos(),
                        10.0F, false, ExplosionSourceType.TNT);
            }
        }
    }

    // Getter and setter for lastPlayer
    public DndCharacter getLastPlayer() {
        return lastPlayer;
    }

    public void setLastPlayer(DndCharacter character) {
        this.lastPlayer = character;
    }
}
