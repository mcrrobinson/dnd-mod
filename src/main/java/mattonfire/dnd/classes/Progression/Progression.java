package mattonfire.dnd.classes.Progression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModBlocks;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server side of class progression: stores each class's {@link ClassProgress}
 * in the player's persistent data, hands out XP, and handles unlock and
 * loadout requests from the skill tree screen.
 */
public final class Progression {
    public static final Identifier S2C_SYNC = new Identifier(DnDClasses.MOD_ID, "progression_sync");
    public static final Identifier S2C_OPEN_ATTUNEMENT = new Identifier(DnDClasses.MOD_ID, "open_attunement");
    public static final Identifier C2S_UNLOCK = new Identifier(DnDClasses.MOD_ID, "unlock_skill");
    public static final Identifier C2S_EQUIP = new Identifier(DnDClasses.MOD_ID, "equip_skill");

    private static final String DATA_KEY = "dndProgression";
    /** Everyone with a class gets this much XP a minute just for playing. */
    private static final int TRICKLE_XP = 1;
    private static final int TRICKLE_TICKS = 60 * 20;
    private static final double ATTUNEMENT_REACH_SQ = 8 * 8;

    /** The attunement table each player last opened; the loadout can only change near it. */
    private static final Map<UUID, BlockPos> OPEN_TABLES = new HashMap<>();

    private Progression() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> OPEN_TABLES.remove(handler.player.getUuid()));

        // ClassLifecycle copies the progress to the new player entity on death and End exit.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % TRICKLE_TICKS != 0)
                return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.isSpectator()) {
                    addXp(player, TRICKLE_XP);
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_UNLOCK, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> unlock(player, id, false));
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_EQUIP, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> equip(player, id, false));
        });

        ProgressionEvents.register();
    }

    public static DndCharacter classOf(PlayerEntity player) {
        DndCharacter dndClass = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        return dndClass == null ? DndCharacter.NONE : dndClass;
    }

    public static ClassProgress get(PlayerEntity player, DndCharacter dndClass) {
        if (player.getWorld().isClient) {
            return ClassProgress.client.dndClass == dndClass ? ClassProgress.client : new ClassProgress(dndClass);
        }
        NbtCompound all = ((IEntityDataSaver) player).getPersistentData().getCompound(DATA_KEY);
        return ClassProgress.fromNbt(dndClass, all.getCompound(dndClass.name()));
    }

    /** Progress in the player's current class. Works on both sides (the client only knows its own player). */
    public static ClassProgress current(PlayerEntity player) {
        return get(player, classOf(player));
    }

    /** Whether the player has this passive equipped in their current class. */
    public static boolean hasPassive(PlayerEntity player, String id) {
        return current(player).hasPassive(id);
    }

    private static void save(ServerPlayerEntity player, ClassProgress progress) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound all = data.getCompound(DATA_KEY);
        all.put(progress.dndClass.name(), progress.toNbt());
        data.put(DATA_KEY, all);
    }

    public static void sync(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        current(player).write(buf);
        ServerPlayNetworking.send(player, S2C_SYNC, buf);
    }

    /** Gives XP in the player's current class, announcing any level-up. */
    public static void addXp(ServerPlayerEntity player, int amount) {
        DndCharacter dndClass = classOf(player);
        if (dndClass == DndCharacter.NONE || amount <= 0)
            return;

        ClassProgress progress = get(player, dndClass);
        int oldLevel = progress.level();
        progress.xp += amount;
        save(player, progress);

        int newLevel = progress.level();
        if (newLevel > oldLevel) {
            String className = name(dndClass);
            Text message = ClassTrees.has(dndClass)
                    ? Text.literal(className + " level " + newLevel + "! You have " + progress.points()
                            + " skill point" + (progress.points() == 1 ? "" : "s") + " to spend (O).")
                    : Text.literal(className + " level " + newLevel + "! Its skill tree is coming soon.");
            player.sendMessage(message.copy().formatted(Formatting.GOLD), false);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.PLAYERS, 0.8F, 1.2F);
        }
        sync(player);
    }

    public static void setXp(ServerPlayerEntity player, DndCharacter dndClass, int xp) {
        ClassProgress progress = get(player, dndClass);
        progress.xp = Math.max(0, xp);
        save(player, progress);
        sync(player);
    }

    public static void reset(ServerPlayerEntity player, DndCharacter dndClass) {
        save(player, new ClassProgress(dndClass));
        sync(player);
    }

    /**
     * Unlocks a node of the player's current class.
     *
     * @param force skip the point and requirement checks (admin command)
     * @return whether it was unlocked
     */
    public static boolean unlock(ServerPlayerEntity player, String id, boolean force) {
        ClassProgress progress = current(player);
        SkillNode node = ClassTrees.node(id);
        boolean allowed = force ? ClassTrees.belongsTo(node, progress.dndClass) && !progress.isUnlocked(id)
                : node != null && progress.canUnlock(node);
        if (!allowed)
            return false;

        progress.unlocked.add(id);
        // Fill an empty slot straight away; swapping out an equipped skill needs a table.
        if (!node.isActive() && progress.passives.size() < ClassProgress.PASSIVE_SLOTS) {
            progress.passives.add(id);
        }
        save(player, progress);
        sync(player);

        player.sendMessage(Text.literal("Unlocked " + node.name() + ".").formatted(Formatting.GREEN), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Called when the player opens an attunement table. */
    public static void openAttunement(ServerPlayerEntity player, BlockPos pos) {
        OPEN_TABLES.put(player.getUuid(), pos.toImmutable());
        sync(player);
        ServerPlayNetworking.send(player, S2C_OPEN_ATTUNEMENT, PacketByteBufs.empty());
    }

    private static boolean atAttunementTable(ServerPlayerEntity player) {
        BlockPos pos = OPEN_TABLES.get(player.getUuid());
        return pos != null && player.getWorld().getBlockState(pos).isOf(ModBlocks.ATTUNEMENT_TABLE)
                && player.squaredDistanceTo(pos.toCenterPos()) <= ATTUNEMENT_REACH_SQ;
    }

    /**
     * Equips an unlocked node: an active replaces the current one, a passive is
     * toggled in or out of the passive slots. Only allowed at an attunement table.
     *
     * @param force skip the attunement table check (admin command)
     * @return whether the loadout changed
     */
    public static boolean equip(ServerPlayerEntity player, String id, boolean force) {
        if (!force && !atAttunementTable(player)) {
            player.sendMessage(Text.literal("Your loadout can only be changed at an Attunement Table.")
                    .formatted(Formatting.RED), true);
            return false;
        }

        ClassProgress progress = current(player);
        SkillNode node = ClassTrees.node(id);
        if (!ClassTrees.belongsTo(node, progress.dndClass) || !progress.isUnlocked(id))
            return false;

        if (node.isActive()) {
            progress.active = id;
        } else if (progress.passives.contains(id)) {
            progress.passives.remove(id);
        } else if (progress.passives.size() < ClassProgress.PASSIVE_SLOTS) {
            progress.passives.add(id);
        } else {
            player.sendMessage(Text.literal("Both passive slots are full. Unequip one first.")
                    .formatted(Formatting.RED), true);
            return false;
        }
        save(player, progress);
        sync(player);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public static String name(DndCharacter dndClass) {
        String lower = dndClass.name().toLowerCase();
        return dndClass == DndCharacter.BLOODHUNTER ? "Blood Hunter"
                : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
