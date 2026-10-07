package mattonfire.dnd.classes;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Tells potion-sourced status effects apart from every other source (class
 * abilities, enchantments, armor, mob attacks, beacons, food...).
 *
 * Vanilla code paths that apply a potion's effects (drinking, splash,
 * lingering clouds, tipped arrows) are wrapped by mixins with
 * {@link #begin()} / {@link #end()}. While inside one of those, effects that
 * {@link #isImmune} says the target is immune to are dropped. Effects applied
 * anywhere else, such as class power-ups, are never touched.
 */
public final class PotionImmunity {
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private PotionImmunity() {
    }

    public static void begin() {
        DEPTH.get()[0]++;
    }

    public static void end() {
        int[] depth = DEPTH.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean inPotionContext() {
        return DEPTH.get()[0] > 0;
    }

    /**
     * Whether a potion-sourced effect should be ignored by this entity.
     * Fighter: no potion buffs (harmful potions such as a witch's splash still
     * land, so it stays a drawback). Artificer and Paladin: unaffected by any
     * potion, good or bad (ability effects still apply).
     */
    public static boolean isImmune(LivingEntity entity, StatusEffect effect) {
        if (!(entity instanceof PlayerEntityExt player) || player.getDndClass() == null) {
            return false;
        }
        switch (player.getDndClass()) {
            case FIGHTER:
                return effect.getCategory() != StatusEffectCategory.HARMFUL;
            case ARTIFICER:
            case PALADIN:
                return true;
            default:
                return false;
        }
    }

    /** True when we are applying a potion's effects and the entity ignores this one. */
    public static boolean shouldBlock(LivingEntity entity, StatusEffect effect) {
        return inPotionContext() && isImmune(entity, effect);
    }
}
