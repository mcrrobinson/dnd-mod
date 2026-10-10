package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructures;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * The Orc Strongholds' grudge rule ({@link SettlementGrudges}): opening the War Chief's hoard (any chest in the
 * Great Longhouse) where an orc within 16 blocks can see you turns every such orc on you. Together with
 * striking an orc, this is how a tolerated Half-Orc or Blood-Sworn player gets on the wrong side of a hold.
 * There's no kin trust.
 */
final class OrcLaw implements SettlementGrudges.Rules {
    OrcLaw() {
    }

    @Override
    public String id() {
        return "dndclasses:bloodtusk_horde";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (offence != SettlementGrudges.Offence.OPEN || !(blockEntity instanceof ChestBlockEntity)
                || !(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        RacialHomes.Visit visit = RacialHomes.homeAt(serverWorld, pos);
        return visit != null && visit.home() == RacialHomes.ORC_STRONGHOLD
                && visit.pieceAt(pos, OrcStrongholdStructures.LONGHOUSE) != null;
    }

    @Override
    public double witnessRange(PlayerEntity player) {
        return TierEffects.witnessRange(TierEffects.tierWith(player, ModEntityTypes.ORC_WARRIOR));
    }

    @Override
    public List<OrcEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(OrcEntity.class, new Box(pos).expand(this.witnessRange(player)),
                orc -> orc.isAlive() && !(orc instanceof OrcChampionEntity) && orc.canSee(player));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        ((OrcEntity) guard).provoke(player);
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        guard.getLookControl().lookAt(player);
        guard.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, 1.0F, guard.getSoundPitch());
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return SettlementGrudges.Verdict.GRUDGE;
    }
}
