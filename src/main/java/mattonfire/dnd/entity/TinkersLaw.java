package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Cogsdeep's grudge rules ({@link SettlementGrudges}). Every gnome and Clockwork Sentinel within 16 blocks who can
 * see you takes offence, and the Sentinels turn on you, if inside the burrow you open or break a chest or barrel,
 * or break one of the workshops' contraptions (a piston, observer, redstone lamp, dispenser, dropper, hopper,
 * repeater, comparator, note block or lightning rod). Striking a gnome or a Sentinel is the other grudge
 * ({@link GnomeTinkerEntity#defend}).
 *
 * <p>Kin trust: a Gnome's first offence each in-game day only gets a warning.
 */
final class TinkersLaw implements SettlementGrudges.Rules {
    TinkersLaw() {
    }

    @Override
    public String id() {
        return "dndclasses:tinkers_guild";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        boolean container = (offence == SettlementGrudges.Offence.OPEN || offence == SettlementGrudges.Offence.BREAK)
                && (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity);
        boolean contraption = offence == SettlementGrudges.Offence.BREAK && isContraption(state);
        if (!container && !contraption) {
            return false;
        }
        RacialHomes.Visit visit = RacialHomes.homeAt(serverWorld, pos);
        return visit != null && visit.home() == RacialHomes.COGSDEEP;
    }

    /** The workshops' machinery. */
    static boolean isContraption(BlockState state) {
        return state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON) || state.isOf(Blocks.OBSERVER)
                || state.isOf(Blocks.REDSTONE_LAMP) || state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER)
                || state.isOf(Blocks.HOPPER) || state.isOf(Blocks.REPEATER) || state.isOf(Blocks.COMPARATOR)
                || state.isOf(Blocks.NOTE_BLOCK) || state.isOf(Blocks.LIGHTNING_ROD);
    }

    /** 16 blocks, adjusted for the player's standing with the Tinkers' Guild. */
    @Override
    public double witnessRange(PlayerEntity player) {
        return TierEffects.witnessRange(TierEffects.tierWith(player, ModEntityTypes.CLOCKWORK_SENTINEL));
    }

    @Override
    public List<MobEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(MobEntity.class, new Box(pos).expand(this.witnessRange(player)),
                npc -> (npc instanceof GnomeTinkerEntity || npc instanceof ClockworkSentinelEntity) && npc.isAlive()
                        && npc.canSee(player));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        if (guard instanceof ClockworkSentinelEntity sentinel) {
            sentinel.provoke(player);
        } else {
            this.warn(guard, player);
        }
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        guard.getLookControl().lookAt(player);
        guard.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, guard.getSoundPitch());
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        if (!RacialHomes.COGSDEEP.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
            return SettlementGrudges.Verdict.GRUDGE;
        }
        SettlementGrudges.Verdict verdict = SettlementGrudges.dailyWarning(player, this);
        if (verdict == SettlementGrudges.Verdict.WARNING) {
            player.sendMessage(Text.translatable("message.dndclasses.home.cogsdeep_warning").formatted(Formatting.GOLD), true);
        }
        return verdict;
    }
}
