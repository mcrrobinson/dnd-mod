package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Phylactery Locket (Very Rare amulet, attunement by a Wizard, Necromancer or Warlock): damage that would kill you
 * leaves you on {@value #HEALTH_LEFT} HP with Resistance V for {@value #RESISTANCE_SECONDS} s and blinks you about
 * {@value #BLINK_DISTANCE} blocks away. Then it's spent until a long rest, or {@value #RECHARGE_MINUTES} minutes.
 * The death hook is in {@code BossItems} (Fabric's ALLOW_DEATH, ahead of the Downed rules, like a totem).
 */
public class PhylacteryLocketItem extends Item {
    public static final float HEALTH_LEFT = 2.0F;
    public static final int RESISTANCE_SECONDS = 3;
    public static final double BLINK_DISTANCE = 8.0;
    public static final int RECHARGE_MINUTES = 20;
    /** World time ({@code getTime}) when a spent locket recharges by itself. */
    private static final String READY_KEY = "locketReadyAt";

    public PhylacteryLocketItem(Settings settings) {
        super(settings);
    }

    public static boolean isCharged(ItemStack stack, World world) {
        NbtCompound nbt = MagicData.get(stack);
        return nbt == null || !nbt.contains(READY_KEY) || world.getTime() >= nbt.getLong(READY_KEY);
    }

    public static void recharge(ItemStack stack) {
        NbtCompound nbt = MagicData.get(stack);
        if (nbt != null)
            nbt.remove(READY_KEY);
    }

    /** The locket takes the killing blow: 2 HP, Resistance V, a blink away, and it's spent. */
    public static void save(ServerPlayerEntity player, ItemStack stack) {
        ServerWorld world = (ServerWorld) player.getWorld();
        MagicData.getOrCreate(stack).putLong(READY_KEY, world.getTime() + RECHARGE_MINUTES * 60L * 20L);
        player.setHealth(HEALTH_LEFT);
        player.extinguish();
        player.fallDistance = 0.0F;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RESISTANCE_SECONDS * 20, 4));
        world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 30, 0.4, 0.6,
                0.4, 0.05);
        Vec3d from = player.getPos();
        boolean blinked = blink(player);
        world.playSound(null, from.x, from.y, from.z, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.7F, 1.4F);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F,
                1.0F);
        player.sendMessage(Text.translatable("item.dndclasses.phylactery_locket.saved")
                .formatted(Formatting.DARK_AQUA), false);
        DnDClasses.LOGGER.info("[Magic] {}'s Phylactery Locket cheated death (blinked: {})", player.getEntityName(),
                blinked);
    }

    /** Like a chorus fruit, but about 8 blocks away: tries random spots on a ring round you. */
    private static boolean blink(ServerPlayerEntity player) {
        var random = player.getRandom();
        double x0 = player.getX(), y0 = player.getY(), z0 = player.getZ();
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = BLINK_DISTANCE + (random.nextDouble() - 0.5) * 2.0;
            double x = x0 + Math.cos(angle) * distance;
            double z = z0 + Math.sin(angle) * distance;
            double y = MathHelper.clamp(y0 + random.nextInt(9) - 4, player.getWorld().getBottomY(),
                    player.getWorld().getBottomY() + player.getWorld().getHeight() - 1);
            if (player.hasVehicle())
                player.stopRiding();
            if (player.teleport(x, y, z, true)) {
                player.fallDistance = 0.0F;
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        if (world != null) {
            tooltip.add((isCharged(stack, world)
                    ? Text.translatable("item.dndclasses.phylactery_locket.charged").formatted(Formatting.AQUA)
                    : Text.translatable("item.dndclasses.phylactery_locket.spent").formatted(Formatting.DARK_GRAY)));
        }
        tooltip.add(Text.translatable("item.dndclasses.phylactery_locket.tooltip").formatted(Formatting.GRAY));
    }
}
