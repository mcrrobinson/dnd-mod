package mattonfire.dnd.quest.dialogue;

import mattonfire.dnd.tavern.BountyNoticeItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Right-clicking an NPC with a role opens its dialogue when it has something to say, before the NPC's own
 * interaction runs. With nothing to say (or while sneaking) the NPC behaves as before: the innkeeper
 * trades, hobbits share food, dwarves barter for gold.
 *
 * <p>Which hand: an empty main hand for every role; the innkeeper also talks when you hold something
 * else, except a bounty notice (handed in as before), a name tag or a spawn egg.
 */
public final class DialogueEvents {
    private DialogueEvents() {
    }

    public static void register() {
        Dialogues.register();
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // The client sends interact-at first (hitResult set), then plain interact: handle only the latter.
            if (world.isClient || hitResult != null || hand != Hand.MAIN_HAND || player.isSneaking()
                    || !(player instanceof ServerPlayerEntity serverPlayer) || !(entity instanceof LivingEntity)
                    || !entity.isAlive() || !talksWhileHolding(entity, player.getMainHandStack())) {
                return ActionResult.PASS;
            }
            return DialogueManager.tryOpen(serverPlayer, entity) ? ActionResult.SUCCESS : ActionResult.PASS;
        });
        ServerPlayNetworking.registerGlobalReceiver(DialogueManager.C2S_CHOOSE, (server, player, handler, buf, sender) -> {
            int index = buf.readVarInt();
            server.execute(() -> DialogueManager.choose(player, index));
        });
        ServerTickEvents.END_SERVER_TICK.register(DialogueManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DialogueManager.forget(handler.getPlayer().getUuid()));
    }

    private static boolean talksWhileHolding(net.minecraft.entity.Entity npc, ItemStack held) {
        if (held.isEmpty()) {
            return !DialogueManager.roles(npc).isEmpty();
        }
        return DialogueManager.hasRole(npc, "innkeeper") && BountyNoticeItem.bounty(held) == null
                && !held.isOf(Items.NAME_TAG) && !(held.getItem() instanceof SpawnEggItem);
    }
}
