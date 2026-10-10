package mattonfire.dnd.magic.items;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.ScheduledBlockRestore;
import mattonfire.dnd.classes.Obstacles.ObstacleInteractions;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Immovable Rod (Uncommon wondrous item): right-click fixes a solid rod block ({@link ImmovableRodBlock})
 * where you look, against the face of the block you aim at within {@value #REACH} blocks, or in mid-air
 * {@value #AIR_DISTANCE} blocks along your view. Looking 60+ degrees down at nothing in reach, it goes right
 * under your feet and lifts you onto it. Right-click again, or right-click the rod, and it lets go; it also returns on its own after {@value #HOLD_SECONDS} s. Only in a
 * free spot (air, water, grass) that you're allowed to build in, not inside a sealed obstacle's protection.
 * The item stays in your hand; its NBT remembers where the rod is.
 */
public class ImmovableRodItem extends Item {
    public static final double REACH = 4.0;
    public static final double AIR_DISTANCE = 2.5;
    public static final int HOLD_SECONDS = 60;
    private static final String PLACED_KEY = "rod";

    public ImmovableRodItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient)
            return TypedActionResult.success(stack, true);
        if (!MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        ServerWorld server = (ServerWorld) world;
        player.getItemCooldownManager().set(this, 10);
        if (recall(server, player, stack))
            return TypedActionResult.success(stack, false);

        boolean underFeet = aimsUnderFeet(player);
        BlockPos pos = underFeet ? underFeet(player) : target(player);
        BlockState original = world.getBlockState(pos);
        BlockState rod = ModBlocks.IMMOVABLE_ROD.getDefaultState();
        boolean free = underFeet
                ? world.doesNotIntersectEntities(player, rod.getCollisionShape(world, pos).offset(pos.getX(), pos.getY(), pos.getZ()))
                : world.canPlace(rod, pos, ShapeContext.absent());
        if (!original.getMaterial().isReplaceable() || world.isOutOfHeightLimit(pos)
                || !world.getWorldBorder().contains(pos) || !world.canPlayerModifyAt(player, pos)
                || !player.canModifyAt(world, pos) || ObstacleInteractions.isProtected(world, pos) || !free) {
            player.sendMessage(Text.translatable("item.dndclasses.immovable_rod.blocked").formatted(Formatting.GRAY),
                    true);
            return TypedActionResult.fail(stack);
        }
        world.setBlockState(pos, rod, ScheduledBlockRestore.FLAGS);
        ScheduledBlockRestore.schedule(server, Map.of(pos.toImmutable(), original), rod, HOLD_SECONDS * 20, 1);
        if (underFeet && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            // Falling into the rod's top: lift them onto it so they stand there
            serverPlayer.requestTeleport(player.getX(), pos.getY() + 1.0, player.getZ());
            serverPlayer.setVelocity(Vec3d.ZERO);
            serverPlayer.fallDistance = 0;
        }
        NbtCompound placed = new NbtCompound();
        placed.put("Pos", NbtHelper.fromBlockPos(pos));
        placed.putString("Dim", world.getRegistryKey().getValue().toString());
        placed.put("State", NbtHelper.fromBlockState(original));
        MagicData.getOrCreate(stack).put(PLACED_KEY, placed);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0F, 0.8F);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.3F, 1.8F);
        DnDClasses.LOGGER.info("[Magic] {} fixed the Immovable Rod at {}", player.getEntityName(),
                pos.toShortString());
        return TypedActionResult.success(stack, false);
    }

    /** Looking steeply down at nothing within reach: the rod goes right under your feet. */
    private static boolean aimsUnderFeet(PlayerEntity player) {
        return player.getPitch() >= 60.0F && player.raycast(REACH, 1.0F, false).getType() == HitResult.Type.MISS;
    }

    /** The block whose top is at or just above the feet (at most a block's lift). */
    private static BlockPos underFeet(PlayerEntity player) {
        return BlockPos.ofFloored(player.getX(), Math.ceil(player.getY() - 1.0E-3) - 1.0, player.getZ());
    }

    /** Where the rod goes: against the aimed face, or in mid-air along the view. */
    private static BlockPos target(PlayerEntity player) {
        HitResult hit = player.raycast(REACH, 1.0F, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = block.getBlockPos();
            if (player.getWorld().getBlockState(hitPos).getMaterial().isReplaceable())
                return hitPos;
            return hitPos.offset(block.getSide());
        }
        Vec3d p = player.getEyePos().add(player.getRotationVec(1.0F).multiply(AIR_DISTANCE));
        return BlockPos.ofFloored(p);
    }

    /** If this rod is fixed somewhere, lets it go and returns true. Forgets a rod that's already gone. */
    private static boolean recall(ServerWorld world, PlayerEntity player, ItemStack stack) {
        NbtCompound magic = MagicData.get(stack);
        if (magic == null || !magic.contains(PLACED_KEY))
            return false;
        NbtCompound placed = magic.getCompound(PLACED_KEY);
        magic.remove(PLACED_KEY);
        BlockPos pos = NbtHelper.toBlockPos(placed.getCompound("Pos"));
        ServerWorld rodWorld = world.getServer().getWorld(net.minecraft.registry.RegistryKey.of(
                net.minecraft.registry.RegistryKeys.WORLD, new net.minecraft.util.Identifier(placed.getString("Dim"))));
        if (rodWorld == null || !rodWorld.isChunkLoaded(pos) || !rodWorld.getBlockState(pos).isOf(ModBlocks.IMMOVABLE_ROD))
            return false;
        BlockState original = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), placed.getCompound("State"));
        rodWorld.setBlockState(pos, original, ScheduledBlockRestore.FLAGS);
        rodWorld.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 1.0F, 1.4F);
        player.sendMessage(Text.translatable("item.dndclasses.immovable_rod.recalled").formatted(Formatting.GRAY),
                true);
        DnDClasses.LOGGER.info("[Magic] {} released the Immovable Rod at {}", player.getEntityName(),
                pos.toShortString());
        return true;
    }

    /** Forgets a rod that returned on its own or was released by someone else, so the tooltip stays right. */
    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot,
            boolean selected) {
        if (world.isClient || world.getTime() % 20 != 0)
            return;
        NbtCompound magic = MagicData.get(stack);
        if (magic == null || !magic.contains(PLACED_KEY))
            return;
        NbtCompound placed = magic.getCompound(PLACED_KEY);
        if (!placed.getString("Dim").equals(world.getRegistryKey().getValue().toString()))
            return;
        BlockPos pos = NbtHelper.toBlockPos(placed.getCompound("Pos"));
        if (world.isChunkLoaded(pos) && !world.getBlockState(pos).isOf(ModBlocks.IMMOVABLE_ROD))
            magic.remove(PLACED_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.immovable_rod.tooltip", HOLD_SECONDS)
                .formatted(Formatting.GRAY));
        NbtCompound magic = MagicData.get(stack);
        if (magic != null && magic.contains(PLACED_KEY)) {
            BlockPos pos = NbtHelper.toBlockPos(magic.getCompound(PLACED_KEY).getCompound("Pos"));
            tooltip.add(Text.translatable("item.dndclasses.immovable_rod.placed", pos.toShortString())
                    .formatted(Formatting.DARK_AQUA));
        }
    }
}
