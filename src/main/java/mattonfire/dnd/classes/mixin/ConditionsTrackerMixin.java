package mattonfire.dnd.classes.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionHolder;
import mattonfire.dnd.classes.Conditions.ConditionInstance;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.World;

/**
 * Conditions on every living entity: the synced mask ({@code DND$CONDITIONS}, read by {@link Conditions#has} on
 * both sides), the server's live instance list, and the movement rules that need both sides (no jumping while
 * rooted or Prone, no sprinting while Prone). Petrified creatures don't heal and can't be poisoned.
 */
@Mixin(LivingEntity.class)
public abstract class ConditionsTrackerMixin extends Entity implements ConditionHolder {
    @Unique
    private static final TrackedData<Integer> DND$CONDITIONS = DataTracker.registerData(LivingEntity.class,
            TrackedDataHandlerRegistry.INTEGER);

    @Unique
    private final List<ConditionInstance> dnd$conditions = new ArrayList<>();
    @Unique
    private boolean dnd$conditionsLoaded;

    public ConditionsTrackerMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void dnd$trackConditions(CallbackInfo ci) {
        this.dataTracker.startTracking(DND$CONDITIONS, 0);
    }

    @Override
    public int dnd$getConditionMask() {
        return this.dataTracker.get(DND$CONDITIONS);
    }

    @Override
    public void dnd$setConditionMask(int mask) {
        this.dataTracker.set(DND$CONDITIONS, mask);
    }

    @Override
    public List<ConditionInstance> dnd$conditions() {
        return this.dnd$conditions;
    }

    @Override
    public boolean dnd$conditionsLoaded() {
        return this.dnd$conditionsLoaded;
    }

    @Override
    public void dnd$setConditionsLoaded(boolean loaded) {
        this.dnd$conditionsLoaded = loaded;
    }

    /** Rooted (Restrained, Stunned, Paralyzed, Petrified) and Prone creatures can't jump. */
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void dnd$noConditionJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!Conditions.canMove(self) || Conditions.has(self, Condition.PRONE)) {
            ci.cancel();
        }
    }

    /** Prone creatures can't sprint. */
    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
    private boolean dnd$noProneSprint(boolean sprinting) {
        LivingEntity self = (LivingEntity) (Object) this;
        return sprinting && !Conditions.has(self, Condition.PRONE) && Conditions.canMove(self);
    }

    /** Stone doesn't heal. */
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void dnd$noPetrifiedHealing(float amount, CallbackInfo ci) {
        if (Conditions.has((LivingEntity) (Object) this, Condition.PETRIFIED)) {
            ci.cancel();
        }
    }

    /** Stone can't be poisoned. */
    @Inject(method = "canHaveStatusEffect", at = @At("HEAD"), cancellable = true)
    private void dnd$petrifiedNoPoison(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffectType() == StatusEffects.POISON
                && Conditions.has((LivingEntity) (Object) this, Condition.PETRIFIED)) {
            cir.setReturnValue(false);
        }
    }
}
