package mattonfire.dnd.classes;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class ManaManager {
    private static final String MANA_KEY = "manorMana";

    public static int getMana(ServerPlayerEntity player) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        return data.contains(MANA_KEY) ? data.getInt(MANA_KEY) : DnDClasses.MANA_ICONS;
    }

    public static void setMana(ServerPlayerEntity player, int amount) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        data.putInt(MANA_KEY, Math.min(amount, DnDClasses.MANA_ICONS));
    }

    public static boolean hasFullMana(ServerPlayerEntity player) {
        return getMana(player) >= DnDClasses.MANA_ICONS;
    }

    public static void resetMana(ServerPlayerEntity player) {
        setMana(player, 0);
    }

    public static void regenerateMana(ServerPlayerEntity player) {
        int currentMana = getMana(player);
        if (currentMana < DnDClasses.MANA_ICONS) {
            int newMana = currentMana + 1;
            setMana(player, newMana);

            PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
            passedData.writeInt(newMana);
            ServerPlayNetworking.send((ServerPlayerEntity) player,
                    DnDClasses.S2C_SYNC_MANA,
                    passedData);
        }
    }
}
