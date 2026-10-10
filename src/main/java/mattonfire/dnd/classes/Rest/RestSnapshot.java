package mattonfire.dnd.classes.Rest;

import net.minecraft.network.PacketByteBuf;

/**
 * What the client knows about its own rest state, sent in
 * {@link RestSync#S2C_REST_STATE}.
 *
 * @param enabled         the {@code dndRests} gamerule; with it off the HUD shows no charges
 * @param group           recharge group ordinal ({@link RechargeGroup})
 * @param sessionKind     0 no rest in progress, else {@link RestKind} ordinal + 1
 * @param sessionProgress ticks into the rest in progress, out of {@code sessionTotal}
 */
public record RestSnapshot(boolean enabled, int charges, int max, int temp, int group, int hitDiceLeft,
        int hitDiceMax, int dieSize, int shortRestsLeft, int sessionKind, int sessionProgress, int sessionTotal) {

    public static final RestSnapshot NONE = new RestSnapshot(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    /** The local player's state, kept up to date by the server. Unused on the server. */
    public static RestSnapshot client = NONE;

    public RechargeGroup rechargeGroup() {
        return RechargeGroup.values()[Math.max(0, Math.min(group, RechargeGroup.values().length - 1))];
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeVarInt(charges);
        buf.writeVarInt(max);
        buf.writeVarInt(temp);
        buf.writeVarInt(group);
        buf.writeVarInt(hitDiceLeft);
        buf.writeVarInt(hitDiceMax);
        buf.writeVarInt(dieSize);
        buf.writeVarInt(shortRestsLeft);
        buf.writeVarInt(sessionKind);
        buf.writeVarInt(sessionProgress);
        buf.writeVarInt(sessionTotal);
    }

    public static RestSnapshot read(PacketByteBuf buf) {
        return new RestSnapshot(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
