package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.UnderdarkBlocks;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.duergar.DuergarHolds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Grey Clans' law in a Duergar Hold (design 9, 8.2; {@link SettlementGrudges}). Every duergar within the
 * faction's witness range ({@code TierEffects.witnessRange}: 16 blocks at Neutral, 24 at Unfriendly and worse) who
 * can see you takes it as theft, the guards turn on you and the Grey Clans' standing drops by the faction's
 * {@code theft_witnessed} (-40), if inside a hold you:
 * <ul>
 * <li>open or break a chest or barrel in the Forge Hall, the mine or the Taskmaster's tower (the forge chest, the
 * ore chest, the strongbox), or</li>
 * <li>break Greysteel anywhere in the hold, or the forge's own things: its anvils, blast furnaces, smithing table,
 * grindstone, Duergar Braziers, Basalt Forge Bricks or iron bars.</li>
 * </ul>
 * The Trade Hall's and the barracks' barrels and chests aren't watched. Striking a duergar is the other grudge
 * ({@link DuergarEntity#callGuards}). No kin trust: no race gets a warning.
 */
final class DuergarLaw extends GuardRules<DuergarEntity> {
    static final Identifier FACTION = new Identifier("dndclasses", "duergar_clans");

    DuergarLaw() {
        super(FACTION, DuergarEntity.class, () -> DuergarEntities.DUERGAR_SOLDIER);
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (!(world instanceof ServerWorld serverWorld) || !Underdark.isUnderdark(serverWorld)) {
            return false;
        }
        boolean container = (offence == SettlementGrudges.Offence.OPEN || offence == SettlementGrudges.Offence.BREAK)
                && (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity);
        boolean breaks = offence == SettlementGrudges.Offence.BREAK;
        if (!container && !(breaks && (isGreysteel(state) || isForgework(state)))) {
            return false;
        }
        StructureStart start = serverWorld.getStructureAccessor().getStructureContaining(pos, DuergarHolds.KEY);
        if (start == null || !start.hasChildren()) {
            return false;
        }
        StructurePieceType room = roomAt(start, pos);
        if (container) {
            return room == DuergarHolds.FORGE || room == DuergarHolds.MINE || room == DuergarHolds.TOWER;
        }
        return isGreysteel(state) || room == DuergarHolds.FORGE;
    }

    /** The building (not the grounds or the wall) whose box holds {@code pos}, or null. */
    @Nullable
    static StructurePieceType roomAt(StructureStart start, BlockPos pos) {
        for (StructurePiece piece : start.getChildren()) {
            StructurePieceType type = piece.getType();
            if (type != DuergarHolds.GROUNDS && type != DuergarHolds.WALL && piece.getBoundingBox().contains(pos)) {
                return type;
            }
        }
        return null;
    }

    /** Greysteel Plate and its shapes. */
    static boolean isGreysteel(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlockFamilies.GREYSTEEL_PLATE.base() || block == ModBlockFamilies.GREYSTEEL_PLATE.stairs()
                || block == ModBlockFamilies.GREYSTEEL_PLATE.slab() || block == ModBlockFamilies.GREYSTEEL_PLATE.trapdoor();
    }

    /** The forge's things. */
    static boolean isForgework(BlockState state) {
        Block block = state.getBlock();
        return state.isIn(BlockTags.ANVIL) || state.isOf(Blocks.BLAST_FURNACE) || state.isOf(Blocks.SMITHING_TABLE)
                || state.isOf(Blocks.GRINDSTONE) || state.isOf(Blocks.IRON_BARS) || state.isOf(Blocks.LAVA_CAULDRON)
                || state.isOf(UnderdarkBlocks.DUERGAR_BRAZIER)
                || block == ModBlockFamilies.BASALT_FORGE_BRICKS.base() || block == ModBlockFamilies.BASALT_FORGE_BRICKS.stairs()
                || block == ModBlockFamilies.BASALT_FORGE_BRICKS.slab() || block == ModBlockFamilies.BASALT_FORGE_BRICKS.wall();
    }
}
