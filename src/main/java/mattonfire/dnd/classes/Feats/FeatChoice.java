package mattonfire.dnd.classes.Feats;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Abilities.Ability;
import net.minecraft.nbt.NbtCompound;

/**
 * One feat a class has taken, with the ability it went to (Resilient only) and what paid for it, so a refund gives
 * back the right currency.
 */
public record FeatChoice(Feat feat, @Nullable Ability ability, Source source) {
    public enum Source {
        /** The level {@code FEAT_LEVEL} slot (or an admin grant). */
        FREE("free"),
        /** {@code FEAT_COST} ability points. */
        ASI("asi");

        private final String id;

        Source(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Source byId(String id) {
            return "asi".equals(id) ? ASI : FREE;
        }
    }

    /** "Resilient (CON)" or the feat's name. */
    public String label() {
        return ability == null ? feat.displayName() : feat.displayName() + " (" + ability.shortKey() + ")";
    }

    /** "level 6" or "ability points", for "Paid with: ...". */
    public String paidWith() {
        return source == Source.ASI ? "2 ability points" : "the level 6 feat";
    }

    public boolean matches(Feat other, @Nullable Ability otherAbility) {
        return feat == other && (!feat.perAbility() || otherAbility == null || ability == otherAbility);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", feat.id());
        if (ability != null) {
            nbt.putString("ability", ability.shortKey());
        }
        nbt.putString("source", source.id());
        return nbt;
    }

    @Nullable
    public static FeatChoice fromNbt(NbtCompound nbt) {
        Feat feat = Feat.byId(nbt.getString("id"));
        if (feat == null) {
            return null;
        }
        Ability ability = nbt.contains("ability") ? Ability.byId(nbt.getString("ability")) : null;
        if (feat.perAbility() && ability == null) {
            return null;
        }
        return new FeatChoice(feat, feat.perAbility() ? ability : null, Source.byId(nbt.getString("source")));
    }
}
