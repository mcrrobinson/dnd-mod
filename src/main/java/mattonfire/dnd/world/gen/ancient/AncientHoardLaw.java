package mattonfire.dnd.world.gen.ancient;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.SettlementGrudges;
import mattonfire.dnd.entity.dragon.AncientDragonEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The hoard law (design 10, 6.3), a {@link SettlementGrudges} rule: open or break a chest in an Ancient's hoard hall
 * while its dragon lives and you wake the wyrm. The dragon (wherever it is within {@value #RANGE} blocks) takes the
 * thief as its target and its Frightful Presence goes off at once ({@link AncientDragonEntity#wakeForHoard}); everyone
 * within {@value #NARRATION_RANGE} blocks reads "Somewhere far above, something vast opens one eye." There's no
 * kin trust and no warning.
 */
final class AncientHoardLaw implements SettlementGrudges.Rules {
    static final double RANGE = 128.0D;
    static final double NARRATION_RANGE = 96.0D;

    @Override
    public String id() {
        return DnDClasses.MOD_ID + ":ancient_hoard";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        return (offence == SettlementGrudges.Offence.OPEN || offence == SettlementGrudges.Offence.BREAK)
                && blockEntity instanceof ChestBlockEntity && world instanceof ServerWorld serverWorld
                && hallAt(serverWorld, pos) != null;
    }

    /** The hoard hall whose dome holds {@code pos}, or null. */
    @Nullable
    static HoardHallPiece hallAt(ServerWorld world, BlockPos pos) {
        StructureStart start = world.getStructureAccessor().getStructureContaining(pos, AncientLairStructures.TAG);
        if (start == null || !start.hasChildren()) {
            return null;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof HoardHallPiece hall && hall.inHall(pos)) {
                return hall;
            }
        }
        return null;
    }

    @Override
    public double witnessRange(PlayerEntity player) {
        return RANGE;
    }

    /** The lair's own dragon, wherever it is: it doesn't need to see the thief, it knows its hoard. */
    @Override
    public List<? extends MobEntity> witnesses(PlayerEntity player, BlockPos pos) {
        if (!(player.world instanceof ServerWorld world)) {
            return List.of();
        }
        HoardHallPiece hall = hallAt(world, pos);
        if (hall == null) {
            return List.of();
        }
        BlockPos lair = hall.layout().lairCentre();
        return world.getEntitiesByClass(AncientDragonEntity.class, new Box(pos).expand(RANGE),
                dragon -> dragon.isAlive() && (lair.equals(dragon.getLair()) || dragon.getLair() == null));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        AncientDragonEntity dragon = (AncientDragonEntity) guard;
        boolean asleep = dragon.getTarget() == null;
        dragon.wakeForHoard(player);
        if (asleep && player.world instanceof ServerWorld world) {
            Text line = Text.translatable("message.dndclasses.ancient_lair.wake").formatted(Formatting.DARK_RED,
                    Formatting.ITALIC);
            for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= NARRATION_RANGE * NARRATION_RANGE)) {
                other.sendMessage(line, false);
            }
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
}
