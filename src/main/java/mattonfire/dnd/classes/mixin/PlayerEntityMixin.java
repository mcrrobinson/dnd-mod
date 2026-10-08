package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity implements PlayerEntityExt {
    private static final String DND_CLASS_KEY = "DndClass";

    boolean dropEntireStack;
    private DndCharacter dndClass;

    public PlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setDndClass(DndCharacter dndClass) {
        this.dndClass = dndClass;
    }

    public DndCharacter getDndClass() {
        return this.dndClass;
    }

    // Save the class with the player so rejoining keeps it instead of reopening the picker.
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeDndClass(NbtCompound nbt, CallbackInfo info) {
        if (this.dndClass != null && this.dndClass != DndCharacter.NONE) {
            nbt.putInt(DND_CLASS_KEY, this.dndClass.getValue());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readDndClass(NbtCompound nbt, CallbackInfo info) {
        if (nbt.contains(DND_CLASS_KEY, NbtElement.INT_TYPE)) {
            try {
                this.dndClass = DndCharacter.fromValue(nbt.getInt(DND_CLASS_KEY));
            } catch (IllegalArgumentException e) {
                this.dndClass = DndCharacter.NONE;
            }
        }
    }

    public void setMana(int amount) {
        IEntityDataSaver player = (IEntityDataSaver) (Object) this;
        NbtCompound nbt = player.getPersistentData();
        nbt.putInt("mana", Math.min(amount, 100));
    }

    private float getCustomPullProgress(int useTicks) {
        float f = (float) useTicks / 3.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    // Some creatures can't swim... here is that. Probably a better way of doing
    // it.
    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof PlayerEntityExt) {
            PlayerEntityExt playerEntity = (PlayerEntityExt) player;
            if (!world.isClient && this.age % 20 == 0 && player instanceof ServerPlayerEntity serverPlayer) {
                Druid.serverTick(serverPlayer);
            }
            if (playerEntity.getDndClass() == DndCharacter.DRUID
                    && !Progression.hasPassive(player, "druid.tidecaller")) {
                if (isSubmergedIn(FluidTags.WATER) && !player.isCreative() &&
                        !player.getAbilities().flying) {
                    this.setVelocity(0.0D, -0.5, 0.0D);
                }
            }
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasStatusEffect(ModEffects.ARROW_STORM)) {

            if (!self.isUsingItem())
                return;

            ItemStack activeItem = self.getActiveItem();
            if (!(activeItem.getItem() instanceof BowItem))
                return;

            int useTicks = self.getItemUseTime();

            // change this if its the ranger class
            float progress;
            if (self instanceof PlayerEntityExt playerEntity) {
                if (playerEntity.getDndClass() == DndCharacter.RANGER) {
                    progress = getCustomPullProgress(useTicks);
                } else {
                    progress = BowItem.getPullProgress(useTicks);
                }
            } else {
                progress = BowItem.getPullProgress(useTicks);
            }

            if (progress >= 1.0F) {
                // Auto-release the bow
                self.stopUsingItem(); // Triggers BowItem#onStoppedUsing
            }
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo info) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof PlayerEntityExt) {
            PlayerEntityExt playerEntity = (PlayerEntityExt) entity;

            // Druids regenerate in the light and get hungry in the dark.
            if (playerEntity.getDndClass() == DndCharacter.DRUID && !world.isClient && this.age % 20 == 0) {
                Druid.lightTick((PlayerEntity) entity);
            }
        }

    }

}