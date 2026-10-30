package mattonfire.dnd.entity.dragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

/**
 * The Ancient Red's lair actions (design 10, 5.8). Damage is {@code dndclasses:lair_action}, credited to the
 * dragon.
 * <ul>
 * <li><b>Magma Vents</b>: under up to {@value #VENT_TARGETS} players, {@value #VENT_WARNING} ticks of warning
 * (smoke, dripping lava, a glowing ring and a rumble), then a vent erupts {@value #VENT_RADIUS} blocks wide and
 * {@value #VENT_HEIGHT} high. DEX {@value AncientSaves#MAGMA_VENTS_DC}: {@value #VENT_DAMAGE} damage and
 * {@value #VENT_BURN_SECONDS} s alight, half and {@value #VENT_BURN_SAVED_SECONDS} s on a success. With
 * mobGriefing the stone at its centre turns to magma.</li>
 * <li><b>Tremor</b>: the lair shakes for {@value #TREMOR_TICKS} ticks and rocks fall around everyone on the
 * ground. DEX {@value AncientSaves#TREMOR_DC}: {@value #TREMOR_DAMAGE} damage and Prone for
 * {@value #TREMOR_PRONE_TICKS} ticks on a failure, nothing on a success.</li>
 * <li><b>Volcanic Gas</b>: {@value #GAS_CLOUDS} clouds ({@value #GAS_RADIUS} blocks, {@value #GAS_TICKS} ticks)
 * within {@value #GAS_SPREAD} blocks of random players. CON {@value AncientSaves#VOLCANIC_GAS_DC} on entering,
 * once per cloud: Poisoned for {@value #GAS_POISON_TICKS} ticks on a failure.</li>
 * </ul>
 */
public final class AncientLairActions {
    public static final int VENT_TARGETS = 4;
    public static final int VENT_WARNING = 30;
    public static final double VENT_RADIUS = 2.5;
    public static final double VENT_HEIGHT = 6.0;
    public static final float VENT_DAMAGE = 10.0F;
    public static final int VENT_BURN_SECONDS = 5;
    public static final int VENT_BURN_SAVED_SECONDS = 2;

    public static final int TREMOR_TICKS = 40;
    public static final float TREMOR_DAMAGE = 4.0F;
    public static final int TREMOR_PRONE_TICKS = 60;

    public static final int GAS_CLOUDS = 3;
    public static final double GAS_RADIUS = 4.0;
    public static final int GAS_TICKS = 160;
    public static final double GAS_SPREAD = 8.0;
    public static final int GAS_POISON_TICKS = 120;

    public static final LairAction MAGMA_VENTS = new LairAction() {
        @Override
        public String id() {
            return "magma_vents";
        }

        @Override
        public void run(AncientDragonEntity dragon, ServerWorld world, List<ServerPlayerEntity> inLair) {
            List<ServerPlayerEntity> targets = new ArrayList<>(inLair);
            Collections.shuffle(targets, new java.util.Random(dragon.getRandom().nextLong()));
            for (ServerPlayerEntity player : targets.subList(0, Math.min(VENT_TARGETS, targets.size()))) {
                dragon.schedule(new Vent(dragon, ground(world, player.getBlockPos())));
            }
        }
    };

    public static final LairAction TREMOR = new LairAction() {
        @Override
        public String id() {
            return "tremor";
        }

        @Override
        public void run(AncientDragonEntity dragon, ServerWorld world, List<ServerPlayerEntity> inLair) {
            dragon.schedule(new Tremor(dragon, inLair));
            AncientDragonEvents.TREMOR.invoker().onTremor(dragon, world);
        }
    };

    public static final LairAction VOLCANIC_GAS = new LairAction() {
        @Override
        public String id() {
            return "volcanic_gas";
        }

        @Override
        public void run(AncientDragonEntity dragon, ServerWorld world, List<ServerPlayerEntity> inLair) {
            if (inLair.isEmpty()) {
                return;
            }
            for (int i = 0; i < GAS_CLOUDS; i++) {
                ServerPlayerEntity player = inLair.get(dragon.getRandom().nextInt(inLair.size()));
                double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
                double distance = dragon.getRandom().nextDouble() * GAS_SPREAD;
                BlockPos at = BlockPos.ofFloored(player.getX() + Math.cos(angle) * distance, player.getY(),
                        player.getZ() + Math.sin(angle) * distance);
                dragon.schedule(new GasCloud(dragon, Vec3d.ofBottomCenter(ground(world, at)).add(0, 1.0, 0)));
            }
        }
    };

    public static final List<LairAction> RED = List.of(MAGMA_VENTS, TREMOR, VOLCANIC_GAS);

    private AncientLairActions() {
    }

    /** The first block with something solid under it, from {@code pos} down up to 8 blocks. */
    static BlockPos ground(ServerWorld world, BlockPos pos) {
        BlockPos.Mutable p = pos.mutableCopy();
        for (int i = 0; i < 8; i++) {
            if (!world.getBlockState(p.down()).getCollisionShape(world, p.down()).isEmpty()) {
                return p.toImmutable();
            }
            p.move(0, -1, 0);
        }
        return pos;
    }

    private static DamageSource damage(AncientDragonEntity dragon) {
        return ModDamageTypes.of(dragon.getWorld(), ModDamageTypes.LAIR_ACTION, dragon);
    }

    private static boolean canHurt(AncientDragonEntity dragon, LivingEntity e) {
        return e.isAlive() && e != dragon && !dragon.isAlly(e)
                && !(e instanceof ServerPlayerEntity p && (p.isCreative() || p.isSpectator()));
    }

    /** One magma vent: a warning, then the eruption. */
    private static final class Vent implements LairAction.Effect {
        private final AncientDragonEntity dragon;
        private final BlockPos pos;
        private int age;

        Vent(AncientDragonEntity dragon, BlockPos pos) {
            this.dragon = dragon;
            this.pos = pos;
        }

        @Override
        public boolean tick(ServerWorld world) {
            this.age++;
            double x = this.pos.getX() + 0.5, y = this.pos.getY() + 0.1, z = this.pos.getZ() + 0.5;
            if (this.age == 1) {
                world.playSound(null, this.pos, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.HOSTILE, 2.0F, 0.5F);
                world.playSound(null, this.pos, SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value(), SoundCategory.HOSTILE,
                        1.5F, 0.6F);
            }
            if (this.age < VENT_WARNING) {
                if (this.age % 2 == 0) {
                    // The ground glows: a ring of embers where the vent will open
                    for (int i = 0; i < 16; i++) {
                        float a = i / 16.0F * MathHelper.TAU;
                        world.spawnParticles(ParticleTypes.SMALL_FLAME, x + MathHelper.cos(a) * VENT_RADIUS, y,
                                z + MathHelper.sin(a) * VENT_RADIUS, 1, 0.05, 0.0, 0.05, 0.0);
                    }
                    world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.8, 0.1, 0.8, 0.01);
                    world.spawnParticles(ParticleTypes.DRIPPING_LAVA, x, y + 0.2, z, 2, 0.6, 0.1, 0.6, 0.0);
                }
                return false;
            }
            this.erupt(world, x, y, z);
            return true;
        }

        private void erupt(ServerWorld world, double x, double y, double z) {
            world.playSound(null, this.pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5F, 0.6F);
            world.playSound(null, this.pos, SoundEvents.BLOCK_LAVA_POP, SoundCategory.HOSTILE, 2.0F, 0.5F);
            for (int h = 0; h < 12; h++) {
                world.spawnParticles(ParticleTypes.FLAME, x, y + h * 0.5, z, 10, 0.6, 0.2, 0.6, 0.02);
            }
            world.spawnParticles(ParticleTypes.LAVA, x, y + 0.5, z, 30, 1.2, 1.0, 1.2, 0.0);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 3.0, z, 20, 1.0, 2.0, 1.0, 0.02);
            MobActions.log(this.dragon, "magma_vent", null);

            Box area = new Box(x - VENT_RADIUS, y - 0.5, z - VENT_RADIUS, x + VENT_RADIUS, y + VENT_HEIGHT, z + VENT_RADIUS);
            for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                    e -> canHurt(this.dragon, e) && MathHelper.square(e.getX() - x) + MathHelper.square(e.getZ() - z)
                            <= VENT_RADIUS * VENT_RADIUS)) {
                if (victim.isFireImmune() || victim.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                    continue;
                }
                SaveResult save = SavingThrow.of(victim, AncientSaves.MAGMA_VENTS).source(this.dragon)
                        .exposure(this.dragon, "magma_vent", 10).tags("dragon", "fire")
                        .onSuccess(net.minecraft.text.Text.translatable(AncientSaves.MAGMA_VENTS.effectKey())).roll();
                if (victim.damage(damage(this.dragon), save.damage(VENT_DAMAGE))) {
                    victim.setOnFireFor(save.succeeded() ? VENT_BURN_SAVED_SECONDS : VENT_BURN_SECONDS);
                }
            }
            if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                BlockPos floor = this.pos.down();
                BlockState state = world.getBlockState(floor);
                if (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.BASE_STONE_NETHER)
                        || state.isOf(Blocks.SMOOTH_BASALT) || state.isOf(Blocks.POLISHED_BLACKSTONE)
                        || state.isOf(Blocks.COBBLESTONE)) {
                    world.setBlockState(floor, Blocks.MAGMA_BLOCK.getDefaultState());
                }
            }
        }
    }

    /** The lair shakes; rocks fall on everyone on the ground halfway through. */
    private static final class Tremor implements LairAction.Effect {
        private final AncientDragonEntity dragon;
        private final List<ServerPlayerEntity> players;
        private int age;

        Tremor(AncientDragonEntity dragon, List<ServerPlayerEntity> players) {
            this.dragon = dragon;
            this.players = List.copyOf(players);
        }

        @Override
        public boolean tick(ServerWorld world) {
            this.age++;
            if (this.age == 1) {
                for (ServerPlayerEntity player : this.players) {
                    world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE,
                            1.0F, 0.5F);
                }
            }
            BlockStateParticleEffect dust = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST,
                    Blocks.BASALT.getDefaultState());
            BlockStateParticleEffect rock = new BlockStateParticleEffect(ParticleTypes.BLOCK,
                    Blocks.BLACKSTONE.getDefaultState());
            for (ServerPlayerEntity player : this.players) {
                if (!player.isAlive() || player.world != world || !player.isOnGround()) {
                    continue;
                }
                // A nudge each other tick shakes the camera a little
                if (this.age % 2 == 0) {
                    player.addVelocity((this.dragon.getRandom().nextDouble() - 0.5) * 0.08, 0.02,
                            (this.dragon.getRandom().nextDouble() - 0.5) * 0.08);
                    player.velocityModified = true;
                }
                world.spawnParticles(dust, player.getX(), player.getY() + 6.0, player.getZ(), 6, 3.0, 1.0, 3.0, 0.0);
                if (this.age % 5 == 0) {
                    world.spawnParticles(rock, player.getX(), player.getY() + 0.2, player.getZ(), 12, 2.0, 0.1, 2.0, 0.1);
                    world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_DEEPSLATE_BREAK, SoundCategory.HOSTILE,
                            1.0F, 0.6F);
                }
                if (this.age == TREMOR_TICKS / 2 && canHurt(this.dragon, player)) {
                    SaveResult save = SavingThrow.of(player, AncientSaves.TREMOR).source(this.dragon)
                            .exposure(this.dragon, "tremor", 20).tags("dragon")
                            .onSuccess(net.minecraft.text.Text.translatable(AncientSaves.TREMOR.effectKey()))
                            .onFailure(net.minecraft.text.Text.translatable("save.dndclasses.effect.tremor.failure"))
                            .roll();
                    if (save.fresh() && save.failed()) {
                        player.damage(damage(this.dragon), TREMOR_DAMAGE);
                        Conditions.of(player, Condition.PRONE).source(this.dragon).key("tremor")
                                .ticks(TREMOR_PRONE_TICKS).apply();
                    }
                }
            }
            return this.age >= TREMOR_TICKS;
        }
    }

    /** A cloud of volcanic gas: a CON save for each creature the first time it's inside. */
    private static final class GasCloud implements LairAction.Effect {
        private static final DustParticleEffect SULPHUR = new DustParticleEffect(new Vector3f(0.75F, 0.72F, 0.35F), 2.5F);
        private final AncientDragonEntity dragon;
        private final Vec3d centre;
        private final Set<UUID> saved = new HashSet<>();
        private int age;

        GasCloud(AncientDragonEntity dragon, Vec3d centre) {
            this.dragon = dragon;
            this.centre = centre;
        }

        @Override
        public boolean tick(ServerWorld world) {
            this.age++;
            if (this.age == 1) {
                world.playSound(null, BlockPos.ofFloored(this.centre), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.HOSTILE, 1.5F, 0.5F);
            }
            if (this.age % 4 == 0) {
                world.spawnParticles(SULPHUR, this.centre.x, this.centre.y, this.centre.z, 30, GAS_RADIUS * 0.5,
                        GAS_RADIUS * 0.4, GAS_RADIUS * 0.5, 0.0);
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, this.centre.x, this.centre.y, this.centre.z, 6,
                        GAS_RADIUS * 0.4, GAS_RADIUS * 0.3, GAS_RADIUS * 0.4, 0.005);
            }
            Box area = Box.of(this.centre, GAS_RADIUS * 2, GAS_RADIUS * 2, GAS_RADIUS * 2);
            for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area,
                    e -> canHurt(this.dragon, e) && e.getPos().add(0, e.getHeight() / 2, 0).isInRange(this.centre, GAS_RADIUS)
                            && !this.saved.contains(e.getUuid()))) {
                this.saved.add(victim.getUuid());
                SaveResult save = SavingThrow.of(victim, AncientSaves.VOLCANIC_GAS).source(this.dragon).tags("poison", "dragon")
                        .onSuccess(net.minecraft.text.Text.translatable(AncientSaves.VOLCANIC_GAS.effectKey()))
                        .onFailure(net.minecraft.text.Text.translatable("save.dndclasses.effect.volcanic_gas.failure"))
                        .roll();
                if (save.failed()) {
                    Conditions.of(victim, Condition.POISONED).source(this.dragon).key("volcanic_gas")
                            .ticks(GAS_POISON_TICKS).apply();
                }
            }
            return this.age >= GAS_TICKS;
        }
    }
}
