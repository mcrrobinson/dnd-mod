package mattonfire.dnd.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.quest.QuestObjective.QuestDrop;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

/**
 * Quest state for the whole world, saved in the overworld as {@code dndclasses_quests}: the started
 * quest instances, and per player the finished quests, the tracked quest and pending actions
 * (rewards waiting at the giver, and stage actions for participants who were offline).
 */
public final class QuestManager extends PersistentState {
    private static final String SAVE_ID = "dndclasses_quests";
    /** Quest drops vanish after 2 minutes (items normally last 5). */
    private static final int QUEST_DROP_LIFETIME = 20 * 120;

    /** Why a quest command or start failed, as a message for the player. */
    public static final class QuestException extends Exception {
        private final Text text;

        QuestException(Text text) {
            super(text.getString());
            this.text = text;
        }

        public Text text() {
            return this.text;
        }
    }

    /** Something to run for a player later. */
    enum PendingKind {
        /** {@code on_accept} actions (the player was added while offline). */
        ACCEPT,
        /** A stage's {@code on_complete} actions (the stage finished while they were offline). */
        STAGE,
        /** The finished quest's rewards, paid at the giver. */
        REWARD
    }

    record Pending(Identifier quest, PendingKind kind, int stage) {
    }

    static final class PlayerRecord {
        final Map<Identifier, Long> finished = new LinkedHashMap<>();
        final List<Pending> pending = new ArrayList<>();
        int tracked = -1;
    }

    private int nextId = 1;
    private final Map<Integer, QuestInstance> instances = new LinkedHashMap<>();
    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    /** Players whose client view is out of date (sent at most once a second). */
    private final Set<UUID> needsSync = new HashSet<>();

    public static QuestManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(QuestManager::fromNbt, QuestManager::new, SAVE_ID);
    }

    // ---------------------------------------------------------------- reading

    public List<QuestInstance> instancesOf(UUID player) {
        List<QuestInstance> result = new ArrayList<>();
        for (QuestInstance instance : this.instances.values()) {
            if (instance.participants().contains(player)) {
                result.add(instance);
            }
        }
        return result;
    }

    @Nullable
    public QuestInstance instanceOf(UUID player, Identifier quest) {
        for (QuestInstance instance : this.instances.values()) {
            if (instance.quest().equals(quest) && instance.participants().contains(player)) {
                return instance;
            }
        }
        return null;
    }

    @Nullable
    public QuestInstance instance(int id) {
        return this.instances.get(id);
    }

    public boolean hasFinished(UUID player, Identifier quest) {
        PlayerRecord record = this.players.get(player);
        return record != null && record.finished.containsKey(quest);
    }

    /** Finished quests, oldest first. */
    public Map<Identifier, Long> finished(UUID player) {
        PlayerRecord record = this.players.get(player);
        return record == null ? Map.of() : java.util.Collections.unmodifiableMap(record.finished);
    }

    /** Finished quests whose rewards are still waiting at the giver. */
    public List<Identifier> unclaimed(UUID player) {
        PlayerRecord record = this.players.get(player);
        List<Identifier> result = new ArrayList<>();
        if (record != null) {
            record.pending.stream().filter(p -> p.kind() == PendingKind.REWARD).forEach(p -> result.add(p.quest()));
        }
        return result;
    }

    /** The tracked instance id, or -1. */
    public int tracked(UUID player) {
        PlayerRecord record = this.players.get(player);
        return record == null ? -1 : record.tracked;
    }

    private PlayerRecord record(UUID player) {
        return this.players.computeIfAbsent(player, uuid -> new PlayerRecord());
    }

    static long today(MinecraftServer server) {
        return server.getOverworld().getTimeOfDay() / 24000L;
    }

    /**
     * Why {@code player} can't take {@code quest} right now (already on it, finished, on cooldown,
     * missing a required quest or standing), or null if they can. Dialogue and rumours use this.
     */
    @Nullable
    public Text whyCantStart(ServerPlayerEntity player, QuestDefinition quest) {
        UUID uuid = player.getUuid();
        if (QuestHooks.ignored.test(player)) {
            return Text.translatable("quest.dndclasses.error.ignored");
        }
        if (this.instanceOf(uuid, quest.id()) != null) {
            return Text.translatable("quest.dndclasses.error.already_active", quest.title());
        }
        if (this.instancesOf(uuid).size() >= QuestDefinition.MAX_ACTIVE) {
            return Text.translatable("quest.dndclasses.error.too_many", QuestDefinition.MAX_ACTIVE);
        }
        Long finishedDay = this.finished(uuid).get(quest.id());
        if (finishedDay != null) {
            if (!quest.repeatable()) {
                return Text.translatable("quest.dndclasses.error.already_done", quest.title());
            }
            long wait = finishedDay + quest.cooldownDays() - today(player.getServer());
            if (wait > 0) {
                return Text.translatable("quest.dndclasses.error.cooldown", quest.title(), wait);
            }
        }
        for (Identifier required : quest.requires().quests()) {
            if (!this.hasFinished(uuid, required)) {
                QuestDefinition other = Quests.get(required);
                return Text.translatable("quest.dndclasses.error.requires_quest",
                        other != null ? other.title() : Text.literal(required.toString()));
            }
        }
        for (Map.Entry<Identifier, ReputationTier> entry : quest.requires().rep().entrySet()) {
            Faction faction = Factions.get(entry.getKey());
            if (faction != null && !Reputation.tier(player, faction).atLeast(entry.getValue())) {
                return Text.translatable("quest.dndclasses.error.requires_rep", entry.getValue().displayName(),
                        faction.displayName());
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- starting and joining

    /**
     * Starts {@code quest} for {@code starter} and every party member who's online, in the same
     * dimension and within {@link PartyManager#SHARE_RADIUS}. Members who already hold it (or are at
     * the limit) are skipped with a message; members further away get a [Join] link.
     *
     * @param admin skip the requirement, finished and cooldown checks (admin commands, rumours)
     */
    public QuestInstance start(ServerPlayerEntity starter, QuestDefinition quest, boolean admin) throws QuestException {
        UUID uuid = starter.getUuid();
        if (admin) {
            if (QuestHooks.ignored.test(starter)) {
                throw new QuestException(Text.translatable("quest.dndclasses.error.ignored"));
            }
            if (this.instanceOf(uuid, quest.id()) != null) {
                throw new QuestException(Text.translatable("quest.dndclasses.error.already_active", quest.title()));
            }
            if (this.instancesOf(uuid).size() >= QuestDefinition.MAX_ACTIVE) {
                throw new QuestException(Text.translatable("quest.dndclasses.error.too_many", QuestDefinition.MAX_ACTIVE));
            }
        } else {
            Text why = this.whyCantStart(starter, quest);
            if (why != null) {
                throw new QuestException(why);
            }
        }
        MinecraftServer server = starter.getServer();
        LinkedHashSet<UUID> participants = new LinkedHashSet<>();
        participants.add(uuid);
        Set<UUID> nearby = new HashSet<>();
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(starter, PartyManager.SHARE_RADIUS)) {
            nearby.add(member.getUuid());
            if (QuestHooks.ignored.test(member)) {
                continue;
            }
            if (this.instanceOf(member.getUuid(), quest.id()) != null
                    || this.instancesOf(member.getUuid()).size() >= QuestDefinition.MAX_ACTIVE) {
                member.sendMessage(Text.translatable("quest.dndclasses.skipped_self", quest.title()).formatted(Formatting.GRAY), false);
                starter.sendMessage(Text.translatable("quest.dndclasses.skipped", member.getDisplayName(), quest.title())
                        .formatted(Formatting.GRAY), false);
                continue;
            }
            participants.add(member.getUuid());
        }
        QuestInstance instance = this.create(server, quest, participants);
        DnDClasses.LOGGER.info("[Quests] #{} {} started by {} with {}", instance.id(), quest.id(),
                starter.getEntityName(), this.names(server, instance));

        // Party members too far away (or in another dimension) can join later.
        Party party = PartyManager.get(server).getParty(uuid);
        if (party != null) {
            for (ServerPlayerEntity member : party.getOnlineMembers(server)) {
                if (member != starter && !nearby.contains(member.getUuid()) && !QuestHooks.ignored.test(member)) {
                    member.sendMessage(Text.translatable("quest.dndclasses.join_offer", starter.getDisplayName(), quest.title())
                            .formatted(Formatting.YELLOW).append(" ").append(joinLink(instance.id())), false);
                }
            }
        }
        return instance;
    }

    /** Starts a quest from a reward action; failures are only logged. */
    void startFromAction(ServerPlayerEntity player, Identifier questId) {
        QuestDefinition quest = Quests.get(questId);
        if (quest == null) {
            DnDClasses.LOGGER.warn("[Quests] start_quest: unknown quest {}", questId);
            return;
        }
        if (this.instanceOf(player.getUuid(), questId) != null) {
            return;
        }
        try {
            this.start(player, quest, true);
        } catch (QuestException e) {
            player.sendMessage(e.text().copy().formatted(Formatting.RED), false);
        }
    }

    /** The next quest in a chain, for the same participants (online or not). */
    void startFollowUp(MinecraftServer server, Identifier questId, QuestInstance from) {
        QuestDefinition quest = Quests.get(questId);
        if (quest == null) {
            DnDClasses.LOGGER.warn("[Quests] start_quest: unknown quest {}", questId);
            return;
        }
        LinkedHashSet<UUID> participants = new LinkedHashSet<>();
        for (UUID uuid : from.participants()) {
            if (this.instanceOf(uuid, questId) == null && this.instancesOf(uuid).size() < QuestDefinition.MAX_ACTIVE) {
                participants.add(uuid);
            }
        }
        if (!participants.isEmpty()) {
            QuestInstance instance = this.create(server, quest, participants);
            DnDClasses.LOGGER.info("[Quests] #{} {} follows on from #{} with {}", instance.id(), questId, from.id(),
                    this.names(server, instance));
        }
    }

    private QuestInstance create(MinecraftServer server, QuestDefinition quest, LinkedHashSet<UUID> participants) {
        QuestInstance instance = new QuestInstance(this.nextId++, quest.id(), 0,
                new int[quest.stage(0).objectives().size()], new LinkedHashSet<>(), participants, today(server));
        this.instances.put(instance.id(), instance);
        for (UUID uuid : participants) {
            this.record(uuid).tracked = instance.id();
        }
        this.forOnline(server, instance, player -> {
            player.sendMessage(Text.translatable("quest.dndclasses.started", quest.title()).formatted(Formatting.GOLD), false);
            this.announceStage(player, quest, instance);
            player.playSound(SoundEvents.UI_TOAST_IN, SoundCategory.PLAYERS, 1.0F, 1.0F);
        });
        this.runActions(server, instance, quest, quest.onAccept(), PendingKind.ACCEPT, 0, participants);
        this.changed(instance);
        return instance;
    }

    /** {@code /quest join}: a party member joins a quest another member started. */
    public QuestInstance join(ServerPlayerEntity player, int instanceId) throws QuestException {
        QuestInstance instance = this.instances.get(instanceId);
        QuestDefinition quest = instance == null ? null : Quests.get(instance.quest());
        if (instance == null || quest == null) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.no_instance", instanceId));
        }
        UUID uuid = player.getUuid();
        if (instance.participants().contains(uuid)) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.already_active", quest.title()));
        }
        if (QuestHooks.ignored.test(player)) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.ignored"));
        }
        Party party = PartyManager.get(player.getServer()).getParty(uuid);
        if (party == null || instance.participants().stream().noneMatch(party::contains)) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.not_party"));
        }
        if (this.instanceOf(uuid, quest.id()) != null) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.already_active", quest.title()));
        }
        if (this.instancesOf(uuid).size() >= QuestDefinition.MAX_ACTIVE) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.too_many", QuestDefinition.MAX_ACTIVE));
        }
        MinecraftServer server = player.getServer();
        this.forOnline(server, instance, other -> other.sendMessage(Text.translatable("quest.dndclasses.joined_other",
                player.getDisplayName(), quest.title()).formatted(Formatting.YELLOW), false));
        instance.participants().add(uuid);
        this.record(uuid).tracked = instance.id();
        player.sendMessage(Text.translatable("quest.dndclasses.joined", quest.title()).formatted(Formatting.GOLD), false);
        this.announceStage(player, quest, instance);
        this.runActions(server, instance, quest, quest.onAccept(), PendingKind.ACCEPT, 0, Set.of(uuid));
        DnDClasses.LOGGER.info("[Quests] #{} {}: {} joined", instance.id(), quest.id(), player.getEntityName());
        this.changed(instance);
        return instance;
    }

    /** The newest quest a party member is on that {@code player} could join, or null. */
    @Nullable
    public QuestInstance latestOffer(ServerPlayerEntity player) {
        Party party = PartyManager.get(player.getServer()).getParty(player.getUuid());
        if (party == null) {
            return null;
        }
        QuestInstance latest = null;
        for (QuestInstance instance : this.instances.values()) {
            if (!instance.participants().contains(player.getUuid()) && instance.participants().stream().anyMatch(party::contains)
                    && this.instanceOf(player.getUuid(), instance.quest()) == null
                    && (latest == null || instance.id() > latest.id())) {
                latest = instance;
            }
        }
        return latest;
    }

    /** Leaves a quest; the others keep it. Returns the quest's title. */
    public Text abandon(ServerPlayerEntity player, Identifier questId) throws QuestException {
        QuestInstance instance = this.instanceOf(player.getUuid(), questId);
        if (instance == null) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.not_active", questId.toString()));
        }
        Text title = title(questId);
        this.changed(instance);
        this.removeParticipant(instance, player.getUuid());
        this.forOnline(player.getServer(), instance, other -> other.sendMessage(Text.translatable(
                "quest.dndclasses.abandoned_other", player.getDisplayName(), title).formatted(Formatting.GRAY), false));
        DnDClasses.LOGGER.info("[Quests] #{} {}: {} abandoned", instance.id(), questId, player.getEntityName());
        return title;
    }

    private void removeParticipant(QuestInstance instance, UUID uuid) {
        instance.participants().remove(uuid);
        if (instance.participants().isEmpty()) {
            this.instances.remove(instance.id());
        }
        PlayerRecord record = this.record(uuid);
        if (record.tracked == instance.id()) {
            record.tracked = this.newest(uuid);
        }
        this.needsSync.add(uuid);
        this.markDirty();
    }

    public void track(ServerPlayerEntity player, Identifier questId) throws QuestException {
        QuestInstance instance = this.instanceOf(player.getUuid(), questId);
        if (instance == null) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.not_active", questId.toString()));
        }
        this.record(player.getUuid()).tracked = instance.id();
        this.needsSync.add(player.getUuid());
        this.markDirty();
    }

    private int newest(UUID uuid) {
        int newest = -1;
        for (QuestInstance instance : this.instancesOf(uuid)) {
            newest = Math.max(newest, instance.id());
        }
        return newest;
    }

    /**
     * A player left (or was removed from) their party: they keep their own copy of every shared
     * quest at its current progress, and the party keeps the original.
     */
    public void onPartyLeft(MinecraftServer server, UUID uuid) {
        for (QuestInstance instance : this.instancesOf(uuid)) {
            if (instance.participants().size() < 2) {
                continue;
            }
            this.changed(instance);
            instance.participants().remove(uuid);
            QuestInstance fork = instance.fork(this.nextId++, uuid);
            this.instances.put(fork.id(), fork);
            PlayerRecord record = this.record(uuid);
            if (record.tracked == instance.id()) {
                record.tracked = fork.id();
            }
            DnDClasses.LOGGER.info("[Quests] #{} {}: {} left the party, forked as #{}", instance.id(), instance.quest(),
                    this.name(server, uuid), fork.id());
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Text.translatable("quest.dndclasses.forked", title(instance.quest()))
                        .formatted(Formatting.GRAY), false);
            }
            this.changed(fork);
        }
    }

    // ---------------------------------------------------------------- progress

    /**
     * A kill by {@code killer} (or their pet). {@code as} is the type it counts as (a phylactery
     * smashed while its Lich reforms counts as the Lich). Each of the killer's quests counts it
     * once, for the first matching unfinished objective of its current stage; matching
     * {@code quest_drop}s roll once per quest.
     */
    void onKill(ServerPlayerEntity killer, LivingEntity killed, EntityType<?> as) {
        if (QuestHooks.ignored.test(killer)) {
            return;
        }
        ServerWorld world = (ServerWorld) killed.getWorld();
        for (QuestInstance instance : this.instancesOf(killer.getUuid())) {
            QuestDefinition quest = Quests.get(instance.quest());
            if (quest == null) {
                continue;
            }
            List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
            int counted = -1;
            QuestDrop drop = null;
            for (int i = 0; i < objectives.size(); i++) {
                if (!(objectives.get(i) instanceof QuestObjective.Kill kill) || !kill.entity().matches(as)
                        || (kill.within() != null && !kill.within().contains(world, killed.getBlockPos()))) {
                    continue;
                }
                if (drop == null && kill.drop() != null) {
                    drop = kill.drop();
                }
                if (counted < 0 && instance.progress(i) < kill.count()) {
                    counted = i;
                }
            }
            if (drop != null && killer.getRandom().nextDouble() < drop.chance()) {
                spawnDrop(world, killed, killer, drop);
            }
            if (counted >= 0) {
                this.advance(killer.getServer(), instance, quest, counted, 1);
            }
        }
    }

    private static void spawnDrop(ServerWorld world, LivingEntity killed, ServerPlayerEntity killer, QuestDrop drop) {
        ItemEntity item = new ItemEntity(world, killed.getX(), killed.getBodyY(0.5D), killed.getZ(),
                new ItemStack(Registries.ITEM.get(drop.item())));
        NbtCompound nbt = new NbtCompound();
        item.writeCustomDataToNbt(nbt);
        nbt.putShort("Age", (short) (6000 - QUEST_DROP_LIFETIME));
        item.readCustomDataFromNbt(nbt);
        // Only the killer can pick it up, so the drop never leaves the quest.
        item.setOwner(killer.getUuid());
        item.setPickupDelay(10);
        world.spawnEntity(item);
    }

    /** Every second: participants standing in a structure their quest asks them to visit. */
    void tickVisits(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (QuestHooks.ignored.test(player) || player.isSpectator()) {
                continue;
            }
            for (QuestInstance instance : this.instancesOf(player.getUuid())) {
                QuestDefinition quest = Quests.get(instance.quest());
                if (quest == null) {
                    continue;
                }
                List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
                for (int i = 0; i < objectives.size(); i++) {
                    if (objectives.get(i) instanceof QuestObjective.Visit visit && instance.progress(i) < 1
                            && visit.structure().contains(player.getWorld(), player.getBlockPos())) {
                        this.advance(server, instance, quest, i, 1);
                        break;
                    }
                }
            }
        }
    }

    /**
     * {@code player} spoke with an NPC with {@code role}: counts {@code talk} objectives for that role,
     * hands in {@code deliver} items for it and {@code collect} items for quests it gives, then pays
     * out any finished quests it gives. NPC dialogue calls this; until it lands,
     * {@code /quest admin <player> talk <role>} does.
     */
    public void talkedTo(ServerPlayerEntity player, String role) {
        if (QuestHooks.ignored.test(player)) {
            return;
        }
        // An objective can finish a stage and open the next, so go round until nothing changes.
        for (int pass = 0; pass < 32 && this.handInOnce(player, role); pass++) {
        }
        this.claim(player, role);
    }

    private boolean handInOnce(ServerPlayerEntity player, String role) {
        for (QuestInstance instance : this.instancesOf(player.getUuid())) {
            QuestDefinition quest = Quests.get(instance.quest());
            if (quest == null) {
                continue;
            }
            List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective objective = objectives.get(i);
                int needed = objective.count() - instance.progress(i);
                if (needed <= 0) {
                    continue;
                }
                if (objective instanceof QuestObjective.Talk talk && talk.role().equals(role)) {
                    this.advance(player.getServer(), instance, quest, i, 1);
                    return true;
                }
                if (objective instanceof QuestObjective.Deliver deliver && deliver.role().equals(role)) {
                    int given = take(player, deliver.item(), needed);
                    if (given > 0) {
                        this.advance(player.getServer(), instance, quest, i, given);
                        return true;
                    }
                }
                if (objective instanceof QuestObjective.Collect collect && role.equals(quest.giver())
                        && count(player, collect.item()) >= needed) {
                    if (collect.consume()) {
                        take(player, collect.item(), needed);
                    }
                    this.advance(player.getServer(), instance, quest, i, needed);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@code player} passed a d20 check for {@code skill} in dialogue. NPC dialogue will call this
     * after its roll; until then only {@code /quest admin <player> pass <skill>} does.
     */
    public void checkPassed(ServerPlayerEntity player, String skill) {
        if (QuestHooks.ignored.test(player)) {
            return;
        }
        for (QuestInstance instance : this.instancesOf(player.getUuid())) {
            QuestDefinition quest = Quests.get(instance.quest());
            if (quest == null) {
                continue;
            }
            List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
            for (int i = 0; i < objectives.size(); i++) {
                if (objectives.get(i) instanceof QuestObjective.Check check && check.skill().equals(skill)
                        && instance.progress(i) < 1) {
                    this.advance(player.getServer(), instance, quest, i, 1);
                    break;
                }
            }
        }
    }

    /** A goblin raid on a {@code settlement} kind was won; {@code defenders} took part. */
    void onRaidWon(MinecraftServer server, List<ServerPlayerEntity> defenders, String settlement) {
        Set<Integer> done = new HashSet<>();
        for (ServerPlayerEntity player : defenders) {
            if (QuestHooks.ignored.test(player)) {
                continue;
            }
            for (QuestInstance instance : this.instancesOf(player.getUuid())) {
                QuestDefinition quest = Quests.get(instance.quest());
                if (quest == null || !done.add(instance.id())) {
                    continue;
                }
                List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
                for (int i = 0; i < objectives.size(); i++) {
                    if (objectives.get(i) instanceof QuestObjective.Defend defend && defend.settlement().equals(settlement)
                            && instance.progress(i) < 1) {
                        this.advance(server, instance, quest, i, 1);
                        break;
                    }
                }
            }
        }
    }

    private static int count(ServerPlayerEntity player, QuestJson.ItemMatch match) {
        PlayerInventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (match.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Removes up to {@code amount} matching items; returns how many it took. */
    private static int take(ServerPlayerEntity player, QuestJson.ItemMatch match, int amount) {
        PlayerInventory inventory = player.getInventory();
        int taken = 0;
        for (int i = 0; i < inventory.size() && taken < amount; i++) {
            ItemStack stack = inventory.getStack(i);
            if (match.matches(stack)) {
                int n = Math.min(stack.getCount(), amount - taken);
                stack.decrement(n);
                taken += n;
            }
        }
        if (taken > 0) {
            inventory.markDirty();
        }
        return taken;
    }

    private void advance(MinecraftServer server, QuestInstance instance, QuestDefinition quest, int index, int amount) {
        QuestObjective objective = quest.stage(instance.stage()).objectives().get(index);
        int before = instance.progress(index);
        int after = Math.min(objective.count(), before + amount);
        if (after == before) {
            return;
        }
        instance.setProgress(index, after);
        this.changed(instance);
        Text line = Text.translatable("quest.dndclasses.progress", objective.description(), after, objective.count())
                .formatted(Formatting.YELLOW);
        this.forOnline(server, instance, player -> player.sendMessage(line, true));
        List<QuestObjective> objectives = quest.stage(instance.stage()).objectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (instance.progress(i) < objectives.get(i).count()) {
                return;
            }
        }
        this.completeStage(server, instance, quest);
    }

    /** Runs the current stage's actions and moves on, or finishes the quest after the last stage. */
    private void completeStage(MinecraftServer server, QuestInstance instance, QuestDefinition quest) {
        int index = instance.stage();
        QuestStage stage = quest.stage(index);
        DnDClasses.LOGGER.info("[Quests] #{} {}: stage {}/{} complete ({})", instance.id(), quest.id(), index + 1,
                quest.stages().size(), this.names(server, instance));
        Text stageName = stage.title() != null ? stage.title() : quest.title();
        boolean last = index + 1 >= quest.stages().size();
        if (!last) {
            this.forOnline(server, instance, player -> {
                player.sendMessage(Text.translatable("quest.dndclasses.stage_complete", stageName).formatted(Formatting.GREEN), false);
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.6F, 1.0F);
            });
        }
        Set<UUID> participants = Set.copyOf(instance.participants());
        if (last) {
            this.finish(server, instance, quest);
        } else {
            instance.setStage(index + 1, quest.stage(index + 1).objectives().size());
            this.forOnline(server, instance, player -> this.announceStage(player, quest, instance));
        }
        this.runActions(server, instance, quest, stage.onComplete(), PendingKind.STAGE, index, participants);
        this.changed(instance);
    }

    /**
     * The quest is done: every participant gets its rewards as a pending payout, claimed by talking
     * to the giver (or straight away if it has none). Rewards are per hero, not split.
     */
    private void finish(MinecraftServer server, QuestInstance instance, QuestDefinition quest) {
        this.instances.remove(instance.id());
        long today = today(server);
        DnDClasses.LOGGER.info("[Quests] #{} {}: finished by {}", instance.id(), quest.id(), this.names(server, instance));
        for (UUID uuid : instance.participants()) {
            PlayerRecord record = this.record(uuid);
            record.finished.remove(quest.id());
            record.finished.put(quest.id(), today);
            record.pending.add(new Pending(quest.id(), PendingKind.REWARD, 0));
            if (record.tracked == instance.id()) {
                record.tracked = this.newest(uuid);
            }
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.sendMessage(Text.translatable("quest.dndclasses.finished", quest.title()).formatted(Formatting.GOLD), false);
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8F, 1.0F);
            if (quest.giver() != null) {
                player.sendMessage(Text.translatable("quest.dndclasses.return_to", QuestObjective.roleName(quest.giver()))
                        .formatted(Formatting.YELLOW), false);
            } else {
                this.claim(player, q -> q.giver() == null);
            }
        }
        this.markDirty();
    }

    /**
     * Pays {@code player}'s finished quests given by {@code role} (any role if null): runs each
     * quest's {@code rewards}. Returns how many quests paid out.
     */
    public int claim(ServerPlayerEntity player, @Nullable String role) {
        return this.claim(player, quest -> role == null || role.equals(quest.giver()));
    }

    private int claim(ServerPlayerEntity player, java.util.function.Predicate<QuestDefinition> which) {
        PlayerRecord record = this.players.get(player.getUuid());
        if (record == null) {
            return 0;
        }
        int paid = 0;
        for (Pending pending : List.copyOf(record.pending)) {
            if (pending.kind() != PendingKind.REWARD) {
                continue;
            }
            QuestDefinition quest = Quests.get(pending.quest());
            if (quest == null) {
                continue;
            }
            if (!which.test(quest)) {
                continue;
            }
            record.pending.remove(pending);
            this.markDirty();
            player.sendMessage(Text.translatable("quest.dndclasses.rewarded", quest.title(),
                    QuestCommand.describe(quest.rewards()).formatted(Formatting.WHITE)).formatted(Formatting.GOLD), false);
            DnDClasses.LOGGER.info("[Quests] {}: rewards paid to {}", quest.id(), player.getEntityName());
            QuestAction.Context context = new QuestAction.Context(this, player.getServer(), player, quest, null);
            for (QuestAction action : quest.rewards()) {
                action.run(context);
            }
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                    0.8F, 1.2F);
            paid++;
        }
        this.needsSync.add(player.getUuid());
        return paid;
    }

    /**
     * Runs {@code actions}: per-instance ones once, the rest for each of {@code who} who's online;
     * offline players get them as a pending action, run when they next join.
     */
    private void runActions(MinecraftServer server, QuestInstance instance, QuestDefinition quest, List<QuestAction> actions,
                            PendingKind kind, int stage, Set<UUID> who) {
        if (actions.isEmpty()) {
            return;
        }
        ServerPlayerEntity any = null;
        for (UUID uuid : who) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) {
                this.record(uuid).pending.add(new Pending(quest.id(), kind, stage));
                this.markDirty();
                continue;
            }
            any = any == null ? player : any;
            QuestAction.Context context = new QuestAction.Context(this, server, player, quest, instance);
            for (QuestAction action : actions) {
                if (!action.perInstance()) {
                    action.run(context);
                }
            }
        }
        QuestAction.Context shared = new QuestAction.Context(this, server, any, quest, instance);
        for (QuestAction action : actions) {
            if (action.perInstance()) {
                action.run(shared);
            }
        }
    }

    /** On join: runs actions missed while offline and pays out quests with no giver. */
    void onJoin(ServerPlayerEntity player) {
        PlayerRecord record = this.players.get(player.getUuid());
        if (record != null) {
            for (Pending pending : List.copyOf(record.pending)) {
                QuestDefinition quest = Quests.get(pending.quest());
                if (pending.kind() == PendingKind.REWARD || quest == null) {
                    continue;
                }
                record.pending.remove(pending);
                List<QuestAction> actions = pending.kind() == PendingKind.ACCEPT ? quest.onAccept()
                        : pending.stage() < quest.stages().size() ? quest.stage(pending.stage()).onComplete() : List.of();
                QuestAction.Context context = new QuestAction.Context(this, player.getServer(), player, quest,
                        this.instanceOf(player.getUuid(), quest.id()));
                for (QuestAction action : actions) {
                    if (!action.perInstance()) {
                        action.run(context);
                    }
                }
                this.markDirty();
            }
            this.claim(player, quest -> quest.giver() == null);
        }
        QuestSync.send(player, this);
    }

    // ---------------------------------------------------------------- admin

    /** Jumps a player's instance to {@code stage} (1-based) with no progress; skipped stages' actions don't run. */
    public void setStage(ServerPlayerEntity player, Identifier questId, int stage) throws QuestException {
        QuestInstance instance = this.instanceOf(player.getUuid(), questId);
        QuestDefinition quest = Quests.get(questId);
        if (instance == null || quest == null) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.not_active", questId.toString()));
        }
        if (stage < 1 || stage > quest.stages().size()) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.no_stage", stage, quest.stages().size()));
        }
        instance.setStage(stage - 1, quest.stage(stage - 1).objectives().size());
        DnDClasses.LOGGER.info("[Quests] #{} {}: set to stage {} by admin", instance.id(), questId, stage);
        this.forOnline(player.getServer(), instance, other -> this.announceStage(other, quest, instance));
        this.changed(instance);
    }

    /** Finishes a player's instance now (for all its participants); stage actions don't run. */
    public void complete(ServerPlayerEntity player, Identifier questId) throws QuestException {
        QuestInstance instance = this.instanceOf(player.getUuid(), questId);
        QuestDefinition quest = Quests.get(questId);
        if (instance == null || quest == null) {
            throw new QuestException(Text.translatable("quest.dndclasses.error.not_active", questId.toString()));
        }
        this.changed(instance);
        this.finish(player.getServer(), instance, quest);
    }

    /** Forgets everything about a quest for a player: active participation, finished record, pending rewards. */
    public void reset(ServerPlayerEntity player, Identifier questId) {
        QuestInstance instance = this.instanceOf(player.getUuid(), questId);
        if (instance != null) {
            this.changed(instance);
            this.removeParticipant(instance, player.getUuid());
        }
        PlayerRecord record = this.record(player.getUuid());
        record.finished.remove(questId);
        record.pending.removeIf(pending -> pending.quest().equals(questId));
        this.needsSync.add(player.getUuid());
        this.markDirty();
    }

    // ---------------------------------------------------------------- messages and sync

    private void announceStage(ServerPlayerEntity player, QuestDefinition quest, QuestInstance instance) {
        QuestStage stage = quest.stage(instance.stage());
        player.sendMessage(Text.literal("  ").append(stage.text().copy()).formatted(Formatting.GRAY), false);
        for (int i = 0; i < stage.objectives().size(); i++) {
            player.sendMessage(objectiveLine(stage.objectives().get(i), instance.progress(i)), false);
        }
    }

    static MutableText objectiveLine(QuestObjective objective, int progress) {
        boolean done = progress >= objective.count();
        MutableText line = Text.literal(done ? "  ✔ " : "  • ").append(objective.description().copy());
        if (objective.count() > 1) {
            line.append(" " + Math.min(progress, objective.count()) + "/" + objective.count());
        }
        return line.formatted(done ? Formatting.DARK_GREEN : Formatting.WHITE);
    }

    static MutableText joinLink(int instanceId) {
        String command = "/quest join " + instanceId;
        return Text.translatable("quest.dndclasses.join_button").styled(style -> style.withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
    }

    static Text title(Identifier questId) {
        QuestDefinition quest = Quests.get(questId);
        return quest != null ? quest.title() : Text.literal(questId.toString());
    }

    private void forOnline(MinecraftServer server, QuestInstance instance, Consumer<ServerPlayerEntity> action) {
        for (UUID uuid : List.copyOf(instance.participants())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                action.accept(player);
            }
        }
    }

    String name(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getEntityName();
        }
        return server.getUserCache() == null ? uuid.toString().substring(0, 8)
                : server.getUserCache().getByUuid(uuid).map(profile -> profile.getName()).orElse(uuid.toString().substring(0, 8));
    }

    private List<String> names(MinecraftServer server, QuestInstance instance) {
        return instance.participants().stream().map(uuid -> this.name(server, uuid)).toList();
    }

    private void changed(QuestInstance instance) {
        this.needsSync.addAll(instance.participants());
        this.markDirty();
    }

    /** Sends the quest view to players whose quests changed (called once a second). */
    void flushSync(MinecraftServer server) {
        if (this.needsSync.isEmpty()) {
            return;
        }
        for (UUID uuid : List.copyOf(this.needsSync)) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                QuestSync.send(player, this);
            }
        }
        this.needsSync.clear();
    }

    void resyncAll(MinecraftServer server) {
        server.getPlayerManager().getPlayerList().forEach(player -> this.needsSync.add(player.getUuid()));
    }

    // ---------------------------------------------------------------- saving

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("NextId", this.nextId);
        NbtList instances = new NbtList();
        this.instances.values().forEach(instance -> instances.add(instance.toNbt()));
        nbt.put("Instances", instances);
        NbtList players = new NbtList();
        for (Map.Entry<UUID, PlayerRecord> entry : this.players.entrySet()) {
            PlayerRecord record = entry.getValue();
            NbtCompound player = new NbtCompound();
            player.put("Id", NbtHelper.fromUuid(entry.getKey()));
            NbtCompound finished = new NbtCompound();
            record.finished.forEach((quest, day) -> finished.putLong(quest.toString(), day));
            player.put("Finished", finished);
            NbtList pending = new NbtList();
            for (Pending item : record.pending) {
                NbtCompound p = new NbtCompound();
                p.putString("Quest", item.quest().toString());
                p.putString("Kind", item.kind().name());
                p.putInt("Stage", item.stage());
                pending.add(p);
            }
            player.put("Pending", pending);
            player.putInt("Tracked", record.tracked);
            players.add(player);
        }
        nbt.put("Players", players);
        return nbt;
    }

    private static QuestManager fromNbt(NbtCompound nbt) {
        QuestManager manager = new QuestManager();
        for (NbtElement element : nbt.getList("Instances", NbtElement.COMPOUND_TYPE)) {
            QuestInstance instance = QuestInstance.fromNbt((NbtCompound) element);
            if (!instance.participants().isEmpty()) {
                manager.instances.put(instance.id(), instance);
            }
        }
        int maxId = manager.instances.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        manager.nextId = Math.max(nbt.getInt("NextId"), maxId + 1);
        for (NbtElement element : nbt.getList("Players", NbtElement.COMPOUND_TYPE)) {
            NbtCompound player = (NbtCompound) element;
            PlayerRecord record = new PlayerRecord();
            NbtCompound finished = player.getCompound("Finished");
            for (String key : finished.getKeys()) {
                record.finished.put(new Identifier(key), finished.getLong(key));
            }
            for (NbtElement p : player.getList("Pending", NbtElement.COMPOUND_TYPE)) {
                NbtCompound pending = (NbtCompound) p;
                PendingKind kind;
                try {
                    kind = PendingKind.valueOf(pending.getString("Kind"));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                record.pending.add(new Pending(new Identifier(pending.getString("Quest")), kind, pending.getInt("Stage")));
            }
            record.tracked = player.getInt("Tracked");
            manager.players.put(NbtHelper.toUuid(player.get("Id")), record);
        }
        return manager;
    }
}
