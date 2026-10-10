package mattonfire.dnd.quest.dialogue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Persuasion;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.quest.QuestAction;
import mattonfire.dnd.quest.QuestDefinition;
import mattonfire.dnd.quest.QuestHooks;
import mattonfire.dnd.quest.QuestInstance;
import mattonfire.dnd.quest.QuestManager;
import mattonfire.dnd.quest.Quests;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.village.Merchant;
import org.jetbrains.annotations.Nullable;

/**
 * Talking to NPCs with a role (command tag {@code dndclasses.role.<role>}): decides whether an NPC has
 * something to say, builds each page on the server and handles the player's choices.
 *
 * <p>The client only ever sees text and answers with an option index ({@code dialogue_choose}); the
 * server keeps the page it showed and checks the chosen option's requirements again before acting, so a
 * stale or forged choice can't accept a quest or roll a check the player isn't offered.
 */
public final class DialogueManager {
    public static final Identifier S2C_OPEN = new Identifier(DnDClasses.MOD_ID, "dialogue_open");
    public static final Identifier S2C_CLOSE = new Identifier(DnDClasses.MOD_ID, "dialogue_close");
    public static final Identifier C2S_CHOOSE = new Identifier(DnDClasses.MOD_ID, "dialogue_choose");

    /** Command tag prefix that gives an entity a role. */
    public static final String ROLE_PREFIX = "dndclasses.role.";
    /** The dialogue ends if the player gets further than this from the NPC. */
    public static final double MAX_DISTANCE = 8.0D;
    /** Player persistent data: checks already tried outside a quest (one try each). */
    private static final String TRIED_KEY = "DndDialogueTried";
    private static final String TRIED_FLAG = "tried:";
    private static final String OFFER_PREFIX = "offer:";

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private DialogueManager() {
    }

    enum Kind { OFFER, ACCEPT, DECLINE, TOPIC, TRADE, GOODBYE }

    /** One option as shown: what it does, and the label sent to the client. */
    record Shown(String key, Kind kind, Text label, @Nullable Identifier quest, @Nullable Dialogue.Option option) {
    }

    record Page(Text speaker, List<Text> lines, List<Shown> options) {
    }

    static final class Session {
        final Entity npc;
        final String role;
        String node = Dialogue.HUB;
        List<Shown> shown = List.of();

        Session(Entity npc, String role) {
            this.npc = npc;
            this.role = role;
        }
    }

    // ---------------------------------------------------------------- roles

    /** The entity's roles, from its {@code dndclasses.role.*} command tags, sorted. */
    public static List<String> roles(Entity entity) {
        List<String> roles = new ArrayList<>();
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(ROLE_PREFIX) && tag.length() > ROLE_PREFIX.length()) {
                roles.add(tag.substring(ROLE_PREFIX.length()));
            }
        }
        roles.sort(null);
        return roles;
    }

    public static boolean hasRole(Entity entity, String role) {
        return entity.getCommandTags().contains(ROLE_PREFIX + role);
    }

    // ---------------------------------------------------------------- opening

    /**
     * {@code player} right-clicked {@code npc}. Opens the dialogue of the first of its roles that has
     * something to say (a quest to offer, a topic, an objective to count or a reward to pay) and returns
     * true; returns false to let the NPC's own interaction run (trades, gifts, barter).
     */
    public static boolean tryOpen(ServerPlayerEntity player, Entity npc) {
        if (QuestHooks.ignored.test(player) || player.isSpectator()) {
            return false;
        }
        QuestManager manager = QuestManager.get(player.getServer());
        String role = null;
        for (String candidate : roles(npc)) {
            if (somethingToSay(player, manager, candidate)) {
                role = candidate;
                break;
            }
        }
        if (role == null) {
            return false;
        }
        if (TierEffects.tierWith(player, npc) == ReputationTier.HOSTILE) {
            player.sendMessage(Text.translatable("dialogue.dndclasses.wont_speak", npc.getDisplayName())
                    .formatted(Formatting.RED), true);
            npc.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 0.9F);
            DnDClasses.LOGGER.info("[Dialogue] {} is Hostile: {} ({}) won't speak", player.getEntityName(),
                    npc.getName().getString(), role);
            return true;
        }
        DnDClasses.LOGGER.info("[Dialogue] {} talks to {} ({})", player.getEntityName(), npc.getName().getString(), role);
        QuestManager.TalkResult talk = manager.talkedTo(player, role);
        List<Text> prefix = new ArrayList<>();
        if (talk.handIns() > 0) {
            prefix.add(Text.translatable("dialogue.dndclasses.handed_in").formatted(Formatting.DARK_GREEN));
        }
        for (Text title : talk.rewarded()) {
            prefix.add(Text.translatable("dialogue.dndclasses.rewarded", title).formatted(Formatting.GOLD));
        }
        Session session = new Session(npc, role);
        SESSIONS.put(player.getUuid(), session);
        if (npc instanceof MobEntity mob) {
            mob.getNavigation().stop();
        }
        show(player, session, Dialogue.HUB, prefix);
        return true;
    }

    private static boolean somethingToSay(ServerPlayerEntity player, QuestManager manager, String role) {
        if (manager.hasBusinessWith(player, role) || !offers(player, manager, role).isEmpty()) {
            return true;
        }
        Dialogue dialogue = Dialogues.get(role);
        if (dialogue != null) {
            for (Dialogue.Option topic : dialogue.topics()) {
                if (visible(player, manager, topic)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Quests this role gives that the player could start now. */
    static List<QuestDefinition> offers(ServerPlayerEntity player, QuestManager manager, String role) {
        List<QuestDefinition> offers = new ArrayList<>();
        for (QuestDefinition quest : Quests.all()) {
            if (role.equals(quest.giver()) && manager.whyCantStart(player, quest) == null) {
                offers.add(quest);
            }
        }
        return offers;
    }

    // ---------------------------------------------------------------- pages

    private static void show(ServerPlayerEntity player, Session session, String node, List<Text> prefix) {
        Page page = page(player, session, node);
        if (page == null) {
            node = Dialogue.HUB;
            page = page(player, session, node);
        }
        session.node = node;
        session.shown = page.options();
        List<Text> lines = new ArrayList<>(prefix);
        lines.addAll(page.lines());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeText(page.speaker());
        buf.writeVarInt(lines.size());
        lines.forEach(buf::writeText);
        buf.writeVarInt(page.options().size());
        page.options().forEach(option -> buf.writeText(option.label()));
        ServerPlayNetworking.send(player, S2C_OPEN, buf);
        DnDClasses.LOGGER.info("[Dialogue] {} sees {}/{}: {}", player.getEntityName(), session.role, node,
                page.options().stream().map(option -> option.label().getString()).toList());
    }

    /** Builds a page; null if the node doesn't exist. */
    @Nullable
    static Page page(ServerPlayerEntity player, Session session, String node) {
        QuestManager manager = QuestManager.get(player.getServer());
        Dialogue dialogue = Dialogues.get(session.role);
        Text npcName = session.npc.getDisplayName();
        Text speaker = dialogue != null && dialogue.speaker() != null ? dialogue.speaker() : npcName;
        List<Shown> options = new ArrayList<>();
        if (node.equals(Dialogue.HUB)) {
            ReputationTier tier = TierEffects.tierWith(player, session.npc);
            Text greeting = dialogue != null ? dialogue.greeting(tier) : null;
            if (greeting == null) {
                greeting = Text.translatable("dialogue.dndclasses.greeting." + tier.id, player.getDisplayName());
            }
            for (QuestDefinition quest : offers(player, manager, session.role)) {
                options.add(new Shown(OFFER_PREFIX + quest.id(), Kind.OFFER,
                        Text.translatable("dialogue.dndclasses.option.offer", quest.title()).formatted(Formatting.GOLD),
                        quest.id(), null));
            }
            if (dialogue != null) {
                addVisible(player, manager, session, dialogue.topics(), options);
            }
            if (session.npc instanceof Merchant) {
                options.add(new Shown("trade", Kind.TRADE,
                        Text.translatable("dialogue.dndclasses.option.trade").formatted(Formatting.GREEN), null, null));
            }
            options.add(goodbye());
            return new Page(speaker, List.of(greeting), options);
        }
        if (node.startsWith(OFFER_PREFIX)) {
            Identifier id = Identifier.tryParse(node.substring(OFFER_PREFIX.length()));
            QuestDefinition quest = id == null ? null : Quests.get(id);
            if (quest == null) {
                return null;
            }
            List<Text> lines = new ArrayList<>();
            lines.add(quest.title().copy().formatted(Formatting.GOLD, Formatting.BOLD));
            lines.add(quest.description() != null ? quest.description() : quest.stage(0).text());
            options.add(new Shown("accept:" + quest.id(), Kind.ACCEPT,
                    Text.translatable("dialogue.dndclasses.option.accept").formatted(Formatting.GOLD), quest.id(), null));
            options.add(new Shown("decline", Kind.DECLINE, Text.translatable("dialogue.dndclasses.option.decline"), null, null));
            return new Page(speaker, lines, options);
        }
        Dialogue.Node page = dialogue == null ? null : dialogue.nodes().get(node);
        if (page == null) {
            return null;
        }
        addVisible(player, manager, session, page.options(), options);
        options.add(goodbye());
        return new Page(page.speaker() != null ? page.speaker() : speaker, page.lines(), options);
    }

    private static Shown goodbye() {
        return new Shown("goodbye", Kind.GOODBYE,
                Text.translatable("dialogue.dndclasses.option.goodbye").formatted(Formatting.GRAY), null, null);
    }

    private static void addVisible(ServerPlayerEntity player, QuestManager manager, Session session,
                                   List<Dialogue.Option> from, List<Shown> into) {
        for (Dialogue.Option option : from) {
            if (!visible(player, manager, option)) {
                continue;
            }
            MutableText label = option.text().copy();
            if (option.check() != null) {
                int dc = Persuasion.dc(player, session.npc, option.check().dc());
                label = Text.translatable("dialogue.dndclasses.option.check",
                        Text.translatable(option.check().skill().translationKey()), dc)
                        .formatted(Formatting.AQUA).append(" ").append(label.formatted(Formatting.WHITE));
            } else if (option.accept() != null) {
                label = label.formatted(Formatting.GOLD);
            }
            into.add(new Shown(option.key(), Kind.TOPIC, label, null, option));
        }
    }

    /** Whether an option's requirements are met (and, for a check, it hasn't been tried yet). */
    static boolean visible(ServerPlayerEntity player, QuestManager manager, Dialogue.Option option) {
        Dialogue.Requires requires = option.requires();
        for (Map.Entry<Identifier, ReputationTier> entry : requires.rep().entrySet()) {
            Faction faction = Factions.get(entry.getKey());
            if (faction == null || !Reputation.tier(player, faction).atLeast(entry.getValue())) {
                return false;
            }
        }
        if (requires.playerClass() != null && !D20.classOf(player).name().equalsIgnoreCase(requires.playerClass())) {
            return false;
        }
        QuestInstance instance = null;
        if (requires.quest() != null) {
            UUID uuid = player.getUuid();
            instance = manager.instanceOf(uuid, requires.quest());
            boolean ok = switch (requires.state()) {
                case ACTIVE -> instance != null && (requires.stage() <= 0 || instance.stage() + 1 == requires.stage())
                        && instance.flags().containsAll(requires.flags())
                        && requires.notFlags().stream().noneMatch(instance.flags()::contains);
                case FINISHED -> manager.hasFinished(uuid, requires.quest());
                case NOT_STARTED -> instance == null && !manager.hasFinished(uuid, requires.quest());
                case AVAILABLE -> {
                    QuestDefinition quest = Quests.get(requires.quest());
                    yield quest != null && manager.whyCantStart(player, quest) == null;
                }
            };
            if (!ok) {
                return false;
            }
        }
        return option.check() == null || !tried(player, instance, option);
    }

    // ---------------------------------------------------------------- choosing

    /** The client picked option {@code index} of the page it was shown (-1: closed the screen). */
    static void choose(ServerPlayerEntity player, int index) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) {
            if (index >= 0) {
                close(player);
            }
            return;
        }
        if (index < 0) {
            SESSIONS.remove(player.getUuid());
            return;
        }
        if (!stillValid(player, session)) {
            end(player, Text.translatable("dialogue.dndclasses.too_far", session.npc.getDisplayName()));
            return;
        }
        if (index >= session.shown.size()) {
            DnDClasses.LOGGER.warn("[Dialogue] {} chose option {} of {}", player.getEntityName(), index, session.shown.size());
            return;
        }
        Shown picked = session.shown.get(index);
        // Re-validate: the option must still be on the page, built afresh from the current state.
        Page page = page(player, session, session.node);
        Shown current = page == null ? null
                : page.options().stream().filter(option -> option.key().equals(picked.key())).findFirst().orElse(null);
        DnDClasses.LOGGER.info("[Dialogue] {} chose {} on {}/{}", player.getEntityName(), picked.key(), session.role,
                session.node);
        if (current == null) {
            show(player, session, session.node,
                    List.of(Text.translatable("dialogue.dndclasses.unavailable").formatted(Formatting.GRAY)));
            return;
        }
        QuestManager manager = QuestManager.get(player.getServer());
        switch (current.kind()) {
            case OFFER -> show(player, session, OFFER_PREFIX + current.quest(), List.of());
            case DECLINE -> show(player, session, Dialogue.HUB, List.of());
            case GOODBYE -> end(player, null);
            case TRADE -> {
                end(player, null);
                session.npc.interact(player, Hand.MAIN_HAND);
            }
            case ACCEPT -> {
                Text result = accept(player, manager, current.quest());
                show(player, session, Dialogue.HUB, List.of(result));
            }
            case TOPIC -> topic(player, manager, session, current.option());
        }
    }

    private static Text accept(ServerPlayerEntity player, QuestManager manager, Identifier questId) {
        QuestDefinition quest = Quests.get(questId);
        if (quest == null) {
            return Text.translatable("dialogue.dndclasses.unavailable").formatted(Formatting.GRAY);
        }
        try {
            manager.start(player, quest, false);
            return Text.translatable("dialogue.dndclasses.accepted", quest.title()).formatted(Formatting.GOLD);
        } catch (QuestManager.QuestException e) {
            return e.text().copy().formatted(Formatting.RED);
        }
    }

    private static void topic(ServerPlayerEntity player, QuestManager manager, Session session, Dialogue.Option option) {
        List<Text> prefix = new ArrayList<>();
        QuestDefinition quest = option.requires().quest() == null ? null : Quests.get(option.requires().quest());
        QuestInstance instance = quest == null ? null : manager.instanceOf(player.getUuid(), quest.id());
        if (option.accept() != null) {
            prefix.add(accept(player, manager, option.accept()));
        }
        runActions(player, manager, quest, instance, option.actions());
        String next = option.go();
        Dialogue.Check check = option.check();
        if (check != null) {
            markTried(player, manager, instance, option);
            int dc = Persuasion.dc(player, session.npc, check.dc());
            D20.Roll roll = SkillCheck.builder(player, check.skill(), dc, "dialogue").roll();
            boolean passed = roll.outcome().succeeded();
            D20.show(player, roll, Text.translatable(passed ? "dialogue.dndclasses.check.success"
                    : "dialogue.dndclasses.check.failure", session.npc.getDisplayName()));
            prefix.add(Text.translatable(passed ? "dialogue.dndclasses.check.passed" : "dialogue.dndclasses.check.failed",
                    Text.translatable(check.skill().translationKey()), roll.total(), dc)
                    .formatted(passed ? Formatting.GREEN : Formatting.RED));
            if (passed) {
                manager.checkPassed(player, check.skill().id());
            }
            // checkPassed may have moved the quest on; actions still belong to the same instance.
            runActions(player, manager, quest, instance, passed ? check.onSuccess() : check.onFailure());
            String branch = passed ? check.success() : check.failure();
            if (branch != null) {
                next = branch;
            }
        }
        if (option.end()) {
            end(player, null);
            return;
        }
        show(player, session, next != null ? next : Dialogue.HUB, prefix);
    }

    private static void runActions(ServerPlayerEntity player, QuestManager manager, @Nullable QuestDefinition quest,
                                   @Nullable QuestInstance instance, List<QuestAction> actions) {
        if (quest == null || actions.isEmpty()) {
            return;
        }
        QuestAction.Context context = new QuestAction.Context(manager, player.getServer(), player, quest, instance);
        for (QuestAction action : actions) {
            action.run(context);
        }
        manager.markDirty();
    }

    // ---------------------------------------------------------------- one try per check

    private static boolean tried(ServerPlayerEntity player, @Nullable QuestInstance instance, Dialogue.Option option) {
        if (instance != null) {
            return instance.flags().contains(TRIED_FLAG + option.key());
        }
        NbtList list = ((IEntityDataSaver) player).getPersistentData().getList(TRIED_KEY, NbtElement.STRING_TYPE);
        for (NbtElement element : list) {
            if (element.asString().equals(option.key())) {
                return true;
            }
        }
        return false;
    }

    private static void markTried(ServerPlayerEntity player, QuestManager manager, @Nullable QuestInstance instance,
                                  Dialogue.Option option) {
        if (instance != null) {
            manager.setFlag(instance, TRIED_FLAG + option.key());
            return;
        }
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtList list = data.getList(TRIED_KEY, NbtElement.STRING_TYPE);
        list.add(NbtString.of(option.key()));
        data.put(TRIED_KEY, list);
    }

    // ---------------------------------------------------------------- ending

    private static boolean stillValid(ServerPlayerEntity player, Session session) {
        return session.npc.isAlive() && !session.npc.isRemoved() && session.npc.getWorld() == player.getWorld()
                && session.npc.squaredDistanceTo(player) <= MAX_DISTANCE * MAX_DISTANCE && player.isAlive();
    }

    private static void end(ServerPlayerEntity player, @Nullable Text why) {
        SESSIONS.remove(player.getUuid());
        close(player);
        if (why != null) {
            player.sendMessage(why.copy().formatted(Formatting.GRAY), true);
        }
    }

    private static void close(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, S2C_CLOSE, PacketByteBufs.empty());
    }

    /** Every tick: NPCs face whoever they're talking to; walking away or the NPC dying ends the talk. */
    static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Session> entry : List.copyOf(SESSIONS.entrySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null) {
                SESSIONS.remove(entry.getKey());
            } else if (!stillValid(player, session)) {
                end(player, Text.translatable("dialogue.dndclasses.too_far", session.npc.getDisplayName()));
            } else if (session.npc instanceof MobEntity mob) {
                mob.getLookControl().lookAt(player);
                mob.getNavigation().stop();
            }
        }
    }

    static void forget(UUID player) {
        SESSIONS.remove(player);
    }
}
