package mattonfire.dnd.magic.items;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Wand of Magic Missiles (Uncommon wand): right-click fires 3 darts at the nearest hostile within
 * {@value #RANGE} blocks and {@value #CONE_DEGREES} degrees of where you look, {@value #DART_DAMAGE} magic
 * damage each (magic ignores armor). {@value #MAX_CHARGES} charges; it regains 1d6+1 at dawn and on a long
 * rest. Spending the last charge rolls a d20: on a 1 the wand crumbles to dust.
 */
public class WandOfMagicMissilesItem extends Item {
    public static final int MAX_CHARGES = 7;
    public static final int DARTS = 3;
    public static final float DART_DAMAGE = 2.0F;
    public static final double RANGE = 24.0;
    /** Half-angle of the aiming cone. */
    public static final double CONE_DEGREES = 30.0;
    private static final String DAWN_KEY = "dawnDay";

    public WandOfMagicMissilesItem(Settings settings) {
        super(settings);
    }

    public static int charges(ItemStack stack) {
        return MathHelper.clamp(MagicData.charges(stack, MAX_CHARGES), 0, MAX_CHARGES);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(stack, true);
        if (!MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        int charges = charges(stack);
        if (charges <= 0) {
            player.sendMessage(Text.translatable("item.dndclasses.wand_of_magic_missiles.empty")
                    .formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        LivingEntity target = findTarget(server);
        if (target == null) {
            player.sendMessage(Text.translatable("item.dndclasses.wand_of_magic_missiles.no_target")
                    .formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        fire((ServerWorld) world, server, target);
        player.getItemCooldownManager().set(this, 10);
        if (!player.getAbilities().creativeMode) {
            charges--;
            MagicData.setCharges(stack, charges);
        }
        DnDClasses.LOGGER.info("[Magic] {} fired the Wand of Magic Missiles at {}, {} charges left",
                player.getEntityName(), target.getEntityName(), charges);
        if (charges == 0) {
            int roll = D20.d20(player);
            if (roll == 1) {
                player.sendMessage(Text.translatable("item.dndclasses.wand_of_magic_missiles.crumbles")
                        .formatted(Formatting.DARK_RED), false);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS,
                        1.0F, 0.8F);
                ((ServerWorld) world).spawnParticles(ParticleTypes.ASH, player.getX(), player.getBodyY(0.6),
                        player.getZ(), 30, 0.3, 0.3, 0.3, 0.02);
                DnDClasses.LOGGER.info("[Magic] {}'s Wand of Magic Missiles crumbled (rolled 1)",
                        player.getEntityName());
                stack.decrement(1);
                return TypedActionResult.success(stack, false);
            }
            player.sendMessage(Text.translatable("item.dndclasses.wand_of_magic_missiles.spent", roll)
                    .formatted(Formatting.GRAY), true);
        }
        return TypedActionResult.success(stack, false);
    }

    /** The nearest hostile in the aiming cone that the player can see. */
    private static @Nullable LivingEntity findTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        double cos = Math.cos(Math.toRadians(CONE_DEGREES));
        List<LivingEntity> candidates = player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(RANGE), e -> e != player && e.isAlive() && !e.isSpectator()
                        && isHostile(e, player));
        return candidates.stream().filter(e -> {
            Vec3d to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            return dist <= RANGE && dist > 0.01 && to.multiply(1.0 / dist).dotProduct(look) >= cos
                    && player.canSee(e);
        }).min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player))).orElse(null);
    }

    private static boolean isHostile(LivingEntity e, PlayerEntity player) {
        if (e instanceof Monster)
            return true;
        return e instanceof net.minecraft.entity.mob.MobEntity mob && mob.getTarget() == player;
    }

    /** Three glowing darts that curve onto the target, then 2 magic damage each (dealt as one hit). */
    private static void fire(ServerWorld world, ServerPlayerEntity player, LivingEntity target) {
        Vec3d from = player.getEyePos().add(player.getRotationVec(1.0F).multiply(0.6)).add(0, -0.2, 0);
        Vec3d to = target.getBoundingBox().getCenter();
        Vec3d dir = to.subtract(from);
        Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
        side = side.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : side.normalize();
        Vec3d up = side.crossProduct(dir).normalize();
        double bow = Math.min(2.0, dir.length() * 0.2);
        Vec3d[] bends = { side.multiply(bow), side.multiply(-bow), up.multiply(bow) };
        int steps = Math.max(8, (int) (dir.length() * 3));
        for (Vec3d bend : bends) {
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3d p = from.add(dir.multiply(t)).add(bend.multiply(4 * t * (1 - t)));
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                if (i % 3 == 0)
                    world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS,
                0.8F, 1.6F);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS,
                1.0F, 1.2F);
        target.damage(world.getDamageSources().indirectMagic(player, player), DART_DAMAGE * DARTS);
    }

    /** Regains 1d6+1 charges (dawn, long rest), up to the max. */
    public static void recharge(ItemStack stack, net.minecraft.util.math.random.Random random) {
        int before = charges(stack);
        if (before >= MAX_CHARGES)
            return;
        MagicData.setCharges(stack, Math.min(MAX_CHARGES, before + 2 + random.nextInt(6)));
    }

    /** Dawn recharge: once per in-game day, when the day number changes while the wand is carried. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || world.getTime() % 20 != 0)
            return;
        long day = world.getTimeOfDay() / 24000L;
        NbtCompound nbt = MagicData.getOrCreate(stack);
        if (!nbt.contains(DAWN_KEY)) {
            nbt.putLong(DAWN_KEY, day);
            return;
        }
        long stored = nbt.getLong(DAWN_KEY);
        if (day != stored) {
            nbt.putLong(DAWN_KEY, day);
            if (day > stored) // not when /time set goes backwards
                recharge(stack, world.getRandom());
        }
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return MagicData.isIdentified(stack) && charges(stack) < MAX_CHARGES;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0F * charges(stack) / MAX_CHARGES);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x9F7FFF;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.wand_of_magic_missiles.charges", charges(stack), MAX_CHARGES)
                .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("item.dndclasses.wand_of_magic_missiles.tooltip").formatted(Formatting.GRAY));
    }
}
