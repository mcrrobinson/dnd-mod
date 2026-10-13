package mattonfire.dnd.entity;

import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Brimstone Sanctums' grudge rules ({@link SettlementGrudges}). Every Tiefling within 16 blocks who can see
 * you takes offence, and the Exiles among them turn on you, if you open or break a chest or barrel inside the
 * sanctum's walls. (Striking a Tiefling is the other grudge: {@link TieflingEntity#damage}.)
 *
 * <p>Kin trust: a Tiefling's first offence each in-game day only gets a warning.
 */
final class SanctumLaw extends GuardRules<TieflingEntity> {
    SanctumLaw() {
        super(FactionIds.BRIMSTONE_COVENANT, TieflingEntity.class, () -> ModEntityTypes.TIEFLING_EXILE);
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (!(world instanceof ServerWorld serverWorld)
                || offence != SettlementGrudges.Offence.OPEN && offence != SettlementGrudges.Offence.BREAK
                || !(blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity)) {
            return false;
        }
        return RacialHomes.visitOf(serverWorld, pos, RacialHomes.BRIMSTONE_SANCTUM) != null;
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return this.kinWarning(player, RacialHomes.BRIMSTONE_SANCTUM, "message.dndclasses.home.sanctum_warning");
    }
}
