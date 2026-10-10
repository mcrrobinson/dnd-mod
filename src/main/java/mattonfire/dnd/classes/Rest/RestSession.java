package mattonfire.dnd.classes.Rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.Classes.BardSkills;
import mattonfire.dnd.entity.boss.Boss;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Short rests in progress at campfires, one per player, held in memory and
 * ticked from {@code END_SERVER_TICK} (see {@link CampfireRest#register}). A
 * rest that fails keeps nothing and doesn't use up a short rest.
 */
public final class RestSession {
    /** Ticks a short rest takes (30 s). */
    public static final int SHORT_REST_TICKS = 30 * 20;
    /** How far the player may drift from where they sat down. */
    public static final double MAX_DRIFT = 1.5;
    /** Any hostile this close interrupts the rest. */
    public static final double HOSTILE_NEAR = 6;
    /** A hostile this close interrupts the rest if it targets the player or a party member. */
    public static final double HOSTILE_TARGETING = 12;
    /** Party members resting at campfires this close together rest together (Song of Rest). */
    public static final double PARTY_RADIUS = 8;
    /** Boss bars reach 64 blocks by default; bosses further away can't be showing one. */
    private static final double BOSS_SEARCH = 96;
    private static final int CHECK_EVERY = 10;
    private static final int HUD_EVERY = 5;

    private static final Map<UUID, RestSession> SESSIONS = new HashMap<>();
    /**
     * Rests that finished in the last {@value #SHORT_REST_TICKS} ticks, so a party member who
     * sat down a little later still counts them as resting with them (a Bard who finished first
     * still gives the rest of the party Song of Rest).
     */
    private static final Map<UUID, RestSession> RECENT = new HashMap<>();

    private final UUID player;
    private final RegistryKey<World> world;
    private final BlockPos campfire;
    private final Vec3d seat;
    private int ticks;
    /** Server tick the rest finished, for {@link #RECENT}. */
    private int finishedAt;

    private RestSession(ServerPlayerEntity player, BlockPos campfire) {
        this.player = player.getUuid();
        this.world = player.getWorld().getRegistryKey();
        this.campfire = campfire.toImmutable();
        this.seat = player.getPos();
    }

    public static boolean isResting(ServerPlayerEntity player) {
        return SESSIONS.containsKey(player.getUuid());
    }

    /**
     * Starts a short rest at {@code campfire}, or tells the player why not.
     *
     * @return whether the rest started
     */
    public static boolean start(ServerPlayerEntity player, BlockPos campfire) {
        Text refusal = Rests.canShortRest(player);
        if (refusal == null) {
            Text danger = danger(player);
            if (danger != null) {
                refusal = Text.literal("You can't rest now: ").append(danger);
            }
        }
        if (refusal != null) {
            DnDClasses.LOGGER.info("[Rest] {} short rest refused: {}", player.getEntityName(), refusal.getString());
            player.sendMessage(Text.literal("").append(refusal).formatted(Formatting.RED), true);
            return false;
        }
        SESSIONS.put(player.getUuid(), new RestSession(player, campfire));
        DnDClasses.LOGGER.info("[Rest] {} started a short rest at {}", player.getEntityName(), campfire.toShortString());
        player.sendMessage(Text.literal("Short rest 0 / 30 s").formatted(Formatting.GOLD), true);
        RestSync.setSession(player, RestKind.SHORT, 0, SHORT_REST_TICKS);
        return true;
    }

    /** Ends the player's rest without its benefits, telling them why (if a reason is given). */
    public static void cancel(ServerPlayerEntity player, String reason) {
        if (SESSIONS.remove(player.getUuid()) == null) {
            return;
        }
        RestSync.clearSession(player);
        DnDClasses.LOGGER.info("[Rest] {} short rest interrupted: {}", player.getEntityName(), reason);
        if (reason != null) {
            player.sendMessage(Text.literal("Short rest interrupted: " + reason).formatted(Formatting.RED), true);
        }
    }

    /** Drops the rest without a message (disconnect, death, class reset). */
    public static void forget(UUID player) {
        SESSIONS.remove(player);
        RECENT.remove(player);
    }

    static void tick(MinecraftServer server) {
        RECENT.values().removeIf(session -> server.getTicks() - session.finishedAt > SHORT_REST_TICKS);
        if (SESSIONS.isEmpty()) {
            return;
        }
        List<ServerPlayerEntity> finished = new ArrayList<>();
        for (RestSession session : new ArrayList<>(SESSIONS.values())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(session.player);
            if (player == null || !player.isAlive() || player.isRemoved()) {
                SESSIONS.remove(session.player);
                continue;
            }
            String reason = session.check(player);
            if (reason != null) {
                cancel(player, reason);
                continue;
            }
            session.ticks++;
            if (session.ticks >= SHORT_REST_TICKS) {
                finished.add(player);
                continue;
            }
            if (session.ticks % HUD_EVERY == 0) {
                RestSync.setSession(player, RestKind.SHORT, session.ticks, SHORT_REST_TICKS);
            }
            if (session.ticks % 20 == 0) {
                player.sendMessage(Text.literal("Short rest " + session.ticks / 20 + " / " + SHORT_REST_TICKS / 20
                        + " s").formatted(Formatting.GOLD), true);
                ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(),
                        player.getY() + 0.2, player.getZ(), 2, 0.6, 0.1, 0.6, 0);
            }
        }
        // Companions are worked out before anyone's session ends, so two rests finishing on the
        // same tick still see each other.
        Map<ServerPlayerEntity, List<ServerPlayerEntity>> companions = new HashMap<>();
        for (ServerPlayerEntity player : finished) {
            companions.put(player, companions(player));
        }
        for (ServerPlayerEntity player : finished) {
            RestSession session = SESSIONS.remove(player.getUuid());
            session.finishedAt = server.getTicks();
            RECENT.put(player.getUuid(), session);
            RestSync.clearSession(player);
            List<ServerPlayerEntity> resting = companions.get(player);
            DnDClasses.LOGGER.info("[Rest] {} finished a short rest with {}", player.getEntityName(),
                    resting.stream().map(ServerPlayerEntity::getEntityName).toList());
            Rests.complete(player, RestKind.SHORT, RestSource.CAMPFIRE, resting, Rests.day(player));
            player.sendMessage(Text.literal("Short rest complete").formatted(Formatting.GOLD), true);
            BardSkills.songOfRest(player, resting);
        }
    }

    /**
     * Everyone resting with the player, the player first: party members resting (or who
     * finished a rest in the last {@value #SHORT_REST_TICKS} ticks) at a campfire within
     * {@value #PARTY_RADIUS} blocks of the player's.
     */
    public static List<ServerPlayerEntity> companions(ServerPlayerEntity player) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        result.add(player);
        RestSession own = SESSIONS.get(player.getUuid());
        if (own == null) {
            return result;
        }
        double radiusSq = PARTY_RADIUS * PARTY_RADIUS;
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(player, 64)) {
            RestSession other = SESSIONS.getOrDefault(member.getUuid(), RECENT.get(member.getUuid()));
            if (other != null && other.world == own.world
                    && other.campfire.getSquaredDistance(own.campfire) <= radiusSq) {
                result.add(member);
            }
        }
        return result;
    }

    /** Why the rest stops this tick, or null to carry on. */
    private String check(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() != world) {
            return "you left";
        }
        if (player.getPos().squaredDistanceTo(seat) > MAX_DRIFT * MAX_DRIFT) {
            return "you moved";
        }
        BlockState state = player.getWorld().getBlockState(campfire);
        if (!(state.getBlock() instanceof CampfireBlock) || !state.get(CampfireBlock.LIT)) {
            return "the campfire went out";
        }
        if ((ticks + 1) % CHECK_EVERY == 0) {
            Text danger = danger(player);
            if (danger == null) {
                danger = RestEvents.ALLOW_REST.invoker().refuse(player, RestKind.SHORT);
            }
            if (danger != null) {
                return danger.getString();
            }
        }
        return null;
    }

    /**
     * Monsters or a boss bar, which stop a rest starting or carrying on. Other systems'
     * vetoes ({@link RestEvents#ALLOW_REST}: dungeons, the downed state) are asked by
     * {@link Rests#canShortRest} at the start and again every {@value #CHECK_EVERY} ticks.
     */
    static Text danger(ServerPlayerEntity player) {
        if (hostileNear(player)) {
            return Text.literal("there are monsters nearby");
        }
        if (inBossFight(player)) {
            return Text.literal("you're in a boss fight");
        }
        return null;
    }

    private static boolean hostileNear(ServerPlayerEntity player) {
        Box area = player.getBoundingBox().expand(HOSTILE_TARGETING);
        double nearSq = HOSTILE_NEAR * HOSTILE_NEAR;
        double targetingSq = HOSTILE_TARGETING * HOSTILE_TARGETING;
        for (LivingEntity mob : player.getWorld().getEntitiesByClass(LivingEntity.class, area,
                e -> e instanceof Monster && e.isAlive())) {
            double distSq = mob.squaredDistanceTo(player);
            if (distSq <= nearSq) {
                return true;
            }
            if (distSq <= targetingSq && mob instanceof MobEntity hostile && hostile.getTarget() != null
                    && (hostile.getTarget() == player || PartyManager.areInSameParty(player, hostile.getTarget()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean inBossFight(ServerPlayerEntity player) {
        for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(BOSS_SEARCH), e -> e instanceof Boss)) {
            if (((Boss) mob).getBossFight().getBar().getPlayers().contains(player)) {
                return true;
            }
        }
        return false;
    }
}
