package mattonfire.dnd.entity.drow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.MobActions;
import mattonfire.dnd.entity.StandingRoom;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.entity.boss.BossMinions;
import mattonfire.dnd.magic.MagicMissiles;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Matron Mother (design 9, 8.3; docs/bosses/matron-mother.md): the Underdark's first boss, high priestess of a
 * drow house, in the Sanctum at the top of the Drow Spire. 220 health, 10 armour, a scourge (8 and Drow Poison,
 * CON {@value DrowSaves#MATRON_POISON_DC}).
 * <ul>
 * <li><b>Spider Swarm</b> every {@value #SWARM_COOLDOWN} ticks: {@value #SWARM_COUNT} cave spiders, at most
 * {@value #MAX_SPIDERS} alive (boss minions: no loot; they vanish when she dies).</li>
 * <li><b>Darkness</b> every {@value #DARKNESS_COOLDOWN} ticks: as the mage's ({@link DrowDarkness}), radius
 * {@value #DARKNESS_RADIUS}, round her target.</li>
 * <li><b>Lash of the Spider Queen</b> every {@value #LASH_COOLDOWN} ticks: players within {@value #LASH_RANGE} blocks
 * make a WIS save (DC {@value DrowSaves#MATRON_LASH_DC}) or are Frightened for {@value DrowSaves#MATRON_LASH_TICKS}
 * ticks.</li>
 * <li><b>Phase 50%: Web Storm</b>, at once and then every {@value #WEB_STORM_COOLDOWN} ticks: every player within
 * {@value #WEB_STORM_RANGE} blocks makes a DEX save (DC {@value DrowSaves#WEB_STORM_DC}); a failure leaves a cobweb at
 * their feet (for {@value #WEB_TICKS} ticks) and Restrains them for {@value #WEB_RESTRAIN_TICKS} ticks. She rises
 * {@value #WEB_STORM_RISE} blocks and throws {@value #STORM_DARTS} Magic Missiles a second for 3 s.</li>
 * <li><b>Phase 25%: Call the Elites</b>, once: every Drow Elite within {@value #CALL_RANGE} blocks (the Spire) gets
 * Strength I and runs to her Sanctum ({@link DrowEliteEntity#answerCall}).</li>
 * </ul>
 * She keeps to her Sanctum: home is where she was placed (before her throne), she idles within {@value #WANDER_RADIUS}
 * blocks of it, and drawn more than {@value #HOME_RADIUS} blocks away she steps through the shadows back home.
 */
public class DrowMatronEntity extends DrowEntity implements Boss {
    public static final Identifier ADVANCEMENT = new Identifier(DnDClasses.MOD_ID, "underdark/matron");
    public static final int XP = 80;
    /** Drawn further than this from home, she steps back through the shadows. */
    public static final int HOME_RADIUS = 14;
    /** She idles this close to home. */
    public static final int WANDER_RADIUS = 4;
    public static final double SPEECH_RANGE = 48.0D;

    public static final int SWARM_COOLDOWN = 300;
    public static final int SWARM_COUNT = 2;
    public static final int MAX_SPIDERS = 6;
    public static final int DARKNESS_COOLDOWN = 440;
    public static final double DARKNESS_RADIUS = 6.0D;
    public static final int LASH_COOLDOWN = 360;
    public static final double LASH_RANGE = 6.0D;
    public static final int WEB_STORM_COOLDOWN = 240;
    public static final double WEB_STORM_RANGE = 16.0D;
    /** How long the storm lasts (she hovers and throws missiles). */
    public static final int WEB_STORM_TICKS = 60;
    public static final double WEB_STORM_RISE = 3.0D;
    public static final int WEB_RESTRAIN_TICKS = 80;
    /** How long a Web Storm's cobweb stays. */
    public static final int WEB_TICKS = 200;
    public static final int STORM_DARTS = 3;
    public static final float STORM_DART_DAMAGE = 2.0F;
    public static final double CALL_RANGE = 48.0D;
    /** Ticks after one spell before the next. */
    private static final int GLOBAL_COOLDOWN = 20;
    public static final String WEB_KEY = "web_storm";

    private static final String[] MATRONS = {
            "Quenthel", "Triel", "Yvonnel", "Malice", "SiNafay", "Mez'Barris", "Zeerith", "Ghenni", "Miz'ri", "Ulviirala",
            "Byrtyn", "Lirdnolu"
    };

    private record Web(BlockPos pos, long until) {
    }

    private final BossFight bossFight;
    private final List<UUID> summons = new ArrayList<>();
    private final List<Web> webs = new ArrayList<>();
    @Nullable
    private BlockPos home;
    private int swarmCooldown = 40;
    private int darknessCooldown = 100;
    private int lashCooldown = 80;
    private int webStormCooldown;
    private int globalCooldown;
    private int stormTicks;
    private double stormBaseY;
    /** She lost her gravity to the storm (and gets it back when it ends). */
    private boolean floating;
    private boolean greeted;

    public DrowMatronEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.bossFight = new BossFight(this, BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10)
                .range(SPEECH_RANGE)
                .music(ModSounds.MUSIC_MATRON_FIGHT)
                .phase(0.5F, BossBar.Color.PINK, this::enterWebStorm)
                .phase(0.25F, BossBar.Color.RED, this::callTheElites)
                .xp(XP)
                .advancement(ADVANCEMENT);
    }

    public static DefaultAttributeContainer.Builder createDrowMatronAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 220.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public BossFight getBossFight() {
        return this.bossFight;
    }

    @Override
    protected void initCombatGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1D, true));
    }

    @Override
    protected void equip(Random random) {
        // Her scourge: a length of chain, like her priestesses'.
        this.equipStack(EquipmentSlot.MAINHAND, blade(Items.CHAIN));
    }

    @Override
    protected Text drowName(Random random) {
        return Text.translatable("entity.dndclasses.drow_matron.named", MATRONS[random.nextInt(MATRONS.length)],
                house(random));
    }

    @Override
    public net.minecraft.entity.EntityData initialize(net.minecraft.world.ServerWorldAccess world,
            net.minecraft.world.LocalDifficulty difficulty, SpawnReason spawnReason,
            @Nullable net.minecraft.entity.EntityData entityData, @Nullable NbtCompound entityNbt) {
        net.minecraft.entity.EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.setPersistent();
        return data;
    }

    // ---- Home ----

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    /** Her Sanctum: she idles within {@value #WANDER_RADIUS} blocks of it and never strays {@value #HOME_RADIUS}. */
    public void setHome(BlockPos home) {
        this.home = home.toImmutable();
        this.setPositionTarget(this.home, WANDER_RADIUS);
    }

    // ---- Talking ----

    private void speak(String line) {
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        Text text = Text.translatable("chat.dndclasses.drow_matron.speech", this.getDisplayName(),
                Text.translatable("entity.dndclasses.drow_matron.say." + line).formatted(Formatting.ITALIC))
                .formatted(Formatting.DARK_PURPLE);
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(this) < SPEECH_RANGE * SPEECH_RANGE)) {
            player.sendMessage(text, false);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity before = this.getTarget();
        super.setTarget(target);
        if (!this.world.isClient && !this.greeted && target instanceof PlayerEntity && this.getTarget() == target
                && target != before) {
            this.greeted = true;
            this.speak("greet");
        }
    }

    // ---- Ticking ----

    @Override
    public void tick() {
        super.tick();
        this.bossFight.tick();
        if (this.world instanceof ServerWorld world && this.isAlive()) {
            if (this.home == null) {
                this.setHome(this.getBlockPos());
            }
            if (this.age % 10 == 0) {
                this.clearOldWebs(world, false);
            }
            if (this.stormTicks > 0) {
                // From the tick, not the AI, so the storm runs its course whatever she's doing.
                this.tickStorm(world);
            }
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        if (this.swarmCooldown > 0) this.swarmCooldown--;
        if (this.darknessCooldown > 0) this.darknessCooldown--;
        if (this.lashCooldown > 0) this.lashCooldown--;
        if (this.webStormCooldown > 0) this.webStormCooldown--;
        if (this.globalCooldown > 0) this.globalCooldown--;
        if (this.age % 20 == 0) {
            this.directSummons(world);
            this.leash(world);
        }
        if (this.stormTicks > 0) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.globalCooldown > 0 || !Conditions.canAct(this)) {
            return;
        }
        double distance = this.squaredDistanceTo(target);
        if (this.bossFight.getPhase() >= 1 && this.webStormCooldown <= 0
                && distance <= WEB_STORM_RANGE * WEB_STORM_RANGE) {
            this.webStorm(world, target);
        } else if (this.swarmCooldown <= 0 && distance <= 24.0D * 24.0D) {
            this.swarm(world, target);
        } else if (this.lashCooldown <= 0 && distance <= LASH_RANGE * LASH_RANGE) {
            this.lash(world, target);
        } else if (this.darknessCooldown <= 0 && distance <= 16.0D * 16.0D && this.getVisibilityCache().canSee(target)) {
            this.darkness(world, target);
        }
    }

    /** Dragged too far from her Sanctum, she steps through the shadows back home. */
    private void leash(ServerWorld world) {
        if (this.home == null || this.home.isWithinDistance(this.getPos(), HOME_RADIUS)) {
            return;
        }
        world.spawnParticles(ParticleTypes.SQUID_INK, this.getX(), this.getBodyY(0.5D), this.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.05D);
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        if (!StandingRoom.around(world, this, this.random, this.home.getX() + 0.5D, this.home.getY(),
                this.home.getZ() + 0.5D, 0.0D, 3.0D, 12, 2, 3, true)) {
            this.refreshPositionAndAngles(x, y, z, this.getYaw(), this.getPitch());
            return;
        }
        this.getNavigation().stop();
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0F, 0.6F);
        world.spawnParticles(ParticleTypes.SQUID_INK, this.getX(), this.getBodyY(0.5D), this.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.05D);
        MobActions.log(this, "shadow step home", this.getTarget());
    }

    // ---- Melee: the scourge ----

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            DrowPoison.afflict(living, this, DrowSaves.MATRON_POISON);
        }
        return hit;
    }

    // ---- Spider Swarm ----

    /** Her living spiders (dropping the dead and the lost). */
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

    private void swarm(ServerWorld world, LivingEntity target) {
        this.swarmCooldown = SWARM_COOLDOWN;
        int room = MAX_SPIDERS - this.livingSummons(world).size();
        if (room <= 0) {
            return;
        }
        this.globalCooldown = GLOBAL_COOLDOWN;
        MobActions.log(this, "spider swarm", target);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.2F, 0.9F);
        for (int i = 0; i < Math.min(SWARM_COUNT, room); i++) {
            CaveSpiderEntity spider = EntityType.CAVE_SPIDER.create(world);
            if (spider == null) {
                continue;
            }
            if (!StandingRoom.around(world, spider, this.random, this.getX(), this.getY(), this.getZ(), 1.5D, 3.5D, 8, 2,
                    2, true)) {
                spider.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            }
            spider.initialize(world, world.getLocalDifficulty(spider.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            spider.addCommandTag(DrowPriestessEntity.SUMMON_TAG);
            BossMinions.mark(spider);
            spider.setTarget(target);
            world.spawnEntityAndPassengers(spider);
            world.spawnParticles(ParticleTypes.SQUID_INK, spider.getX(), spider.getY() + 0.3D, spider.getZ(), 8, 0.3D, 0.2D, 0.3D, 0.01D);
            this.summons.add(spider.getUuid());
        }
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

    // ---- Darkness and the Lash ----

    private void darkness(ServerWorld world, LivingEntity target) {
        this.darknessCooldown = DARKNESS_COOLDOWN;
        this.globalCooldown = GLOBAL_COOLDOWN;
        MobActions.log(this, "darkness", target);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1.2F, 0.6F);
        DrowDarkness.cast(world, target.getPos().add(0.0D, 1.0D, 0.0D), DARKNESS_RADIUS, this);
    }

    private void lash(ServerWorld world, LivingEntity target) {
        this.lashCooldown = LASH_COOLDOWN;
        this.globalCooldown = GLOBAL_COOLDOWN;
        MobActions.log(this, "lash of the spider queen", target);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 2.0F, 0.4F);
        world.spawnParticles(ParticleTypes.SQUID_INK, this.getX(), this.getBodyY(0.5D), this.getZ(), 40, LASH_RANGE * 0.3D,
                0.4D, LASH_RANGE * 0.3D, 0.05D);
        for (PlayerEntity player : this.playersWithin(world, LASH_RANGE)) {
            if (Conditions.isImmune(player, Condition.FRIGHTENED, this)) {
                continue;
            }
            if (SavingThrow.of(player, DrowSaves.MATRON_LASH).source(this).tags("fear")
                    .onFailure(Text.translatable("save.dndclasses.effect.matron_lash.failed")).roll().failed()) {
                Conditions.of(player, Condition.FRIGHTENED).source(this).ticks(DrowSaves.MATRON_LASH_TICKS).apply();
            }
        }
    }

    private List<PlayerEntity> playersWithin(ServerWorld world, double range) {
        return world.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(range),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.squaredDistanceTo(this) <= range * range);
    }

    // ---- Phase 1: the Web Storm ----

    private void enterWebStorm() {
        this.speak("web_storm");
        MobActions.log(this, "phase: web storm", this.getTarget());
        if (this.world instanceof ServerWorld world) {
            this.webStorm(world, this.getTarget());
        }
    }

    private void webStorm(ServerWorld world, @Nullable LivingEntity target) {
        this.webStormCooldown = WEB_STORM_COOLDOWN;
        this.globalCooldown = GLOBAL_COOLDOWN;
        MobActions.log(this, "web storm", target);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5F, 0.7F);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 1.5F, 0.6F);
        world.spawnParticles(ParticleTypes.CLOUD, this.getX(), this.getBodyY(0.8D), this.getZ(), 60, 4.0D, 1.5D, 4.0D, 0.05D);
        for (PlayerEntity player : this.playersWithin(world, WEB_STORM_RANGE)) {
            SaveResult result = SavingThrow.of(player, DrowSaves.WEB_STORM).source(this).exposure(this, WEB_KEY, 20)
                    .tags("web")
                    .onSuccess(Text.translatable("save.dndclasses.effect.web_storm.saved"))
                    .onFailure(Text.translatable("save.dndclasses.effect.web_storm.failed"))
                    .roll();
            if (result.failed()) {
                BlockPos web = this.ensnare(world, player);
                DnDClasses.LOGGER.info("[DrowMatron] Web Storm: {} failed the DEX save, webbed at {}",
                        player.getName().getString(), web == null ? "-" : web.toShortString());
            } else {
                DnDClasses.LOGGER.info("[DrowMatron] Web Storm: {} saved, no web", player.getName().getString());
            }
        }
        // She rises and rains Magic Missiles down for the rest of the storm.
        this.stormTicks = WEB_STORM_TICKS;
        this.stormBaseY = this.getY();
        if (!this.hasNoGravity()) {
            this.setNoGravity(true);
            this.floating = true;
        }
    }

    /** A cobweb at the player's feet (if there's room) and Restrained. Returns where the web went, or null. */
    @Nullable
    private BlockPos ensnare(ServerWorld world, PlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        BlockPos placed = null;
        if (world.getBlockState(feet).isAir()) {
            world.setBlockState(feet, Blocks.COBWEB.getDefaultState());
            this.webs.add(new Web(feet.toImmutable(), world.getTime() + WEB_TICKS));
            placed = feet.toImmutable();
        }
        if (!Conditions.isImmune(player, Condition.RESTRAINED, this)) {
            Conditions.of(player, Condition.RESTRAINED).source(this).key(WEB_KEY).ticks(WEB_RESTRAIN_TICKS).apply();
        }
        world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3D, player.getZ(), 12, 0.3D, 0.2D, 0.3D, 0.01D);
        return placed;
    }

    private void tickStorm(ServerWorld world) {
        this.stormTicks--;
        double up = this.stormBaseY + WEB_STORM_RISE - this.getY();
        this.setVelocity(this.getVelocity().x * 0.5D, up > 0.05D ? Math.min(0.3D, up * 0.5D) : 0.0D,
                this.getVelocity().z * 0.5D);
        this.velocityDirty = true;
        if (this.stormTicks % 20 == 10) {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) {
                List<PlayerEntity> players = this.playersWithin(world, WEB_STORM_RANGE);
                target = players.isEmpty() ? null : players.get(this.random.nextInt(players.size()));
            }
            if (target != null) {
                MobActions.log(this, "storm missiles", target);
                this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                MagicMissiles.fire(world, this, target, STORM_DARTS, STORM_DART_DAMAGE);
            }
        }
        if (this.stormTicks <= 0) {
            this.land();
        }
    }

    private void land() {
        this.stormTicks = 0;
        if (this.floating) {
            this.floating = false;
            this.setNoGravity(false);
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, false, false));
        }
    }

    /** Removes the Web Storm's cobwebs that have lasted their time (or all of them). */
    private void clearOldWebs(ServerWorld world, boolean all) {
        long now = world.getTime();
        this.webs.removeIf(web -> {
            if (!all && now < web.until()) {
                return false;
            }
            if (world.isChunkLoaded(web.pos()) && world.getBlockState(web.pos()).isOf(Blocks.COBWEB)) {
                world.removeBlock(web.pos(), false);
            }
            return world.isChunkLoaded(web.pos()) || all;
        });
    }

    /** How many of the Web Storm's cobwebs are still standing (tests). */
    public int webCount() {
        return this.webs.size();
    }

    // ---- Phase 2: Call the Elites ----

    private void callTheElites() {
        this.speak("elites");
        LivingEntity foe = this.getTarget() instanceof PlayerEntity ? this.getTarget() : this.getAttacker();
        MobActions.log(this, "call the elites", foe);
        if (!(this.world instanceof ServerWorld world)) {
            return;
        }
        BlockPos sanctum = this.home != null ? this.home : this.getBlockPos();
        List<DrowEliteEntity> elites = world.getEntitiesByClass(DrowEliteEntity.class,
                this.getBoundingBox().expand(CALL_RANGE), e -> e.isAlive() && e.squaredDistanceTo(this) <= CALL_RANGE * CALL_RANGE);
        for (DrowEliteEntity elite : elites) {
            elite.answerCall(sanctum, foe);
        }
        world.playSound(null, this.getBlockPos(), SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 2.0F, 1.4F);
        DnDClasses.LOGGER.info("[DrowMatron] {} calls the elites: {} answer", this.getName().getString(), elites.size());
    }

    // ---- Death and removal ----

    @Override
    public void onDeath(DamageSource source) {
        if (!this.world.isClient && !this.dead && !this.isRemoved()) {
            this.bossFight.onDeath();
            this.speak("death");
        }
        super.onDeath(source);
        if (this.world instanceof ServerWorld world) {
            for (CaveSpiderEntity spider : this.livingSummons(world)) {
                world.spawnParticles(ParticleTypes.SMOKE, spider.getX(), spider.getY() + 0.3D, spider.getZ(), 8, 0.3D, 0.2D, 0.3D, 0.01D);
                spider.discard();
            }
            this.summons.clear();
            this.clearOldWebs(world, true);
            this.land();
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossFight.onStoppedTrackingBy(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && this.world instanceof ServerWorld world) {
            this.clearOldWebs(world, true);
        }
        super.remove(reason);
        this.bossFight.onRemoved();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    // ---- Saving ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.bossFight.writeNbt(nbt);
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
        nbt.putBoolean("Greeted", this.greeted);
        nbt.putBoolean("StormFloating", this.floating);
        NbtList summons = new NbtList();
        for (UUID uuid : this.summons) {
            summons.add(NbtHelper.fromUuid(uuid));
        }
        nbt.put("Summons", summons);
        NbtList webs = new NbtList();
        for (Web web : this.webs) {
            NbtCompound entry = NbtHelper.fromBlockPos(web.pos());
            entry.putLong("Until", web.until());
            webs.add(entry);
        }
        nbt.put("Webs", webs);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.bossFight.readNbt(nbt);
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
        this.greeted = nbt.getBoolean("Greeted");
        if (nbt.getBoolean("StormFloating")) {
            // Saved mid-storm: the storm is over, so she gets her gravity back.
            this.setNoGravity(false);
        }
        this.floating = false;
        this.summons.clear();
        for (var element : nbt.getList("Summons", 11)) {
            this.summons.add(NbtHelper.toUuid(element));
        }
        this.webs.clear();
        for (var element : nbt.getList("Webs", 10)) {
            NbtCompound entry = (NbtCompound) element;
            this.webs.add(new Web(NbtHelper.toBlockPos(entry), entry.getLong("Until")));
        }
    }
}
