package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.temple.DragonTempleStructures;
import mattonfire.dnd.world.gen.temple.SanctumPiece;
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
import org.jetbrains.annotations.Nullable;

/**
 * The Dragon Temples' grudge rules ({@link SettlementGrudges}). Every Dragonborn within 16 blocks who can see
 * you takes offence, and the Templars among them turn on you, if you:
 * <ul>
 * <li>break a block of one of the courtyard's dragon statues, or of a breath altar in the sanctum</li>
 * <li>open the reliquary (any chest in the sanctum)</li>
 * </ul>
 * There's no kin trust: Dragonborn are held to the same law.
 */
final class TempleLaw implements SettlementGrudges.Rules {
    TempleLaw() {
    }

    @Nullable
    private static RacialHomes.Visit temple(ServerWorld world, BlockPos pos) {
        RacialHomes.Visit visit = RacialHomes.homeAt(world, pos);
        return visit != null && visit.home() == RacialHomes.DRAGON_TEMPLE ? visit : null;
    }

    @Override
    public String id() {
        return "dndclasses:dragonsworn";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        return switch (offence) {
            case BREAK -> {
                RacialHomes.Visit visit = temple(serverWorld, pos);
                yield visit != null && (visit.pieceAt(pos, DragonTempleStructures.STATUE) != null
                        || SanctumPiece.altarOf(state) != null && visit.pieceAt(pos, DragonTempleStructures.SANCTUM) != null);
            }
            case OPEN -> {
                RacialHomes.Visit visit = blockEntity instanceof ChestBlockEntity ? temple(serverWorld, pos) : null;
                yield visit != null && visit.pieceAt(pos, DragonTempleStructures.SANCTUM) != null;
            }
            default -> false;
        };
    }

    /** 16 blocks, adjusted for the player's standing with the Dragonsworn. */
    @Override
    public double witnessRange(PlayerEntity player) {
        return TierEffects.witnessRange(TierEffects.tierWith(player, ModEntityTypes.DRAGONBORN_TEMPLAR));
    }

    @Override
    public List<DragonbornEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(DragonbornEntity.class, new Box(pos).expand(this.witnessRange(player)),
                npc -> npc.isAlive() && npc.canSee(player));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        if (guard instanceof DragonbornTemplarEntity templar) {
            templar.provoke(player);
        } else {
            guard.getLookControl().lookAt(player);
            guard.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, guard.getSoundPitch());
        }
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        guard.getLookControl().lookAt(player);
        guard.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, guard.getSoundPitch());
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        return SettlementGrudges.Verdict.GRUDGE;
    }
}
