package mattonfire.dnd.classes.Progression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mattonfire.dnd.classes.DndCharacter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * A player's progress in one class: XP, unlocked nodes, their ranks, the
 * equipped loadout (one active, up to two passives) and the bestiary. Shared by
 * the server, which owns it, and the client, which gets a copy for the current
 * class.
 */
public class ClassProgress {
    /** Total XP needed for each level; the index is the level. */
    public static final int[] LEVEL_XP = { 0, 40, 120, 250, 450, 700, 1000, 1350, 1750, 2200, 2700 };
    public static final int MAX_LEVEL = LEVEL_XP.length - 1;
    public static final int PASSIVE_SLOTS = 2;

    /** The local player's progress, kept up to date by the server. Unused on the server. */
    public static ClassProgress client = new ClassProgress(DndCharacter.NONE);

    public final DndCharacter dndClass;
    public int xp;
    public final Set<String> unlocked = new LinkedHashSet<>();
    public String active = "";
    public final List<String> passives = new ArrayList<>();
    /** Ranks above 1, by node id; a missing entry is rank 1. */
    public final Map<String, Integer> ranks = new LinkedHashMap<>();
    /** Entity type ids killed while playing this class, for classes with a bestiary. */
    public final Set<String> learned = new LinkedHashSet<>();
    /** Learned entities unlocked at an Attunement Table. */
    public final Set<String> bestiary = new LinkedHashSet<>();

    /** Why a node can't go up a rank, or {@link #OK}. */
    public enum RankUp {
        OK, LOCKED, MAX_RANK, LEVEL, POINTS
    }

    public ClassProgress(DndCharacter dndClass) {
        this.dndClass = dndClass;
    }

    public static int levelFor(int xp) {
        int level = 0;
        while (level < MAX_LEVEL && xp >= LEVEL_XP[level + 1]) {
            level++;
        }
        return level;
    }

    public int level() {
        return levelFor(xp);
    }

    public int pointsSpent() {
        int spent = 0;
        for (String id : unlocked) {
            SkillNode node = ClassTrees.node(id);
            if (node != null) {
                spent += node.pointCost();
            }
        }
        for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            if (ClassTrees.node(entry.getKey()) != null) {
                spent += Math.max(0, entry.getValue() - 1) * Ranks.POINT_COST;
            }
        }
        return spent;
    }

    public int points() {
        return level() - pointsSpent();
    }

    public boolean isUnlocked(String id) {
        SkillNode node = ClassTrees.node(id);
        return node != null && (node.isRoot() || unlocked.contains(id));
    }

    /** Whether the node is next in line: its requirement is unlocked but it isn't. */
    public boolean isReachable(SkillNode node) {
        return !isUnlocked(node.id()) && node.requires().stream().anyMatch(this::isUnlocked);
    }

    public boolean canUnlock(SkillNode node) {
        return ClassTrees.belongsTo(node, dndClass) && isReachable(node) && points() >= node.pointCost();
    }

    /** The node's rank, from 1 (also for locked nodes, so their values can be shown). */
    public int rank(String id) {
        return Math.max(1, Math.min(ranks.getOrDefault(id, 1), Ranks.maxRank(id)));
    }

    /** Whether the node could go up a rank now, at an Attunement Table. */
    public RankUp canRankUp(SkillNode node) {
        if (!ClassTrees.belongsTo(node, dndClass) || !isUnlocked(node.id()))
            return RankUp.LOCKED;
        int next = rank(node.id()) + 1;
        if (next > node.maxRank())
            return RankUp.MAX_RANK;
        if (level() < Ranks.levelFor(node.id(), next))
            return RankUp.LEVEL;
        if (points() < Ranks.POINT_COST)
            return RankUp.POINTS;
        return RankUp.OK;
    }

    public boolean usesBestiary() {
        ClassSkills skills = ClassTrees.skills(dndClass);
        return skills != null && skills.usesBestiary();
    }

    /** Rank of the root special needed to unlock a learned entity; MAX_VALUE if it can't be. */
    public int bestiaryRank(String entityId) {
        ClassSkills skills = ClassTrees.skills(dndClass);
        Identifier id = Identifier.tryParse(entityId);
        if (skills == null || id == null || !Registries.ENTITY_TYPE.containsId(id))
            return Integer.MAX_VALUE;
        return skills.bestiaryRank(Registries.ENTITY_TYPE.get(id));
    }

    public boolean canUnlockBestiary(String entityId) {
        SkillNode root = ClassTrees.root(dndClass);
        return usesBestiary() && root != null && learned.contains(entityId) && !bestiary.contains(entityId)
                && rank(root.id()) >= bestiaryRank(entityId);
    }

    /** The equipped active, falling back to the class's original power-up. */
    public SkillNode activeNode() {
        SkillNode node = ClassTrees.node(active);
        if (node != null && node.isActive() && ClassTrees.belongsTo(node, dndClass) && isUnlocked(node.id())) {
            return node;
        }
        return ClassTrees.root(dndClass);
    }

    public boolean hasPassive(String id) {
        return passives.contains(id) && isUnlocked(id);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("xp", xp);
        nbt.put("unlocked", toList(unlocked));
        nbt.putString("active", active);
        nbt.put("passives", toList(passives));
        NbtCompound rankNbt = new NbtCompound();
        ranks.forEach(rankNbt::putInt);
        nbt.put("ranks", rankNbt);
        nbt.put("learned", toList(learned));
        nbt.put("bestiary", toList(bestiary));
        return nbt;
    }

    public static ClassProgress fromNbt(DndCharacter dndClass, NbtCompound nbt) {
        ClassProgress progress = new ClassProgress(dndClass);
        progress.xp = nbt.getInt("xp");
        nbt.getList("unlocked", NbtElement.STRING_TYPE).forEach(e -> progress.unlocked.add(e.asString()));
        progress.active = nbt.getString("active");
        nbt.getList("passives", NbtElement.STRING_TYPE).forEach(e -> progress.passives.add(e.asString()));
        NbtCompound rankNbt = nbt.getCompound("ranks");
        for (String id : rankNbt.getKeys()) {
            progress.ranks.put(id, rankNbt.getInt(id));
        }
        nbt.getList("learned", NbtElement.STRING_TYPE).forEach(e -> progress.learned.add(e.asString()));
        nbt.getList("bestiary", NbtElement.STRING_TYPE).forEach(e -> progress.bestiary.add(e.asString()));
        return progress;
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(dndClass.getValue());
        buf.writeNbt(toNbt());
    }

    public static ClassProgress read(PacketByteBuf buf) {
        DndCharacter dndClass = DndCharacter.fromValue(buf.readInt());
        NbtCompound nbt = buf.readNbt();
        return fromNbt(dndClass, nbt == null ? new NbtCompound() : nbt);
    }

    private static NbtList toList(Iterable<String> values) {
        NbtList list = new NbtList();
        for (String value : values) {
            list.add(NbtString.of(value));
        }
        return list;
    }
}
