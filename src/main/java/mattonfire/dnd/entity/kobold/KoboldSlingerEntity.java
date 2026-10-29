package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.MobActions;
import mattonfire.dnd.entity.SlingStoneEntity;
import mattonfire.dnd.entity.kobold.ai.KoboldSlingGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Kobold Slinger: 7 health, 1 armour, a dagger (2). It slings a stone ({@value SlingStoneEntity#KOBOLD_DAMAGE}
 * damage) at 6-16 blocks every {@value #COOLDOWN} (+-{@value #COOLDOWN_JITTER}) ticks, and slingers loose together
 * on a shout ({@link KoboldVolleys}). In bright sun 30% of its stones fly wide.
 */
public class KoboldSlingerEntity extends KoboldEntity {
    public static final int COOLDOWN = 30;
    public static final int COOLDOWN_JITTER = 5;
    /** Chance a sunlit slinger's stone flies wide (4x the spread). */
    public static final float SUN_MISS_CHANCE = 0.3F;
    private static final float SPEED = 1.6F;
    private static final float SPREAD = 2.0F;

    /** Ticks until it's ready to sling again. */
    private int slingCooldown;
    /** A volley shot waiting to fly: the age it fires at (-1 = none) and at what. */
    private int volleyAt = -1;
    @Nullable
    private LivingEntity volleyTarget;

    public KoboldSlingerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return koboldAttributes(7.0D, 1.0D, 0.33D, 2.0D);
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new KoboldSlingGoal(this));
    }

    /** Ready to sling, with no volley shot already waiting. */
    public boolean isReady() {
        return this.slingCooldown <= 0 && this.volleyAt < 0;
    }

    /** No volley shot already waiting (a slinger joins one volley at a time). */
    public boolean isReadyToJoin() {
        return this.volleyAt < 0;
    }

    /** Ticks until ready (0 when it is). */
    public int cooldownLeft() {
        return Math.max(0, this.slingCooldown);
    }

    /** Joins a volley: slings at {@code target} {@code delay} ticks from now. */
    void scheduleShot(LivingEntity target, int delay) {
        this.volleyTarget = target;
        this.volleyAt = this.age + delay;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.slingCooldown > 0) {
            this.slingCooldown--;
        }
        if (this.volleyAt >= 0 && this.age >= this.volleyAt) {
            LivingEntity target = this.volleyTarget;
            this.volleyAt = -1;
            this.volleyTarget = null;
            if (target != null && target.isAlive()) {
                this.sling(target);
            }
        }
    }

    /** Slings one stone at {@code target} and restarts the cooldown. */
    public void sling(LivingEntity target) {
        this.slingCooldown = COOLDOWN - COOLDOWN_JITTER + this.random.nextInt(COOLDOWN_JITTER * 2 + 1);
        SlingStoneEntity stone = new SlingStoneEntity(this.world, this, SlingStoneEntity.KOBOLD_DAMAGE);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.5D) - stone.getY();
        double dz = target.getZ() - this.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean wide = KoboldCourage.isSunlit(this) && this.random.nextFloat() < SUN_MISS_CHANCE;
        stone.setVelocity(dx, dy + flat * 0.13D, dz, SPEED, wide ? SPREAD * 4.0F : SPREAD);
        this.world.spawnEntity(stone);
        this.triggerAnim("action_controller", "sling");
        this.playSound(SoundEvents.ENTITY_SNOWBALL_THROW, 0.8F, 0.6F + this.random.nextFloat() * 0.2F);
        MobActions.log(this, wide ? "sling (wide)" : "sling", target);
        DnDClasses.LOGGER.info("[Kobold] {} slings at {} (t={}{})", this.getName().getString(),
                target.getName().getString(), this.world.getTime(), wide ? ", wide" : "");
        this.setYaw((float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
    }
}
