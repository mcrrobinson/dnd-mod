package mattonfire.dnd.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Effects.AntiMagicEffect;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Attunement (design section 3.3): an item that requires attunement only works for the player bonded to it,
 * and a player has {@link #slots} bonds (3; an Artificer 4 from class level 5 and 5 from 10).
 * <ul>
 * <li>The bonds are the player's, not their class's: a list in the player's persistent data under
 * {@value #DATA_KEY}, so {@code ClassLifecycle} copies them on death and End exit and they survive a relog.
 * The stack keeps its own id ({@link MagicData#uuid}) and who it's bonded to ({@link MagicData#attunedTo}),
 * so the bond comes back when a lost item is picked up again.</li>
 * <li>Attuning takes a {@value #CHANNEL_TICKS}-tick channel at an Attunement Table (the Magic Items tab),
 * cancelled by moving more than 8 blocks away, or one free attune per short rest. Attuning identifies the
 * item (and would reveal a curse).</li>
 * <li>Unattuning is free at a table, except for a cursed bond. A bond whose item is gone shows as missing in
 * the tab and can be released there.</li>
 * <li>{@link #isActive} is the one check magic effects use: identified, bonded if it needs it, the class
 * allowed (a Thief Rogue ignores that) and no anti-magic.</li>
 * </ul>
 */
public final class Attunement {
    public static final Identifier C2S_ATTUNE = new Identifier(DnDClasses.MOD_ID, "attune_item");
    public static final Identifier C2S_UNATTUNE = new Identifier(DnDClasses.MOD_ID, "unattune_item");
    public static final Identifier C2S_FORGE_BLESS = new Identifier(DnDClasses.MOD_ID, "forge_bless");
    public static final Identifier S2C_SYNC = new Identifier(DnDClasses.MOD_ID, "attunement_sync");

    public static final int BASE_SLOTS = 3;
    /** Artificer, Magic Item Adept: 4 slots from this class level... */
    public static final int ARTIFICER_ADEPT_LEVEL = 5;
    /** ...and 5 from this one. */
    public static final int ARTIFICER_MASTER_LEVEL = 10;
    /** The Thief Rogue's Use Magic Device: ignores the class an item asks for. */
    public static final String THIEF_SUBCLASS = "rogue.thief";
    /** 3 seconds at the table. */
    public static final int CHANNEL_TICKS = 60;

    static final String DATA_KEY = "dndAttunement";

    /** A channel in progress at a table. */
    private record Channel(int slot, UUID item, int endTick) {
    }

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    private Attunement() {
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_ATTUNE, (server, player, handler, buf, sender) -> {
            int slot = buf.readVarInt();
            server.execute(() -> startChannel(player, slot));
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_UNATTUNE, (server, player, handler, buf, sender) -> {
            UUID uuid = buf.readUuid();
            server.execute(() -> release(player, uuid, false));
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_FORGE_BLESS, (server, player, handler, buf, sender) -> {
            int slot = buf.readVarInt();
            server.execute(() -> ForgeBlessing.bless(player, slot));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CHANNELS.remove(handler.player.getUuid()));
        ServerTickEvents.END_SERVER_TICK.register(Attunement::tickChannels);

        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind == RestKind.SHORT) {
                shortRestAttune(player);
            } else {
                ForgeBlessing.onLongRest(player);
            }
        });
    }

    // ---- Bonds ----

    public static List<AttunementSnapshot.Bond> bonds(PlayerEntity player) {
        List<AttunementSnapshot.Bond> bonds = new ArrayList<>();
        NbtList list = ((IEntityDataSaver) player).getPersistentData().getList(DATA_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound nbt = list.getCompound(i);
            if (!nbt.containsUuid("uuid"))
                continue;
            Text name;
            try {
                name = Text.Serializer.fromJson(nbt.getString("name"));
            } catch (RuntimeException e) {
                name = null;
            }
            bonds.add(new AttunementSnapshot.Bond(nbt.getUuid("uuid"), nbt.getString("item"),
                    name != null ? name : Text.literal(nbt.getString("item")), nbt.getBoolean("cursed"),
                    nbt.getString("curse"), nbt.getString("tier")));
        }
        return bonds;
    }

    private static void saveBonds(PlayerEntity player, List<AttunementSnapshot.Bond> bonds) {
        NbtList list = new NbtList();
        for (AttunementSnapshot.Bond bond : bonds) {
            NbtCompound nbt = new NbtCompound();
            nbt.putUuid("uuid", bond.uuid());
            nbt.putString("item", bond.item());
            nbt.putString("name", Text.Serializer.toJson(bond.name()));
            nbt.putBoolean("cursed", bond.cursed());
            nbt.putString("curse", bond.curse());
            nbt.putString("tier", bond.tier());
            list.add(nbt);
        }
        ((IEntityDataSaver) player).getPersistentData().put(DATA_KEY, list);
        Curse.onBondsChanged(player);
    }

    public static @Nullable AttunementSnapshot.Bond bond(PlayerEntity player, UUID uuid) {
        for (AttunementSnapshot.Bond bond : bonds(player))
            if (bond.uuid().equals(uuid))
                return bond;
        return null;
    }

    /**
     * Whether the stack is bonded to this player. The server checks the player's bond list; the client only
     * has the stack's own {@code attunedTo}.
     */
    public static boolean isBondedTo(PlayerEntity player, ItemStack stack) {
        UUID uuid = MagicData.uuid(stack);
        if (uuid == null || !player.getUuid().equals(MagicData.attunedTo(stack)))
            return false;
        return player.getWorld().isClient || bond(player, uuid) != null;
    }

    /** Attunement slots: 3, or the Artificer's 4 (class level 5) and 5 (class level 10). */
    public static int slots(PlayerEntity player) {
        if (Progression.classOf(player) == DndCharacter.ARTIFICER) {
            int level = Progression.current(player).level();
            if (level >= ARTIFICER_MASTER_LEVEL)
                return BASE_SLOTS + 2;
            if (level >= ARTIFICER_ADEPT_LEVEL)
                return BASE_SLOTS + 1;
        }
        return BASE_SLOTS;
    }

    /** Whether the player's class may use an item made for these classes (always, for a Thief Rogue). */
    public static boolean classAllowed(PlayerEntity player, MagicItems.Info info) {
        if (info.classes().isEmpty())
            return true;
        if (info.classes().contains(Progression.classOf(player)))
            return true;
        ClassProgress progress = Progression.current(player);
        return progress.hasSubclass(THIEF_SUBCLASS);
    }

    /**
     * Whether the item's magic works for this player right now: it's a magic item, identified, bonded to them
     * if it needs attunement, made for their class (or they're a Thief), and they're not in an anti-magic
     * field. Effects check this through {@link MagicEffects}.
     */
    public static boolean isActive(PlayerEntity player, ItemStack stack) {
        MagicItems.Info info = MagicItems.info(stack);
        if (info == null || MagicData.isDormant(stack))
            return false;
        if (AntiMagicEffect.isSuppressed(player))
            return false;
        if (!classAllowed(player, info))
            return false;
        return !info.attunement() || isBondedTo(player, stack);
    }

    // ---- Attuning ----

    /** Why this player can't attune to the stack, or null if they can. */
    public static @Nullable Text refusal(ServerPlayerEntity player, ItemStack stack) {
        MagicItems.Info info = MagicItems.info(stack);
        if (info == null || !info.attunement())
            return Text.literal("That item doesn't need attunement.");
        UUID uuid = MagicData.uuid(stack);
        if (uuid != null && bond(player, uuid) != null)
            return Text.literal("You're already attuned to that.");
        UUID owner = MagicData.attunedTo(stack);
        if (owner != null && !owner.equals(player.getUuid())) {
            ServerPlayerEntity other = player.getServer().getPlayerManager().getPlayer(owner);
            // A bond the owner released while the item was away from them is stale and can be taken over.
            boolean stale = other != null && (uuid == null || bond(other, uuid) == null);
            if (!stale) {
                String name = other != null ? other.getName().getString() : MagicData.attunedName(stack);
                return Text.literal("That item is attuned to " + (name.isEmpty() ? "someone else" : name) + ".");
            }
        }
        if (!classAllowed(player, info)) {
            return Text.literal("Only " + classList(info) + " can attune to that.");
        }
        int slots = slots(player);
        if (bonds(player).size() >= slots)
            return Text.literal("You can only be attuned to " + slots + " items.");
        if (AntiMagicEffect.isSuppressed(player))
            return Text.translatable("effect.dndclasses.anti_magic.blocked");
        return null;
    }

    private static String classList(MagicItems.Info info) {
        List<String> names = new ArrayList<>();
        for (DndCharacter c : DndCharacter.values())
            if (info.classes().contains(c))
                names.add(MagicNames.className(c) + "s");
        if (names.size() == 1)
            return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
    }

    /**
     * Bonds the stack to the player, if {@link #refusal} allows it. Identifies the item and makes its curse
     * known.
     *
     * @return whether it was attuned
     */
    public static boolean attune(ServerPlayerEntity player, ItemStack stack) {
        Text refusal = refusal(player, stack);
        if (refusal != null) {
            player.sendMessage(refusal.copy().formatted(Formatting.RED), false);
            return false;
        }
        Curse curse = Curse.byId(MagicData.curse(stack));
        List<AttunementSnapshot.Bond> bonds = addBond(player, stack);
        player.sendMessage(Text.literal("You attune to ").formatted(Formatting.AQUA).append(stack.toHoverableText())
                .append(Text.literal(". (" + bonds.size() + "/" + slots(player) + " bonds)").formatted(Formatting.AQUA)),
                false);
        if (curse != null) {
            player.sendMessage(Text.translatable("magic.dndclasses.curse.binds", stack.toHoverableText(),
                    curse.displayName().formatted(Formatting.RED)).formatted(Formatting.DARK_RED), false);
            player.sendMessage(curse.description().formatted(Formatting.GRAY, Formatting.ITALIC), false);
        }
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /**
     * Writes the bond on both sides: identifies the item, reveals its curse (cursed armor also gets Curse of
     * Binding), marks it bonded and adds it to the player's list.
     */
    private static List<AttunementSnapshot.Bond> addBond(ServerPlayerEntity player, ItemStack stack) {
        UUID uuid = MagicData.ensureUuid(stack);
        MagicData.setIdentified(stack, true);
        Curse curse = Curse.byId(MagicData.curse(stack));
        if (curse != null) {
            MagicData.setCurseKnown(stack, true);
            Curse.addBinding(stack);
        }
        MagicData.setAttunedTo(stack, player.getUuid(), player.getName().getString());
        MagicItems.Info info = MagicItems.info(stack);

        List<AttunementSnapshot.Bond> bonds = bonds(player);
        bonds.removeIf(b -> b.uuid().equals(uuid));
        bonds.add(new AttunementSnapshot.Bond(uuid, Registries.ITEM.getId(stack.getItem()).toString(),
                stack.getName().copy(), curse != null, curse == null ? "" : curse.id,
                info == null ? "" : info.tier().id));
        saveBonds(player, bonds);
        sync(player);
        MagicEffects.updateAttributes(player);
        return bonds;
    }

    /**
     * A curse binds the item to the player ({@link Curse#bind}): no table, no refusal. It takes a slot even if
     * that puts the player over their limit.
     */
    static void bindCursed(ServerPlayerEntity player, ItemStack stack) {
        addBond(player, stack);
    }

    /**
     * Remove Curse succeeded: the cursed bond ends (the item stays cursed and identified). A cursed item still
     * worn or held is moved into the main inventory (or dropped if it's full) so it doesn't bind again at once,
     * and armor loses the Curse of Binding the curse put on it.
     *
     * @return the freed bond, or null if there was none
     */
    public static @Nullable AttunementSnapshot.Bond breakCurse(ServerPlayerEntity player, UUID uuid) {
        AttunementSnapshot.Bond bond = bond(player, uuid);
        if (bond == null)
            return null;
        List<AttunementSnapshot.Bond> bonds = bonds(player);
        bonds.removeIf(b -> b.uuid().equals(uuid));
        saveBonds(player, bonds);
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!uuid.equals(MagicData.uuid(stack)))
                continue;
            MagicData.setAttunedTo(stack, null);
            Curse.removeBinding(stack);
            boolean equipped = i == inventory.selectedSlot || i >= PlayerInventory.MAIN_SIZE;
            if (equipped) {
                inventory.setStack(i, ItemStack.EMPTY);
                int free = -1;
                for (int j = PlayerInventory.getHotbarSize(); j < PlayerInventory.MAIN_SIZE; j++) {
                    if (inventory.getStack(j).isEmpty()) {
                        free = j;
                        break;
                    }
                }
                if (free >= 0)
                    inventory.setStack(free, stack);
                else
                    player.dropItem(stack, false, true);
            }
        }
        sync(player);
        MagicEffects.updateAttributes(player);
        return bond;
    }

    /** Takes off the Curse of Binding a curse put on this item (for {@code /dndmagic uncurse}). */
    public static void removeCurseBinding(ItemStack stack) {
        Curse.removeBinding(stack);
    }

    /** The bond a Remove Curse aims at: the cursed item in the main hand, else the first cursed bond. */
    public static @Nullable AttunementSnapshot.Bond cursedBond(PlayerEntity player) {
        List<AttunementSnapshot.Bond> bonds = bonds(player);
        UUID held = MagicData.uuid(player.getMainHandStack());
        for (AttunementSnapshot.Bond bond : bonds)
            if (bond.cursed() && bond.uuid().equals(held))
                return bond;
        for (AttunementSnapshot.Bond bond : bonds)
            if (bond.cursed())
                return bond;
        return null;
    }

    /**
     * Admin uncurse ({@code /dndmagic uncurse}): the bond with this item stops being cursed, or ends if the item
     * doesn't need attunement.
     */
    public static void uncurseBond(ServerPlayerEntity player, ItemStack stack) {
        UUID uuid = MagicData.uuid(stack);
        AttunementSnapshot.Bond bond = uuid == null ? null : bond(player, uuid);
        if (bond == null)
            return;
        MagicItems.Info info = MagicItems.info(stack);
        List<AttunementSnapshot.Bond> bonds = bonds(player);
        bonds.removeIf(b -> b.uuid().equals(uuid));
        if (info != null && info.attunement()) {
            bonds.add(new AttunementSnapshot.Bond(uuid, bond.item(), bond.name(), false, "", bond.tier()));
        } else {
            MagicData.setAttunedTo(stack, null);
        }
        saveBonds(player, bonds);
        sync(player);
        MagicEffects.updateAttributes(player);
    }

    /**
     * Ends a bond, by the item's id: Unattune for an item you carry, Release for a lost one. Needs a table and
     * an uncursed bond.
     *
     * @param force skip the table and curse checks (admin command)
     * @return whether the bond ended
     */
    public static boolean release(ServerPlayerEntity player, UUID uuid, boolean force) {
        AttunementSnapshot.Bond bond = bond(player, uuid);
        if (bond == null)
            return false;
        if (!force) {
            if (!Progression.atAttunementTable(player)) {
                player.sendMessage(Text.literal("Bonds can only be released at an Attunement Table.")
                        .formatted(Formatting.RED), false);
                return false;
            }
            if (bond.cursed()) {
                player.sendMessage(Text.literal("A curse binds you to ").append(bond.name())
                        .append(". Only Remove Curse can break it.").formatted(Formatting.RED), false);
                return false;
            }
        }
        List<AttunementSnapshot.Bond> bonds = bonds(player);
        bonds.removeIf(b -> b.uuid().equals(uuid));
        saveBonds(player, bonds);
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (uuid.equals(MagicData.uuid(stack)))
                MagicData.setAttunedTo(stack, null);
        }
        sync(player);
        MagicEffects.updateAttributes(player);
        player.sendMessage(Text.literal("You end your bond with ").formatted(Formatting.GRAY).append(bond.name())
                .append(Text.literal(".").formatted(Formatting.GRAY)), false);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** Starts the 3 s channel at the table for the item in inventory slot {@code slot}. */
    public static void startChannel(ServerPlayerEntity player, int slot) {
        if (!Progression.atAttunementTable(player)) {
            player.sendMessage(Text.literal("Items are attuned at an Attunement Table.").formatted(Formatting.RED),
                    false);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.size())
            return;
        ItemStack stack = inventory.getStack(slot);
        Text refusal = refusal(player, stack);
        if (refusal != null) {
            player.sendMessage(refusal.copy().formatted(Formatting.RED), false);
            return;
        }
        UUID uuid = MagicData.ensureUuid(stack);
        CHANNELS.put(player.getUuid(), new Channel(slot, uuid, player.getServer().getTicks() + CHANNEL_TICKS));
        player.sendMessage(Text.literal("Attuning to ").formatted(Formatting.LIGHT_PURPLE)
                .append(stack.toHoverableText()).append(Text.literal("...").formatted(Formatting.LIGHT_PURPLE)), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT,
                SoundCategory.PLAYERS, 1.0F, 1.4F);
        sync(player);
    }

    private static void tickChannels(MinecraftServer server) {
        if (CHANNELS.isEmpty())
            return;
        int now = server.getTicks();
        Iterator<Map.Entry<UUID, Channel>> it = CHANNELS.entrySet().iterator();
        List<ServerPlayerEntity> toSync = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<UUID, Channel> entry = it.next();
            Channel channel = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            ItemStack stack = player.getInventory().getStack(channel.slot());
            Text interrupted = null;
            if (!Progression.atAttunementTable(player)) {
                interrupted = Text.literal("Attunement interrupted: you left the table.");
            } else if (!channel.item().equals(MagicData.uuid(stack))) {
                interrupted = Text.literal("Attunement interrupted: the item left your hands.");
            } else if (!player.isAlive()) {
                interrupted = Text.literal("Attunement interrupted.");
            }
            if (interrupted != null) {
                it.remove();
                player.sendMessage(interrupted.copy().formatted(Formatting.RED), false);
                toSync.add(player);
                continue;
            }
            if (now % 4 == 0) {
                ServerWorld world = player.getWorld();
                world.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 6,
                        0.5, 0.6, 0.5, 0.6);
            }
            if (now >= channel.endTick()) {
                it.remove();
                if (!attune(player, stack))
                    toSync.add(player);
            }
        }
        toSync.forEach(Attunement::sync);
    }

    /**
     * A short rest attunes one item: the main hand's, else the offhand's, else the first one carried that
     * needs it and can be attuned.
     */
    public static void shortRestAttune(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        List<Integer> order = new ArrayList<>();
        order.add(inventory.selectedSlot);
        order.add(PlayerInventory.OFF_HAND_SLOT);
        for (int i = 0; i < inventory.size(); i++)
            order.add(i);
        for (int slot : order) {
            ItemStack stack = inventory.getStack(slot);
            MagicItems.Info info = MagicItems.info(stack);
            if (info == null || !info.attunement() || refusal(player, stack) != null)
                continue;
            player.sendMessage(Text.literal("While you rest, you study your gear.").formatted(Formatting.GRAY),
                    false);
            attune(player, stack);
            return;
        }
    }

    /** Once a second: clears a stale {@code attunedTo} from carried items whose bond was released while away. */
    static void secondTick(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        List<AttunementSnapshot.Bond> bonds = null;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty() || !player.getUuid().equals(MagicData.attunedTo(stack)))
                continue;
            if (bonds == null)
                bonds = bonds(player);
            UUID uuid = MagicData.uuid(stack);
            if (uuid == null || bonds.stream().noneMatch(b -> b.uuid().equals(uuid)))
                MagicData.setAttunedTo(stack, null);
        }
    }

    // ---- Sync ----

    public static void sync(ServerPlayerEntity player) {
        if (player.networkHandler == null)
            return;
        Channel channel = CHANNELS.get(player.getUuid());
        int ticksLeft = channel == null ? 0 : Math.max(0, channel.endTick() - player.getServer().getTicks());
        AttunementSnapshot snapshot = new AttunementSnapshot(slots(player), bonds(player), ForgeBlessing.state(player),
                channel == null ? -1 : channel.slot(), ticksLeft, 0);
        PacketByteBuf buf = PacketByteBufs.create();
        snapshot.write(buf);
        ServerPlayNetworking.send(player, S2C_SYNC, buf);
    }
}
