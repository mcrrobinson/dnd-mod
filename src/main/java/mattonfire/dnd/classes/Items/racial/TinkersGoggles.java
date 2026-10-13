package mattonfire.dnd.classes.Items.racial;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The server half of Tinker's Goggles. A client wearing them asks ({@link #C2S_PEEK}) about the container block
 * under its crosshair; the server checks the player has the goggles on and really is looking at that block within
 * {@link TinkersGogglesItem#RANGE} (its own raycast, so no peeking through walls), and answers ({@link #S2C_CONTENTS})
 * with the container's contents, merged by item, at most {@link #MAX_ENTRIES} of them. Double chests count as one.
 *
 * <p>A container whose loot hasn't been rolled yet (a structure chest nobody has opened) answers "sealed": peeking
 * doesn't roll it, so its loot table, lock and grudges stay as they are.
 */
public final class TinkersGoggles {
    public static final Identifier C2S_PEEK = new Identifier(DnDClasses.MOD_ID, "goggles_peek");
    public static final Identifier S2C_CONTENTS = new Identifier(DnDClasses.MOD_ID, "goggles_contents");
    public static final int MAX_ENTRIES = 27;

    private TinkersGoggles() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_PEEK, (server, player, handler, buf, sender) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> peek(player, pos));
        });
    }

    /** What the goggles see at {@code pos} for {@code player}: nothing (no answer) unless it's a container in sight. */
    static void peek(ServerPlayerEntity player, BlockPos pos) {
        if (!TinkersGogglesItem.wears(player) || player.isSpectator()) {
            return;
        }
        ServerWorld world = player.getWorld();
        Vec3d eye = player.getEyePos();
        if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > (TinkersGogglesItem.RANGE + 1.0D) * (TinkersGogglesItem.RANGE + 1.0D)) {
            return;
        }
        HitResult hit = player.raycast(TinkersGogglesItem.RANGE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK || !blockHit.getBlockPos().equals(pos)) {
            return;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof Inventory inventory)) {
            return;
        }
        boolean sealed = blockEntity instanceof LootableContainerBlockEntity lootable && lootable.createNbt().contains("LootTable");
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            Inventory both = ChestBlock.getInventory(chest, state, world, pos, true);
            if (both != null) {
                inventory = both;
            }
        }
        List<ItemStack> contents = sealed ? List.of() : merge(inventory);
        PacketByteBuf out = PacketByteBufs.create();
        out.writeBlockPos(pos);
        out.writeBoolean(sealed);
        out.writeVarInt(contents.size());
        for (ItemStack stack : contents) {
            // The item and its count apart: a merged count can pass the byte the stack's own count is sent in.
            out.writeItemStack(stack.copyWithCount(1));
            out.writeVarInt(stack.getCount());
        }
        ServerPlayNetworking.send(player, S2C_CONTENTS, out);
    }

    /** The inventory's stacks, like items merged (counts may go past a stack), in slot order of first appearance. */
    static List<ItemStack> merge(Inventory inventory) {
        List<ItemStack> merged = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            boolean added = false;
            for (ItemStack seen : merged) {
                if (ItemStack.canCombine(seen, stack)) {
                    seen.increment(stack.getCount());
                    added = true;
                    break;
                }
            }
            if (!added && merged.size() < MAX_ENTRIES) {
                merged.add(stack.copy());
            }
        }
        return merged;
    }
}
