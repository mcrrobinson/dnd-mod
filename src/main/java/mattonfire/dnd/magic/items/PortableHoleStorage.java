package mattonfire.dnd.magic.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;

/**
 * What every Portable Hole holds: {@value PortableHoleItem#SIZE} slots per hole, keyed by the cloth's
 * {@code MagicData} uuid and kept in the Overworld's saved data ({@code dndclasses_portable_holes}), not in the
 * item. The cloth stays light, the contents don't care where the cloth is, and two screens on one hole share one
 * inventory, so nothing can be duplicated.
 */
public class PortableHoleStorage extends PersistentState {
    private static final String ID = "dndclasses_portable_holes";

    private final Map<UUID, SimpleInventory> holes = new HashMap<>();

    public static PortableHoleStorage get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(PortableHoleStorage::fromNbt,
                PortableHoleStorage::new, ID);
    }

    /** The hole's inventory, made empty on first use. Changes mark the state dirty. */
    public SimpleInventory inventory(UUID hole) {
        return holes.computeIfAbsent(hole, id -> track(new SimpleInventory(PortableHoleItem.SIZE)));
    }

    /** The hole's inventory if it has one. */
    public @Nullable SimpleInventory peek(UUID hole) {
        return holes.get(hole);
    }

    /** How many stacks are in the hole. */
    public int used(UUID hole) {
        SimpleInventory inv = holes.get(hole);
        if (inv == null)
            return 0;
        int used = 0;
        for (int i = 0; i < inv.size(); i++)
            if (!inv.getStack(i).isEmpty())
                used++;
        return used;
    }

    /** Destroys the hole and everything in it (the Astral rift). Returns how many stacks were lost. */
    public int destroy(UUID hole) {
        int lost = used(hole);
        SimpleInventory inv = holes.remove(hole);
        if (inv != null)
            inv.clear();
        markDirty();
        return lost;
    }

    private SimpleInventory track(SimpleInventory inventory) {
        inventory.addListener(inv -> markDirty());
        return inventory;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound all = new NbtCompound();
        for (Map.Entry<UUID, SimpleInventory> e : holes.entrySet()) {
            SimpleInventory inv = e.getValue();
            DefaultedList<ItemStack> items = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
            boolean empty = true;
            for (int i = 0; i < inv.size(); i++) {
                items.set(i, inv.getStack(i));
                empty &= inv.getStack(i).isEmpty();
            }
            if (!empty)
                all.put(e.getKey().toString(), Inventories.writeNbt(new NbtCompound(), items, false));
        }
        nbt.put("Holes", all);
        return nbt;
    }

    private static PortableHoleStorage fromNbt(NbtCompound nbt) {
        PortableHoleStorage state = new PortableHoleStorage();
        NbtCompound all = nbt.getCompound("Holes");
        for (String key : all.getKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            DefaultedList<ItemStack> items = DefaultedList.ofSize(PortableHoleItem.SIZE, ItemStack.EMPTY);
            Inventories.readNbt(all.getCompound(key), items);
            SimpleInventory inv = new SimpleInventory(PortableHoleItem.SIZE);
            for (int i = 0; i < items.size(); i++)
                inv.setStack(i, items.get(i));
            state.holes.put(id, state.track(inv));
        }
        return state;
    }
}
