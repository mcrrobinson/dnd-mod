package mattonfire.dnd.classes.Rest;

import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.dungeon.RoomRole;
import mattonfire.dnd.dungeon.RoomState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * Short rests at campfires: sneak + right-click a lit campfire (or soul campfire)
 * with an empty main hand to sit for {@link RestSession#SHORT_REST_TICKS}.
 * Attacking, using an item, block or entity, or taking damage interrupts it; the
 * power-up key does too (in {@code DnDClasses.sendPowerupPacket}).
 */
public final class CampfireRest {
    private CampfireRest() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RestSession::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RestSession.forget(
                handler.getPlayer().getUuid()));

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            boolean restStart = player.isSneaking() && player.getMainHandStack().isEmpty()
                    && isLitCampfire(world.getBlockState(pos));
            if (world.isClient) {
                return restStart ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            ServerPlayerEntity server = (ServerPlayerEntity) player;
            if (RestSession.isResting(server)) {
                // Clicking the campfire again (or anything else) stops resting.
                RestSession.cancel(server, restStart ? "you got up" : "you used a block");
                return restStart ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            if (!restStart || player.isSpectator()) {
                return ActionResult.PASS;
            }
            RestSession.start(server, pos);
            return ActionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            interrupt(player, "you used an item");
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            interrupt(player, "you used something");
            return ActionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            interrupt(player, "you attacked");
            return ActionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            interrupt(player, "you started digging");
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0 && entity instanceof ServerPlayerEntity player) {
                RestSession.cancel(player, "you were hurt");
            }
            return true;
        });

        // Dungeons: no long rests inside one whose boss is alive, and short rests only in the
        // entrance, the antechamber or a cleared room.
        RestEvents.ALLOW_REST.register((player, kind) -> {
            ServerWorld world = (ServerWorld) player.getWorld();
            DungeonRegistry dungeons = DungeonRegistry.get(world);
            BlockPos pos = player.getBlockPos();
            if (!dungeons.isInsideUncleared(pos)) {
                return null;
            }
            if (kind == RestKind.SHORT) {
                DungeonState.Room room = dungeons.roomAt(pos);
                if (room != null && (room.role() == RoomRole.ENTRANCE || room.role() == RoomRole.ANTECHAMBER
                        || room.state() == RoomState.CLEARED)) {
                    return null;
                }
            }
            return Text.literal("This place is too dangerous to rest.");
        });
    }

    public static boolean isLitCampfire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock && state.get(CampfireBlock.LIT);
    }

    private static void interrupt(PlayerEntity player, String reason) {
        if (player instanceof ServerPlayerEntity server) {
            RestSession.cancel(server, reason);
        }
    }
}
