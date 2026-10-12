package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * One kind of class-gated obstacle (an Arcane Seal, later a Holy Ward, a Thornwall...). The block
 * ({@link ObstacleBlock}) and its block entity are shared; everything that differs between kinds
 * lives here. Registered in {@link ObstacleTypes}.
 *
 * <p>Text keys, all under {@link #translationKey()}: the name itself, {@code .verb} ("dispel it",
 * used in "A Wizard or Warlock could dispel it"), {@code .fallback} (what anyone else can do),
 * and the roll lines {@code .success}, {@code .critical}, {@code .failure} and {@code .fumble}.
 */
public abstract class ObstacleType {
    private final Identifier id;

    protected ObstacleType(Identifier id) {
        this.id = id;
    }

    public final Identifier id() {
        return id;
    }

    public final String translationKey() {
        return "obstacle." + id.getNamespace() + "." + id.getPath();
    }

    /**
     * The check this obstacle rolls, or null for obstacles you get past without one. The modifier is the
     * solver's character-sheet bonus for it.
     */
    @Nullable
    public abstract Skill skill();

    /**
     * The check {@code player} rolls (a Guardian Beast: Animal Handling for a Druid, Performance for a Bard).
     * Defaults to {@link #skill()}. Used by the server roll and the client hint alike.
     */
    @Nullable
    public Skill skillFor(PlayerEntity player) {
        return skill();
    }

    /** Who may attempt it. The modifier always comes from the sheet; this only gates and orders solvers. */
    public abstract Eligibility eligibility(DndCharacter dndClass);

    /** Classes that may attempt it, primaries first. */
    public final List<DndCharacter> solvers() {
        List<DndCharacter> primary = new ArrayList<>();
        List<DndCharacter> secondary = new ArrayList<>();
        for (DndCharacter c : DndCharacter.values()) {
            if (c == DndCharacter.NONE) {
                continue;
            }
            Eligibility e = eligibility(c);
            if (e == Eligibility.PRIMARY) {
                primary.add(c);
            } else if (e == Eligibility.SECONDARY) {
                secondary.add(c);
            }
        }
        primary.addAll(secondary);
        return primary;
    }

    /** Whether "take your time" (sneak + right-click, out of combat) is allowed at this tier. */
    public boolean allowsTakeYourTime(Tier tier) {
        return tier != Tier.VERY_HARD;
    }

    /** Whether a survival player can break it by hand as its fallback. Sets the block's hardness. */
    public abstract boolean breakable();

    /** The block's hardness: {@link ObstacleTypes#SEAL_HARDNESS} if {@link #breakable()}, else unbreakable. */
    public float hardness() {
        return breakable() ? ObstacleTypes.SEAL_HARDNESS : -1.0f;
    }

    /** The block's blast resistance: out of reach of explosions unless a type's fallback is blowing it up. */
    public float blastResistance() {
        return ObstacleTypes.BLAST_RESISTANCE;
    }

    /** Whether right-clicking it does anything (false for things you cross, like ledges). */
    public boolean interactive() {
        return true;
    }

    /**
     * Whether a block in this state has collision and an outline. Sealed obstacles are in the way and
     * open ones aren't, unless a type says otherwise (a chasm's bridge is solid once open).
     */
    public boolean solid(ObstacleState state) {
        return state == ObstacleState.SEALED;
    }

    /**
     * Whether the hint names who can deal with it from its own text ({@code .who}) rather than
     * listing its solver classes (for obstacles anyone can work, or nobody can roll for).
     */
    public boolean ownWhoText() {
        return false;
    }

    /**
     * Whether the obstacle is still hidden from {@code player}: it looks like the blocks round it, gives no
     * crosshair hint, and right-clicking it does nothing until it's found (a Hidden Door). Called on both
     * sides, from the synced block entity. Creative players see everything.
     */
    public boolean hiddenFrom(BlockView world, BlockPos pos, PlayerEntity player) {
        return false;
    }

    /**
     * Whether it passes for an ordinary full block while sealed (shaded like one), so it doesn't stand out
     * from the wall it hides in.
     */
    public boolean disguised() {
        return false;
    }

    /**
     * Whether the 1-block shell round it is protected (no building in, no digging out). Hidden obstacles
     * turn it off, since a refused block would give them away.
     */
    public boolean protectsShell() {
        return true;
    }

    /** The HUD label of its roll (null: the skill's name). */
    @Nullable
    public String rollLabel() {
        return null;
    }

    /** The item the obstacle wants in the main hand (null: an empty hand). */
    @Nullable
    public ItemStack requiredItem() {
        return null;
    }

    /**
     * Whether right-clicking with {@code held} in the main hand works it: by default an empty hand, or the
     * {@link #requiredItem()}. A type that uses items up per attempt also takes them in hand.
     */
    public boolean accepts(PlayerEntity player, ItemStack held) {
        ItemStack required = requiredItem();
        return required == null ? held.isEmpty() : ItemStack.areItemsEqual(held, required);
    }

    /**
     * Server side, just before a roll (or a take-your-time focus) starts: pays what the attempt costs, such as
     * the items it uses up, and returns true; or tells the player why not and returns false. Never called for
     * creative players or for obstacles with no roll.
     */
    public boolean payForAttempt(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        return true;
    }

    /**
     * Whether a block in this state stops the entity in {@code context} (its collision shape). By default
     * {@link #solid}; a Fire Curtain lets fire-proof players through. It's asked on both sides, so it must
     * only use state the client knows too.
     */
    public boolean collides(ObstacleState state, ShapeContext context) {
        return solid(state);
    }

    /**
     * Server side, after {@link #accepts}: handles the click itself instead of a roll (a Singing Door plays its
     * tune to an empty hand). Returns true when it did.
     */
    public boolean intercept(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        return false;
    }

    /** Per-roll extras, such as an instrument bonus. Returns the builder. */
    public D20.Builder rollExtras(D20.Builder builder, ServerPlayerEntity player) {
        return builder;
    }

    /** A check on this obstacle has just been rolled, before its result plays out. */
    public void onRolled(ServerWorld world, BlockPos pos, ServerPlayerEntity player, D20.Roll roll) {
    }

    /**
     * How fast {@code player} breaks a sealed block of it, per tick ({@code vanilla}: the usual speed, or 0 if it
     * isn't {@link #breakable()}). A Thornwall is cut at a fixed speed with shears or an axe.
     */
    public float breakingDelta(PlayerEntity player, float vanilla) {
        return vanilla;
    }

    /** A block next to one of its blocks changed (fire was lit beside a Thornwall). Server side. */
    public void neighborChanged(ServerWorld world, BlockPos pos, BlockPos from) {
    }

    public int xp(Tier tier) {
        return tier.xp;
    }

    /**
     * Server side, before the roll: returns true (after telling the player why) when something
     * stops this player trying right now, such as the Beholder's Anti-Magic.
     */
    public boolean blocked(ServerPlayerEntity player) {
        return false;
    }

    /**
     * Gets {@code solver} past the obstacle at {@code pos} after a successful check (or a use, for
     * obstacles with no check). Returns whether it earns class XP. By default the whole group opens.
     * Even when this returns true, a group pays out its XP only once until it's reset
     * ({@link ObstacleGroups#claimReward}).
     */
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        return ObstacleGroups.open(world, pos) > 0;
    }

    /** The obstacle group at {@code pos} has just opened for {@code solver}. */
    public void onSolved(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
    }

    /** A failed check's sting. */
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
    }

    /** A natural 1's sting. Defaults to the failure sting. */
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        onFailure(world, pos, player);
    }

    /** Whether a natural 1 raises {@link ObstacleEvents#ALARM}. */
    public boolean alarmsOnFumble() {
        return true;
    }

    /** A survival player has just broken one sealed block of this obstacle (only if {@link #breakable()}). */
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
    }

    /**
     * Whether breaking a sealed block by hand opens the whole group in place instead: the block isn't removed,
     * {@link #onBrokenByHand} opens the group, so a reset puts it back whole (a smashed Clockwork Door).
     */
    public boolean opensWhenBroken() {
        return false;
    }

    /**
     * Keys in a block's {@code Data} that hold progress towards the obstacle (potions thrown, health paid,
     * "already earned XP" flags). {@link ObstacleGroups#reset} clears them on every block of the group,
     * sealed or not; a reseal on a timer keeps them.
     */
    public List<String> progressKeys() {
        return List.of();
    }

    /** Effects when a group opens or reseals, played at each block. */
    /** {@code first} is true for one block of the group, for sounds. */
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
    }

    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
    }

    /** A block tick scheduled with {@code world.scheduleBlockTick} for one of its blocks. */
    public void scheduledTick(ServerWorld world, BlockPos pos, BlockState state) {
    }

    /** An explosion has just removed the block at {@code pos} (already air). */
    public void onExploded(ServerWorld world, BlockPos pos) {
    }

    /** Registers any server events the type needs. Called once, from {@link ObstacleTypes#register()}. */
    public void registerEvents() {
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
