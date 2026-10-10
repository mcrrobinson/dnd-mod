package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Eye of the Tyrant (Legendary wondrous item, attunement): right-click a creature within {@value #RANGE} blocks to
 * fire one of three eye rays at random: Paralysis (Freeze {@value #PARALYSIS_SECONDS} s), Telekinesis (Levitation
 * {@value #TELEKINESIS_SECONDS} s) or Disintegration ({@value #DISINTEGRATION_DAMAGE} magic damage).
 * {@value #MAX_CHARGES} charges; one comes back at each dawn and on a long rest. While it's active its bearer is
 * immune to the Beholder's anti-magic cone ({@code BossItems.blocksEffect}).
 */
public class EyeOfTheTyrantItem extends Item {
    public static final int MAX_CHARGES = 3;
    public static final double RANGE = 32.0;
    public static final int PARALYSIS_SECONDS = 3;
    public static final int TELEKINESIS_SECONDS = 3;
    public static final float DISINTEGRATION_DAMAGE = 12.0F;
    private static final String DAWN_KEY = "dawnDay";

    public enum Ray {
        PARALYSIS(new Vector3f(0.4F, 0.8F, 1.0F)),
        TELEKINESIS(new Vector3f(0.8F, 0.4F, 1.0F)),
        DISINTEGRATION(new Vector3f(0.4F, 1.0F, 0.3F));

        final Vector3f color;

        Ray(Vector3f color) {
            this.color = color;
        }
    }

    public EyeOfTheTyrantItem(Settings settings) {
        super(settings);
    }

    public static int charges(ItemStack stack) {
        return MathHelper.clamp(MagicData.charges(stack, MAX_CHARGES), 0, MAX_CHARGES);
    }

    /** One charge back (dawn, long rest), up to the max. */
    public static void recharge(ItemStack stack) {
        int before = charges(stack);
        if (before < MAX_CHARGES)
            MagicData.setCharges(stack, before + 1);
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
            player.sendMessage(Text.translatable("item.dndclasses.eye_of_the_tyrant.empty").formatted(Formatting.GRAY),
                    true);
            return TypedActionResult.fail(stack);
        }
        LivingEntity target = findTarget(server);
        if (target == null) {
            player.sendMessage(Text.translatable("item.dndclasses.eye_of_the_tyrant.no_target")
                    .formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        Ray ray = Ray.values()[world.getRandom().nextInt(Ray.values().length)];
        fire((ServerWorld) world, server, target, ray);
        player.getItemCooldownManager().set(this, 20);
        if (!player.getAbilities().creativeMode) {
            charges--;
            MagicData.setCharges(stack, charges);
        }
        player.sendMessage(Text.translatable("item.dndclasses.eye_of_the_tyrant.ray." + ray.name().toLowerCase(),
                target.getDisplayName()).formatted(Formatting.LIGHT_PURPLE), true);
        DnDClasses.LOGGER.info("[Magic] {} fired the Eye of the Tyrant's {} ray at {}, {} charges left",
                player.getEntityName(), ray, target.getEntityName(), charges);
        return TypedActionResult.success(stack, false);
    }

    /** The living creature under the crosshair within range, with nothing solid in between. */
    private static @Nullable LivingEntity findTarget(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(RANGE));
        var block = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        Vec3d reach = block.getPos();
        Box box = player.getBoundingBox().stretch(reach.subtract(eye)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, reach, box,
                e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && e != player,
                eye.squaredDistanceTo(reach));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void fire(ServerWorld world, ServerPlayerEntity player, LivingEntity target, Ray ray) {
        Vec3d from = player.getEyePos().add(player.getRotationVec(1.0F).multiply(0.6)).add(0, -0.2, 0);
        Vec3d to = target.getBoundingBox().getCenter();
        Vec3d step = to.subtract(from);
        int steps = Math.max(8, (int) (step.length() * 3));
        ParticleEffect dust = new DustParticleEffect(ray.color, 1.2F);
        for (int i = 0; i <= steps; i++) {
            Vec3d p = from.add(step.multiply((double) i / steps));
            world.spawnParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS,
                0.5F, 1.8F);
        switch (ray) {
            case PARALYSIS -> target.addStatusEffect(
                    new StatusEffectInstance(ModEffects.FREEZE, PARALYSIS_SECONDS * 20, 0), player);
            case TELEKINESIS -> target.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.LEVITATION, TELEKINESIS_SECONDS * 20, 1), player);
            case DISINTEGRATION -> {
                target.damage(world.getDamageSources().indirectMagic(player, player), DISINTEGRATION_DAMAGE);
                world.spawnParticles(ParticleTypes.ASH, to.x, to.y, to.z, 25, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    /** Dawn recharge: once per in-game day, when the day number changes while the Eye is carried. */
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
            if (day > stored)
                recharge(stack);
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
        return 0x5AC850;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.eye_of_the_tyrant.charges", charges(stack), MAX_CHARGES)
                .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("item.dndclasses.eye_of_the_tyrant.tooltip").formatted(Formatting.GRAY));
    }
}
