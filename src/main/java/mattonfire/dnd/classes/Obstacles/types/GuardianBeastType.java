package mattonfire.dnd.classes.Obstacles.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Items.InstrumentItem;
import mattonfire.dnd.classes.Misc.BardCompanions;
import mattonfire.dnd.classes.Obstacles.ObstacleAuras;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleInteractions;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.entity.OwlbearEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A chained or denning beast (wolves, a polar bear, an Owlbear) guarding a bridge or a den. The obstacle block is
 * the beast's tether post; the beasts within {@value #BIND_RADIUS} blocks of it (any mob in
 * {@code #dndclasses:guardian_beasts}) are bound to it the first time a player comes near, by a command tag
 * ({@value #TAG_PREFIX}x_y_z) and the {@code Data.Beasts} UUID list.
 * <ul>
 * <li>While sealed, bound beasts stay within {@value #LEASH} blocks of the post, growl at players within
 * {@value #WARN_RADIUS} blocks, and attack anyone who comes within {@value #GUARD_RADIUS} blocks of it.</li>
 * <li>A Druid (Animal Handling) or a Bard (Performance, +{@value #INSTRUMENT_BONUS} holding an instrument) calms
 * them by right-clicking the post or a beast: the group opens for {@value #CALM_TICKS} ticks, and the beasts
 * ignore players and step {@value #ASIDE} blocks aside. Then they go back on guard.</li>
 * <li>A Bard's natural 20 on an Owlbear makes it their companion for {@value #COMPANION_TICKS} ticks.</li>
 * <li>Fallback: fight them. Once every bound beast is dead the post opens for good (no class XP).</li>
 * </ul>
 */
public final class GuardianBeastType extends ObstacleType {
    public static final TagKey<net.minecraft.entity.EntityType<?>> BEASTS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "guardian_beasts"));
    /** Selector-safe: {@code @e[tag=dnd_guardian.3_150_5]}. */
    public static final String TAG_PREFIX = "dnd_guardian.";
    public static final String BEASTS_KEY = "Beasts";
    public static final String SLAIN_KEY = "Slain";

    public static final int BIND_RADIUS = 8;
    /** Bound beasts walk back to the post from further than this. */
    public static final double LEASH = 6.0;
    public static final double WARN_RADIUS = 7.0;
    public static final double GUARD_RADIUS = 3.0;
    /** Calmed: 10 minutes. */
    public static final int CALM_TICKS = 10 * 60 * 20;
    /** How far from the post a calmed beast steps. */
    public static final double ASIDE = 5.0;
    public static final int INSTRUMENT_BONUS = 2;
    /** A Bard's Owlbear companion stays for a Minecraft day. */
    public static final int COMPANION_TICKS = 24000;
    /** Posts within this range of a player are ticked. */
    public static final int TICK_RANGE = 48;
    public static final int TICK_INTERVAL = 20;
    public static final int GROWL_INTERVAL = 60;

    /** Bards whose roll on a post was a natural 20, until the post opens (26 ticks later). */
    private final Map<UUID, BlockPos> criticalBards = new HashMap<>();

    public GuardianBeastType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.ANIMAL_HANDLING;
    }

    @Override
    public Skill skillFor(PlayerEntity player) {
        return D20.classOf(player) == DndCharacter.BARD ? Skill.PERFORMANCE : Skill.ANIMAL_HANDLING;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return switch (dndClass) {
            case DRUID, BARD -> Eligibility.PRIMARY;
            default -> Eligibility.NONE;
        };
    }

    @Override
    public boolean breakable() {
        return false;
    }

    /** It's a post: solid either way. */
    @Override
    public boolean solid(ObstacleState state) {
        return true;
    }

    @Override
    public boolean accepts(PlayerEntity player, ItemStack held) {
        return held.isEmpty() || held.getItem() instanceof InstrumentItem;
    }

    @Override
    public D20.Builder rollExtras(D20.Builder builder, ServerPlayerEntity player) {
        if (D20.classOf(player) == DndCharacter.BARD && player.getMainHandStack().getItem() instanceof InstrumentItem) {
            builder.bonus("obstacle.dndclasses.instrument", INSTRUMENT_BONUS);
        }
        return builder;
    }

    @Override
    public boolean intercept(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        if (beasts(world, pos).isEmpty()) {
            player.sendMessage(Text.translatable(translationKey() + ".no_beast"), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        return ObstacleGroups.openFor(world, pos, CALM_TICKS) > 0;
    }

    @Override
    public void onRolled(ServerWorld world, BlockPos pos, ServerPlayerEntity player, D20.Roll roll) {
        if (roll.outcome() == D20.Outcome.CRITICAL && D20.classOf(player) == DndCharacter.BARD) {
            criticalBards.put(player.getUuid(), pos.toImmutable());
        } else {
            criticalBards.remove(player.getUuid());
        }
    }

    @Override
    public void onSolved(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        boolean critical = criticalBards.remove(solver.getUuid()) != null;
        for (MobEntity beast : beasts(world, pos)) {
            calm(beast, pos);
            world.spawnParticles(ParticleTypes.HEART, beast.getX(), beast.getBodyY(1.0), beast.getZ(), 5, 0.4, 0.3,
                    0.4, 0.0);
            if (critical && beast instanceof OwlbearEntity owlbear) {
                owlbear.getCommandTags().remove(tagFor(beast));
                BardCompanions.adoptFor(owlbear, solver, COMPANION_TICKS);
                solver.sendMessage(Text.translatable(translationKey() + ".companion"), false);
                DnDClasses.LOGGER.info("[Obstacle] {} won an owlbear companion at {}", solver.getEntityName(),
                        pos.toShortString());
            }
        }
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        for (MobEntity beast : beasts(world, pos)) {
            growl(world, beast);
        }
    }

    /** A natural 1: the beasts go for you. */
    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        for (MobEntity beast : beasts(world, pos)) {
            growl(world, beast);
            attack(beast, player);
        }
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
    }

    @Override
    public List<String> progressKeys() {
        return List.of(BEASTS_KEY, SLAIN_KEY);
    }

    // ---- Beasts ----

    private static String tagFor(BlockPos post) {
        return TAG_PREFIX + post.getX() + "_" + post.getY() + "_" + post.getZ();
    }

    private String tagFor(Entity beast) {
        for (String tag : beast.getCommandTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                return tag;
            }
        }
        return "";
    }

    /** The post a beast guards, or null. */
    @Nullable
    public static BlockPos postOf(Entity entity) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                String[] parts = tag.substring(TAG_PREFIX.length()).split("_");
                try {
                    return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Whether a calmed guardian beast may not target {@code target}: players, while its post is open. Called from
     * {@code MobEntityMixin.setTarget}.
     */
    public static boolean blocksTarget(MobEntity mob, @Nullable LivingEntity target) {
        if (!(target instanceof PlayerEntity) || mob.getWorld().isClient) {
            return false;
        }
        BlockPos post = postOf(mob);
        if (post == null || !mob.getWorld().isChunkLoaded(post)) {
            return false;
        }
        BlockState state = mob.getWorld().getBlockState(post);
        return state.getBlock() instanceof ObstacleBlock block && block.type() instanceof GuardianBeastType
                && state.get(ObstacleBlock.STATE) == ObstacleState.OPEN;
    }

    /** The living beasts bound to the post's group, binding nearby ones first if it has none yet. */
    public List<MobEntity> beasts(ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof ObstacleBlockEntity be)) {
            return List.of();
        }
        if (!be.data().contains(BEASTS_KEY)) {
            bind(world, pos);
        }
        List<MobEntity> found = new ArrayList<>();
        for (NbtElement element : be.data().getList(BEASTS_KEY, NbtElement.INT_ARRAY_TYPE)) {
            Entity entity = world.getEntity(NbtHelper.toUuid(element));
            if (entity instanceof MobEntity mob && mob.isAlive() && postOf(mob) != null) {
                found.add(mob);
            }
        }
        return found;
    }

    /** Binds the guardian beasts round the post's group to it. Returns how many. */
    public int bind(ServerWorld world, BlockPos pos) {
        List<BlockPos> group = ObstacleGroups.group(world, pos);
        if (group.isEmpty()) {
            return 0;
        }
        Set<String> ownTags = new HashSet<>();
        for (BlockPos block : group) {
            ownTags.add(tagFor(block));
        }
        BlockPos post = group.get(0);
        String tag = tagFor(post);
        NbtList list = new NbtList();
        Box box = new Box(post).expand(BIND_RADIUS);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, m -> m.isAlive()
                && m.getType().isIn(BEASTS) && !BardCompanions.isCompanion(m)
                && !(m instanceof TameableEntity tame && tame.isTamed()))) {
            String old = tagFor((Entity) mob);
            if (!old.isEmpty() && !ownTags.contains(old)) {
                continue; // another post's beast
            }
            if (!old.isEmpty()) {
                mob.getCommandTags().remove(old);
            }
            mob.addCommandTag(tag);
            mob.setPersistent();
            list.add(NbtHelper.fromUuid(mob.getUuid()));
        }
        if (list.isEmpty()) {
            return 0;
        }
        for (BlockPos block : group) {
            if (world.getBlockEntity(block) instanceof ObstacleBlockEntity be) {
                be.data().put(BEASTS_KEY, list.copy());
                be.data().remove(SLAIN_KEY);
                be.markDirty();
            }
        }
        DnDClasses.LOGGER.info("[Obstacle] guardian post at {} bound {} beasts", post.toShortString(), list.size());
        return list.size();
    }

    private static void calm(MobEntity beast, BlockPos post) {
        beast.setTarget(null);
        if (beast instanceof Angerable angry) {
            angry.stopAnger();
        }
        stepAside(beast, post);
    }

    private static void stepAside(MobEntity beast, BlockPos post) {
        Vec3d center = Vec3d.ofBottomCenter(post);
        Vec3d away = new Vec3d(beast.getX() - center.x, 0, beast.getZ() - center.z);
        if (away.lengthSquared() < 0.01) {
            away = new Vec3d(1, 0, 0);
        }
        Vec3d to = center.add(away.normalize().multiply(ASIDE));
        beast.getNavigation().startMovingTo(to.x, beast.getY(), to.z, 1.0);
    }

    private static void attack(MobEntity beast, PlayerEntity player) {
        if (beast instanceof Angerable angry) {
            angry.setAngryAt(player.getUuid());
            angry.chooseRandomAngerTime();
        }
        beast.setTarget(player);
    }

    private static void growl(ServerWorld world, MobEntity beast) {
        world.playSound(null, beast.getBlockPos(), beast instanceof OwlbearEntity
                ? SoundEvents.ENTITY_POLAR_BEAR_WARNING : SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.HOSTILE,
                1.0f, 0.9f);
        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, beast.getX(), beast.getBodyY(1.0), beast.getZ(), 1, 0.2,
                0.2, 0.2, 0.0);
    }

    private static boolean guardable(PlayerEntity player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    // ---- Events ----

    @Override
    public int auraInterval() {
        return TICK_INTERVAL;
    }

    @Override
    public int auraRange() {
        return TICK_RANGE;
    }

    @Override
    public void registerEvents() {
        UseEntityCallback.EVENT.register(this::useBeast);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getWorld() instanceof ServerWorld world && entity instanceof MobEntity mob) {
                slain(world, mob);
            }
        });
    }

    /** Right-clicking a bound beast works its post. */
    private ActionResult useBeast(PlayerEntity player, World world, Hand hand, Entity entity,
            @Nullable net.minecraft.util.hit.EntityHitResult hit) {
        if (hand != Hand.MAIN_HAND || player.isSpectator() || !(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        BlockPos post = postOf(entity);
        if (post == null || !isSealedMine(world.getBlockState(post)) || !accepts(player, player.getMainHandStack())) {
            return ActionResult.PASS;
        }
        ObstacleInteractions.use(serverWorld, post, this, serverPlayer);
        return ActionResult.SUCCESS;
    }

    /** A bound beast died: once all of them have, the post opens for good (the fight fallback, no XP). */
    private void slain(ServerWorld world, MobEntity mob) {
        BlockPos post = postOf(mob);
        if (post == null || !(world.getBlockEntity(post) instanceof ObstacleBlockEntity be)
                || !isMine(world.getBlockState(post))) {
            return;
        }
        int bound = be.data().getList(BEASTS_KEY, NbtElement.INT_ARRAY_TYPE).size();
        int slain = ObstacleGroups.dataInt(world, post, SLAIN_KEY) + 1;
        ObstacleGroups.setDataInt(world, post, SLAIN_KEY, slain);
        DnDClasses.LOGGER.info("[Obstacle] guardian beast slain at {}: {}/{}", post.toShortString(), slain, bound);
        if (slain >= bound) {
            ObstacleGroups.setResealTicks(world, post, 0);
            ObstacleGroups.open(world, post);
        }
    }

    /** Each post near a player, once: sealed, its beasts guard it; open, they stand aside. */
    @Override
    public void auraTick(ServerWorld world, ObstacleAuras.Nearby nearby) {
        Set<BlockPos> seen = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            for (BlockPos pos : nearby.within(player, TICK_RANGE)) {
                if (!world.isChunkLoaded(pos) || !seen.add(pos)) {
                    continue;
                }
                BlockState state = world.getBlockState(pos);
                if (isMine(state)) {
                    tickPost(world, pos, state.get(ObstacleBlock.STATE) == ObstacleState.SEALED);
                }
            }
        }
    }

    private void tickPost(ServerWorld world, BlockPos post, boolean sealed) {
        List<MobEntity> beasts = beasts(world, post);
        if (beasts.isEmpty()) {
            return;
        }
        Vec3d center = Vec3d.ofCenter(post);
        if (!sealed) {
            // Calm: no players as targets, and out of the way
            for (MobEntity beast : beasts) {
                if (beast.getTarget() instanceof PlayerEntity) {
                    calm(beast, post);
                } else if (beast.getPos().squaredDistanceTo(center) < (ASIDE - 2) * (ASIDE - 2)
                        && beast.getNavigation().isIdle()) {
                    stepAside(beast, post);
                }
            }
            return;
        }
        PlayerEntity intruder = world.getClosestPlayer(center.x, center.y, center.z, GUARD_RADIUS,
                e -> e instanceof PlayerEntity p && guardable(p));
        PlayerEntity near = intruder != null ? null : world.getClosestPlayer(center.x, center.y, center.z,
                WARN_RADIUS, e -> e instanceof PlayerEntity p && guardable(p));
        boolean growl = world.getTime() % GROWL_INTERVAL == 0;
        for (MobEntity beast : beasts) {
            if (intruder != null && beast.getTarget() != intruder) {
                attack(beast, intruder);
                growl(world, beast);
            } else if (near != null && beast.getTarget() == null) {
                beast.getLookControl().lookAt(near);
                if (growl) {
                    growl(world, beast);
                }
            }
            if (beast.getTarget() == null && beast.getPos().squaredDistanceTo(center) > LEASH * LEASH
                    && beast instanceof PathAwareEntity) {
                beast.getNavigation().startMovingTo(center.x, center.y, center.z, 1.0);
            }
        }
    }
}
