package mattonfire.dnd.classes.Rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Party camps: a night-time long rest anywhere, around a lit campfire, with a watch roll and the risk of
 * an ambush.
 *
 * <ol>
 * <li>Right-click a lit campfire with a {@link #BEDROLL} at night ({@link #NIGHT_START}-{@link #NIGHT_END}).
 * Party members who do the same within {@value #RADIUS} blocks in the first {@value #JOIN_TICKS} ticks
 * join the camp.</li>
 * <li>When the join window closes the best watcher rolls ({@link CampWatch#roll}). A failed watch schedules
 * an ambush 15-45 s into the camp.</li>
 * <li>The ambush pauses the camp: campers can move and fight. Once no ambusher is alive within
 * {@value CampWatch#AMBUSH_RADIUS} blocks and every camper has sat back down (sneak + right-click the
 * campfire), the camp carries on.</li>
 * <li>After {@value #CAMP_TICKS} ticks of camping each camper gets a long rest ({@link RestSource#CAMP}).</li>
 * </ol>
 *
 * A camper leaves with no rest if they go more than {@value #RADIUS} blocks from the fire (
 * {@value CampWatch#AMBUSH_RADIUS} during an ambush), take damage outside an ambush, sneak-click the fire
 * again, or another system vetoes the rest ({@link RestEvents#ALLOW_REST}). Dawn or the fire going out
 * ends the whole camp. Held in memory, ticked from {@code END_SERVER_TICK}.
 */
public final class PartyCamp {
    /** Ticks of camping for a long rest (60 s). */
    public static final int CAMP_TICKS = 60 * 20;
    /** Ticks after a camp starts in which party members can join it (10 s). */
    public static final int JOIN_TICKS = 10 * 20;
    /** Campers stay within this many blocks of the fire; party members this close can join. */
    public static final double RADIUS = 8;
    /** The vanilla sleep window: {@code timeOfDay % 24000} from here... */
    public static final long NIGHT_START = 12542;
    /** ...to here. */
    public static final long NIGHT_END = 23460;
    private static final int CHECK_EVERY = 10;
    private static final int HUD_EVERY = 5;

    public static final Item BEDROLL = Registry.register(Registries.ITEM,
            new Identifier(DnDClasses.MOD_ID, "bedroll"), new BedrollItem(new FabricItemSettings().maxCount(1)));

    /** Camps by id, in start order. */
    private static final Map<UUID, PartyCamp> CAMPS = new LinkedHashMap<>();
    /** Camper -> their camp. */
    private static final Map<UUID, PartyCamp> BY_CAMPER = new HashMap<>();

    enum Phase {
        /** Camping (the join window is the first {@link #JOIN_TICKS} of it). */
        CAMPING,
        /** Ambushed: progress frozen, campers free to fight. */
        AMBUSH
    }

    final UUID id = UUID.randomUUID();
    final MinecraftServer server;
    final RegistryKey<World> world;
    final BlockPos campfire;
    final long startedDay;
    /** Camper -> whether they're sitting at the fire. */
    final Map<UUID, Boolean> campers = new LinkedHashMap<>();
    Phase phase = Phase.CAMPING;
    /** Ticks of camping done (frozen during an ambush). */
    int progress;
    /** Whether the watch roll has been made. */
    boolean watched;
    /** Camp progress at which the ambush comes, -1 for none. */
    int ambushAt = -1;
    /** Whether the watch rolled a natural 1 (a bigger ambush with an elite). */
    boolean bigAmbush;
    /** The ambushers still being tracked. */
    final List<UUID> ambushers = new ArrayList<>();
    /** Whether the campers have been told the ambushers are gone. */
    boolean toldClear;

    private PartyCamp(ServerPlayerEntity founder, BlockPos campfire) {
        this.server = founder.getServer();
        this.world = founder.getWorld().getRegistryKey();
        this.campfire = campfire.toImmutable();
        this.startedDay = Rests.day(founder);
    }

    // ---------------------------------------------------------------- public API

    public static boolean inCamp(ServerPlayerEntity player) {
        return BY_CAMPER.containsKey(player.getUuid());
    }

    /** Whether the player's camp is paused by an ambush. */
    public static boolean ambushed(ServerPlayerEntity player) {
        PartyCamp camp = BY_CAMPER.get(player.getUuid());
        return camp != null && camp.phase == Phase.AMBUSH;
    }

    /** Whether it's camping time: the vanilla sleep window on the overworld clock (as {@link Rests#day}). */
    public static boolean isNight(MinecraftServer server) {
        long time = server.getOverworld().getTimeOfDay() % Rests.TICKS_PER_DAY;
        return time >= NIGHT_START && time <= NIGHT_END;
    }

    /**
     * A player right-clicked a lit campfire with a Bedroll, or a camper sneak-clicked one. Starts or joins a
     * camp, sits back down after an ambush, or (sitting, outside an ambush) gets up and leaves the camp.
     */
    public static void use(ServerPlayerEntity player, BlockPos fire) {
        PartyCamp own = BY_CAMPER.get(player.getUuid());
        if (own != null) {
            own.click(player, fire);
            return;
        }
        Text refusal = refusal(player);
        if (refusal != null) {
            DnDClasses.LOGGER.info("[Camp] {} camp refused: {}", player.getEntityName(), refusal.getString());
            player.sendMessage(Text.literal("").append(refusal).formatted(Formatting.RED), true);
            return;
        }
        PartyCamp join = partyCampNear(player, fire);
        if (join != null) {
            if (join.progress >= JOIN_TICKS || join.watched) {
                player.sendMessage(Text.literal("Your party has already settled in: you're too late to join their camp.")
                        .formatted(Formatting.RED), true);
                return;
            }
            join.add(player);
            DnDClasses.LOGGER.info("[Camp] {} joined camp {} at {}", player.getEntityName(), join.id,
                    join.campfire.toShortString());
            join.broadcast(Text.literal(player.getEntityName() + " joins the camp.").formatted(Formatting.GOLD), false);
            return;
        }
        PartyCamp camp = new PartyCamp(player, fire);
        CAMPS.put(camp.id, camp);
        camp.add(player);
        DnDClasses.LOGGER.info("[Camp] {} made camp {} at {} (day {})", player.getEntityName(), camp.id,
                fire.toShortString(), camp.startedDay);
        player.sendMessage(Text.literal("You make camp. Party members have " + JOIN_TICKS / 20
                + " s to join you by the fire.").formatted(Formatting.GOLD), false);
    }

    /** Why the player can't make or join a camp now, or null. */
    @Nullable
    static Text refusal(ServerPlayerEntity player) {
        if (!isNight(player.getServer())) {
            return Text.literal("You can only make camp at night.");
        }
        if (BedRest.isResting(player) || player.isSleeping()) {
            return Text.literal("You're already resting.");
        }
        Text refusal = Rests.canLongRest(player);
        if (refusal != null) {
            return refusal;
        }
        Text danger = RestSession.danger(player);
        return danger == null ? null : Text.literal("You can't make camp now: ").append(danger);
    }

    /** Takes the player out of their camp with no rest, telling them why (if a reason is given). */
    public static void leave(ServerPlayerEntity player, @Nullable String reason) {
        PartyCamp camp = BY_CAMPER.get(player.getUuid());
        if (camp == null) {
            return;
        }
        camp.remove(player.getUuid());
        RestSync.clearSession(player);
        DnDClasses.LOGGER.info("[Camp] {} left camp {}: {}", player.getEntityName(), camp.id, reason);
        if (reason != null) {
            player.sendMessage(Text.literal("You break camp: " + reason + ". No long rest.").formatted(Formatting.RED),
                    true);
        }
    }

    /** Drops the player from any camp without a message (disconnect). */
    public static void forget(UUID player) {
        PartyCamp camp = BY_CAMPER.get(player);
        if (camp != null) {
            camp.remove(player);
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PartyCamp::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CAMPS.clear();
            BY_CAMPER.clear();
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0 && entity instanceof ServerPlayerEntity player && inCamp(player) && !ambushed(player)) {
                leave(player, "you were hurt");
            }
            return true;
        });
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> entries.add(BEDROLL));
    }

    // ---------------------------------------------------------------- camp internals

    /** The camp of one of the player's party members whose fire is within {@value #RADIUS} blocks of this one. */
    @Nullable
    private static PartyCamp partyCampNear(ServerPlayerEntity player, BlockPos fire) {
        for (PartyCamp camp : CAMPS.values()) {
            if (camp.world != player.getWorld().getRegistryKey()
                    || camp.campfire.getSquaredDistance(fire) > RADIUS * RADIUS) {
                continue;
            }
            for (UUID camper : camp.campers.keySet()) {
                ServerPlayerEntity other = player.getServer().getPlayerManager().getPlayer(camper);
                if (PartyManager.areInSameParty(player, other)) {
                    return camp;
                }
            }
        }
        return null;
    }

    private void add(ServerPlayerEntity player) {
        campers.put(player.getUuid(), true);
        BY_CAMPER.put(player.getUuid(), this);
        RestSync.setSession(player, RestKind.LONG, progress, CAMP_TICKS);
        player.sendMessage(Text.literal("Camping " + progress / 20 + " / " + CAMP_TICKS / 20 + " s")
                .formatted(Formatting.AQUA), true);
    }

    private void remove(UUID player) {
        campers.remove(player);
        BY_CAMPER.remove(player);
        if (campers.isEmpty()) {
            end();
        }
    }

    /** Drops the whole camp. */
    private void end() {
        CAMPS.remove(id);
        for (UUID camper : campers.keySet()) {
            BY_CAMPER.remove(camper);
        }
        campers.clear();
    }

    private void click(ServerPlayerEntity player, BlockPos fire) {
        if (phase == Phase.AMBUSH) {
            if (fire.getSquaredDistance(campfire) > RADIUS * RADIUS) {
                player.sendMessage(Text.literal("That's not your camp's fire.").formatted(Formatting.RED), true);
                return;
            }
            if (!ambushers.isEmpty()) {
                player.sendMessage(Text.literal("You can't settle down with ambushers about.")
                        .formatted(Formatting.RED), true);
                return;
            }
            if (Boolean.TRUE.equals(campers.get(player.getUuid()))) {
                return;
            }
            campers.put(player.getUuid(), true);
            DnDClasses.LOGGER.info("[Camp] {} sits back down at camp {}", player.getEntityName(), id);
            RestSync.setSession(player, RestKind.LONG, progress, CAMP_TICKS);
            long sitting = campers.values().stream().filter(b -> b).count();
            broadcast(Text.literal(player.getEntityName() + " sits back down by the fire (" + sitting + " / "
                    + campers.size() + ")").formatted(Formatting.GOLD), true);
            return;
        }
        leave(player, "you got up");
    }

    static void tick(MinecraftServer server) {
        if (CAMPS.isEmpty()) {
            return;
        }
        for (PartyCamp camp : new ArrayList<>(CAMPS.values())) {
            camp.tickCamp(server);
        }
    }

    private void tickCamp(MinecraftServer server) {
        ServerWorld level = server.getWorld(world);
        if (level == null) {
            end();
            return;
        }
        // Campers who went away
        for (UUID uuid : new ArrayList<>(campers.keySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null || !player.isAlive() || player.isRemoved()) {
                if (player != null) {
                    RestSync.clearSession(player);
                }
                remove(uuid);
                continue;
            }
            String reason = check(player);
            if (reason != null) {
                leave(player, reason);
            }
        }
        if (campers.isEmpty()) {
            return;
        }
        if (!CampfireRest.isLitCampfire(level.getBlockState(campfire))) {
            endAll(server, "the campfire went out");
            return;
        }
        if (!isNight(server)) {
            endAll(server, "dawn came before you'd rested");
            return;
        }
        if (phase == Phase.AMBUSH) {
            tickAmbush(server, level);
            return;
        }

        progress++;
        if (!watched && progress >= JOIN_TICKS) {
            watched = true;
            CampWatch.roll(this, campers(server));
        }
        if (ambushAt >= 0 && progress >= ambushAt) {
            ambushAt = -1;
            startAmbush(server, level);
            return;
        }
        if (progress >= CAMP_TICKS) {
            finish(server);
            return;
        }
        for (ServerPlayerEntity player : campers(server)) {
            if (progress % HUD_EVERY == 0) {
                RestSync.setSession(player, RestKind.LONG, progress, CAMP_TICKS);
            }
            if (progress % 20 == 0) {
                player.sendMessage(Text.literal("Camping " + progress / 20 + " / " + CAMP_TICKS / 20 + " s")
                        .formatted(Formatting.AQUA), true);
            }
        }
        if (progress % 40 == 0) {
            level.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, campfire.getX() + 0.5, campfire.getY() + 1.0,
                    campfire.getZ() + 0.5, 1, 0.1, 0.2, 0.1, 0.01);
        }
    }

    /** Why the camper leaves this tick, or null. */
    @Nullable
    private String check(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() != world) {
            return "you left";
        }
        double limit = phase == Phase.AMBUSH ? CampWatch.AMBUSH_RADIUS : RADIUS;
        if (player.squaredDistanceTo(Vec3d.ofCenter(campfire)) > limit * limit) {
            return "you wandered off";
        }
        if (phase != Phase.AMBUSH && (progress + 1) % CHECK_EVERY == 0) {
            Text veto = RestEvents.ALLOW_REST.invoker().refuse(player, RestKind.LONG);
            if (veto != null) {
                return veto.getString();
            }
        }
        return null;
    }

    private void startAmbush(MinecraftServer server, ServerWorld level) {
        List<ServerPlayerEntity> campers = campers(server);
        List<LivingEntity> spawned = CampWatch.spawnAmbush(level, campfire, campers, bigAmbush);
        if (spawned.isEmpty()) {
            DnDClasses.LOGGER.info("[Camp] camp {}: no room for an ambush, the night stays quiet", id);
            return;
        }
        phase = Phase.AMBUSH;
        toldClear = false;
        for (LivingEntity mob : spawned) {
            ambushers.add(mob.getUuid());
        }
        for (UUID uuid : this.campers.keySet()) {
            this.campers.put(uuid, false);
        }
        for (ServerPlayerEntity player : campers) {
            RestSync.clearSession(player);
            player.sendMessage(Text.literal("Ambush! ").formatted(Formatting.RED, Formatting.BOLD)
                    .append(Text.literal(spawned.size() + " shapes rush out of the dark.").formatted(Formatting.RED)),
                    false);
        }
    }

    private void tickAmbush(MinecraftServer server, ServerWorld level) {
        Vec3d centre = Vec3d.ofCenter(campfire);
        ambushers.removeIf(uuid -> {
            Entity mob = level.getEntity(uuid);
            return mob == null || !mob.isAlive()
                    || mob.squaredDistanceTo(centre) > CampWatch.AMBUSH_RADIUS * CampWatch.AMBUSH_RADIUS;
        });
        List<ServerPlayerEntity> campers = campers(server);
        long sitting = this.campers.values().stream().filter(b -> b).count();
        if (ambushers.isEmpty() && !toldClear) {
            toldClear = true;
            DnDClasses.LOGGER.info("[Camp] camp {}: the ambush is over", id);
            broadcast(Text.literal("The ambushers are gone. Sneak + right-click the campfire to sit back down.")
                    .formatted(Formatting.GOLD), false);
        }
        if (ambushers.isEmpty() && sitting == this.campers.size()) {
            phase = Phase.CAMPING;
            DnDClasses.LOGGER.info("[Camp] camp {} resumes at {} / {} ticks", id, progress, CAMP_TICKS);
            for (ServerPlayerEntity player : campers) {
                Text veto = RestEvents.ALLOW_REST.invoker().refuse(player, RestKind.LONG);
                if (veto != null) {
                    leave(player, veto.getString());
                    continue;
                }
                RestSync.setSession(player, RestKind.LONG, progress, CAMP_TICKS);
                player.sendMessage(Text.literal("The camp settles down again.").formatted(Formatting.GOLD), true);
            }
            return;
        }
        if (level.getTime() % 20 == 0) {
            Text bar = ambushers.isEmpty()
                    ? Text.literal("Sit back down by the fire (" + sitting + " / " + this.campers.size() + ")")
                            .formatted(Formatting.GOLD)
                    : Text.literal("Ambush! " + ambushers.size() + " left").formatted(Formatting.RED);
            for (ServerPlayerEntity player : campers) {
                if (!Boolean.TRUE.equals(this.campers.get(player.getUuid())) || ambushers.isEmpty()) {
                    player.sendMessage(bar, true);
                }
            }
        }
    }

    private void finish(MinecraftServer server) {
        List<ServerPlayerEntity> campers = campers(server);
        end();
        DnDClasses.LOGGER.info("[Camp] camp {} finished with {}", id,
                campers.stream().map(ServerPlayerEntity::getEntityName).toList());
        for (ServerPlayerEntity player : campers) {
            RestSync.clearSession(player);
            Text refusal = Rests.canLongRest(player, startedDay);
            if (refusal != null) {
                player.sendMessage(Text.literal("").append(refusal).formatted(Formatting.RED), true);
                continue;
            }
            Rests.complete(player, RestKind.LONG, RestSource.CAMP, campers, startedDay);
            player.sendMessage(Text.literal("You wake rested by the embers.").formatted(Formatting.GOLD), true);
        }
    }

    private void endAll(MinecraftServer server, String reason) {
        for (ServerPlayerEntity player : campers(server)) {
            leave(player, reason);
        }
        end();
    }

    /** The online campers. */
    List<ServerPlayerEntity> campers(MinecraftServer server) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        for (UUID uuid : campers.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                result.add(player);
            }
        }
        return result;
    }

    private void broadcast(Text text, boolean actionBar) {
        for (ServerPlayerEntity player : campers(server)) {
            player.sendMessage(text, actionBar);
        }
    }

    /** Called by {@link CampWatch} with the watch result. */
    void scheduleAmbush(int at, boolean big) {
        this.ambushAt = at;
        this.bigAmbush = big;
    }

    BlockPos campfire() {
        return campfire;
    }

    /** Debug, {@code -Ddndclasses.debug.forceAmbush=true}: every watch fails. */
    static boolean forceAmbush() {
        return Boolean.getBoolean("dndclasses.debug.forceAmbush");
    }
}
