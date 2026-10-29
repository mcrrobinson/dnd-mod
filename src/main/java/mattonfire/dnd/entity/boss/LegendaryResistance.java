package mattonfire.dnd.entity.boss;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.entity.MobActions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Legendary Resistance (design 10, 5.7): {@link #max} times between refills, a {@link LegendaryCreature}'s fresh
 * failed save becomes a success. Half-damage saves (breath and blasts) are left alone, so the charges go on the
 * saves that matter: Stunning Strike, the Eye of the Tyrant, a DM's called save.
 *
 * <p>The owner refills it ({@link #refill}) when a fight ends without a death and at each dawn. One
 * {@link SavingThrow.ResultHook} (registered by {@link #register}) spends the charges for every legendary creature.
 */
public final class LegendaryResistance {
    /** Players this close see the "shrugs it off" line. */
    public static final double NOTICE_RANGE = 48.0;
    private static final String NBT = "LegendaryResistance";

    private final int max;
    private int left;

    public LegendaryResistance(int max) {
        this.max = max;
        this.left = max;
    }

    public int max() {
        return this.max;
    }

    public int left() {
        return this.left;
    }

    public void refill() {
        this.left = this.max;
    }

    /** Spends a charge if one is left. */
    public boolean spend() {
        if (this.left <= 0) {
            return false;
        }
        this.left--;
        return true;
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt(NBT, this.left);
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains(NBT)) {
            this.left = Math.max(0, Math.min(this.max, nbt.getInt(NBT)));
        }
    }

    /** "◆◆◆" for three charges left, padded with "◇" for spent ones. */
    public String diamonds() {
        return "◆".repeat(this.left) + "◇".repeat(this.max - this.left);
    }

    /** Registers the save hook; call once at startup. */
    public static void register() {
        SavingThrow.registerResultHook((target, ability, result) -> {
            if (!(target instanceof LegendaryCreature creature) || target.getWorld().isClient
                    || !result.fresh() || result.succeeded() || result.halvesDamage()) {
                return result;
            }
            LegendaryResistance resistance = creature.legendaryResistance();
            if (resistance == null || !resistance.spend()) {
                return result;
            }
            announce(target, resistance.left());
            return result.overruled(Text.translatable("save.dndclasses.legendary_resistance"));
        });
    }

    private static void announce(LivingEntity creature, int left) {
        DnDClasses.LOGGER.info("[LegendaryResistance] {} turns a failed save into a success ({} left)",
                creature.getName().getString(), left);
        MobActions.log(creature, "legendary_resistance (" + left + " left)", null);
        if (!(creature.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Text line = Text.translatable("boss.dndclasses.legendary_resistance", creature.getDisplayName(), left)
                .formatted(Formatting.GOLD);
        for (ServerPlayerEntity player : world.getPlayers(
                p -> p.squaredDistanceTo(creature) < NOTICE_RANGE * NOTICE_RANGE)) {
            player.sendMessage(line, true);
        }
    }
}
