package mattonfire.dnd.classes.Conditions;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The owner's detail packet ({@link #S2C_CONDITIONS}): every instance with its time left, source and next save,
 * for the HUD and the Frightened movement clamp. Other clients only need the tracked mask. Also the Prone
 * stand-up request ({@link #C2S_STAND_UP}).
 */
public final class ConditionSync {
    /**
     * varint count, then per instance: varint ordinal, varint remainingTicks (-1 indefinite), boolean hasSource
     * [+ varint sourceEntityId (-1 if not loaded) + Text sourceName], byte saveAbility (-1 none) [+ varint dc,
     * varint ticksToNextSave].
     */
    public static final Identifier S2C_CONDITIONS = new Identifier(DnDClasses.MOD_ID, "conditions");
    /** A Prone player pressed jump. */
    public static final Identifier C2S_STAND_UP = new Identifier(DnDClasses.MOD_ID, "stand_up");
    /** Ticks after falling Prone before you can stand with jump. */
    public static final int STAND_UP_DELAY = 10;

    private ConditionSync() {
    }

    static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            Conditions.ensureLoaded(player);
            if (!Conditions.all(player).isEmpty()) {
                ConditionTicker.track(player);
            }
            send(player);
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_STAND_UP,
                (server, player, handler, buf, sender) -> server.execute(() -> standUp(player)));
    }

    /** Sends the owner's detail packet (count 0 when none are left). */
    public static void send(ServerPlayerEntity player) {
        if (player.networkHandler == null) {
            return;
        }
        List<ConditionInstance> list = Conditions.all(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(list.size());
        long now = player.getWorld().getTime();
        for (ConditionInstance inst : list) {
            buf.writeVarInt(inst.condition().ordinal());
            buf.writeVarInt(inst.remainingTicks(player.getWorld()));
            boolean hasSource = inst.source() != null || inst.sourceName() != null;
            buf.writeBoolean(hasSource);
            if (hasSource) {
                Entity source = Conditions.resolve(player, inst.source());
                buf.writeVarInt(source == null ? -1 : source.getId());
                buf.writeText(inst.sourceName() != null ? inst.sourceName() : Text.empty());
            }
            RepeatSave save = inst.save();
            buf.writeByte(save == null ? -1 : save.ability().ordinal());
            if (save != null) {
                buf.writeVarInt(save.dc());
                buf.writeVarInt((int) Math.max(0, save.nextAt() - now));
            }
        }
        ServerPlayNetworking.send(player, S2C_CONDITIONS, buf);
    }

    /** Prone players stand up with jump, at least {@value #STAND_UP_DELAY} ticks after falling; not while held. */
    static void standUp(ServerPlayerEntity player) {
        ConditionInstance prone = null;
        for (ConditionInstance inst : Conditions.all(player)) {
            if (inst.condition() == Condition.PRONE && (prone == null || inst.appliedAt() > prone.appliedAt())) {
                prone = inst;
            }
        }
        if (prone == null) {
            return;
        }
        if (Conditions.has(player, Condition.RESTRAINED) || !Conditions.canAct(player)
                || !Conditions.canMove(player)) {
            player.sendMessage(Text.translatable("condition.dndclasses.prone.held"), true);
            return;
        }
        if (player.getWorld().getTime() - prone.appliedAt() < STAND_UP_DELAY) {
            return;
        }
        Conditions.endMatching(player, inst -> inst.condition() == Condition.PRONE,
                ConditionEvents.EndReason.REMOVED, "stood up");
    }
}
