package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Gorgon's Gaze (Rare wondrous item, attunement), from the Medusa: once per dawn, use it on a non-boss mob within
 * {@value #RANGE} blocks. The mob makes a CON save against DC {@value #DC} (mobs roll with +1) or is Petrified for
 * good: a statue. The charge comes back at dawn and on a long rest, like the Eye of the Tyrant's.
 */
public class GorgonsGazeItem extends Item {
    public static final int MAX_CHARGES = 1;
    public static final double RANGE = 12.0;
    public static final int DC = 14;

    public GorgonsGazeItem(Settings settings) {
        super(settings);
    }

    public static int charges(ItemStack stack) {
        return Math.max(0, Math.min(MAX_CHARGES, MagicData.charges(stack, MAX_CHARGES)));
    }

    /** The charge back (dawn, long rest). */
    public static void recharge(ItemStack stack) {
        MagicData.setCharges(stack, MAX_CHARGES);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(stack, true);
        if (!MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        if (charges(stack) <= 0) {
            player.sendMessage(Text.translatable("item.dndclasses.gorgons_gaze.empty").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        LivingEntity target = findTarget(server);
        if (!(target instanceof MobEntity mob) || target instanceof Boss) {
            player.sendMessage(Text.translatable(target instanceof Boss ? "item.dndclasses.gorgons_gaze.boss"
                    : "item.dndclasses.gorgons_gaze.no_target").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS,
                0.6F, 1.5F);
        Vec3d at = mob.getBoundingBox().getCenter();
        serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 20, 0.3, 0.5, 0.3, 0.05);
        player.getItemCooldownManager().set(this, 20);
        if (!player.getAbilities().creativeMode)
            MagicData.setCharges(stack, charges(stack) - 1);

        SaveResult save = SavingThrow.of(mob, Ability.CON, DC).label("item.dndclasses.gorgons_gaze").source(player)
                .tags("magic", "petrify").roll();
        boolean stone = save.failed() && Conditions.of(mob, Condition.PETRIFIED).source(player).key("gorgons_gaze")
                .ticks(Conditions.INDEFINITE).apply() != null;
        player.sendMessage(Text.translatable(stone ? "item.dndclasses.gorgons_gaze.petrified"
                : "item.dndclasses.gorgons_gaze.resisted", mob.getDisplayName())
                .formatted(stone ? Formatting.DARK_GREEN : Formatting.GRAY), true);
        DnDClasses.LOGGER.info("[Magic] {} used Gorgon's Gaze on {}: {}", player.getEntityName(),
                mob.getName().getString(), stone ? "petrified" : "resisted");
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

    /** Dawn recharge: once per in-game day, when the day number changes while it's carried. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || world.getTime() % 20 != 0)
            return;
        if (MagicData.newDawn(stack, world))
            recharge(stack);
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
        return 0x6AA84F;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.gorgons_gaze.charges", charges(stack), MAX_CHARGES)
                .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("item.dndclasses.gorgons_gaze.tooltip").formatted(Formatting.GRAY));
    }
}
