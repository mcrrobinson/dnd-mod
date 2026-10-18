package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Registry.CogsdeepBlocks;
import mattonfire.dnd.faction.FactionIds;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Cogsdeep's grudge rules ({@link SettlementGrudges}). Every gnome and Clockwork Sentinel within 16 blocks who can
 * see you takes offence, and the Sentinels turn on you, if inside the burrow you open or break a chest or barrel,
 * or break one of the workshops' contraptions (a piston, observer, redstone lamp, dispenser, dropper, hopper,
 * repeater, comparator, note block or lightning rod) or the burrow's clockwork (a cogwork panel, gearwall,
 * clockface, glowcrystal, pipework, blueprint table or telescope lens). Striking a gnome or a Sentinel is the other grudge
 * ({@link SettlementNpcEntity#callGuards}).
 *
 * <p>Kin trust: a Gnome's first offence each in-game day only gets a warning.
 */
final class TinkersLaw extends GuardRules<MobEntity> {
    TinkersLaw() {
        super(FactionIds.TINKERS_GUILD, MobEntity.class, () -> ModEntityTypes.CLOCKWORK_SENTINEL);
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
        return RacialHomes.visitOf(serverWorld, pos, RacialHomes.COGSDEEP) != null;
    }

    /** The workshops' machinery. */
    static boolean isContraption(BlockState state) {
        return state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON) || state.isOf(Blocks.OBSERVER)
                || state.isOf(Blocks.REDSTONE_LAMP) || state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER)
                || state.isOf(Blocks.HOPPER) || state.isOf(Blocks.REPEATER) || state.isOf(Blocks.COMPARATOR)
                || state.isOf(Blocks.NOTE_BLOCK) || state.isOf(Blocks.LIGHTNING_ROD) || isClockwork(state);
    }

    /** Cogsdeep's own machinery blocks. */
    static boolean isClockwork(BlockState state) {
        return state.isOf(CogsdeepBlocks.COGWORK_PANEL) || state.isOf(CogsdeepBlocks.GEARWALL)
                || state.isOf(CogsdeepBlocks.CLOCKFACE) || state.isOf(CogsdeepBlocks.PIPEWORK)
                || state.isOf(CogsdeepBlocks.BLUEPRINT_TABLE) || state.isOf(CogsdeepBlocks.TELESCOPE_LENS)
                || CogsdeepBlocks.GLOWCRYSTALS.containsValue(state.getBlock());
    }

    /** The burrow's folk: its gnomes and its Sentinels. */
    @Override
    protected boolean canWitness(MobEntity npc, PlayerEntity player) {
        return (npc instanceof GnomeTinkerEntity || npc instanceof ClockworkSentinelEntity) && super.canWitness(npc, player);
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return this.kinWarning(player, RacialHomes.COGSDEEP, "message.dndclasses.home.cogsdeep_warning");
    }
}
