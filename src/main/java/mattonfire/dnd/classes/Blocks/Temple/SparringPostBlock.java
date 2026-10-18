package mattonfire.dnd.classes.Blocks.Temple;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Sparring Post: a straw-and-rope wrapped post with a padded front, for the Dragon Temple's training yard.
 * Hitting it doesn't break it: the action bar shows the melee damage the swing would have dealt (attack damage
 * attribute, the weapon's damage enchantments, the attack cooldown and a jump crit, as vanilla works it out
 * against an unarmoured target). Sneak to break it. Hits count at most every {@link #MIN_HIT_TICKS} ticks, as
 * holding the button repeats the attack.
 */
public class SparringPostBlock extends HorizontalFacingBlock {
    public static final int MIN_HIT_TICKS = 5;
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 16, 13);
    /** Each player's last counted hit (world time), for the swing's charge. */
    private static final Map<UUID, Long> LAST_HIT = new java.util.HashMap<>();

    public SparringPostBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /** Hooks left-clicks on sparring posts (both sides: the client mustn't start breaking it either). */
    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.isSpectator() || player.isSneaking()
                    || !(world.getBlockState(pos).getBlock() instanceof SparringPostBlock)) {
                return ActionResult.PASS;
            }
            if (world instanceof ServerWorld serverWorld) {
                hit(player, serverWorld, pos);
            }
            return ActionResult.SUCCESS;
        });
    }

    private static void hit(PlayerEntity player, ServerWorld world, BlockPos pos) {
        long now = world.getTime();
        Long last = LAST_HIT.get(player.getUuid());
        if (last != null && now - last >= 0 && now - last < MIN_HIT_TICKS) {
            return;
        }
        LAST_HIT.put(player.getUuid(), now);
        // The charge since the last counted hit, as PlayerEntity.attack scales it.
        float charge = last == null || now - last < 0 ? 1.0F
                : MathHelper.clamp(((now - last) + 0.5F) / player.getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
        float base = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float enchant = EnchantmentHelper.getAttackDamage(player.getMainHandStack(), EntityGroup.DEFAULT);
        float damage = base * (0.2F + charge * charge * 0.8F) + enchant * charge;
        boolean crit = charge > 0.9F && player.fallDistance > 0.0F && !player.isOnGround() && !player.isClimbing()
                && !player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle() && !player.isSprinting();
        if (crit) {
            damage *= 1.5F;
        }
        String amount = String.format(Locale.ROOT, "%.1f", damage);
        int percent = Math.round(charge * 100);
        player.sendMessage(Text.translatable(crit ? "message.dndclasses.sparring_post.crit"
                : "message.dndclasses.sparring_post.hit", amount, percent).formatted(Formatting.GOLD), true);
        world.playSound(null, pos, crit ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT
                : charge > 0.9F ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, pos, SoundEvents.BLOCK_WOOL_HIT, SoundCategory.BLOCKS, 1.0F, 0.8F);
        DnDClasses.LOGGER.info("[SparringPost] {} hit for {} ({}% charge{})", player.getEntityName(), amount, percent,
                crit ? ", crit" : "");
    }
}
