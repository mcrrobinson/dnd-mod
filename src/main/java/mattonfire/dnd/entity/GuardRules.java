package mattonfire.dnd.entity;

import java.util.List;
import java.util.function.Supplier;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * What most settlements' grudge rules share ({@link SettlementGrudges.Rules}): the faction's folk ({@code folk})
 * within the faction's witness range ({@link TierEffects#witnessRange}, 16 blocks at Neutral) who can see the
 * player witness it; the guards among them ({@link SettlementGuard}) turn on the player and the rest frown (a
 * villager's "no"). By default there's no kin trust: every offence is a grudge, and a warning is a frown.
 */
abstract class GuardRules<T extends MobEntity> implements SettlementGrudges.Rules {
    private final Identifier faction;
    private final Class<T> folk;
    /** A member of the faction, for the player's standing (read when needed: entity types register late). */
    private final Supplier<EntityType<?>> member;

    GuardRules(Identifier faction, Class<T> folk, Supplier<EntityType<?>> member) {
        this.faction = faction;
        this.folk = folk;
        this.member = member;
    }

    @Override
    public String id() {
        return this.faction.toString();
    }

    /** 16 blocks, adjusted for the player's standing with the faction. */
    @Override
    public double witnessRange(PlayerEntity player) {
        return TierEffects.witnessRange(TierEffects.tierWith(player, this.member.get()));
    }

    @Override
    public List<T> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(this.folk, new Box(pos).expand(this.witnessRange(player)),
                npc -> this.canWitness(npc, player));
    }

    /** Whether {@code npc}, one of the folk in range, sees {@code player}. */
    protected boolean canWitness(T npc, PlayerEntity player) {
        return npc.isAlive() && npc.canSee(player);
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        if (guard instanceof SettlementGuard settlementGuard) {
            settlementGuard.provoke(player);
        } else {
            frown(guard, player);
        }
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        frown(guard, player);
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return SettlementGrudges.Verdict.GRUDGE;
    }

    /** One of the folk who saw it, but doesn't fight: looks at the player and says no. */
    protected static void frown(MobEntity npc, PlayerEntity player) {
        npc.getLookControl().lookAt(player);
        npc.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, npc.getSoundPitch());
    }

    /**
     * Kin trust for {@code home}'s race: the first offence each in-game day is a warning ({@code warningKey} on the
     * action bar), the rest grudges; any other race always gets a grudge.
     */
    protected SettlementGrudges.Verdict kinWarning(ServerPlayerEntity player, RacialHomes.Home home, String warningKey) {
        if (!home.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
            return SettlementGrudges.Verdict.GRUDGE;
        }
        SettlementGrudges.Verdict verdict = SettlementGrudges.dailyWarning(player, this);
        if (verdict == SettlementGrudges.Verdict.WARNING) {
            player.sendMessage(Text.translatable(warningKey).formatted(Formatting.GOLD), true);
        }
        return verdict;
    }
}
