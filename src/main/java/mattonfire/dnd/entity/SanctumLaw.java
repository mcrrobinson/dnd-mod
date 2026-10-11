package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.block.BlockState;
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
 * The Brimstone Sanctums' grudge rules ({@link SettlementGrudges}). Every Tiefling within 16 blocks who can see
 * you takes offence, and the Exiles among them turn on you, if you open or break a chest or barrel inside the
 * sanctum's walls. (Striking a Tiefling is the other grudge: {@link TieflingEntity#damage}.)
 *
 * <p>Kin trust: a Tiefling's first offence each in-game day only gets a warning.
 */
final class SanctumLaw implements SettlementGrudges.Rules {
    SanctumLaw() {
    }

    @Override
    public String id() {
        return "dndclasses:brimstone_covenant";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (!(world instanceof ServerWorld serverWorld)
                || offence != SettlementGrudges.Offence.OPEN && offence != SettlementGrudges.Offence.BREAK
                || !(blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity)) {
            return false;
        }
        RacialHomes.Visit visit = RacialHomes.homeAt(serverWorld, pos);
        return visit != null && visit.home() == RacialHomes.BRIMSTONE_SANCTUM;
    }

    /** 16 blocks, adjusted for the player's standing with the Brimstone Covenant. */
    @Override
    public double witnessRange(PlayerEntity player) {
        return TierEffects.witnessRange(TierEffects.tierWith(player, ModEntityTypes.TIEFLING_EXILE));
    }

    @Override
    public List<TieflingEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(TieflingEntity.class, new Box(pos).expand(this.witnessRange(player)),
                npc -> npc.isAlive() && npc.canSee(player));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        if (guard instanceof TieflingExileEntity exile) {
            exile.provoke(player);
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
        if (!RacialHomes.BRIMSTONE_SANCTUM.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
            return SettlementGrudges.Verdict.GRUDGE;
        }
        SettlementGrudges.Verdict verdict = SettlementGrudges.dailyWarning(player, this);
        if (verdict == SettlementGrudges.Verdict.WARNING) {
            player.sendMessage(Text.translatable("message.dndclasses.home.sanctum_warning").formatted(Formatting.GOLD), true);
        }
        return verdict;
    }
}
