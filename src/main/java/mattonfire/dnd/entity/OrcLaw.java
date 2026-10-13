package mattonfire.dnd.entity;

import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructures;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Orc Strongholds' grudge rule ({@link SettlementGrudges}): opening the War Chief's hoard (any chest in the
 * Great Longhouse) where an orc within 16 blocks can see you turns every such orc on you. Together with
 * striking an orc, this is how a tolerated Half-Orc or Blood-Sworn player gets on the wrong side of a hold.
 * There's no kin trust.
 */
final class OrcLaw extends GuardRules<OrcEntity> {
    OrcLaw() {
        super(FactionIds.BLOODTUSK_HORDE, OrcEntity.class, () -> ModEntityTypes.ORC_WARRIOR);
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (offence != SettlementGrudges.Offence.OPEN || !(blockEntity instanceof ChestBlockEntity)
                || !(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        RacialHomes.Visit visit = RacialHomes.visitOf(serverWorld, pos, RacialHomes.ORC_STRONGHOLD);
        return visit != null && visit.pieceAt(pos, OrcStrongholdStructures.LONGHOUSE) != null;
    }

    /** The Champion keeps out of it (he only fights the Trial). */
    @Override
    protected boolean canWitness(OrcEntity orc, PlayerEntity player) {
        return orc.isAlive() && !(orc instanceof OrcChampionEntity) && orc.canSee(player);
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        guard.getLookControl().lookAt(player);
        guard.playSound(SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, 1.0F, guard.getSoundPitch());
    }
}
