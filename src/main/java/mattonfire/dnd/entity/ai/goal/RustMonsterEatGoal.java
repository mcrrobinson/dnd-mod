package mattonfire.dnd.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.MobActions;
import mattonfire.dnd.entity.RustMonsterEntity;
import mattonfire.dnd.magic.Corrosion;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * The Rust Monster's hunger for metal: it seeks dropped items in {@code #dndclasses:rust_monster_food}
 * within {@link #SEARCH_RANGE} blocks, even mid-fight, walks over and eats one stack in
 * {@link #EAT_TICKS} ticks. It heals {@link RustMonsterEntity#HEAL_PER_ITEM} per item and is sated
 * ({@link RustMonsterEntity#sate}), so throwing it iron buys time.
 */
public class RustMonsterEatGoal extends Goal {
    public static final double SEARCH_RANGE = 12.0D;
    public static final int EAT_TICKS = 20;
    private static final double REACH_SQ = 2.0D * 2.0D;

    private final RustMonsterEntity mob;
    @Nullable
    private ItemEntity food;
    private int eatTicks;
    private int repathIn;

    public RustMonsterEatGoal(RustMonsterEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (this.mob.age % 5 != 0) {
            return false;
        }
        this.food = this.findFood();
        return this.food != null;
    }

    @Override
    public boolean shouldContinue() {
        return this.food != null && this.food.isAlive() && !this.food.getStack().isEmpty()
                && this.mob.squaredDistanceTo(this.food) <= SEARCH_RANGE * SEARCH_RANGE * 1.5D;
    }

    @Override
    public void start() {
        this.eatTicks = 0;
        this.repathIn = 0;
    }

    @Override
    public void stop() {
        this.food = null;
        this.eatTicks = 0;
        this.mob.setEating(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ItemEntity item = this.food;
        if (item == null) {
            return;
        }
        this.mob.getLookControl().lookAt(item.getX(), item.getY(), item.getZ());
        if (this.mob.squaredDistanceTo(item) > REACH_SQ) {
            this.mob.setEating(false);
            this.eatTicks = 0;
            if (--this.repathIn <= 0) {
                this.repathIn = 10;
                this.mob.getNavigation().startMovingTo(item, 1.2D);
            }
            return;
        }
        this.mob.getNavigation().stop();
        if (this.eatTicks == 0) {
            this.mob.setEating(true);
        }
        if (++this.eatTicks % 5 == 0) {
            this.mob.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.8F, 0.6F);
            ((ServerWorld) this.mob.world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, item.getStack()),
                    item.getX(), item.getY() + 0.2D, item.getZ(), 4, 0.15D, 0.1D, 0.15D, 0.05D);
        }
        if (this.eatTicks >= EAT_TICKS) {
            this.eat(item);
        }
    }

    private void eat(ItemEntity item) {
        ItemStack stack = item.getStack();
        int count = stack.getCount();
        String name = stack.getName().getString();
        DnDClasses.LOGGER.info("[Rust] eats {} {}", count, name);
        MobActions.log(this.mob, "eats " + count + " " + name, null);
        item.discard();
        this.mob.heal(RustMonsterEntity.HEAL_PER_ITEM * count);
        this.mob.playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.0F, 0.6F);
        this.mob.sate();
        this.food = null;
    }

    @Nullable
    private ItemEntity findFood() {
        List<ItemEntity> items = this.mob.world.getEntitiesByClass(ItemEntity.class,
                this.mob.getBoundingBox().expand(SEARCH_RANGE, 4.0D, SEARCH_RANGE),
                e -> e.isAlive() && e.getStack().isIn(Corrosion.RUST_MONSTER_FOOD) && this.mob.canSee(e));
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double dist = this.mob.squaredDistanceTo(item);
            if (dist < bestDist && dist <= SEARCH_RANGE * SEARCH_RANGE) {
                best = item;
                bestDist = dist;
            }
        }
        return best;
    }
}
