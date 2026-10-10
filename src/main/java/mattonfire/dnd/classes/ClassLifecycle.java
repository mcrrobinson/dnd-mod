package mattonfire.dnd.classes;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Items.ClassGuidebook;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.ClassTrees;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.classes.Race.RaceStats;
import mattonfire.dnd.classes.SkillChecks.AttackRolls;
import mattonfire.dnd.classes.SkillChecks.Lockpicking;
import mattonfire.dnd.classes.SkillChecks.Persuasion;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The one server-side path for a player's class:
 * <ul>
 * <li>{@link #change} picks or switches the class (the picker packet and
 * {@code /dndclass set}). Only this clears effects and sets starting health.</li>
 * <li>Join, respawn and End exit only re-apply the class's attribute base values
 * ({@link ClassStats#apply}), so effects and health are kept.</li>
 * <li>A new player entity (death, End exit) gets the class and all the mod's
 * persistent data (progress, mana, Druid forms) copied over.</li>
 * <li>Disconnect drops per-player server state.</li>
 * </ul>
 */
public final class ClassLifecycle {
    private ClassLifecycle() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(DnDClasses.C2S_CLASS_PICK_PACKET_ID,
                (server, player, handler, buf, sender) -> {
                    int id = buf.readInt();
                    server.execute(() -> onPick(player, id));
                });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            DndCharacter dndClass = classOf(player);
            if (dndClass != DndCharacter.NONE) {
                ClassStats.apply(player, dndClass);
            }
            // Race first: the client opens the race picker before the class picker.
            RaceLifecycle.onJoin(player);
            // NONE makes the client open the class picker.
            sendClass(player, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, dndClass);
        });

        // Death and leaving the End make a new player entity; vanilla doesn't know about the mod's data.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (newPlayer instanceof PlayerEntityExt ext) {
                ext.setDndClass(classOf(oldPlayer));
            }
            RaceLifecycle.copy(oldPlayer, newPlayer);
            NbtCompound oldData = ((IEntityDataSaver) oldPlayer).getPersistentData();
            NbtCompound newData = ((IEntityDataSaver) newPlayer).getPersistentData();
            for (String key : oldData.getKeys()) {
                newData.put(key, oldData.get(key).copy());
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            DndCharacter dndClass = classOf(newPlayer);
            // Race modifiers before the health below, so a Dwarf's extra heart counts.
            RaceLifecycle.afterRespawn(newPlayer);
            if (dndClass != DndCharacter.NONE) {
                ClassStats.apply(newPlayer, dndClass);
            }
            if (dndClass != DndCharacter.NONE || newPlayer.getMaxHealth() != 20.0f) {
                // The new entity got its health while it still had vanilla max health.
                newPlayer.setHealth(alive ? Math.min(oldPlayer.getHealth(), newPlayer.getMaxHealth())
                        : newPlayer.getMaxHealth());
            }
            if (dndClass != DndCharacter.NONE) {
                ClassGuidebook.giveIfMissing(newPlayer);
                if (!alive) {
                    DnDClasses.sendRespawnHint(newPlayer);
                }
            }
            // The client has a new player entity too. A classless player who died gets the picker.
            if (dndClass != DndCharacter.NONE || !alive) {
                sendClass(newPlayer, DnDClasses.S2C_CLASS_QUERY_PACKET_ID, dndClass);
            }
            ManaManager.sync(newPlayer);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer(), server));
    }

    public static DndCharacter classOf(PlayerEntity player) {
        DndCharacter dndClass = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        return dndClass == null ? DndCharacter.NONE : dndClass;
    }

    /**
     * The class picker. Only a player without a class may pick one; switching
     * afterwards is admin-only ({@code /dndclass set}).
     */
    private static void onPick(ServerPlayerEntity player, int id) {
        DndCharacter picked;
        try {
            picked = DndCharacter.fromValue(id);
        } catch (IllegalArgumentException e) {
            DnDClasses.LOGGER.warn("{} sent an unknown class id {}", player.getEntityName(), id);
            return;
        }
        DndCharacter current = classOf(player);
        if (picked == DndCharacter.NONE || current != DndCharacter.NONE) {
            // Tell the client its real class again so it stops showing the picker.
            player.closeHandledScreen();
            sendClass(player, DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID, current);
            return;
        }
        change(player, picked);
        DnDClasses.sendRespawnHint(player);
    }

    /**
     * Switches a player to a class (first pick or admin change). Clears effects
     * and sets the class's starting health; every class only sets attribute base
     * values, so switching back and forth never stacks anything.
     */
    public static void change(ServerPlayerEntity player, DndCharacter dndClass) {
        for (ClassSkills skills : ClassTrees.all()) {
            skills.forget(player);
        }
        player.clearStatusEffects();
        Druid.onClassReset(player);
        if (player instanceof PlayerEntityExt ext) {
            ext.setDndClass(dndClass);
        }
        ClassStats.apply(player, dndClass);
        // Race modifiers stay through a class change; re-applying is idempotent.
        RaceStats.apply(player);
        Float health = ClassStats.pickHealth(dndClass);
        if (health != null) {
            player.setHealth(health);
        }
        ClassStats.capHealth(player);

        ClassInfo info = ClassInfo.get(dndClass);
        if (info != null) {
            sendIntro(player, info);
        }

        // Close the class picker.
        player.closeHandledScreen();
        sendClass(player, DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID, dndClass);

        // Every class change hands out the guidebook if the player has lost theirs.
        if (dndClass != DndCharacter.NONE) {
            ClassGuidebook.giveIfMissing(player);
        }
        Progression.sync(player);
    }

    /** Drops per-player server state when a player leaves. */
    private static void forget(ServerPlayerEntity player, MinecraftServer server) {
        for (ClassSkills skills : ClassTrees.all()) {
            skills.forget(player);
        }
        AttackRolls.forget(player.getUuid());
        Featherfall.forget(player.getUuid());
        Lockpicking.pruneRetries(player.getWorld().getTime());
        Persuasion.pruneOldDays(server.getOverworld().getTimeOfDay() / 24000L);
        PartyManager.get(server).forgetInvites(player.getUuid(), server.getTicks());
    }

    private static void sendClass(ServerPlayerEntity player, Identifier packet, DndCharacter dndClass) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dndClass.getValue());
        ServerPlayNetworking.send(player, packet, buf);
    }

    /** Tells the player what their new class does, from the same data as the guidebook and README. */
    private static void sendIntro(PlayerEntity player, ClassInfo info) {
        player.sendMessage(Text.literal("The " + info.name() + "\n").formatted(Formatting.UNDERLINE, Formatting.GOLD),
                false);
        player.sendMessage(Text.literal("Pros: " + ClassInfo.joinForGame(info.pros())).formatted(Formatting.GREEN),
                false);
        player.sendMessage(Text.literal("Cons: " + ClassInfo.joinForGame(info.cons())).formatted(Formatting.RED),
                false);
        player.sendMessage(Text.literal("Special: " + ClassInfo.joinForGame(info.special()))
                .formatted(Formatting.DARK_PURPLE), false);
        player.sendMessage(Text.literal("Read your Class Guidebook for the details.").formatted(Formatting.GRAY,
                Formatting.ITALIC), false);
    }
}
