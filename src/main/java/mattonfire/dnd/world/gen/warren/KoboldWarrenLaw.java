package mattonfire.dnd.world.gen.warren;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.SettlementGrudges;
import mattonfire.dnd.entity.kobold.KoboldEntity;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import net.minecraft.block.BlockState;
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
 * The warren law (design 10, 4.3; a {@link SettlementGrudges.Rules}):
 * <ul>
 * <li>Opening or smashing the chief's hoard chest where a kobold within {@value #WITNESS_RANGE} blocks can see you:
 * every kobold of the warren (within {@value #WARREN_RANGE} blocks) comes for you, whatever your standing, for the
 * fight and 30 s after ({@link KoboldEntity#provoke}). The Kobold Tribes' {@code theft_witnessed} (-15).</li>
 * <li>Breaking a Kobold Egg Clutch: {@value #EGG_PENALTY} with the Kobold Tribes whether a kobold sees it or not ("the
 * warren will know"), and if one does, the warren comes for you as above.</li>
 * </ul>
 */
public final class KoboldWarrenLaw implements SettlementGrudges.Rules {
    public static final double WITNESS_RANGE = 24.0D;
    /** Every kobold this close to the witness turns on the thief. */
    public static final double WARREN_RANGE = 48.0D;
    public static final int EGG_PENALTY = -30;

    KoboldWarrenLaw() {
    }

    @Override
    public String id() {
        return FactionIds.KOBOLDS.toString();
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        return (offence == SettlementGrudges.Offence.OPEN || offence == SettlementGrudges.Offence.BREAK)
                && blockEntity instanceof ChestBlockEntity && world instanceof ServerWorld server
                && KoboldWarren.pieceAt(server, pos, WarrenChiefPiece.class) != null;
    }

    @Override
    public double witnessRange(PlayerEntity player) {
        return WITNESS_RANGE;
    }

    @Override
    public List<KoboldEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(KoboldEntity.class, new Box(pos).expand(WITNESS_RANGE),
                kobold -> kobold.isAlive() && kobold.canSee(player));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        for (KoboldEntity kobold : player.world.getEntitiesByClass(KoboldEntity.class,
                guard.getBoundingBox().expand(WARREN_RANGE), KoboldEntity::isAlive)) {
            kobold.provoke(player);
            kobold.setTarget(player);
        }
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        this.provoke(guard, player);
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return SettlementGrudges.Verdict.GRUDGE;
    }

    /** {@code player} broke the egg clutch at {@code pos}. */
    public static void eggClutchBroken(ServerPlayerEntity player, BlockPos pos) {
        if (player.isCreative() || player.isSpectator() || mattonfire.dnd.dm.DungeonMaster.isDm(player)) {
            return;
        }
        Faction kobolds = Factions.get(FactionIds.KOBOLDS);
        if (kobolds != null) {
            Reputation.add(player, kobolds, EGG_PENALTY, Reputation.Source.THEFT);
        }
        KoboldWarrenLaw law = (KoboldWarrenLaw) KoboldWarrens.LAW;
        List<KoboldEntity> witnesses = law.witnesses(player, pos);
        DnDClasses.LOGGER.info("[Grudges] {} broke a kobold egg clutch at {}: {} Kobold Tribes, {} witness(es)",
                player.getEntityName(), pos.toShortString(), EGG_PENALTY, witnesses.size());
        if (!witnesses.isEmpty()) {
            KoboldEntity first = witnesses.get(0);
            first.playSound(SoundEvents.ENTITY_FOX_SCREECH, 1.5F, 1.4F);
            player.sendMessage(Text.translatable("message.dndclasses.kobold_warren.eggs").formatted(Formatting.RED), true);
            law.provoke(first, player);
        }
    }
}
