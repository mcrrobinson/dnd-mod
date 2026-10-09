package mattonfire.dnd.classes.Music;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Items.InstrumentItem;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Every Bard's instrument slot: a lute that's always at hand, played with its
 * own key. It's virtual, not an inventory slot, so there's no item to drop,
 * move, lose on death or duplicate. The client draws it next to the hotbar
 * ({@code Client/Hud/InstrumentSlotHud}) and sends {@link #C2S_PLAY} when the
 * key is pressed. It shares the lute item's cooldown.
 */
public final class BardInstrumentSlot {
    public static final Identifier C2S_PLAY = new Identifier(DnDClasses.MOD_ID, "play_bard_instrument");

    private BardInstrumentSlot() {
    }

    /** The instrument in the slot. */
    public static InstrumentItem instrument() {
        return (InstrumentItem) ModItems.LUTE;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_PLAY,
                (server, player, handler, buf, sender) -> server.execute(() -> play(player)));
    }

    private static void play(ServerPlayerEntity player) {
        if (Progression.classOf(player) != DndCharacter.BARD || player.isSpectator() || !player.isAlive()
                || player.getItemCooldownManager().isCoolingDown(instrument()))
            return;
        instrument().playAsBard(player);
    }
}
