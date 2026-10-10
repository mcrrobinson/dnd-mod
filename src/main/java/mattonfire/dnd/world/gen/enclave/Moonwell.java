package mattonfire.dnd.world.gen.enclave;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * The Moonwell's gift. Moonwater is a potion of Regeneration II (20 s) and Night Vision (2 minutes). An
 * Elf who fills an empty glass bottle at an enclave's Moonwell by night draws it for free, once a night
 * (remembered as {@code DndMoonwaterNight} in persistent data); anyone else gets plain water there and has
 * to buy Moonwater from the Speaker.
 */
public final class Moonwell {
    public static final int REGENERATION_TICKS = 20 * 20;
    public static final int NIGHT_VISION_TICKS = 20 * 120;
    /** Night runs from dusk (13000) to dawn (23000) of each in-game day. */
    public static final int DUSK = 13000;
    public static final int DAWN = 23000;
    private static final String NIGHT_KEY = "DndMoonwaterNight";
    private static final int COLOR = 0xC6DDFF;

    private Moonwell() {
    }

    /** A bottle of Moonwater. */
    public static ItemStack moonwater() {
        ItemStack stack = new ItemStack(Items.POTION);
        PotionUtil.setCustomPotionEffects(stack, List.of(
                new StatusEffectInstance(StatusEffects.REGENERATION, REGENERATION_TICKS, 1),
                new StatusEffectInstance(StatusEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0)));
        stack.getOrCreateNbt().putInt(PotionUtil.CUSTOM_POTION_COLOR_KEY, COLOR);
        stack.setCustomName(Text.translatable("item.dndclasses.moonwater").styled(style -> style.withItalic(false)));
        return stack;
    }

    public static boolean isNight(ServerWorld world) {
        long time = world.getTimeOfDay() % 24000L;
        return time >= DUSK && time < DAWN;
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient || !stack.isOf(Items.GLASS_BOTTLE) || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }
            HitResult hit = player.raycast(5.0D, 0.0F, true);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
                return TypedActionResult.pass(stack);
            }
            BlockPos pos = blockHit.getBlockPos();
            if (!world.getFluidState(pos).isIn(FluidTags.WATER) || !atMoonwell(serverPlayer.getWorld(), pos)) {
                return TypedActionResult.pass(stack);
            }
            return draw(serverPlayer, stack, pos);
        });
    }

    private static boolean atMoonwell(ServerWorld world, BlockPos pos) {
        RacialHomes.Visit visit = RacialHomes.homeAt(world, pos);
        return visit != null && visit.home() == RacialHomes.ELVEN_ENCLAVE
                && visit.pieceAt(pos, ElvenEnclaveStructures.MOONWELL) != null;
    }

    private static TypedActionResult<ItemStack> draw(ServerPlayerEntity player, ItemStack stack, BlockPos pos) {
        ServerWorld world = player.getWorld();
        if (!isNight(world)) {
            hint(player, "message.dndclasses.moonwell.day");
            return TypedActionResult.pass(stack);
        }
        if (!RacialHomes.ELVEN_ENCLAVE.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
            hint(player, "message.dndclasses.moonwell.not_kin");
            return TypedActionResult.pass(stack);
        }
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        long night = world.getTimeOfDay() / 24000L;
        if (data.contains(NIGHT_KEY) && data.getLong(NIGHT_KEY) == night) {
            hint(player, "message.dndclasses.moonwell.drawn");
            return TypedActionResult.pass(stack);
        }
        data.putLong(NIGHT_KEY, night);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        ItemStack moonwater = moonwater();
        if (!player.giveItemStack(moonwater)) {
            player.dropItem(moonwater, false);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0F, 1.2F);
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 12, 0.4D, 0.3D, 0.4D, 0.02D);
        player.sendMessage(Text.translatable("message.dndclasses.moonwell.drawn_free").formatted(Formatting.AQUA), true);
        DnDClasses.LOGGER.info("[Moonwell] {} drew Moonwater at {} (night {})", player.getEntityName(), pos.toShortString(), night);
        return TypedActionResult.success(stack);
    }

    private static void hint(PlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.GRAY), true);
    }
}
