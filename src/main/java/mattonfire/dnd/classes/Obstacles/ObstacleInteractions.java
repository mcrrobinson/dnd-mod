package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Right-clicking obstacles, following the lockpicking pattern:
 * <ul>
 * <li>Empty main hand (or the type's {@link ObstacleType#requiredItem}). A class that can't attempt
 * it gets a hint on the action bar.</li>
 * <li>The roll is a {@link SkillCheck} (d20 + the solver's character-sheet bonus for the type's skill,
 * tagged {@value #CHECK_TAG}) against the tier's DC. On a success the group opens
 * {@value #OPEN_DELAY_TICKS} ticks later, once the HUD has shown the die, and the solver earns
 * class XP. On a failure the type's sting fires and you wait {@value #FAIL_COOLDOWN_TICKS} ticks
 * (natural 1: {@value #FUMBLE_COOLDOWN_TICKS} ticks, the fumble sting and an alarm).</li>
 * <li><b>Take your time:</b> sneak + right-click with no hostile mob within {@value #HOSTILE_RADIUS}
 * blocks starts a {@value #TAKE_YOUR_TIME_TICKS}-tick focus. Moving or taking damage breaks it; it
 * resolves as a taken 20 + the sheet bonus, with no crit. Not allowed on Very Hard obstacles.</li>
 * <li><b>Protected volume:</b> in survival, nobody can place blocks in, or break non-obstacle blocks
 * in, the 1-block shell round a sealed obstacle, so you can't dig or build round it.</li>
 * </ul>
 * Creative players open obstacles by right-clicking and ignore the protection.
 */
public final class ObstacleInteractions {
    public static final int FAIL_COOLDOWN_TICKS = 30;
    public static final int FUMBLE_COOLDOWN_TICKS = 100;
    /** Long enough for the HUD's die to land and the result to be read. */
    public static final int OPEN_DELAY_TICKS = 26;
    public static final int TAKE_YOUR_TIME_TICKS = 160;
    public static final int TAKE_YOUR_TIME_NATURAL = 20;
    public static final int HOSTILE_RADIUS = 16;
    /** Blocks round a sealed obstacle that can't be built in or dug out. */
    public static final int PROTECTION_SHELL = 1;
    /** Roll tag, so sheet advantage filters can pick out obstacle checks. */
    public static final String CHECK_TAG = "obstacle";

    private record PendingOpen(ServerPlayerEntity player, BlockPos pos, ObstacleType type, Tier tier, long openAt) {
    }

    private record Channel(ServerPlayerEntity player, BlockPos pos, ObstacleType type, Tier tier, Skill skill,
            Vec3d start, float health, long startedAt) {
    }

    private static final List<PendingOpen> PENDING = new ArrayList<>();
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    /** Player UUID to the world time they may try again. */
    private static final Map<UUID, Long> RETRY_AT = new HashMap<>();

    private ObstacleInteractions() {
    }

    static void register() {
        UseBlockCallback.EVENT.register(ObstacleInteractions::refusePlacement);
        PlayerBlockBreakEvents.BEFORE.register(ObstacleInteractions::allowBreak);
        ServerTickEvents.END_SERVER_TICK.register(ObstacleInteractions::tick);
    }

    /** Called on disconnect: drops the player's focus and pending opens, and retry waits that are over. */
    public static void forget(UUID player, long now) {
        CHANNELS.remove(player);
        PENDING.removeIf(p -> p.player().getUuid().equals(player));
        RETRY_AT.values().removeIf(t -> now >= t);
    }

    // ---- Right-click ----

    static ActionResult onUse(ObstacleBlock block, BlockState state, World world, BlockPos pos, PlayerEntity player,
            Hand hand) {
        if (state.get(ObstacleBlock.STATE) != ObstacleState.SEALED || hand != Hand.MAIN_HAND
                || player.isSpectator()) {
            return ActionResult.PASS;
        }
        ObstacleType type = block.type();
        if (type.hiddenFrom(world, pos, player)) {
            // Just a wall, as far as this player knows
            return ActionResult.PASS;
        }
        if (!type.accepts(player, player.getMainHandStack())) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        use(serverWorld, pos, type, serverPlayer);
        return ActionResult.SUCCESS;
    }

    /**
     * Works the sealed obstacle at {@code pos} for {@code player}, as a right-click on it would (after the hand
     * check): an intercept, a creative open or an attempt. Also used for obstacles worked through something else,
     * such as right-clicking a Guardian Beast.
     */
    public static void use(ServerWorld world, BlockPos pos, ObstacleType type, ServerPlayerEntity player) {
        if (type.intercept(world, pos, player)) {
            return;
        }
        if (player.isCreative()) {
            type.solve(world, pos, player);
            return;
        }
        attempt(player, world, pos, type);
    }

    private static void attempt(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ObstacleType type) {
        Tier tier = world.getBlockEntity(pos) instanceof ObstacleBlockEntity be ? be.tier() : Tier.MEDIUM;
        Eligibility eligibility = type.eligibility(D20.classOf(player));
        Skill skill = type.skillFor(player);
        if (eligibility.canTry() && skill == null) {
            // Nothing to roll (a bridge winch): whoever got here just works it
            solve(world, pos, type, tier, player);
            return;
        }
        if (!eligibility.canTry() || skill == null) {
            player.sendMessage(ObstacleText.cannot(type, tier), true);
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.BLOCKS, 0.8f, 0.6f);
            return;
        }
        UUID id = player.getUuid();
        if (CHANNELS.containsKey(id) || PENDING.stream().anyMatch(p -> p.player() == player)) {
            return;
        }
        long now = world.getTime();
        Long retryAt = RETRY_AT.get(id);
        if (retryAt != null && now < retryAt) {
            player.sendMessage(Text.translatable("obstacle.dndclasses.steady"), true);
            return;
        }
        if (type.blocked(player)) {
            return;
        }
        if (player.isSneaking()) {
            if (!type.allowsTakeYourTime(tier)) {
                player.sendMessage(Text.translatable("obstacle.dndclasses.focus.too_hard"), true);
            } else if (hostileNearby(world, player)) {
                player.sendMessage(Text.translatable("obstacle.dndclasses.focus.not_safe"), true);
            } else {
                CHANNELS.put(id, new Channel(player, pos.toImmutable(), type, tier, skill, player.getPos(),
                        player.getHealth(), now));
                player.sendMessage(Text.translatable("obstacle.dndclasses.focus.start",
                        TAKE_YOUR_TIME_TICKS / 20), true);
                world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.8f, 1.0f);
            }
            return;
        }

        resolve(player, world, pos, type, tier, builder(player, type, skill, tier).roll());
    }

    /** The obstacle roll: the type's skill against the tier's DC, under the type's HUD label if it has one. */
    private static D20.Builder builder(ServerPlayerEntity player, ObstacleType type, Skill skill, Tier tier) {
        D20.Builder builder = type.rollExtras(SkillCheck.builder(player, skill, tier.dc, CHECK_TAG), player);
        return type.rollLabel() == null ? builder : builder.label(type.rollLabel());
    }

    private static boolean hostileNearby(ServerWorld world, PlayerEntity player) {
        Box box = player.getBoundingBox().expand(HOSTILE_RADIUS);
        return !world.getEntitiesByClass(MobEntity.class, box, m -> m instanceof Monster && m.isAlive()).isEmpty();
    }

    private static void resolve(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ObstacleType type, Tier tier,
            D20.Roll roll) {
        String key = type.translationKey();
        long now = world.getTime();
        DnDClasses.LOGGER.info("[Obstacle] {} rolled {} {} + {} = {} vs DC {} on {} at {} -> {}",
                player.getEntityName(), type.skillFor(player), roll.natural(), roll.modifier(), roll.total(), roll.dc(),
                type.id(), pos.toShortString(), roll.outcome());
        type.onRolled(world, pos, player, roll);
        if (roll.outcome().succeeded()) {
            RETRY_AT.remove(player.getUuid());
            D20.show(player, roll, Text.translatable(key
                    + (roll.outcome() == D20.Outcome.CRITICAL ? ".critical" : ".success")));
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.2f);
            PENDING.add(new PendingOpen(player, pos.toImmutable(), type, tier, now + OPEN_DELAY_TICKS));
            return;
        }
        boolean fumble = roll.outcome() == D20.Outcome.FUMBLE;
        RETRY_AT.put(player.getUuid(), now + (fumble ? FUMBLE_COOLDOWN_TICKS : FAIL_COOLDOWN_TICKS));
        D20.show(player, roll, Text.translatable(key + (fumble ? ".fumble" : ".failure")));
        if (fumble) {
            type.onFumble(world, pos, player);
            if (type.alarmsOnFumble()) {
                ObstacleEvents.ALARM.invoker().onAlarm(world, pos, ObstacleEvents.ALARM_RADIUS, player);
            }
        } else {
            type.onFailure(world, pos, player);
        }
        DnDClasses.LOGGER.info("[Obstacle] {} took the sting: health {}, weakness {}", player.getEntityName(),
                player.getHealth(), player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.WEAKNESS));
    }

    // ---- Ticking: pending opens and focus channels ----

    private static void tick(MinecraftServer server) {
        PENDING.removeIf(ObstacleInteractions::tryOpen);
        CHANNELS.values().removeIf(ObstacleInteractions::tickChannel);
    }

    /** Opens a solved obstacle once its roll has shown. Returns true when done with it. */
    private static boolean tryOpen(PendingOpen pending) {
        ServerPlayerEntity player = pending.player();
        if (player.isRemoved()) {
            return true;
        }
        ServerWorld world = player.getWorld();
        if (world.getTime() < pending.openAt()) {
            return false;
        }
        if (!ObstacleBlock.isSealed(world.getBlockState(pending.pos()))) {
            return true;
        }
        solve(world, pending.pos(), pending.type(), pending.tier(), player);
        return true;
    }

    /**
     * Opens the group for its solver and rewards them: the tier's class XP, once per group until it's reset
     * (a group that reseals on a timer and is solved again earns nothing more).
     */
    public static void solve(ServerWorld world, BlockPos pos, ObstacleType type, Tier tier, ServerPlayerEntity solver) {
        boolean rewarded = type.solve(world, pos, solver) && !solver.isCreative()
                && ObstacleGroups.claimReward(world, pos);
        int xp = rewarded ? type.xp(tier) : 0;
        DnDClasses.LOGGER.info("[Obstacle] {} solved {} at {}: +{} class XP",
                solver.getEntityName(), type.id(), pos.toShortString(), xp);
        if (xp > 0) {
            Progression.addXp(solver, xp);
        }
        type.onSolved(world, pos, solver);
        // Only a rewarded solve counts: not a re-crank, a part payment or anything else that earns nothing
        if (rewarded) {
            ObstacleEvents.SOLVED.invoker().onSolved(solver, type, tier, pos);
        }
    }

    /** Returns true when the channel is over (finished or broken). */
    private static boolean tickChannel(Channel channel) {
        ServerPlayerEntity player = channel.player();
        if (player.isRemoved() || !player.isAlive()) {
            return true;
        }
        ServerWorld world = player.getWorld();
        if (!ObstacleBlock.isSealed(world.getBlockState(channel.pos()))) {
            return true;
        }
        if (player.getPos().squaredDistanceTo(channel.start()) > 0.25 || player.getHealth() < channel.health()) {
            player.sendMessage(Text.translatable("obstacle.dndclasses.focus.broken"), true);
            DnDClasses.LOGGER.info("[Obstacle] {} lost focus", player.getEntityName());
            return true;
        }
        long elapsed = world.getTime() - channel.startedAt();
        if (elapsed < TAKE_YOUR_TIME_TICKS) {
            if (elapsed > 0 && elapsed % 20 == 0) {
                player.sendMessage(Text.translatable("obstacle.dndclasses.focus.progress",
                        (TAKE_YOUR_TIME_TICKS - elapsed) / 20), true);
            }
            return false;
        }
        // Take 20: no crit, no fumble, no sting
        D20.Roll roll = builder(player, channel.type(), channel.skill(), channel.tier())
                .take(TAKE_YOUR_TIME_NATURAL).roll();
        if (roll.outcome().succeeded()) {
            resolve(player, world, channel.pos(), channel.type(), channel.tier(), roll);
        } else {
            D20.show(player, roll, Text.translatable("obstacle.dndclasses.focus.beyond"));
            RETRY_AT.put(player.getUuid(), world.getTime() + FAIL_COOLDOWN_TICKS);
        }
        return true;
    }

    // ---- Protected volume ----

    /** Whether pos is inside, or in the shell round, a sealed obstacle that protects its shell. */
    public static boolean isProtected(World world, BlockPos pos) {
        for (BlockPos near : BlockPos.iterate(pos.add(-PROTECTION_SHELL, -PROTECTION_SHELL, -PROTECTION_SHELL),
                pos.add(PROTECTION_SHELL, PROTECTION_SHELL, PROTECTION_SHELL))) {
            BlockState state = world.getBlockState(near);
            if (ObstacleBlock.isSealed(state) && ((ObstacleBlock) state.getBlock()).type().protectsShell()) {
                return true;
            }
        }
        return false;
    }

    private static ActionResult refusePlacement(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (player.isCreative() || player.isSpectator()) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) {
            return ActionResult.PASS;
        }
        ItemPlacementContext context = new ItemPlacementContext(player, hand, stack, hit);
        if (!context.canPlace() || !isProtected(world, context.getBlockPos())) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            player.sendMessage(Text.translatable("obstacle.dndclasses.protected.place"), true);
        }
        return ActionResult.FAIL;
    }

    private static boolean allowBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
            BlockEntity blockEntity) {
        if (player.isCreative() || state.getBlock() instanceof ObstacleBlock || !isProtected(world, pos)) {
            return true;
        }
        player.sendMessage(Text.translatable("obstacle.dndclasses.protected.break"), true);
        return false;
    }
}
