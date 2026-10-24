package mattonfire.dnd.entity.hireling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.DownedEvents;
import mattonfire.dnd.classes.Misc.BardCompanions;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.entity.HirelingEntity;
import mattonfire.dnd.entity.HobbitEntity;
import mattonfire.dnd.faction.ReputationTier;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.Nullable;

/**
 * Hirelings: the entity type, hiring and paying, friendly fire, loyalty events, Downed allies, persistence
 * across logouts and dimensions, and the HUD packet. See {@code docs/systems/hirelings.md}.
 *
 * <p>Hooks for the role abilities and party synergies ticket: {@link #hirelingsOf} (the loaded hirelings of a
 * player) and {@link HirelingEntity#role()}.
 */
public final class Hirelings {
    public static final EntityType<HirelingEntity> HIRELING = Registry.register(Registries.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "hireling"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, HirelingEntity::new)
                    .dimensions(EntityDimensions.changing(0.6F, 1.95F))
                    .trackRangeChunks(10)
                    .build());
    public static final Item HIRELING_SPAWN_EGG = Registry.register(Registries.ITEM,
            new Identifier(DnDClasses.MOD_ID, "hireling_spawn_egg"),
            new SpawnEggItem(HIRELING, 0x6B4A2B, 0xC9A45C, new FabricItemSettings()));

    /** S2C to an owner every 10 ticks: their hirelings for the HUD. */
    public static final Identifier S2C_HUD = new Identifier(DnDClasses.MOD_ID, "hireling_hud");
    public static final byte HUD_WAITING = 1;
    public static final byte HUD_KNOCKED_OUT = 2;
    public static final byte HUD_DEVOTED = 4;
    /** Not loaded right now (left behind, or in an unloaded area). */
    public static final byte HUD_AWAY = 8;

    public static final int MAX_PER_PLAYER = 2;
    public static final int MAX_PER_PARTY = 4;
    /** {@code /hireling hire} reaches candidates this close. */
    public static final double HIRE_RANGE = 8.0D;
    /** Followers this close to their owner go through a portal with them. */
    public static final double PORTAL_RANGE = 16.0D;
    /** A conscious follower this close keeps its Downed owner (or party member) Downed instead of a Last Stand. */
    public static final double ALLY_RANGE = DownedEvents.ALLY_RANGE;

    private static final List<Entity> PENDING_DISCARD = new ArrayList<>();
    /** Victim -> hirelings that hurt it (and when), for kill and assist loyalty. */
    private static final Map<UUID, Map<UUID, Long>> ASSISTS = new HashMap<>();
    private static final long ASSIST_TICKS = 20 * 30;
    /** Each owner's position at the end of the last tick, for followers going through a portal with them. */
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();
    /** The owner whose hirelings {@link #onJoin} is recreating (not in the player list yet when JOIN fires). */
    @Nullable
    private static UUID joining;
    /** Owners sent a non-empty HUD list, so they get one empty list when they have none left. */
    private static final Set<UUID> HUD_SENT = new HashSet<>();

    private Hirelings() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(HIRELING, HirelingEntity.createHirelingAttributes());
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> entries.add(HIRELING_SPAWN_EGG));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> HirelingCommand.register(dispatcher));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Hirelings::allowDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof HirelingEntity hireling && hireling.isHired() && !bypasses(source, amount)) {
                hireling.knockOut();
                return false;
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(Hirelings::afterDeath);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (killer instanceof ServerPlayerEntity player && (killed instanceof HobbitEntity || killed instanceof VillagerEntity)) {
                for (HirelingEntity hireling : hirelingsOf(player)) {
                    hireling.changeLoyalty(HirelingEntity.LOYALTY_KIN_KILLED, "owner killed " + killed.getName().getString());
                }
            }
        });
        DownedEvents.EXTRA_ALLY.register(Hirelings::keepsDowned);

        ServerEntityEvents.ENTITY_LOAD.register(Hirelings::onLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof HirelingEntity hireling && hireling.isHired()
                    && entity.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                refreshRecord(hireling);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onLeave(handler.getPlayer()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(Hirelings::followThroughPortal);
        ServerTickEvents.END_SERVER_TICK.register(Hirelings::tick);
    }

    // ---- Queries ----

    /** True if the hit kills outright: the void and {@code /kill} (as for players' death saves). */
    public static boolean bypasses(DamageSource source, float amount) {
        return DownedEvents.bypasses(source, amount);
    }

    /** The player's hirelings that are loaded right now, in any world. */
    public static List<HirelingEntity> hirelingsOf(ServerPlayerEntity player) {
        List<HirelingEntity> list = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return list;
        }
        for (HirelingRegistry.Record record : HirelingRegistry.get(server).of(player.getUuid())) {
            HirelingEntity hireling = loaded(server, record);
            if (hireling != null) {
                list.add(hireling);
            }
        }
        return list;
    }

    @Nullable
    public static HirelingEntity loaded(MinecraftServer server, HirelingRegistry.Record record) {
        ServerWorld world = server.getWorld(record.dimension);
        Entity entity = world == null ? null : world.getEntity(record.uuid);
        if (entity instanceof HirelingEntity hireling && hireling.isAlive() && hireling.isHired()) {
            return hireling;
        }
        for (ServerWorld other : server.getWorlds()) {
            entity = other.getEntity(record.uuid);
            if (entity instanceof HirelingEntity hireling && hireling.isAlive() && hireling.isHired()) {
                return hireling;
            }
        }
        return null;
    }

    public static boolean sameParty(@Nullable MinecraftServer server, @Nullable UUID a, @Nullable UUID b) {
        if (server == null || a == null || b == null || a.equals(b)) {
            return false;
        }
        Party party = PartyManager.get(server).getParty(a);
        return party != null && party.contains(b);
    }

    /**
     * Whether {@code entity} is on the side of the player {@code owner}: the owner, a party member, any hireling,
     * or a companion or pet of the owner or a party member.
     */
    public static boolean isFriend(@Nullable MinecraftServer server, @Nullable UUID owner, @Nullable Entity entity) {
        if (owner == null || entity == null) {
            return false;
        }
        if (owner.equals(entity.getUuid()) || entity instanceof HirelingEntity) {
            return true;
        }
        if (entity instanceof PlayerEntity) {
            return sameParty(server, owner, entity.getUuid());
        }
        UUID keeper = BardCompanions.ownerOf(entity);
        if (keeper == null && entity instanceof Tameable tameable) {
            keeper = tameable.getOwnerUuid();
        }
        return keeper != null && (owner.equals(keeper) || sameParty(server, owner, keeper));
    }

    /** How many hirelings the player has (loaded or not). */
    public static int count(MinecraftServer server, UUID player) {
        return HirelingRegistry.get(server).of(player).size();
    }

    /** Hirelings of the player's whole party, the player included. */
    public static int partyCount(MinecraftServer server, UUID player) {
        Party party = PartyManager.get(server).getParty(player);
        if (party == null) {
            return count(server, player);
        }
        int total = 0;
        for (UUID member : party.getMembers()) {
            total += count(server, member);
        }
        return total;
    }

    // ---- Hiring and paying ----

    /** Hires the candidate for the player if they can: range, standing, limits and gold. */
    public static boolean hire(ServerPlayerEntity player, HirelingEntity candidate) {
        MinecraftServer server = player.getServer();
        if (!candidate.isForHire() || !candidate.isAlive()) {
            player.sendMessage(Text.translatable("hireling.dndclasses.not_for_hire").formatted(Formatting.RED), false);
            return false;
        }
        if (candidate.world != player.world || player.squaredDistanceTo(candidate) > HIRE_RANGE * HIRE_RANGE) {
            player.sendMessage(Text.translatable("hireling.dndclasses.too_far", candidate.firstName()).formatted(Formatting.RED), false);
            return false;
        }
        if (HirelingWages.hobbitTier(player) == ReputationTier.HOSTILE) {
            player.sendMessage(Text.translatable("hireling.dndclasses.refuse.hostile").formatted(Formatting.RED), false);
            return false;
        }
        if (count(server, player.getUuid()) >= MAX_PER_PLAYER) {
            player.sendMessage(Text.translatable("hireling.dndclasses.refuse.limit", MAX_PER_PLAYER).formatted(Formatting.RED), false);
            return false;
        }
        if (partyCount(server, player.getUuid()) >= MAX_PER_PARTY) {
            player.sendMessage(Text.translatable("hireling.dndclasses.refuse.party_limit", MAX_PER_PARTY).formatted(Formatting.RED), false);
            return false;
        }
        int fee = HirelingWages.fee(candidate.role(), player);
        if (!HirelingWages.take(player, fee)) {
            player.sendMessage(Text.translatable("hireling.dndclasses.refuse.gold", HirelingWages.describe(fee))
                    .formatted(Formatting.RED), false);
            return false;
        }
        candidate.hireBy(player, fee);
        HirelingRegistry.get(server).add(player.getUuid(), candidate);
        player.sendMessage(Text.translatable("hireling.dndclasses.hired", candidate.getDisplayName(), HirelingWages.describe(fee))
                .formatted(Formatting.GREEN), false);
        player.world.playSound(null, candidate.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        DnDClasses.LOGGER.info("[Hireling] {} hired {} ({}) for {} nuggets", player.getEntityName(),
                candidate.hirelingName(), candidate.role().id(), fee);
        return true;
    }

    /**
     * Admin spawn: a hired hireling with a {@link HirelingRole#FEE_DAYS}-day purse, no gold taken, next to the
     * owner.
     */
    public static HirelingEntity spawnHired(ServerPlayerEntity owner, HirelingRole role, HirelingRace race, @Nullable String firstName) {
        ServerWorld world = owner.getWorld();
        HirelingEntity hireling = HIRELING.create(world);
        String name = firstName != null && !firstName.isBlank()
                ? firstName + " " + HirelingNames.surname(race, world.getRandom())
                : HirelingNames.pick(race, world.getRandom());
        hireling.setup(role, race, world.getRandom().nextInt(race.skins), name);
        hireling.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), owner.getYaw(), 0.0F);
        hireling.hireBy(owner, HirelingWages.fee(role, owner));
        // The record first: ENTITY_LOAD discards a hired hireling that has none.
        HirelingRegistry.get(owner.getServer()).add(owner.getUuid(), hireling);
        world.spawnEntity(hireling);
        refreshRecord(hireling);
        DnDClasses.LOGGER.info("[Hireling] spawned {} ({}, {}) for {}", hireling.hirelingName(), role.id(), race.id(),
                owner.getEntityName());
        return hireling;
    }

    /** Pays {@code days} days' wages into the purse from the player's gold, up to {@value HirelingWages#MAX_PURSE_DAYS} days. */
    public static boolean payDays(ServerPlayerEntity player, HirelingEntity hireling, int days) {
        int wage = HirelingWages.wage(hireling.role(), player);
        int room = wage * HirelingWages.MAX_PURSE_DAYS - hireling.purse();
        int amount = Math.min(wage * days, room);
        if (amount <= 0) {
            player.sendMessage(Text.translatable("hireling.dndclasses.purse_full", hireling.firstName()).formatted(Formatting.YELLOW), false);
            return false;
        }
        if (!HirelingWages.take(player, amount)) {
            player.sendMessage(Text.translatable("hireling.dndclasses.refuse.gold", HirelingWages.describe(amount))
                    .formatted(Formatting.RED), false);
            return false;
        }
        hireling.setPurse(hireling.purse() + amount);
        refreshRecord(hireling);
        player.sendMessage(Text.translatable("hireling.dndclasses.paid", hireling.firstName(), HirelingWages.describe(amount),
                hireling.daysPaid()).formatted(Formatting.GREEN), false);
        DnDClasses.LOGGER.info("[Hireling] {} paid {} {} nuggets (purse {})", player.getEntityName(), hireling.firstName(),
                amount, hireling.purse());
        return true;
    }

    /** Right-click with gold: the whole stack goes in, as far as the purse's cap allows. */
    public static void payFromHand(ServerPlayerEntity player, HirelingEntity hireling, ItemStack held) {
        int value = HirelingWages.valueOf(held.getItem());
        int wage = HirelingWages.wage(hireling.role(), player);
        int room = wage * HirelingWages.MAX_PURSE_DAYS - hireling.purse();
        int items = Math.min(held.getCount(), room / value);
        if (items <= 0) {
            player.sendMessage(Text.translatable("hireling.dndclasses.purse_full", hireling.firstName()).formatted(Formatting.YELLOW), false);
            return;
        }
        if (!player.getAbilities().creativeMode) {
            held.decrement(items);
        }
        hireling.setPurse(hireling.purse() + items * value);
        refreshRecord(hireling);
        player.sendMessage(Text.translatable("hireling.dndclasses.paid", hireling.firstName(),
                HirelingWages.describe(items * value), hireling.daysPaid()).formatted(Formatting.GREEN), false);
        DnDClasses.LOGGER.info("[Hireling] {} paid {} {} nuggets by hand (purse {})", player.getEntityName(),
                hireling.firstName(), items * value, hireling.purse());
    }

    // ---- Chat cards ----

    /** The candidate's card: who, what, the wage and a [Hire] button. */
    public static void sendHireCard(ServerPlayerEntity player, HirelingEntity hireling) {
        HirelingRole role = hireling.role();
        int wage = HirelingWages.wage(role, player);
        int fee = wage * HirelingRole.FEE_DAYS;
        player.sendMessage(hireling.getDisplayName().copy().formatted(Formatting.GOLD), false);
        player.sendMessage(Text.empty().append(hireling.race().text()).append(" ").append(role.partyRole.text())
                .append(Text.literal(" · " + (int) Math.ceil(hireling.getHealth()) + "/" + (int) hireling.getMaxHealth()
                        + " health").formatted(Formatting.GRAY)), false);
        player.sendMessage(role.blurb().formatted(Formatting.GRAY), false);
        player.sendMessage(Text.translatable("hireling.dndclasses.card.wage", HirelingWages.describe(wage),
                HirelingRole.FEE_DAYS).formatted(Formatting.YELLOW), false);
        player.sendMessage(button(Text.translatable("hireling.dndclasses.card.hire", fee), Formatting.GREEN,
                "/hireling hire " + hireling.getUuidAsString(), Text.translatable("hireling.dndclasses.card.hire_hover",
                        HirelingWages.describe(fee))), false);
        DnDClasses.LOGGER.info("[Hireling] {} reads the hire card of {} ({}, fee {})", player.getEntityName(),
                hireling.hirelingName(), role.id(), fee);
    }

    /** The owner's card: health, loyalty, purse, mode, and [Pay 1 day] [Wait]/[Follow] [Dismiss]. */
    public static void sendStatusCard(ServerPlayerEntity player, HirelingEntity hireling) {
        player.sendMessage(hireling.getDisplayName().copy().formatted(Formatting.GOLD)
                .append(Text.literal(" (" + hireling.race().text().getString() + ")").formatted(Formatting.GRAY)), false);
        MutableText loyalty = Text.translatable("hireling.dndclasses.card.loyalty", hireling.loyalty())
                .formatted(hireling.isDevoted() ? Formatting.GOLD : Formatting.WHITE);
        if (hireling.isDevoted()) {
            loyalty.append(Text.translatable("hireling.dndclasses.card.devoted").formatted(Formatting.GOLD));
        }
        player.sendMessage(Text.literal((int) Math.ceil(hireling.getHealth()) + "/" + (int) hireling.getMaxHealth()
                + " health · ").formatted(Formatting.GRAY).append(loyalty), false);
        player.sendMessage(Text.translatable("hireling.dndclasses.card.purse", HirelingWages.describe(hireling.purse()),
                hireling.daysPaid()).formatted(Formatting.YELLOW)
                .append(Text.literal(" · ").formatted(Formatting.GRAY))
                .append(Text.translatable(hireling.isKnockedOut() ? "hireling.dndclasses.mode.knocked_out"
                        : hireling.isWaiting() ? "hireling.dndclasses.mode.waiting" : "hireling.dndclasses.mode.following")
                        .formatted(Formatting.AQUA)), false);
        String id = hireling.getUuidAsString();
        MutableText buttons = Text.empty()
                .append(button(Text.translatable("hireling.dndclasses.card.pay"), Formatting.GREEN, "/hireling pay " + id + " 1",
                        Text.translatable("hireling.dndclasses.card.pay_hover", HirelingWages.describe(HirelingWages.wage(hireling.role(), player)))))
                .append(" ")
                .append(hireling.isWaiting()
                        ? button(Text.translatable("hireling.dndclasses.card.follow"), Formatting.AQUA, "/hireling follow " + id, null)
                        : button(Text.translatable("hireling.dndclasses.card.wait"), Formatting.AQUA, "/hireling wait " + id, null))
                .append(" ")
                .append(button(Text.translatable("hireling.dndclasses.card.dismiss"), Formatting.RED, "/hireling dismiss " + id,
                        Text.translatable("hireling.dndclasses.card.dismiss_hover")));
        player.sendMessage(buttons, false);
    }

    private static MutableText button(Text label, Formatting color, String command, @Nullable Text hover) {
        return Text.literal("[").append(label).append("]").styled(style -> {
            style = style.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            return hover != null ? style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)) : style;
        });
    }

    // ---- Registry bookkeeping ----

    /** Refreshes the hireling's registry record from it (no-op if it has none). */
    public static void refreshRecord(HirelingEntity hireling) {
        MinecraftServer server = hireling.getServer();
        if (server == null || hireling.ownerUuid() == null || !hireling.isHired()) {
            return;
        }
        HirelingRegistry registry = HirelingRegistry.get(server);
        HirelingRegistry.Record record = registry.find(hireling.ownerUuid(), hireling.getUuid());
        if (record != null && record.generation == hireling.generation()) {
            registry.update(record, hireling);
        }
    }

    /** Drops the hireling from its owner's list (it left, was dismissed or died). */
    public static void forget(HirelingEntity hireling) {
        MinecraftServer server = hireling.getServer();
        if (server != null && hireling.ownerUuid() != null) {
            HirelingRegistry.get(server).remove(hireling.ownerUuid(), hireling.getUuid());
        }
    }

    /** Moves the hireling to a free spot 2-3 blocks from the player (same world), or onto them if there's none. */
    public static void teleportNear(HirelingEntity hireling, PlayerEntity player) {
        BlockPos base = player.getBlockPos();
        BlockPos spot = null;
        for (int attempt = 0; attempt < 12 && spot == null; attempt++) {
            int dx = hireling.getRandom().nextBetween(-3, 3);
            int dz = hireling.getRandom().nextBetween(-3, 3);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            for (int dy = -1; dy <= 1 && spot == null; dy++) {
                BlockPos pos = base.add(dx, dy, dz);
                if (canStand(hireling, pos)) {
                    spot = pos;
                }
            }
        }
        if (spot == null) {
            spot = base;
        }
        hireling.refreshPositionAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, hireling.getYaw(), hireling.getPitch());
        hireling.getNavigation().stop();
    }

    private static boolean canStand(HirelingEntity hireling, BlockPos pos) {
        var world = hireling.world;
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), net.minecraft.util.math.Direction.UP)
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
                && world.getFluidState(pos).isEmpty();
    }

    // ---- Events ----

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker == null) {
            return true;
        }
        MinecraftServer server = entity.getServer();
        if (entity instanceof HirelingEntity hireling && hireling.isHired()) {
            if (hireling.isOwner(attacker)) {
                // Its owner's blows do no harm, but they hurt its pride.
                hireling.changeLoyalty(HirelingEntity.LOYALTY_HIT_BY_OWNER, "hit by owner");
                return false;
            }
            if (isFriend(server, hireling.ownerUuid(), attacker)) {
                return false;
            }
        }
        if (attacker instanceof HirelingEntity hireling && hireling.isHired()) {
            if (isFriend(server, hireling.ownerUuid(), entity)) {
                return false;
            }
            ASSISTS.computeIfAbsent(entity.getUuid(), key -> new HashMap<>()).put(hireling.getUuid(), entity.world.getTime());
        }
        return true;
    }

    private static void afterDeath(LivingEntity entity, DamageSource source) {
        Map<UUID, Long> helpers = ASSISTS.remove(entity.getUuid());
        // A kill needs a killer (a hireling, a player...): /kill and falling into the void don't count.
        if (helpers == null || source.getAttacker() == null || !(entity.world instanceof ServerWorld world)) {
            return;
        }
        for (UUID id : helpers.keySet()) {
            if (world.getEntity(id) instanceof HirelingEntity hireling && hireling.isHired()) {
                hireling.creditKill(entity);
            }
        }
    }

    /** {@code DownedEvents.EXTRA_ALLY}: a conscious follower within 64 blocks of the dying player keeps them Downed. */
    private static boolean keepsDowned(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        for (HirelingEntity hireling : player.getWorld().getEntitiesByClass(HirelingEntity.class,
                player.getBoundingBox().expand(ALLY_RANGE), h -> h.isActive() && !h.isWaiting())) {
            if (hireling.squaredDistanceTo(player) <= ALLY_RANGE * ALLY_RANGE
                    && (hireling.isOwner(player) || sameParty(server, hireling.ownerUuid(), player.getUuid()))) {
                DnDClasses.LOGGER.info("[Hireling] {} keeps {} from a Last Stand", hireling.firstName(), player.getEntityName());
                return true;
            }
        }
        return false;
    }

    /** The nearest Downed, unstable owner or party member within {@code range} of the hireling, or null. */
    @Nullable
    public static ServerPlayerEntity downedToHelp(HirelingEntity hireling, double range) {
        ServerPlayerEntity owner = hireling.ownerPlayer();
        if (owner == null) {
            return null;
        }
        List<ServerPlayerEntity> people = new ArrayList<>();
        people.add(owner);
        Party party = PartyManager.get(owner.getServer()).getParty(owner.getUuid());
        if (party != null) {
            people.addAll(party.getOnlineMembers(owner.getServer()));
        }
        ServerPlayerEntity best = null;
        double bestDistance = range * range;
        for (ServerPlayerEntity person : people) {
            if (person.world != hireling.world || !person.isAlive() || !Downed.is(person) || Downed.isStable(person)) {
                continue;
            }
            double distance = hireling.squaredDistanceTo(person);
            if (distance <= bestDistance) {
                best = person;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * A hireling entered a world: stale copies (dismissed, an older generation) and followers whose owner is
     * offline are discarded at the end of the tick.
     */
    private static void onLoad(Entity entity, ServerWorld world) {
        if (!(entity instanceof HirelingEntity hireling) || hireling.isForHire()) {
            return;
        }
        if (hireling.isDeparting() || hireling.ownerUuid() == null) {
            PENDING_DISCARD.add(hireling);
            return;
        }
        HirelingRegistry registry = HirelingRegistry.get(world.getServer());
        HirelingRegistry.Record record = registry.find(hireling.ownerUuid(), hireling.getUuid());
        if (record == null || record.generation != hireling.generation()) {
            DnDClasses.LOGGER.info("[Hireling] discarding a stale copy of {}", hireling.hirelingName());
            PENDING_DISCARD.add(hireling);
            return;
        }
        boolean ownerOnline = hireling.ownerUuid().equals(joining)
                || world.getServer().getPlayerManager().getPlayer(hireling.ownerUuid()) != null;
        if (!ownerOnline && !hireling.isWaiting()) {
            // Its owner left while it was unloaded: keep the snapshot, recreate it when they're back.
            registry.update(record, hireling);
            record.generation++;
            record.stashed = true;
            registry.markDirty();
            PENDING_DISCARD.add(hireling);
            DnDClasses.LOGGER.info("[Hireling] {} stashed (loaded while its owner is offline)", hireling.firstName());
        }
    }

    /** The owner left: followers are snapshotted and put away; waiting ones stay where they are. */
    private static void onLeave(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        HirelingRegistry registry = HirelingRegistry.get(server);
        for (HirelingRegistry.Record record : registry.of(player.getUuid())) {
            HirelingEntity hireling = loaded(server, record);
            if (hireling != null) {
                registry.update(record, hireling);
            }
            if (record.waiting) {
                continue;
            }
            record.generation++;
            record.stashed = true;
            if (hireling != null) {
                hireling.discard();
                DnDClasses.LOGGER.info("[Hireling] {} stashed with {}'s logout", hireling.firstName(), player.getEntityName());
            }
        }
        registry.markDirty();
        LAST_POS.remove(player.getUuid());
        HUD_SENT.remove(player.getUuid());
    }

    /** The owner joined: today's wages start now, and stashed followers come back beside them. */
    private static void onJoin(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        HirelingRegistry registry = HirelingRegistry.get(server);
        registry.setJoinDay(player.getUuid(), mattonfire.dnd.world.GameTime.day(server));
        joining = player.getUuid();
        try {
            recreate(player, server, registry);
        } finally {
            joining = null;
        }
    }

    private static void recreate(ServerPlayerEntity player, MinecraftServer server, HirelingRegistry registry) {
        for (HirelingRegistry.Record record : registry.of(player.getUuid())) {
            if (!record.stashed) {
                continue;
            }
            HirelingEntity existing = loaded(server, record);
            if (existing != null) {
                existing.discard();
            }
            ServerWorld world = player.getWorld();
            Optional<Entity> created = EntityType.getEntityFromNbt(record.snapshot, world);
            if (created.isEmpty() || !(created.get() instanceof HirelingEntity hireling)) {
                DnDClasses.LOGGER.warn("[Hireling] couldn't recreate {} for {}", record.name, player.getEntityName());
                continue;
            }
            hireling.setGeneration(record.generation);
            hireling.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
            teleportNear(hireling, player);
            record.stashed = false;
            if (world.spawnEntity(hireling)) {
                registry.update(record, hireling);
                DnDClasses.LOGGER.info("[Hireling] {} rejoins {}", hireling.firstName(), player.getEntityName());
            } else {
                record.stashed = true;
            }
        }
        registry.markDirty();
    }

    /** Followers within {@value #PORTAL_RANGE} blocks of where the owner was go through the portal with them. */
    private static void followThroughPortal(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        Vec3d from = LAST_POS.get(player.getUuid());
        if (from == null) {
            return;
        }
        HirelingRegistry registry = HirelingRegistry.get(player.getServer());
        for (HirelingRegistry.Record record : List.copyOf(registry.of(player.getUuid()))) {
            if (!(origin.getEntity(record.uuid) instanceof HirelingEntity hireling) || !hireling.isActive()
                    || hireling.isWaiting() || hireling.squaredDistanceTo(from) > PORTAL_RANGE * PORTAL_RANGE) {
                continue;
            }
            HirelingEntity moved = FabricDimensions.teleport(hireling, destination,
                    new TeleportTarget(player.getPos(), Vec3d.ZERO, player.getYaw(), 0.0F));
            if (moved != null) {
                registry.update(record, moved);
                DnDClasses.LOGGER.info("[Hireling] {} follows {} to {}", moved.firstName(), player.getEntityName(),
                        destination.getRegistryKey().getValue());
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (!PENDING_DISCARD.isEmpty()) {
            for (Entity entity : List.copyOf(PENDING_DISCARD)) {
                if (!entity.isRemoved()) {
                    entity.discard();
                }
            }
            PENDING_DISCARD.clear();
        }
        HirelingRegistry registry = HirelingRegistry.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!registry.of(player.getUuid()).isEmpty()) {
                LAST_POS.put(player.getUuid(), player.getPos());
            }
        }
        if (server.getTicks() % 10 == 0) {
            syncHud(server, registry);
        }
        if (server.getTicks() % 200 == 0 && !ASSISTS.isEmpty()) {
            long now = server.getOverworld().getTime();
            for (Iterator<Map<UUID, Long>> it = ASSISTS.values().iterator(); it.hasNext(); ) {
                Map<UUID, Long> helpers = it.next();
                helpers.values().removeIf(time -> now - time > ASSIST_TICKS || time > now);
                if (helpers.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    // ---- HUD ----

    private static void syncHud(MinecraftServer server, HirelingRegistry registry) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            List<HirelingRegistry.Record> records = registry.of(player.getUuid());
            if (records.isEmpty() && !HUD_SENT.contains(player.getUuid())) {
                continue;
            }
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(records.size());
            for (HirelingRegistry.Record record : records) {
                HirelingEntity hireling = loaded(server, record);
                if (hireling != null) {
                    byte flags = 0;
                    if (hireling.isWaiting()) {
                        flags |= HUD_WAITING;
                    }
                    if (hireling.isKnockedOut()) {
                        flags |= HUD_KNOCKED_OUT;
                    }
                    if (hireling.isDevoted()) {
                        flags |= HUD_DEVOTED;
                    }
                    buf.writeVarInt(hireling.world == player.world ? hireling.getId() : -1);
                    buf.writeString(hireling.hirelingName());
                    buf.writeVarInt(hireling.role().ordinal());
                    buf.writeFloat(hireling.getHealth());
                    buf.writeFloat(hireling.getMaxHealth());
                    buf.writeVarInt(hireling.loyalty());
                    buf.writeVarInt(hireling.daysPaid());
                    buf.writeByte(flags);
                } else {
                    NbtCompound nbt = record.snapshot;
                    HirelingRole role = HirelingRole.byId(nbt.getString("Role"));
                    if (role == null) {
                        role = HirelingRole.TANK;
                    }
                    int loyalty = nbt.getInt("Loyalty");
                    byte flags = HUD_AWAY;
                    if (record.waiting) {
                        flags |= HUD_WAITING;
                    }
                    if ((nbt.getByte("HirelingFlags") & HirelingEntity.FLAG_KNOCKED_OUT) != 0) {
                        flags |= HUD_KNOCKED_OUT;
                    }
                    if (loyalty >= HirelingEntity.DEVOTED_LOYALTY) {
                        flags |= HUD_DEVOTED;
                    }
                    buf.writeVarInt(-1);
                    buf.writeString(record.name);
                    buf.writeVarInt(role.ordinal());
                    buf.writeFloat(nbt.getFloat("Health"));
                    buf.writeFloat(role.health);
                    buf.writeVarInt(loyalty);
                    buf.writeVarInt(nbt.getInt("Purse") / Math.max(1, HirelingWages.wage(role, player)));
                    buf.writeByte(flags);
                }
            }
            ServerPlayNetworking.send(player, S2C_HUD, buf);
            if (records.isEmpty()) {
                HUD_SENT.remove(player.getUuid());
            } else {
                HUD_SENT.add(player.getUuid());
            }
        }
    }

    /** For {@code /hireling hire nearest}: the closest candidate within {@link #HIRE_RANGE}. */
    @Nullable
    public static HirelingEntity nearestCandidate(ServerPlayerEntity player) {
        HirelingEntity best = null;
        double bestDistance = HIRE_RANGE * HIRE_RANGE;
        for (HirelingEntity hireling : player.getWorld().getEntitiesByClass(HirelingEntity.class,
                player.getBoundingBox().expand(HIRE_RANGE), HirelingEntity::isForHire)) {
            double distance = hireling.squaredDistanceTo(player);
            if (distance <= bestDistance) {
                best = hireling;
                bestDistance = distance;
            }
        }
        return best;
    }
}
