package mattonfire.dnd.classes.Blocks.orc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.entity.OrcEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * War Drum: a big hide-headed drum of the Bloodtusk Horde. Hitting it (left or right click) plays a deep beat;
 * unless the drum beat within the last {@link #COOLDOWN} ticks, the beat also rallies orcs and Half-Orc players
 * within {@link #RANGE} blocks with Strength I for {@link #DURATION} ticks. The Fighting Pit's drum starts the
 * Trial of Strength on a right click instead ({@link mattonfire.dnd.entity.OrcTrial}).
 */
public class WarDrumBlock extends Block {
    public static final double RANGE = 16.0D;
    public static final int DURATION = 20 * 10;
    public static final int COOLDOWN = 20 * 30;

    /**
     * Rally hook: called with the drummer and each Half-Orc player the beat rallied (not on a beat during the
     * cooldown). Racial-home bonuses or quests can listen here.
     */
    public static BiConsumer<PlayerEntity, ServerPlayerEntity> onRally = (drummer, rallied) -> {
    };

    /** Game time each drum (by dimension and position) last rallied. */
    private static final Map<String, Long> LAST_RALLY = new HashMap<>();

    public WarDrumBlock(Settings settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            beat((ServerWorld) world, pos, player);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClient) {
            beat((ServerWorld) world, pos, player);
        }
    }

    /**
     * Beats the drum at {@code pos}: the sound always, the rally unless it's cooling down. Returns how many
     * creatures it rallied (0 during the cooldown).
     */
    public static int beat(ServerWorld world, BlockPos pos, PlayerEntity drummer) {
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), SoundCategory.BLOCKS, 3.0F, 0.5F);
        world.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 1, 0.0D,
                0.0D, 0.0D, 0.0D);
        String key = world.getRegistryKey().getValue() + "@" + pos.asLong();
        long now = world.getTime();
        Long last = LAST_RALLY.get(key);
        if (last != null && now - last < COOLDOWN) {
            DnDClasses.LOGGER.info("[WarDrum] beat at {} (cooling down, {} ticks left)", pos.toShortString(),
                    COOLDOWN - (now - last));
            return 0;
        }
        LAST_RALLY.put(key, now);
        int rallied = 0;
        Box area = new Box(pos).expand(RANGE);
        for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e.isAlive() && e.squaredDistanceTo(pos.toCenterPos()) <= RANGE * RANGE)) {
            boolean halfOrc = living instanceof ServerPlayerEntity player && RaceLifecycle.activeRaceOf(player) == DndRace.HALFORC;
            if (!(living instanceof OrcEntity) && !halfOrc) {
                continue;
            }
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, DURATION, 0));
            rallied++;
            if (halfOrc) {
                ServerPlayerEntity player = (ServerPlayerEntity) living;
                player.sendMessage(Text.translatable("message.dndclasses.war_drum.rally").formatted(Formatting.DARK_RED), true);
                onRally.accept(drummer, player);
            }
        }
        DnDClasses.LOGGER.info("[WarDrum] beat at {} rallied {}", pos.toShortString(), rallied);
        return rallied;
    }
}
