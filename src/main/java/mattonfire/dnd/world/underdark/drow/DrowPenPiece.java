package mattonfire.dnd.world.underdark.drow;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.classes.Blocks.underdark.ChainCageBlock;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.world.underdark.UnderdarkPiece;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A Drow Outpost's captive pen, facing the shrine (local +b): three cells side by side, each 2 wide and 3 deep under a
 * Drowstone roof, iron bars between them and across the front, and in each front a gate two Chain Cages high, locked
 * ({@link ChainCageBlock}: a Rogue picks it, anyone can break it).
 * <ul>
 * <li>The first cell holds a captive and the second, half the time, another: a hobbit, a mountain dwarf or a wood
 * elf, kneeling ({@code Captive:1b}). Opening the gate frees it: +{@value mattonfire.dnd.world.underdark.Captives#REPUTATION}
 * with its folk, and it walks off.</li>
 * <li>The third cell keeps a beast from {@code #dndclasses:underdark/pen_beasts} (a spider unless a data pack or a
 * later ticket adds more). Opening that gate lets it out.</li>
 * </ul>
 */
public class DrowPenPiece extends UnderdarkPiece {
    /** The front wall is this far out from the centre (local +b). */
    public static final int DEPTH = 2;
    /** The cells' left columns (local a); each is 2 wide. */
    static final int[] CELLS = {-4, -1, 2};
    /** Captives a pen may hold. */
    private static final List<EntityType<? extends MobEntity>> CAPTIVES = List.of(ModEntityTypes.HOBBIT,
            ModEntityTypes.MOUNTAIN_DWARF, ModEntityTypes.WOOD_ELF);

    public DrowPenPiece(BlockPos center, Direction front, long seed) {
        super(DrowOutposts.PEN, center, front, seed, -6, -1, -DEPTH - 1, 5, 4, DEPTH + 1);
    }

    public DrowPenPiece(NbtCompound nbt) {
        super(DrowOutposts.PEN, nbt);
    }

    /** Cell {@code i}'s gate (its lower cage), in the world. */
    public BlockPos gate(int i) {
        return this.at(CELLS[i] + 1, 0, DEPTH);
    }

    /** Where you stand in front of cell {@code i}'s gate, in the world. */
    public BlockPos outside(int i) {
        return this.at(CELLS[i] + 1, 0, DEPTH + 2);
    }

    @Override
    protected void build(Builder b) {
        BlockState bricks = ModBlockFamilies.DROWSTONE_BRICKS.base().getDefaultState();
        BlockState bars = Blocks.IRON_BARS.getDefaultState();
        BlockState gate = UnderdarkBlocks.CHAIN_CAGE.getDefaultState().with(ChainCageBlock.FACING, Direction.SOUTH)
                .with(ChainCageBlock.LOCKED, true);
        for (int a = -5; a <= 4; a++) {
            for (int c = -DEPTH; c <= DEPTH; c++) {
                for (int y = -1; y <= 3; y++) {
                    BlockState state;
                    if (y == -1 || y == 3) {
                        state = bricks;
                    } else if (a == -5 || a == 4 || c == -DEPTH) {
                        state = bricks;
                    } else if (c == DEPTH) {
                        state = isGate(a) ? gate : bars;
                    } else if (a == -2 || a == 1) {
                        state = bars;
                    } else {
                        state = Blocks.AIR.getDefaultState();
                    }
                    b.set(a, y, c, state);
                }
            }
        }
        // Chains hanging in the back corners, and lanterns on the roof.
        for (int left : CELLS) {
            b.set(left, 2, -1, Blocks.CHAIN);
        }
        b.lantern(-5, 4, DEPTH, UnderdarkBlocks.FAERIE_LANTERN);
        b.lantern(4, 4, DEPTH, UnderdarkBlocks.FAERIE_LANTERN);
        // The captives, and the beast.
        EntityType<? extends MobEntity> first = CAPTIVES.get(b.random.nextInt(CAPTIVES.size()));
        EntityType<? extends MobEntity> second = CAPTIVES.get(b.random.nextInt(CAPTIVES.size()));
        boolean two = b.random.nextBoolean();
        b.captive(CELLS[0] + 1, 0, 0, first, Direction.SOUTH);
        if (two) {
            b.captive(CELLS[1] + 1, 0, 0, second, Direction.SOUTH);
        }
        EntityType<?> beast = beast(b);
        if (beast != null) {
            this.beast(b, beast);
        }
    }

    private static boolean isGate(int a) {
        for (int left : CELLS) {
            if (a == left + 1) {
                return true;
            }
        }
        return false;
    }

    /** A beast from the pen-beasts tag (drawn whether or not this chunk builds the cell). */
    private static EntityType<?> beast(Builder b) {
        List<EntityType<?>> beasts = new ArrayList<>();
        for (RegistryEntry<EntityType<?>> entry : Registries.ENTITY_TYPE.iterateEntries(DrowOutposts.PEN_BEASTS)) {
            beasts.add(entry.value());
        }
        int roll = b.random.nextInt(Math.max(1, beasts.size()));
        return beasts.isEmpty() ? null : beasts.get(roll);
    }

    @SuppressWarnings("unchecked")
    private void beast(Builder b, EntityType<?> type) {
        try {
            b.resident(CELLS[2], 0, 0, (EntityType<MobEntity>) type, 1);
        } catch (ClassCastException e) {
            // Not a mob: nothing to keep in the pen.
        }
    }
}
