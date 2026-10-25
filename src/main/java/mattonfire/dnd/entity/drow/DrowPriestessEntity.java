package mattonfire.dnd.entity.drow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * A Drow Priestess of the Spider Queen: 40 health, 5 armour, a scourge (6 and Drow Poison). In a fight she:
 * <ul>
 * <li><b>Summons Spiders</b> every {@value #SUMMON_COOLDOWN} ticks: {@value #SUMMON_COUNT} cave spiders (tagged
 * {@value #SUMMON_TAG}) that fight her target, at most {@value #MAX_SUMMONS} alive. They vanish when she dies.</li>
 * <li>Casts <b>Healing Word</b> every {@value #HEAL_COOLDOWN} ticks: the most hurt drow within {@value #HEAL_RANGE}
 * blocks (herself too) heals {@value #HEAL_AMOUNT}.</li>
 * <li>Calls the <b>Dread of the Spider Queen</b> every {@value #DREAD_COOLDOWN} ticks: players within
 * {@value #DREAD_RANGE} blocks make a WIS save (DC {@value DrowSaves#DREAD_DC}) or are Frightened of her for
 * {@value DrowSaves#DREAD_TICKS} ticks.</li>
 * </ul>
 */
public class DrowPriestessEntity extends DrowEntity {
    public static final String SUMMON_TAG = "drow_summon";
    public static final int SUMMON_COOLDOWN = 300;
    public static final int SUMMON_COUNT = 2;
    public static final int MAX_SUMMONS = 4;
    public static final int HEAL_COOLDOWN = 200;
    public static final double HEAL_RANGE = 8.0D;
    public static final float HEAL_AMOUNT = 6.0F;
    public static final int DREAD_COOLDOWN = 360;
    public static final double DREAD_RANGE = 6.0D;

    private final List<UUID> summons = new ArrayList<>();
    private int summonCooldown = 20;
    private int healCooldown = HEAL_COOLDOWN / 2;
    private int dreadCooldown = 60;

    public DrowPriestessEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createDrowPriestessAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, true));
    }

    @Override
    protected void equip(Random random) {
        // The scourge: a length of chain.
        this.equipStack(EquipmentSlot.MAINHAND, blade(Items.CHAIN));
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            DrowPoison.afflict(living, this, DrowSaves.PRIESTESS_POISON);
        }
        return hit;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        if (this.summonCooldown > 0) this.summonCooldown--;
        if (this.healCooldown > 0) this.healCooldown--;
        if (this.dreadCooldown > 0) this.dreadCooldown--;
        if (this.age % 20 == 0) {
            this.directSummons(world);
        }
        if (this.healCooldown <= 0) {
            this.healingWord(world);
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.summonCooldown <= 0 && this.squaredDistanceTo(target) <= 24.0D * 24.0D) {
            this.summonSpiders(world, target);
        }
        if (this.dreadCooldown <= 0 && this.squaredDistanceTo(target) <= DREAD_RANGE * DREAD_RANGE) {
            this.dread(world);
        }
    }

    // ---- Summon Spiders ----

    /** Her living summons (dropping the dead and the lost). */
    public List<CaveSpiderEntity> livingSummons(ServerWorld world) {
        List<CaveSpiderEntity> alive = new ArrayList<>();
        this.summons.removeIf(uuid -> {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof CaveSpiderEntity spider && spider.isAlive()) {
                alive.add(spider);
                return false;
            }
            return true;
        });
        return alive;
    }

    private void summonSpiders(ServerWorld world, LivingEntity target) {
        this.summonCooldown = SUMMON_COOLDOWN;
        int room = MAX_SUMMONS - this.livingSummons(world).size();
        if (room <= 0) {
            return;
        }
        MobActions.log(this, "summon spiders", target);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.0F, 1.2F);
        for (int i = 0; i < Math.min(SUMMON_COUNT, room); i++) {
            CaveSpiderEntity spider = EntityType.CAVE_SPIDER.create(world);
            if (spider == null) {
                continue;
            }
            BlockPos pos = this.getBlockPos().add(this.random.nextInt(5) - 2, 0, this.random.nextInt(5) - 2);
            if (!world.isSpaceEmpty(spider, spider.getType().getDimensions().getBoxAt(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))) {
                pos = this.getBlockPos();
            }
            spider.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
            spider.initialize(world, world.getLocalDifficulty(pos), SpawnReason.MOB_SUMMONED, null, null);
            spider.addCommandTag(SUMMON_TAG);
            spider.setTarget(target);
            world.spawnEntityAndPassengers(spider);
            world.spawnParticles(ParticleTypes.SQUID_INK, spider.getX(), spider.getY() + 0.3D, spider.getZ(), 8, 0.3D, 0.2D, 0.3D, 0.01D);
            this.summons.add(spider.getUuid());
        }
        DnDClasses.LOGGER.info("[Drow] {} summons spiders ({} alive)", this.getName().getString(), this.livingSummons(world).size());
    }

    /** Her spiders fight what she fights. */
    private void directSummons(ServerWorld world) {
        LivingEntity target = this.getTarget();
        for (CaveSpiderEntity spider : this.livingSummons(world)) {
            if (target != null && target.isAlive() && spider.getTarget() != target) {
                spider.setTarget(target);
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (this.world instanceof ServerWorld world) {
            for (CaveSpiderEntity spider : this.livingSummons(world)) {
                world.spawnParticles(ParticleTypes.SMOKE, spider.getX(), spider.getY() + 0.3D, spider.getZ(), 8, 0.3D, 0.2D, 0.3D, 0.01D);
                spider.discard();
            }
            this.summons.clear();
        }
    }

    // ---- Healing Word ----

    private void healingWord(ServerWorld world) {
        DrowEntity hurt = world.getEntitiesByClass(DrowEntity.class, this.getBoundingBox().expand(HEAL_RANGE),
                        d -> d.isAlive() && d.getHealth() <= d.getMaxHealth() - 3.0F && (d == this || this.canSee(d)))
                .stream().min(Comparator.comparingDouble(d -> d.getHealth() / d.getMaxHealth())).orElse(null);
        if (hurt == null) {
            return;
        }
        this.healCooldown = HEAL_COOLDOWN;
        hurt.heal(HEAL_AMOUNT);
        MobActions.log(this, "healing word", hurt);
        world.playSound(null, hurt.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 0.8F, 1.4F);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, hurt.getX(), hurt.getBodyY(0.6D), hurt.getZ(), 8, 0.4D, 0.5D, 0.4D, 0.0D);
    }

    // ---- Dread of the Spider Queen ----

    private void dread(ServerWorld world) {
        this.dreadCooldown = DREAD_COOLDOWN;
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(DREAD_RANGE),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.squaredDistanceTo(this) <= DREAD_RANGE * DREAD_RANGE);
        if (players.isEmpty()) {
            return;
        }
        MobActions.log(this, "dread", this.getTarget());
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 1.5F, 0.5F);
        world.spawnParticles(ParticleTypes.SQUID_INK, this.getX(), this.getBodyY(0.5D), this.getZ(), 30, DREAD_RANGE * 0.3D, 0.4D, DREAD_RANGE * 0.3D, 0.05D);
        for (PlayerEntity player : players) {
            if (Conditions.isImmune(player, Condition.FRIGHTENED, this)) {
                continue;
            }
            if (SavingThrow.of(player, DrowSaves.DREAD).source(this).tags("fear")
                    .onFailure(Text.translatable("save.dndclasses.effect.drow_dread.failed")).roll().failed()) {
                Conditions.of(player, Condition.FRIGHTENED).source(this).ticks(DrowSaves.DREAD_TICKS).apply();
            }
        }
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtList list = new NbtList();
        for (UUID uuid : this.summons) {
            list.add(NbtHelper.fromUuid(uuid));
        }
        nbt.put("Summons", list);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.summons.clear();
        for (var element : nbt.getList("Summons", 11)) {
            this.summons.add(NbtHelper.toUuid(element));
        }
    }
}
