package mattonfire.dnd.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/**
 * What the client knows about its own player's attunement, sent by {@link Attunement#sync}: the slots, the
 * bonds (with each bonded item's name, so a lost item can still be listed), the Forge blessing and a channel
 * in progress at the table.
 *
 * @param forge        0 not a Forge Cleric, 1 the blessing is ready, 2 used until the next long rest
 * @param channelSlot  the inventory slot being attuned at the table, or -1
 * @param channelTicks ticks left on that channel when this was sent
 */
public record AttunementSnapshot(int slots, List<Bond> bonds, int forge, int channelSlot, int channelTicks,
        long receivedAt) {
    /**
     * One bond: the item's id, its name when bonded, and whether a curse holds it.
     *
     * @param curse the {@link Curse} id, or "" (a cursed bond from before curses existed may have none)
     * @param tier  the item's {@link MagicTier} id when bonded (Remove Curse's DC), or ""
     */
    public record Bond(UUID uuid, String item, Text name, boolean cursed, String curse, String tier) {
    }

    public static AttunementSnapshot client = new AttunementSnapshot(Attunement.BASE_SLOTS, List.of(), 0, -1, 0, 0);

    public boolean bonded(UUID uuid) {
        for (Bond bond : bonds)
            if (bond.uuid().equals(uuid))
                return true;
        return false;
    }

    /** Fraction of the channel done at {@code now} (milliseconds), or -1 if there's none. */
    public float channelProgress(long now) {
        if (channelSlot < 0)
            return -1;
        float doneTicks = Attunement.CHANNEL_TICKS - channelTicks + (now - receivedAt) / 50.0F;
        return Math.min(1.0F, Math.max(0.0F, doneTicks / Attunement.CHANNEL_TICKS));
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(slots);
        buf.writeVarInt(bonds.size());
        for (Bond bond : bonds) {
            buf.writeUuid(bond.uuid());
            buf.writeString(bond.item());
            buf.writeText(bond.name());
            buf.writeBoolean(bond.cursed());
            buf.writeString(bond.curse());
            buf.writeString(bond.tier());
        }
        buf.writeVarInt(forge);
        buf.writeVarInt(channelSlot);
        buf.writeVarInt(channelTicks);
    }

    public static AttunementSnapshot read(PacketByteBuf buf) {
        int slots = buf.readVarInt();
        int n = buf.readVarInt();
        List<Bond> bonds = new ArrayList<>();
        for (int i = 0; i < n; i++)
            bonds.add(new Bond(buf.readUuid(), buf.readString(), buf.readText(), buf.readBoolean(), buf.readString(),
                    buf.readString()));
        return new AttunementSnapshot(slots, bonds, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                System.currentTimeMillis());
    }
}
