package mattonfire.dnd.classes.Progression;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.SkillNode.Kind;
import mattonfire.dnd.classes.Rest.Charges;
import mattonfire.dnd.classes.Rest.RechargeGroup;
import mattonfire.dnd.classes.Rest.RestState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * One class's skill tree and everything its skills do. Each class has its own
 * subclass in {@code Progression.Classes}, listed in {@link ClassTrees}.
 *
 * The hooks are only called for players whose current class this is, with
 * that class's progress, so check {@code progress.hasPassive(id)} for passives
 * and {@code progress.hasSubclass(id)} for subclass features.
 */
public abstract class ClassSkills {
    public abstract DndCharacter dndClass();

    /**
     * The tree, root first; empty while the class has no tree. Every tree has
     * the same shape, see {@link ClassTrees}.
     */
    public abstract List<SkillNode> nodes();

    /**
     * The class's two subclass ids: the right branch (column 2, listed first in
     * {@link #nodes()}) then the left. {@link ClassTrees} works out which nodes
     * each one locks; the text is in {@code class_info.json}.
     */
    public abstract List<String> subclassIds();

    /** The class's two subclasses, right branch first. */
    public final List<Subclass> subclasses() {
        return ClassTrees.subclasses(dndClass());
    }

    /** Registers any extra event hooks. Called once at startup. */
    public void register() {
    }

    /**
     * Fires a non-root active skill (the root is the class's power-up in
     * {@code PowerUpEffect}).
     *
     * @return false if nothing happened, so no mana is spent
     */
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        return false;
    }

    /**
     * The mana an active costs this player (subclass features can lower it). Both sides: the client
     * reads it for the HUD with {@code ClassProgress.client}.
     */
    public int manaCost(ClassProgress progress, SkillNode node) {
        return node.manaCost();
    }

    /** {@link #manaCost} for the progress's class, or the node's own cost. */
    public static int manaCostFor(ClassProgress progress, SkillNode node) {
        ClassSkills skills = ClassTrees.skills(progress.dndClass);
        return skills == null ? node.manaCost() : skills.manaCost(progress, node);
    }

    /**
     * Called after the player's equipped active (the root special included) fired and its mana was
     * spent. Server only.
     */
    public void afterActivate(ServerPlayerEntity player, ClassProgress progress, @org.jetbrains.annotations.Nullable SkillNode node) {
    }

    /** XP on top of the {@link ProgressionEvents#HOSTILE_KILL_XP} every class gets for a hostile kill. */
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return 0;
    }

    /** Called after the player kills something (passives like healing on kill). */
    public void onKill(ServerPlayerEntity player, ClassProgress progress, LivingEntity killed, DamageSource source) {
    }

    /** Changes damage the player deals, before armor. Server only. */
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        return amount;
    }

    /** Changes damage the player takes, before armor. Server only. */
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        return amount;
    }

    /** Called once a second on the server. */
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
    }

    /**
     * Drops this class's per-player state for the player and undoes anything it
     * applied that wouldn't end by itself once the player isn't this class. Called
     * for every class when a player changes class or disconnects, so it must be
     * safe for players who never were this class. Keep cooldowns that are still
     * running (relogging shouldn't reset them); just prune the ones that ended.
     */
    public void forget(ServerPlayerEntity player) {
    }

    /**
     * Whether the class has a bestiary: kills while playing it are learned, and
     * learned entities are unlocked at an Attunement Table for its root special.
     */
    public boolean usesBestiary() {
        return false;
    }

    /** Whether killing this entity adds it to the bestiary. Only asked when {@link #usesBestiary()}. */
    public boolean learnsFrom(LivingEntity killed) {
        return true;
    }

    /**
     * Rank of the root special needed to unlock this learned entity at the
     * table (its tier); above the root's max rank means never. Both sides.
     */
    public int bestiaryRank(EntityType<?> type) {
        return 1;
    }

    /** How this class gets charges back from rests; the 5e-style default from {@link Charges#defaultGroup}. */
    public RechargeGroup rechargeGroup() {
        return Charges.defaultGroup(dndClass());
    }

    /**
     * Charges a short rest gives back (capped at {@code max} by the caller): all of
     * them for a short-rest class, 1 otherwise. May change {@code state}, which is
     * saved afterwards.
     */
    public int shortRestCharges(ServerPlayerEntity player, RestState state, int max) {
        return rechargeGroup() == RechargeGroup.SHORT ? max : 1;
    }

    /**
     * Called after a short rest's benefits, with everyone resting together (the
     * player included). For bonuses like the Bard's Song of Rest.
     */
    public void onShortRest(ServerPlayerEntity player, List<ServerPlayerEntity> companions) {
    }

    /** Passives that are just an attribute bonus; added and removed automatically. */
    public List<AttributeBonus> attributeBonuses() {
        return List.of();
    }

    protected static SkillNode active(String id, String name, String description, String icon, int manaCost,
            int pointCost, int col, int row, String... requires) {
        return new SkillNode(id, name, description, icon, Kind.ACTIVE, manaCost, pointCost, col, row,
                List.of(requires));
    }

    protected static SkillNode passive(String id, String name, String description, String icon, int pointCost,
            int col, int row, String... requires) {
        return new SkillNode(id, name, description, icon, Kind.PASSIVE, 0, pointCost, col, row, List.of(requires));
    }
}
