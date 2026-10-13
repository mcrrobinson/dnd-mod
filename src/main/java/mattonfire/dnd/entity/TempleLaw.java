package mattonfire.dnd.entity;

import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.temple.DragonTempleStructures;
import mattonfire.dnd.world.gen.temple.SanctumPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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
final class TempleLaw extends GuardRules<DragonbornEntity> {
    TempleLaw() {
        super(FactionIds.DRAGONSWORN, DragonbornEntity.class, () -> ModEntityTypes.DRAGONBORN_TEMPLAR);
    }

    @Nullable
    private static RacialHomes.Visit temple(ServerWorld world, BlockPos pos) {
        return RacialHomes.visitOf(world, pos, RacialHomes.DRAGON_TEMPLE);
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
}
