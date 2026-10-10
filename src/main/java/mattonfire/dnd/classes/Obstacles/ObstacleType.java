package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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

    /** The item the obstacle wants in the main hand (null: an empty hand). */
    @Nullable
    public ItemStack requiredItem() {
        return null;
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

    /** Effects when a group opens or reseals, played at each block. */
    /** {@code first} is true for one block of the group, for sounds. */
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
    }

    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
