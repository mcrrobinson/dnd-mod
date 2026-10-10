package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Decanter of Endless Water (Uncommon wondrous item): a water bucket that never empties. Sneak-use: a geyser
 * that shoves the mobs in front of you about {@value #PUSH_BLOCKS} blocks back and puts out fire (burning
 * mobs and fire blocks), with a {@value #GEYSER_COOLDOWN_SECONDS} s cooldown kept on the item.
 */
public class DecanterOfEndlessWaterItem extends BucketItem {
    public static final double PUSH_BLOCKS = 4.0;
    public static final double REACH = 6.0;
    public static final int GEYSER_COOLDOWN_SECONDS = 30;
    private static final String READY_KEY = "geyserReadyAt";

    public DecanterOfEndlessWaterItem(Settings settings) {
        super(Fluids.WATER, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking())
            return geyser(world, player, stack);
        if (!world.isClient && !MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        TypedActionResult<ItemStack> result = super.use(world, player, hand);
        // The bucket logic hands back an empty bucket; the decanter stays as it is.
        return result.getResult().isAccepted() ? TypedActionResult.success(stack, world.isClient)
                : TypedActionResult.pass(stack);
    }

    private TypedActionResult<ItemStack> geyser(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient)
            return TypedActionResult.success(stack, true);
        if (!MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        long now = world.getTime();
        long readyAt = MagicData.get(stack) == null ? 0 : MagicData.get(stack).getLong(READY_KEY);
        if (now < readyAt) {
            player.sendMessage(Text.translatable("item.dndclasses.decanter_of_endless_water.cooldown",
                    (readyAt - now + 19) / 20).formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        MagicData.getOrCreate(stack).putLong(READY_KEY, now + GEYSER_COOLDOWN_SECONDS * 20L);
        ServerWorld server = (ServerWorld) world;
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        flat = flat.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : flat.normalize();

        // Shove: a 1.6 blocks/tick start decays to roughly 4 blocks of travel on the ground.
        Box area = player.getBoundingBox().stretch(look.multiply(REACH)).expand(1.5);
        int pushed = 0;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3d to = target.getPos().subtract(player.getPos());
            if (to.length() > REACH || to.normalize().dotProduct(look) < 0.5)
                continue;
            double resist = 1.0 - target.getAttributeValue(
                    net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if (resist <= 0)
                continue;
            target.setVelocity(target.getVelocity().add(flat.multiply(1.6 * resist)).add(0, 0.35 * resist, 0));
            target.velocityModified = true;
            target.extinguish();
            pushed++;
        }
        player.extinguish();

        // Put out fire along the jet.
        int doused = 0;
        for (double d = 0.5; d <= REACH; d += 0.5) {
            BlockPos center = BlockPos.ofFloored(eye.add(look.multiply(d)));
            for (BlockPos pos : BlockPos.iterate(center.add(-1, -2, -1), center.add(1, 1, 1))) {
                var state = world.getBlockState(pos);
                if (state.getBlock() instanceof AbstractFireBlock) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    doused++;
                } else if (state.getBlock() instanceof CampfireBlock && state.get(CampfireBlock.LIT)) {
                    CampfireBlock.extinguish(player, world, pos, state);
                    world.setBlockState(pos, state.with(CampfireBlock.LIT, false));
                    doused++;
                }
            }
            Vec3d p = eye.add(look.multiply(d)).add(0, -0.3, 0);
            server.spawnParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 8, 0.3, 0.3, 0.3, 0.2);
            server.spawnParticles(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, 3, 0.2, 0.2, 0.2, 0.05);
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1.0F,
                0.7F);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        DnDClasses.LOGGER.info("[Magic] {} used the Decanter's geyser: pushed {}, doused {} fires",
                player.getEntityName(), pushed, doused);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (MagicData.isIdentified(stack))
            tooltip.add(Text.translatable("item.dndclasses.decanter_of_endless_water.tooltip")
                    .formatted(Formatting.GRAY));
    }
}
