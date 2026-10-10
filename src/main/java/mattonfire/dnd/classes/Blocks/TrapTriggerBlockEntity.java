package mattonfire.dnd.classes.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.SkillChecks.Perceivable;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.SkillChecks.TrapSense;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The state of one dungeon trap, kept in its trigger tile: what kind it is, whether it's armed, and the
 * blocks it works through (dart launchers, flame vents, the crumbling tiles of a pit, a trapped chest).
 *
 * <ul>
 * <li><b>Set off</b> by anything standing on the trigger or one of its pressure tiles (vents, crumbling
 * floor): players in survival or adventure, mobs, or a thrown item. An arrow hitting a tile sets it off
 * too ({@link TrapTriggerBlock#onProjectileHit}). A trap fires once and is then spent.</li>
 * <li><b>Spotted</b>: every 10 ticks, players within {@link #SPOT_RANGE} blocks whom {@link TrapSense} lets
 * see the trap get an outline of its tiles drawn in particles, only for them.</li>
 * <li><b>Disarmed</b> by a Rogue ({@link mattonfire.dnd.classes.SkillChecks.TrapDisarm}).</li>
 * <li><b>Re-armed</b> when its dungeon is reset ({@link DungeonState#resets()} moves on): a spent or
 * disarmed trap notices within a second of its chunk being loaded.</li>
 * </ul>
 *
 * Numbers by tier: DC {@link TrapKind#dc}; darts 2/3/4/5 damage each; flames 4 + 2 per tier over I.
 */
public class TrapTriggerBlockEntity extends BlockEntity implements Perceivable {
    public static final double SPOT_RANGE = 6.0;
    public static final int DARTS = 3;
    private static final int DART_INTERVAL = 5;
    private static final int POISON_DART_TICKS = 4 * 20;
    private static final int FLAME_TICKS = 20;
    private static final int FIRE_SECONDS = 3;
    /** Between stepping on a pit and it giving way. */
    private static final int CRUMBLE_DELAY = 10;
    private static final float NEEDLE_DAMAGE = 2.0F;
    private static final int NEEDLE_POISON_TICKS = 6 * 20;
    private static final DustParticleEffect OUTLINE = new DustParticleEffect(new Vector3f(1.0F, 0.45F, 0.1F), 0.9F);

    private TrapKind kind = TrapKind.DART;
    private long startKey;
    private int tier = 1;
    private boolean armed = true;
    private boolean disarmed;
    /** The dungeon's reset count when the trap was spent or disarmed. */
    private int spentAt;
    /** Launchers, vents or the trapped chest. */
    private final List<BlockPos> linked = new ArrayList<>();
    /** A pit's crumbling tiles (air once it has opened). */
    private final List<BlockPos> crumbles = new ArrayList<>();
    /** A pit's top ladder rung, put in when the floor gives way so you can climb out. */
    @Nullable
    private BlockPos ladder;
    private Direction ladderFacing = Direction.NORTH;

    // Running effects (saved, so a trap mid-volley finishes after a reload)
    private int dartsLeft;
    private long nextDart;
    private boolean poisonDarts;
    @Nullable
    private UUID dartTarget;
    private long flameUntil = -1L;
    private long crumbleAt = -1L;

    /** Who has been told they spotted this trap since it was last armed (not saved). */
    private final Set<UUID> spotted = new HashSet<>();
    /** Who found this trap with a Search (V) since it was last armed (not saved). */
    private final Set<UUID> searched = new HashSet<>();

    public TrapTriggerBlockEntity(BlockPos pos, BlockState state) {
        super(TrapBlocks.TRAP_TRIGGER_ENTITY, pos, state);
    }

    /** Set up during generation (world positions). */
    public void setup(TrapKind kind, long startKey, int tier, List<BlockPos> linked, List<BlockPos> crumbles,
            @Nullable BlockPos ladder, Direction ladderFacing) {
        this.kind = kind;
        this.startKey = startKey;
        this.tier = tier;
        this.armed = true;
        this.disarmed = false;
        this.linked.clear();
        this.linked.addAll(linked);
        this.crumbles.clear();
        this.crumbles.addAll(crumbles);
        this.ladder = ladder;
        this.ladderFacing = ladderFacing;
        this.markDirty();
    }

    public TrapKind kind() {
        return this.kind;
    }

    public boolean isArmed() {
        return this.armed;
    }

    public boolean isDisarmed() {
        return this.disarmed;
    }

    public long startKey() {
        return this.startKey;
    }

    public List<BlockPos> linked() {
        return this.linked;
    }

    /** Whether {@code pos} is one of this trap's blocks (besides the trigger). */
    public boolean owns(BlockPos pos) {
        return this.linked.contains(pos) || this.crumbles.contains(pos);
    }

    @Nullable
    private DungeonState dungeon() {
        if (this.world instanceof ServerWorld server && this.startKey != 0L) {
            return DungeonRegistry.get(server).get(this.startKey);
        }
        return null;
    }

    /** The dungeon's current tier (an admin may have changed it), else the one it was built with. */
    public int tier() {
        DungeonState dungeon = this.dungeon();
        return dungeon != null ? dungeon.tier() : this.tier;
    }

    public int dc() {
        return TrapKind.dc(this.tier());
    }

    private int resets() {
        DungeonState dungeon = this.dungeon();
        return dungeon != null ? dungeon.resets() : this.spentAt;
    }

    // ---------------------------------------------------------------- ticking

    public static void tick(World world, BlockPos pos, BlockState state, TrapTriggerBlockEntity trap) {
        if (!(world instanceof ServerWorld server)) {
            return;
        }
        long now = world.getTime();
        trap.runEffects(server, now);
        if (trap.armed) {
            if (trap.kind != TrapKind.NEEDLE && now % 2 == 0) {
                trap.checkPressure(server);
            }
            if (now % 10 == 0) {
                trap.showToSpotters(server);
            }
        } else if (now % 20 == 0 && trap.dartsLeft <= 0 && trap.crumbleAt < 0 && trap.flameUntil < 0
                && trap.resets() != trap.spentAt) {
            trap.rearm("dungeon reset");
        }
    }

    /** The trigger and every tile that sets it off. */
    private List<BlockPos> pressureTiles() {
        List<BlockPos> tiles = new ArrayList<>();
        tiles.add(this.pos);
        for (BlockPos p : this.linked) {
            if (this.world.getBlockState(p).getBlock() instanceof FlameVentBlock) {
                tiles.add(p);
            }
        }
        for (BlockPos p : this.crumbles) {
            if (this.world.getBlockState(p).getBlock() instanceof CrumblingFloorBlock) {
                tiles.add(p);
            }
        }
        return tiles;
    }

    private static Box above(List<BlockPos> tiles, double height) {
        Box box = null;
        for (BlockPos p : tiles) {
            Box b = new Box(p.getX(), p.getY() + 1, p.getZ(), p.getX() + 1, p.getY() + 1 + height, p.getZ() + 1);
            box = box == null ? b : box.union(b);
        }
        return box;
    }

    /** Living things and dropped items count; creative and spectator players don't. */
    private static boolean canSetOff(Entity e) {
        if (e instanceof PlayerEntity player) {
            return player.isAlive() && !player.isCreative() && !player.isSpectator();
        }
        return (e instanceof LivingEntity living && living.isAlive()) || e instanceof ItemEntity;
    }

    private void checkPressure(ServerWorld world) {
        List<BlockPos> tiles = this.pressureTiles();
        Box box = above(tiles, 0.5);
        for (Entity e : world.getEntitiesByClass(Entity.class, box, TrapTriggerBlockEntity::canSetOff)) {
            BlockPos under = BlockPos.ofFloored(e.getX(), e.getY() - 0.05, e.getZ());
            if (tiles.contains(under) && (e.isOnGround() || e.getY() - (under.getY() + 1) < 0.05)) {
                this.trigger(world, e);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- setting it off

    /**
     * Sets the trap off ({@code cause} is whatever stepped on it, shot it or failed to disarm it; may be null).
     * Does nothing if it isn't armed.
     */
    public void trigger(ServerWorld world, @Nullable Entity cause) {
        if (!this.armed) {
            return;
        }
        this.spend(false);
        DnDClasses.LOGGER.info("[Trap] {} trap at {} set off by {} (tier {}, DC {})", this.kind.id(),
                this.pos.toShortString(), cause == null ? "nothing" : cause.getName().getString(), this.tier(), this.dc());
        world.playSound(null, this.pos, SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON, SoundCategory.BLOCKS, 0.8F, 0.6F);
        long now = world.getTime();
        switch (this.kind) {
            case DART -> this.startDarts(world, cause, now);
            case FLAME -> this.flames(world, now);
            case PIT -> {
                this.crumbleAt = now + CRUMBLE_DELAY;
                world.playSound(null, this.pos, SoundEvents.BLOCK_POINTED_DRIPSTONE_BREAK, SoundCategory.BLOCKS, 1.0F, 0.6F);
                for (BlockPos p : this.crumbles) {
                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(p)),
                            p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.0);
                }
            }
            case NEEDLE -> {
                if (cause instanceof LivingEntity victim) {
                    this.needle(world, victim);
                }
            }
        }
        this.markDirty();
    }

    /** Spent (false) or disarmed (true): no longer armed until the dungeon resets. */
    private void spend(boolean disarm) {
        this.armed = false;
        this.disarmed = disarm;
        this.spentAt = this.resets();
        this.setArmedState(false);
        this.spotted.clear();
        this.searched.clear();
        this.markDirty();
    }

    /** A Rogue's successful disarm. */
    public void disarm() {
        if (!this.armed) {
            return;
        }
        this.spend(true);
        DnDClasses.LOGGER.info("[Trap] {} trap at {} disarmed", this.kind.id(), this.pos.toShortString());
    }

    /** Arms it again: put back a pit's floor, put out the vents. */
    public void rearm(String why) {
        if (this.world == null) {
            return;
        }
        this.armed = true;
        this.disarmed = false;
        this.dartsLeft = 0;
        this.crumbleAt = -1L;
        this.flameUntil = -1L;
        this.spotted.clear();
        this.searched.clear();
        for (BlockPos p : this.crumbles) {
            if (!(this.world.getBlockState(p).getBlock() instanceof CrumblingFloorBlock)) {
                this.world.setBlockState(p, TrapBlocks.CRUMBLING_FLOOR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        this.setVents(false);
        this.setArmedState(true);
        this.markDirty();
        DnDClasses.LOGGER.info("[Trap] {} trap at {} re-armed ({})", this.kind.id(), this.pos.toShortString(), why);
    }

    private void setArmedState(boolean armed) {
        BlockState state = this.world.getBlockState(this.pos);
        if (state.contains(TrapTriggerBlock.ARMED) && state.get(TrapTriggerBlock.ARMED) != armed) {
            this.world.setBlockState(this.pos, state.with(TrapTriggerBlock.ARMED, armed), Block.NOTIFY_ALL);
        }
    }

    private void setVents(boolean lit) {
        for (BlockPos p : this.linked) {
            BlockState state = this.world.getBlockState(p);
            if (state.getBlock() instanceof FlameVentBlock && state.get(Properties.LIT) != lit) {
                this.world.setBlockState(p, state.with(Properties.LIT, lit), Block.NOTIFY_ALL);
            }
        }
    }

    private void runEffects(ServerWorld world, long now) {
        if (this.dartsLeft > 0 && now >= this.nextDart) {
            this.fireDart(world);
            this.dartsLeft--;
            this.nextDart = now + DART_INTERVAL;
            this.markDirty();
        }
        if (this.flameUntil >= 0) {
            if (now >= this.flameUntil) {
                this.flameUntil = -1L;
                this.setVents(false);
                this.markDirty();
            } else if (now % 2 == 0) {
                for (BlockPos p : this.linked) {
                    if (world.getBlockState(p).getBlock() instanceof FlameVentBlock) {
                        world.spawnParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 8,
                                0.2, 0.5, 0.2, 0.02);
                    }
                }
            }
        }
        if (this.crumbleAt >= 0 && now >= this.crumbleAt) {
            this.crumbleAt = -1L;
            this.openPit(world);
            this.markDirty();
        }
    }

    // ---------------------------------------------------------------- darts

    private void startDarts(ServerWorld world, @Nullable Entity cause, long now) {
        LivingEntity target = cause instanceof LivingEntity living ? living : null;
        if (target == null) {
            target = world.getClosestEntity(LivingEntity.class, net.minecraft.entity.ai.TargetPredicate.DEFAULT, null,
                    this.pos.getX() + 0.5, this.pos.getY() + 1.0, this.pos.getZ() + 0.5, new Box(this.pos).expand(4.0));
        }
        this.dartTarget = target == null ? null : target.getUuid();
        this.poisonDarts = true;
        if (target != null) {
            SaveResult save = SavingThrow.of(target, Ability.DEX, this.dc()).label("save.dndclasses.trap.dart")
                    .exposure(this.exposureKey(), "dart", 40).tags("trap")
                    .onSuccess(Text.translatable("save.dndclasses.trap.dart.success"))
                    .onFailure(Text.translatable("save.dndclasses.trap.dart.failure")).roll();
            this.poisonDarts = save.failed();
        }
        this.dartsLeft = DARTS;
        this.nextDart = now;
    }

    private void fireDart(ServerWorld world) {
        Entity target = this.dartTarget == null ? null : world.getEntity(this.dartTarget);
        Vec3d aim = target != null && target.isAlive() && target.squaredDistanceTo(Vec3d.ofCenter(this.pos)) < 16 * 16
                ? target.getPos().add(0, target.getHeight() * 0.6, 0)
                : Vec3d.ofCenter(this.pos).add(0, 1.0, 0);
        for (BlockPos launcher : this.linked) {
            BlockState state = world.getBlockState(launcher);
            if (!(state.getBlock() instanceof DartLauncherBlock)) {
                continue;
            }
            Direction facing = state.get(DartLauncherBlock.FACING);
            Vec3d from = Vec3d.ofCenter(launcher).add(facing.getOffsetX() * 0.7, 0.0, facing.getOffsetZ() * 0.7);
            ArrowEntity dart = new ArrowEntity(world, from.x, from.y, from.z);
            Vec3d dir = aim.subtract(from);
            dart.setVelocity(dir.x, dir.y + dir.horizontalLength() * 0.05, dir.z, 1.5F, 1.5F);
            dart.setDamage(1.0 + 0.67 * (this.tier() - 1));
            dart.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
            dart.addCommandTag(TrapBlocks.DART_TAG);
            if (this.poisonDarts) {
                dart.addEffect(new StatusEffectInstance(StatusEffects.POISON, POISON_DART_TICKS, 0));
            }
            world.spawnEntity(dart);
            world.playSound(null, launcher, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0F, 1.4F);
        }
    }

    // ---------------------------------------------------------------- flames

    private void flames(ServerWorld world, long now) {
        this.flameUntil = now + FLAME_TICKS;
        this.setVents(true);
        world.playSound(null, this.pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.2F, 0.8F);
        List<BlockPos> row = new ArrayList<>();
        row.add(this.pos);
        for (BlockPos p : this.linked) {
            if (world.getBlockState(p).getBlock() instanceof FlameVentBlock) {
                row.add(p);
            }
        }
        float base = 4.0F + 2.0F * (this.tier() - 1);
        Box box = above(row, 2.5).expand(0.3, 0.0, 0.3);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())))) {
            SaveResult save = SavingThrow.of(victim, Ability.DEX, this.dc()).label("save.dndclasses.trap.flame")
                    .exposure(this.exposureKey(), "flame", 40).tags("trap").halvesDamage().roll();
            float damage = save.damage(base);
            DnDClasses.LOGGER.info("[Trap] flame hits {} for {} ({})", victim.getName().getString(), damage,
                    save.succeeded() ? "saved, half" : "failed save, full");
            victim.damage(ModDamageTypes.of(world, ModDamageTypes.TRAP), damage);
            if (save.failed()) {
                victim.setOnFireFor(FIRE_SECONDS);
            }
        }
    }

    // ---------------------------------------------------------------- pit

    private void openPit(ServerWorld world) {
        BlockPos.Mutable safe = this.pos.up().mutableCopy();
        Box box = above(this.crumbles, 1.0);
        if (box != null) {
            for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, box,
                    e -> e.isAlive() && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())))) {
                SaveResult save = SavingThrow.of(victim, Ability.DEX, this.dc()).label("save.dndclasses.trap.pit")
                        .exposure(this.exposureKey(), "pit", 40).tags("trap")
                        .onSuccess(Text.translatable("save.dndclasses.trap.pit.success"))
                        .onFailure(Text.translatable("save.dndclasses.trap.pit.failure")).roll();
                if (save.succeeded()) {
                    victim.requestTeleport(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                    victim.fallDistance = 0.0F;
                    DnDClasses.LOGGER.info("[Trap] {} catches the edge of the pit", victim.getName().getString());
                } else {
                    DnDClasses.LOGGER.info("[Trap] {} falls into the pit", victim.getName().getString());
                }
            }
        }
        for (BlockPos p : this.crumbles) {
            BlockState was = world.getBlockState(p);
            if (!(was.getBlock() instanceof CrumblingFloorBlock)) {
                continue;
            }
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, was), p.getX() + 0.5, p.getY() + 0.5,
                    p.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.0);
            BlockState now = p.equals(this.ladder)
                    ? Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, this.ladderFacing)
                    : Blocks.AIR.getDefaultState();
            world.setBlockState(p, now, Block.NOTIFY_ALL);
        }
        world.playSound(null, this.pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.2F, 0.5F);
    }

    // ---------------------------------------------------------------- needle

    private void needle(ServerWorld world, LivingEntity victim) {
        SaveResult save = SavingThrow.of(victim, Ability.CON, this.dc()).label("save.dndclasses.trap.needle")
                .exposure(this.exposureKey(), "needle", 40).tags("trap", "poison")
                .onSuccess(Text.translatable("save.dndclasses.trap.needle.success"))
                .onFailure(Text.translatable("save.dndclasses.trap.needle.failure")).roll();
        int ticks = save.succeeded() ? NEEDLE_POISON_TICKS / 2 : NEEDLE_POISON_TICKS;
        DnDClasses.LOGGER.info("[Trap] needle jabs {} for {} + Poison II {} ticks", victim.getName().getString(),
                NEEDLE_DAMAGE, ticks);
        victim.damage(ModDamageTypes.of(world, ModDamageTypes.TRAP), NEEDLE_DAMAGE);
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, ticks, 1));
        world.playSound(null, this.pos.up(), SoundEvents.ENTITY_BEE_STING, SoundCategory.BLOCKS, 1.0F, 1.2F);
        if (victim instanceof PlayerEntity player) {
            player.sendMessage(Text.translatable("trap.dndclasses.needle.jab"), true);
        }
    }

    private String exposureKey() {
        return "trap@" + this.pos.asLong();
    }

    // ---------------------------------------------------------------- spotting

    private void showToSpotters(ServerWorld world) {
        Vec3d centre = Vec3d.ofCenter(this.pos);
        List<BlockPos> outline = this.kind == TrapKind.NEEDLE ? this.linked : this.pressureTiles();
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.squaredDistanceTo(centre) <= SPOT_RANGE * SPOT_RANGE)) {
            if (!this.spottedBy(player)) {
                continue;
            }
            for (BlockPos tile : outline) {
                // A needle is outlined round its chest; floor traps on their top face
                double y = this.kind == TrapKind.NEEDLE ? tile.getY() + 0.5 : tile.getY() + 1.05;
                for (int i = 0; i < 4; i++) {
                    double t = i / 4.0;
                    outline(world, player, tile.getX() + t, y, tile.getZ());
                    outline(world, player, tile.getX() + 1, y, tile.getZ() + t);
                    outline(world, player, tile.getX() + 1 - t, y, tile.getZ() + 1);
                    outline(world, player, tile.getX(), y, tile.getZ() + 1 - t);
                }
            }
            if (this.spotted.add(player.getUuid())) {
                player.sendMessage(Text.translatable("trap.dndclasses.spotted", Text.translatable(this.kind.translationKey())), true);
                DnDClasses.LOGGER.info("[Trap] {} spots the {} trap at {}", player.getEntityName(), this.kind.id(),
                        this.pos.toShortString());
            }
        }
    }

    /** Whether the player sees this trap: found with a Search, or noticed by {@link TrapSense}. */
    public boolean spottedBy(ServerPlayerEntity player) {
        return this.searched.contains(player.getUuid()) || TrapSense.get().spots(player, this, this.dc());
    }

    // A Search (V) within range rolls against an armed trap's DC; finding it outlines it like a passive spot.

    @Override
    public int perceptionDc() {
        return this.dc();
    }

    @Override
    public Vec3d perceptionPos() {
        return Vec3d.ofCenter(this.pos);
    }

    @Override
    public boolean hiddenFrom(ServerPlayerEntity player) {
        return this.armed && !this.spottedBy(player);
    }

    @Override
    public void perceive(ServerPlayerEntity player, boolean searched) {
        if (searched && this.searched.add(player.getUuid()) && this.world instanceof ServerWorld server) {
            this.showToSpotters(server);
        }
    }

    private static void outline(ServerWorld world, ServerPlayerEntity player, double x, double y, double z) {
        world.spawnParticles(player, OUTLINE, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Kind", this.kind.id());
        nbt.putLong("StartKey", this.startKey);
        nbt.putInt("Tier", this.tier);
        nbt.putBoolean("Armed", this.armed);
        nbt.putBoolean("Disarmed", this.disarmed);
        nbt.putInt("SpentAt", this.spentAt);
        nbt.put("Linked", writePositions(this.linked));
        nbt.put("Crumbles", writePositions(this.crumbles));
        if (this.ladder != null) {
            nbt.putLong("Ladder", this.ladder.asLong());
            nbt.putString("LadderFacing", this.ladderFacing.asString());
        }
        nbt.putInt("DartsLeft", this.dartsLeft);
        nbt.putLong("NextDart", this.nextDart);
        nbt.putBoolean("PoisonDarts", this.poisonDarts);
        if (this.dartTarget != null) {
            nbt.putUuid("DartTarget", this.dartTarget);
        }
        nbt.putLong("FlameUntil", this.flameUntil);
        nbt.putLong("CrumbleAt", this.crumbleAt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.kind = TrapKind.byId(nbt.getString("Kind"));
        this.startKey = nbt.getLong("StartKey");
        this.tier = nbt.contains("Tier") ? nbt.getInt("Tier") : 1;
        this.armed = !nbt.contains("Armed") || nbt.getBoolean("Armed");
        this.disarmed = nbt.getBoolean("Disarmed");
        this.spentAt = nbt.getInt("SpentAt");
        readPositions(nbt.getList("Linked", NbtElement.INT_ARRAY_TYPE), this.linked);
        readPositions(nbt.getList("Crumbles", NbtElement.INT_ARRAY_TYPE), this.crumbles);
        this.ladder = nbt.contains("Ladder") ? BlockPos.fromLong(nbt.getLong("Ladder")) : null;
        Direction facing = Direction.byName(nbt.getString("LadderFacing"));
        this.ladderFacing = facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing;
        this.dartsLeft = nbt.getInt("DartsLeft");
        this.nextDart = nbt.getLong("NextDart");
        this.poisonDarts = nbt.getBoolean("PoisonDarts");
        this.dartTarget = nbt.containsUuid("DartTarget") ? nbt.getUuid("DartTarget") : null;
        this.flameUntil = nbt.contains("FlameUntil") ? nbt.getLong("FlameUntil") : -1L;
        this.crumbleAt = nbt.contains("CrumbleAt") ? nbt.getLong("CrumbleAt") : -1L;
    }

    private static NbtList writePositions(List<BlockPos> positions) {
        NbtList list = new NbtList();
        for (BlockPos p : positions) {
            list.add(new NbtIntArray(new int[] {p.getX(), p.getY(), p.getZ()}));
        }
        return list;
    }

    private static void readPositions(NbtList list, List<BlockPos> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            int[] a = list.getIntArray(i);
            if (a.length == 3) {
                into.add(new BlockPos(a[0], a[1], a[2]));
            }
        }
    }

    // ---------------------------------------------------------------- lookup

    /** The trap a linked block (vent, crumbling tile, launcher, trapped chest) belongs to, or null. */
    @Nullable
    public static TrapTriggerBlockEntity ownerOf(World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TrapTriggerBlockEntity self) {
            return self;
        }
        for (BlockPos p : BlockPos.iterate(pos.add(-5, -3, -5), pos.add(5, 1, 5))) {
            if (world.getBlockEntity(p) instanceof TrapTriggerBlockEntity trap && trap.owns(pos)) {
                return trap;
            }
        }
        return null;
    }

    /** Other floor traps of the same dungeon within {@code range} blocks, nearest first. */
    public List<TrapTriggerBlockEntity> neighbours(int range) {
        List<TrapTriggerBlockEntity> found = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(this.pos.add(-range, -1, -range), this.pos.add(range, 1, range))) {
            if (!p.equals(this.pos) && this.world.getBlockEntity(p) instanceof TrapTriggerBlockEntity trap
                    && trap.kind != TrapKind.NEEDLE && trap.startKey == this.startKey) {
                found.add(trap);
            }
        }
        found.sort((a, b) -> Double.compare(a.pos.getSquaredDistance(this.pos), b.pos.getSquaredDistance(this.pos)));
        return found;
    }
}
