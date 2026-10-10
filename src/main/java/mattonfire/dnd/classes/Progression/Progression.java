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
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server side of class progression: stores each class's {@link ClassProgress}
 * in the player's persistent data, hands out XP, and handles unlock, rank,
 * loadout and bestiary requests from the skill tree screen.
 */
public final class Progression {
    public static final Identifier S2C_SYNC = new Identifier(DnDClasses.MOD_ID, "progression_sync");
    public static final Identifier S2C_OPEN_ATTUNEMENT = new Identifier(DnDClasses.MOD_ID, "open_attunement");
    public static final Identifier C2S_UNLOCK = new Identifier(DnDClasses.MOD_ID, "unlock_skill");
    public static final Identifier C2S_EQUIP = new Identifier(DnDClasses.MOD_ID, "equip_skill");
    public static final Identifier C2S_RANK_UP = new Identifier(DnDClasses.MOD_ID, "rank_up_skill");
    public static final Identifier C2S_BESTIARY_UNLOCK = new Identifier(DnDClasses.MOD_ID, "bestiary_unlock");
    public static final Identifier C2S_CHOOSE_SUBCLASS = new Identifier(DnDClasses.MOD_ID, "choose_subclass");

    private static final String DATA_KEY = "dndProgression";
    /** The Druid's kill list from before the bestiary; becomes its learned set on first load. */
    private static final String OLD_DRUID_KILLS_KEY = "druidKilledAnimals";
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
        ServerPlayNetworking.registerGlobalReceiver(C2S_RANK_UP, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> rankUp(player, id, false));
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_BESTIARY_UNLOCK, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> unlockBestiary(player, id, false));
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_CHOOSE_SUBCLASS, (server, player, handler, buf, sender) -> {
            String id = buf.readString();
            server.execute(() -> chooseSubclass(player, id, false));
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
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound saved = data.getCompound(DATA_KEY).getCompound(dndClass.name());
        ClassProgress progress = ClassProgress.fromNbt(dndClass, saved);
        if (dndClass == DndCharacter.DRUID && !saved.contains("learned")) {
            // Kept until something saves the progress, which writes "learned".
            data.getList(OLD_DRUID_KILLS_KEY, NbtElement.STRING_TYPE)
                    .forEach(e -> progress.learned.add(e.asString()));
        }
        if (!saved.isEmpty() && !saved.contains("subclass") && player instanceof ServerPlayerEntity serverPlayer) {
            migrateSubclass(serverPlayer, progress);
        }
        return progress;
    }

    /** Progress saved before subclasses gets one from the branch it's furthest down (see {@link ClassProgress#migrateSubclass}). */
    private static void migrateSubclass(ServerPlayerEntity player, ClassProgress progress) {
        int refunded = progress.migrateSubclass();
        // Saved even with no subclass picked, so this only runs once per class.
        save(player, progress);
        Subclass chosen = progress.subclass();
        if (refunded > 0 && chosen != null && player.networkHandler != null) {
            player.sendMessage(Text.literal("Your " + name(progress.dndClass) + " tree now follows the subclass "
                    + chosen.name() + ". " + refunded + " point" + (refunded == 1 ? "" : "s") + " refunded.")
                    .formatted(Formatting.GOLD), false);
        }
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
            if (oldLevel < ClassProgress.SUBCLASS_LEVEL && progress.canChooseSubclass()) {
                player.sendMessage(Text.literal("You can now choose a subclass at an Attunement Table.")
                        .formatted(Formatting.LIGHT_PURPLE), false);
            }
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

    /**
     * Raises a node of the player's current class by one rank, for a skill
     * point. Needs an attunement table and the class level for the rank.
     *
     * @param force skip the table, level and point checks (admin command)
     * @return whether it ranked up
     */
    public static boolean rankUp(ServerPlayerEntity player, String id, boolean force) {
        ClassProgress progress = current(player);
        SkillNode node = ClassTrees.node(id);
        ClassProgress.RankUp check = progress.canRankUp(node);
        if (!force) {
            if (!atAttunementTable(player)) {
                player.sendMessage(Text.literal("Skills can only be ranked up at an Attunement Table.")
                        .formatted(Formatting.RED), true);
                return false;
            }
            if (check == ClassProgress.RankUp.LEVEL) {
                player.sendMessage(Text.literal(node.name() + " " + Ranks.roman(progress.rank(id) + 1) + " needs "
                        + name(progress.dndClass) + " level " + Ranks.levelFor(id, progress.rank(id) + 1) + ".")
                        .formatted(Formatting.RED), true);
                return false;
            }
            if (check == ClassProgress.RankUp.POINTS) {
                player.sendMessage(Text.literal("You need a skill point to rank up " + node.name() + ".")
                        .formatted(Formatting.RED), true);
                return false;
            }
        }
        if (check == ClassProgress.RankUp.LOCKED || check == ClassProgress.RankUp.MAX_RANK)
            return false;
        return setRank(player, progress, node, progress.rank(id) + 1);
    }

    /**
     * Sets a node's rank without checking points, level or the table (admin
     * command). The node must be unlocked.
     *
     * @return whether the rank was set
     */
    public static boolean setRank(ServerPlayerEntity player, String id, int rank) {
        ClassProgress progress = current(player);
        SkillNode node = ClassTrees.node(id);
        if (!ClassTrees.belongsTo(node, progress.dndClass) || !progress.isUnlocked(id) || rank < 1
                || rank > node.maxRank())
            return false;
        return setRank(player, progress, node, rank);
    }

    private static boolean setRank(ServerPlayerEntity player, ClassProgress progress, SkillNode node, int rank) {
        if (rank <= 1) {
            progress.ranks.remove(node.id());
        } else {
            progress.ranks.put(node.id(), rank);
        }
        save(player, progress);
        sync(player);

        player.sendMessage(Text.literal(node.name() + " is now rank " + Ranks.roman(rank) + ".")
                .formatted(Formatting.GOLD), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    /**
     * Adds an entity to the bestiary of the player's current class, if it has one.
     *
     * @return whether it was new
     */
    public static boolean learn(ServerPlayerEntity player, String entityId) {
        ClassProgress progress = current(player);
        if (!progress.usesBestiary() || !progress.learned.add(entityId))
            return false;
        save(player, progress);
        sync(player);
        player.sendMessage(Text.literal("Learned " + entityName(entityId)
                + ". Unlock it at an Attunement Table.").formatted(Formatting.DARK_GREEN), true);
        return true;
    }

    /**
     * Unlocks a learned entity in the bestiary. Needs an attunement table and
     * the root special's rank for the entity's tier.
     *
     * @param force skip every check but the class having a bestiary; also learns it (admin command)
     * @return whether it was unlocked
     */
    public static boolean unlockBestiary(ServerPlayerEntity player, String entityId, boolean force) {
        ClassProgress progress = current(player);
        if (!progress.usesBestiary() || progress.bestiary.contains(entityId))
            return false;
        if (!force) {
            if (!atAttunementTable(player)) {
                player.sendMessage(Text.literal("The bestiary can only be changed at an Attunement Table.")
                        .formatted(Formatting.RED), true);
                return false;
            }
            if (!progress.canUnlockBestiary(entityId))
                return false;
        }
        progress.learned.add(entityId);
        progress.bestiary.add(entityId);
        save(player, progress);
        sync(player);

        player.sendMessage(Text.literal("Unlocked " + entityName(entityId) + ".").formatted(Formatting.GREEN), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** An entity type's display name from its id, or the id if it's unknown. */
    public static String entityName(String entityId) {
        Identifier id = Identifier.tryParse(entityId);
        return id != null && Registries.ENTITY_TYPE.containsId(id)
                ? Registries.ENTITY_TYPE.get(id).getName().getString()
                : entityId;
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

    /**
     * Chooses a subclass of the player's current class. Needs class level
     * {@link ClassProgress#SUBCLASS_LEVEL}, an Attunement Table and no subclass yet.
     *
     * @param force skip the level and table checks, and replace a chosen subclass (admin command)
     * @return whether it was chosen
     */
    public static boolean chooseSubclass(ServerPlayerEntity player, String id, boolean force) {
        ClassProgress progress = current(player);
        Subclass subclass = ClassTrees.subclass(id);
        if (subclass == null || subclass.dndClass() != progress.dndClass)
            return false;
        if (progress.hasSubclass(id))
            return false;
        if (!force) {
            if (!progress.subclass.isEmpty()) {
                player.sendMessage(Text.literal("You've already chosen a subclass.").formatted(Formatting.RED), true);
                return false;
            }
            if (progress.level() < ClassProgress.SUBCLASS_LEVEL) {
                player.sendMessage(Text.literal("Subclasses are chosen at " + name(progress.dndClass) + " level "
                        + ClassProgress.SUBCLASS_LEVEL + ".").formatted(Formatting.RED), true);
                return false;
            }
            if (!atAttunementTable(player)) {
                player.sendMessage(Text.literal("Subclasses are chosen at an Attunement Table.")
                        .formatted(Formatting.RED), true);
                return false;
            }
        } else if (progress.subclass() != null) {
            progress.removeSubclassNodes(progress.subclass());
        }
        progress.subclass = id;
        save(player, progress);
        sync(player);

        player.getServer().getPlayerManager().broadcast(Text.literal(title(player) + " has chosen a subclass: "
                + subclass.name() + ".").formatted(Formatting.LIGHT_PURPLE), false);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    /**
     * Clears the subclass of the player's current class, refunding the nodes only
     * it could have (and the capstone above them), with their ranks. For the
     * admin command, and later the Tome of Clear Thought.
     *
     * @return the points refunded, or -1 if there was no subclass
     */
    public static int clearSubclass(ServerPlayerEntity player) {
        ClassProgress progress = current(player);
        Subclass subclass = progress.subclass();
        if (subclass == null && progress.subclass.isEmpty())
            return -1;
        int refunded = subclass == null ? 0 : progress.removeSubclassNodes(subclass);
        progress.subclass = "";
        save(player, progress);
        sync(player);
        player.sendMessage(Text.literal("Your " + name(progress.dndClass) + " subclass was cleared. " + refunded
                + " point" + (refunded == 1 ? "" : "s") + " refunded.").formatted(Formatting.GOLD), false);
        return refunded;
    }

    /** "Matt the Battle Master Fighter", or "Matt the Fighter" before a subclass. */
    public static String title(ServerPlayerEntity player) {
        ClassProgress progress = current(player);
        Subclass subclass = progress.subclass();
        String name = player.getName().getString();
        if (progress.dndClass == DndCharacter.NONE)
            return name;
        return name + " the " + (subclass != null ? subclass.title() : name(progress.dndClass));
    }

    public static String name(DndCharacter dndClass) {
        String lower = dndClass.name().toLowerCase();
        return dndClass == DndCharacter.BLOODHUNTER ? "Blood Hunter"
                : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
