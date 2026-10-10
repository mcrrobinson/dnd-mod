package mattonfire.dnd.classes.Blocks;

import java.util.Locale;
import mattonfire.dnd.dungeon.PuzzleRooms;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * A rune pillar's face in a dungeon puzzle room ({@code dndclasses:rune_block}). Using it turns the
 * carved glyph on to the next one ({@link #GLYPH} 0-5: sun, moon, eye, flame, crown, skull) and tells the
 * room's ward ({@link PuzzleRooms}). Sneak-using it with an empty hand studies the murals instead (an
 * Investigation or Arcana check for the defaced glyph). Unbreakable.
 */
public class RuneBlock extends HorizontalFacingBlock {
    public static final IntProperty GLYPH = IntProperty.of("glyph", 0, 5);

    /** The six glyphs, in the order a rune turns through them. */
    public enum Glyph {
        SUN(Blocks.ORANGE_TERRACOTTA, new String[]{".#.", "###", ".#."}),
        MOON(Blocks.LIGHT_BLUE_TERRACOTTA, new String[]{"##.", "#..", "##."}),
        EYE(Blocks.LIME_TERRACOTTA, new String[]{".#.", "#.#", ".#."}),
        FLAME(Blocks.RED_TERRACOTTA, new String[]{".#.", "##.", "###"}),
        CROWN(Blocks.YELLOW_TERRACOTTA, new String[]{"#.#", "###", "###"}),
        SKULL(Blocks.WHITE_TERRACOTTA, new String[]{"###", "#.#", ".#."});

        private final Block paint;
        /** The mural pattern, top row first: '#' painted, '.' background. */
        private final String[] pattern;

        Glyph(Block paint, String[] pattern) {
            this.paint = paint;
            this.pattern = pattern;
        }

        public Block paint() {
            return this.paint;
        }

        public boolean painted(int row, int col) {
            return this.pattern[row].charAt(col) == '#';
        }

        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public Text text() {
            return Text.translatable("puzzle.dndclasses.glyph." + this.id());
        }

        public static Glyph of(int index) {
            Glyph[] all = values();
            return all[Math.floorMod(index, all.length)];
        }
    }

    public RuneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(GLYPH, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, GLYPH);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    public static BlockState state(Block block, Direction facing, int glyph) {
        return block.getDefaultState().with(FACING, facing).with(GLYPH, Math.floorMod(glyph, 6));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND || player.isSpectator()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld server) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }
        if (player.isSneaking() && player.getMainHandStack().isEmpty()) {
            PuzzleRooms.study(server, pos, serverPlayer);
            return ActionResult.CONSUME;
        }
        if (PuzzleRooms.isLocked(server, pos)) {
            player.sendMessage(Text.translatable("puzzle.dndclasses.locked").formatted(Formatting.GOLD), true);
            return ActionResult.CONSUME;
        }
        int next = (state.get(GLYPH) + 1) % 6;
        world.setBlockState(pos, state.with(GLYPH, next), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.6F, 1.4F);
        player.sendMessage(Glyph.of(next).text().copy().formatted(Formatting.AQUA), true);
        PuzzleRooms.onRuneTurned(server, pos, serverPlayer);
        return ActionResult.CONSUME;
    }
}
