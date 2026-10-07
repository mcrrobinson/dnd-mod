package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity implements PlayerEntityExt {
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

    public int addProgress(DndCharacter character, int amount) {
        IEntityDataSaver player = (IEntityDataSaver) (Object) this;
        NbtCompound nbt = player.getPersistentData();
        int progress = nbt.getInt(character.toString());
        progress += amount;
        nbt.putInt(character.toString(), progress);
        return progress;
    }

    public int getProgress(DndCharacter character) {
        IEntityDataSaver player = (IEntityDataSaver) (Object) this;
        NbtCompound nbt = player.getPersistentData();
        return nbt.getInt(character.toString());
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
            if (playerEntity.getDndClass() == DndCharacter.DRUID) {
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