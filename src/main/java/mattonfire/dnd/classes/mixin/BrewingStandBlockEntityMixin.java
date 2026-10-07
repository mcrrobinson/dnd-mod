package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.classes.BrewingStandAccess;
import mattonfire.dnd.classes.Progression.Classes.AlchemistSkills;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin implements BrewingStandAccess {

    @Unique
    private static final String LAST_PLAYER_KEY = "DndLastPlayerClass";

    @Unique
    private static final String LAST_USER_KEY = "DndLastUser";

    @Unique
    private DndCharacter lastPlayer;

    @Unique
    private UUID lastUser;

    /** Who is brewing in the stand being ticked, and whether Efficient Brewer saves its ingredient. */
    @Unique
    private static UUID dnd$brewer;

    @Unique
    private static boolean dnd$saveIngredient;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void onBrewComplete(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity,
            CallbackInfo ci) {
        if (!world.isClient) { // Ensure this runs only on the server
            BrewingStandAccessor accessor = (BrewingStandAccessor) blockEntity; // Cast to our Accessor
            BrewingStandAccess instance = (BrewingStandAccess) blockEntity;
            dnd$brewer = instance.getLastUser();

            if (accessor.getBrewTime() == 1) { // Use the accessor method
                // Only explode when we know the brew was started by a non-Alchemist.
                // If nobody has used this stand (e.g. hopper-fed), don't explode.
                DndCharacter lastPlayer = instance.getLastPlayer();
                if (lastPlayer == null || lastPlayer == DndCharacter.ALCHEMIST) {
                    return;
                }

                // Define a custom damage source
                DamageSource customExplosionSource = ModDamageTypes.of(world,
                        ModDamageTypes.BREWING_STAND_DAMAGE_SOURCE);

                // Create an explosion using the custom damage source
                world.createExplosion(null, customExplosionSource, null,
                        blockEntity.getPos().toCenterPos(),
                        10.0F, false, ExplosionSourceType.TNT);

                // The explosion removed the stand. Skip the rest of the vanilla tick, which
                // would finish the brew and setBlockState the stand back into the crater.
                if (blockEntity.isRemoved()) {
                    ci.cancel();
                }
            }
        }
    }

    // Alchemist brewing XP and Efficient Brewer.
    @Inject(method = "craft", at = @At("HEAD"))
    private static void dnd$onCraft(World world, BlockPos pos, DefaultedList<ItemStack> slots, CallbackInfo ci) {
        dnd$saveIngredient = AlchemistSkills.onBrew(world, dnd$brewer, slots);
    }

    @WrapOperation(method = "craft", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;decrement(I)V"))
    private static void dnd$efficientBrewer(ItemStack ingredient, int amount, Operation<Void> original) {
        if (!dnd$saveIngredient) {
            original.call(ingredient, amount);
        }
        dnd$saveIngredient = false;
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readLastPlayer(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(LAST_PLAYER_KEY)) {
            try {
                this.lastPlayer = DndCharacter.fromValue(nbt.getInt(LAST_PLAYER_KEY));
            } catch (IllegalArgumentException e) {
                this.lastPlayer = null;
            }
        }
        this.lastUser = nbt.containsUuid(LAST_USER_KEY) ? nbt.getUuid(LAST_USER_KEY) : null;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeLastPlayer(NbtCompound nbt, CallbackInfo ci) {
        if (this.lastPlayer != null) {
            nbt.putInt(LAST_PLAYER_KEY, this.lastPlayer.getValue());
        }
        if (this.lastUser != null) {
            nbt.putUuid(LAST_USER_KEY, this.lastUser);
        }
    }

    // Getter and setter for lastPlayer
    public DndCharacter getLastPlayer() {
        return lastPlayer;
    }

    public void setLastPlayer(DndCharacter character) {
        this.lastPlayer = character;
        ((BrewingStandBlockEntity) (Object) this).markDirty();
    }

    public UUID getLastUser() {
        return lastUser;
    }

    public void setLastUser(UUID user) {
        this.lastUser = user;
        ((BrewingStandBlockEntity) (Object) this).markDirty();
    }
}
