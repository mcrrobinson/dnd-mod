package mattonfire.dnd.world.gen.ancient;

import java.util.HashMap;
import java.util.Map;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.SettlementGrudges;
import mattonfire.dnd.entity.dragon.AncientDragonEntity;
import mattonfire.dnd.entity.dragon.AncientDragonEvents;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

/**
 * The Ancient lairs' slain state (design 10, 6.5): lair centre to the world time its dragon died. Written when an
 * Ancient with a lair dies ({@link AncientDragonEvents#SLAIN}); a slain lair never raises its dragon again
 * ({@link mattonfire.dnd.entity.boss.StructureBosses#placeAncient}). Also hooks the dragon's tremors (dust from the
 * hoard hall's ceiling) and the hoard law ({@link AncientHoardLaw}).
 */
public class AncientLairs extends PersistentState {
    private static final String ID = DnDClasses.MOD_ID + "_ancient_lairs";

    /** Lair centre (BlockPos.asLong) -> world time its dragon was slain. */
    private final Map<Long, Long> slainAt = new HashMap<>();

    public static AncientLairs get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(AncientLairs::fromNbt, AncientLairs::new, ID);
    }

    static void register() {
        SettlementGrudges.add(new AncientHoardLaw());
        AncientDragonEvents.SLAIN.register((dragon, world, lair) -> {
            if (lair != null) {
                get(world).markSlain(lair, world.getTime());
                DnDClasses.LOGGER.info("[Lair] {} is slain: its lair at {} is empty for good",
                        dragon.getName().getString(), lair.toShortString());
            }
        });
        AncientDragonEvents.TREMOR.register(AncientLairs::tremor);
    }

    public boolean isSlain(BlockPos lair) {
        return this.slainAt.containsKey(lair.asLong());
    }

    /** When the lair's dragon died (world time), or -1. */
    public long slainAt(BlockPos lair) {
        return this.slainAt.getOrDefault(lair.asLong(), -1L);
    }

    public void markSlain(BlockPos lair, long time) {
        this.slainAt.putIfAbsent(lair.asLong(), time);
        this.markDirty();
    }

    /** Forgets that a lair's dragon was slain (tests). */
    public void clear(BlockPos lair) {
        if (this.slainAt.remove(lair.asLong()) != null) {
            this.markDirty();
        }
    }

    /** The hoard hall of the Ancient lair centred on {@code lair}, or null. */
    @Nullable
    public static HoardHallPiece hall(ServerWorld world, BlockPos lair) {
        StructureStart start = world.getStructureAccessor().getStructureContaining(lair, AncientLairStructures.TAG);
        if (start != null && start.hasChildren()) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof HoardHallPiece hall && hall.layout().lairCentre().equals(lair)) {
                    return hall;
                }
            }
        }
        return null;
    }

    /** The tremor shakes dust down from the hoard hall's dome onto anyone inside. Cosmetic. */
    private static void tremor(AncientDragonEntity dragon, ServerWorld world) {
        BlockPos lair = dragon.getLair();
        HoardHallPiece hall = lair == null ? null : hall(world, lair);
        if (hall == null) {
            return;
        }
        BlockPos c = hall.layout().hallCenter();
        boolean heard = false;
        for (ServerPlayerEntity player : world.getPlayers(p -> hall.inHall(p.getBlockPos()))) {
            heard = true;
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GRAVEL_BREAK,
                    SoundCategory.BLOCKS, 1.5F, 0.5F);
        }
        if (!heard) {
            return;
        }
        Random random = world.random;
        BlockStateParticleEffect dust = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST,
                Blocks.BLACKSTONE.getDefaultState());
        for (int i = 0; i < 80; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double r = Math.sqrt(random.nextDouble()) * (AncientLairLayout.HALL_RADIUS - 1);
            double t = r / AncientLairLayout.HALL_RADIUS;
            double y = c.getY() + AncientLairLayout.HALL_HEIGHT * Math.sqrt(1.0D - t * t) - 0.5D;
            world.spawnParticles(dust, c.getX() + 0.5D + Math.cos(angle) * r, y, c.getZ() + 0.5D + Math.sin(angle) * r,
                    1, 0.1D, 0.0D, 0.1D, 0.0D);
        }
        DnDClasses.LOGGER.info("[Lair] the tremor shakes dust from the hoard hall at {}", c.toShortString());
    }

    private static AncientLairs fromNbt(NbtCompound nbt) {
        AncientLairs state = new AncientLairs();
        NbtList list = nbt.getList("Lairs", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            state.slainAt.put(entry.getLong("Pos"), entry.getLong("SlainAt"));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        this.slainAt.forEach((pos, time) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("Pos", pos);
            entry.putLong("SlainAt", time);
            list.add(entry);
        });
        nbt.put("Lairs", list);
        return nbt;
    }
}
