package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.RangerSkills;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity implements PlayerEntityExt {
    private static final String DND_CLASS_KEY = "DndClass";
    private static final String DND_RACE_KEY = "DndRace";
    private static final String DND_ANCESTRY_KEY = "DndAncestry";
    /**
     * Race (low 4 bits) and Dragonborn ancestry (high 4 bits). On the DataTracker rather than a packet
     * because every client needs every player's race (racial sizes and rendering). Registered only here.
     */
    @Unique
    private static final TrackedData<Byte> DND$RACE = DataTracker.registerData(PlayerEntity.class,
            TrackedDataHandlerRegistry.BYTE);
    /** The race whose body size applies ({@link PlayerEntityExt#getBodyRace}). Not saved: RaceStats sets it. */
    @Unique
    private static final TrackedData<Byte> DND$BODY_RACE = DataTracker.registerData(PlayerEntity.class,
            TrackedDataHandlerRegistry.BYTE);

    boolean dropEntireStack;
    private DndCharacter dndClass;
    /** Age at the last Arrow Storm auto-release, for its fire rate. */
    @Unique
    private int dnd$lastStormShot = Integer.MIN_VALUE / 2;

    public PlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setDndClass(DndCharacter dndClass) {
        this.dndClass = dndClass;
    }

    public DndCharacter getDndClass() {
        return this.dndClass;
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void dnd$initRaceTracker(CallbackInfo info) {
        this.dataTracker.startTracking(DND$RACE, (byte) 0);
        this.dataTracker.startTracking(DND$BODY_RACE, (byte) 0);
    }

    @Override
    public DndRace getDndRace() {
        try {
            return DndRace.fromValue(this.dataTracker.get(DND$RACE) & 0x0F);
        } catch (IllegalArgumentException e) {
            return DndRace.NONE;
        }
    }

    @Override
    public DragonAncestry getDragonAncestry() {
        try {
            return DragonAncestry.fromValue((this.dataTracker.get(DND$RACE) >> 4) & 0x0F);
        } catch (IllegalArgumentException e) {
            return DragonAncestry.NONE;
        }
    }

    @Override
    public void setDndRace(DndRace race, DragonAncestry ancestry) {
        DndRace r = race == null ? DndRace.NONE : race;
        DragonAncestry a = ancestry == null || !r.hasAncestry() ? DragonAncestry.NONE : ancestry;
        this.dataTracker.set(DND$RACE, (byte) (r.getValue() | (a.getValue() << 4)));
        this.calculateDimensions();
    }

    @Override
    public DndRace getBodyRace() {
        try {
            return DndRace.fromValue(this.dataTracker.get(DND$BODY_RACE));
        } catch (IllegalArgumentException e) {
            return DndRace.NONE;
        }
    }

    @Override
    public void setBodyRace(DndRace race) {
        byte value = (byte) (race == null ? DndRace.NONE : race).getValue();
        if (this.dataTracker.get(DND$BODY_RACE) != value) {
            this.dataTracker.set(DND$BODY_RACE, value);
            this.calculateDimensions();
        }
    }

    // Save the class with the player so rejoining keeps it instead of reopening the picker.
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeDndClass(NbtCompound nbt, CallbackInfo info) {
        if (this.dndClass != null && this.dndClass != DndCharacter.NONE) {
            nbt.putInt(DND_CLASS_KEY, this.dndClass.getValue());
        }
        if (getDndRace() != DndRace.NONE) {
            nbt.putInt(DND_RACE_KEY, getDndRace().getValue());
            nbt.putInt(DND_ANCESTRY_KEY, getDragonAncestry().getValue());
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
        if (nbt.contains(DND_RACE_KEY, NbtElement.INT_TYPE)) {
            DndRace race;
            DragonAncestry ancestry;
            try {
                race = DndRace.fromValue(nbt.getInt(DND_RACE_KEY));
            } catch (IllegalArgumentException e) {
                race = DndRace.NONE;
            }
            try {
                ancestry = DragonAncestry.fromValue(nbt.getInt(DND_ANCESTRY_KEY));
            } catch (IllegalArgumentException e) {
                ancestry = DragonAncestry.NONE;
            }
            setDndRace(race, ancestry);
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
                if (isSubmergedIn(FluidTags.WATER) && !player.isCreative() && !player.isSpectator() &&
                        !player.getAbilities().flying) {
                    // Pull down, but keep walking along the bottom possible
                    this.setVelocity(this.getVelocity().x, -0.5, this.getVelocity().z);
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

            // Auto-release the bow once drawn, but no sooner than the rank's fire rate allows.
            if (progress >= 1.0F && this.age - this.dnd$lastStormShot >= RangerSkills
                    .arrowStormShotDelay((PlayerEntity) (Object) this)) {
                this.dnd$lastStormShot = this.age;
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