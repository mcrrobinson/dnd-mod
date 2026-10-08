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
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.BrewingStandScreenHandler;
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
    private static final String ARMED_KEY = "DndBrewArmed";

    @Unique
    private static final String LAST_USER_KEY = "DndLastUser";

    @Unique
    private boolean brewArmed;

    @Unique
    private UUID lastUser;

    /** Who is brewing in the stand being ticked, and whether Efficient Brewer saves its ingredient. */
    @Unique
    private static UUID dnd$brewer;

    @Unique
    private static boolean dnd$saveIngredient;

    /** brewTime of the stand being ticked, before the vanilla tick ran. */
    @Unique
    private static int dnd$brewTimeBefore;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void onBrewComplete(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity,
            CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        BrewingStandAccessor accessor = (BrewingStandAccessor) blockEntity;
        BrewingStandAccess instance = (BrewingStandAccess) blockEntity;
        dnd$brewer = instance.getLastUser();
        dnd$brewTimeBefore = accessor.getBrewTime();

        // Vanilla finishes the brew this tick only if it can still craft with the same ingredient;
        // otherwise it cancels the brew, and a cancelled brew doesn't explode.
        if (accessor.getBrewTime() != 1 || !instance.isBrewArmed()) {
            return;
        }
        DefaultedList<ItemStack> slots = accessor.dnd$getInventory();
        if (!BrewingStandAccessor.dnd$canCraft(slots) || !slots.get(3).isOf(accessor.dnd$getItemBrewing())) {
            return;
        }
        instance.setBrewArmed(false);

        DamageSource customExplosionSource = ModDamageTypes.of(world, ModDamageTypes.BREWING_STAND_DAMAGE_SOURCE);
        // Hurts everyone nearby but leaves the terrain alone.
        world.createExplosion(null, customExplosionSource, null, pos.toCenterPos(), 10.0F, false,
                ExplosionSourceType.NONE);
        // The brew is ruined: the stand breaks and drops itself and its contents. Breaking it after
        // the explosion keeps the dropped items from being blown up.
        world.breakBlock(pos, true);

        // Skip the rest of the vanilla tick, which would finish the brew and setBlockState the
        // stand back into place.
        if (blockEntity.isRemoved() || !world.getBlockState(pos).isOf(state.getBlock())) {
            ci.cancel();
        }
    }

    // When a brew starts, arm it if a non-Alchemist (and no Alchemist) has this stand open.
    // Brews started with nobody looking, e.g. by a hopper, are never armed.
    @Inject(method = "tick", at = @At("RETURN"))
    private static void dnd$armOnBrewStart(World world, BlockPos pos, BlockState state,
            BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        BrewingStandAccess instance = (BrewingStandAccess) blockEntity;
        int brewTime = ((BrewingStandAccessor) blockEntity).getBrewTime();
        if (brewTime == 0) {
            if (instance.isBrewArmed()) {
                instance.setBrewArmed(false);
            }
        } else if (dnd$brewTimeBefore == 0) {
            instance.setBrewArmed(dnd$startedByNonAlchemist(world, blockEntity));
        }
    }

    @Unique
    private static boolean dnd$startedByNonAlchemist(World world, BrewingStandBlockEntity blockEntity) {
        boolean nonAlchemist = false;
        for (PlayerEntity player : world.getPlayers()) {
            if (!(player.currentScreenHandler instanceof BrewingStandScreenHandler handler)
                    || handler.slots.isEmpty() || handler.slots.get(0).inventory != blockEntity) {
                continue;
            }
            DndCharacter dndClass = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
            if (dndClass == DndCharacter.ALCHEMIST) {
                return false;
            }
            if (dndClass != null && dndClass != DndCharacter.NONE) {
                nonAlchemist = true;
            }
        }
        return nonAlchemist;
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
        this.brewArmed = nbt.getBoolean(ARMED_KEY);
        this.lastUser = nbt.containsUuid(LAST_USER_KEY) ? nbt.getUuid(LAST_USER_KEY) : null;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeLastPlayer(NbtCompound nbt, CallbackInfo ci) {
        if (this.brewArmed) {
            nbt.putBoolean(ARMED_KEY, true);
        }
        if (this.lastUser != null) {
            nbt.putUuid(LAST_USER_KEY, this.lastUser);
        }
    }

    public boolean isBrewArmed() {
        return brewArmed;
    }

    public void setBrewArmed(boolean armed) {
        this.brewArmed = armed;
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
