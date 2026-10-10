package mattonfire.dnd.magic;

import java.util.UUID;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Rest.DndRules;
import mattonfire.dnd.classes.Rest.Rests;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The Forge Domain Cleric's subclass feature, Blessing of the Forge: once per long rest (once per in-game
 * day with the {@code dndRests} gamerule off), at an Attunement Table's Items tab, make the held weapon or a
 * worn armor piece +1 ({@link MagicData#isForgeBlessed}, counted by {@link MagicData#activePlus}, at most +3).
 *
 * <p>The blessing belongs to the Cleric's rest: the item stores who blessed it ({@code forgeBy}) and in
 * which rest ({@code forgeKey}: {@code rest:<long rests so far>}, plus {@code /day:<day>} with rests off).
 * Once a second, any blessed item an online player carries loses the +1 if its Cleric is online and has
 * rested since (or isn't a Forge Cleric any more), wherever the item is.
 */
public final class ForgeBlessing {
    public static final String FORGE_SUBCLASS = "cleric.forge";
    /** Persistent data: the rest key the Cleric last used the blessing in. */
    private static final String USED_KEY = "dndForgeBlessingUsed";
    private static final String BY_KEY = "forgeBy";
    private static final String REST_KEY = "forgeKey";
    /** Persistent data: how many long rests the player has had, for {@link #restKey}. */
    private static final String RESTS_KEY = "dndForgeLongRests";

    private ForgeBlessing() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    tick(server, player);
                }
            }
        });
    }

    public static boolean isForgeCleric(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.CLERIC
                && Progression.current(player).hasSubclass(FORGE_SUBCLASS);
    }

    /**
     * Which rest a blessing belongs to: the Cleric's long rests so far, plus today's day number with rests
     * off (no long rests then, so the blessing is once a day; an admin {@code /dndclass rest long} still
     * ends it).
     */
    private static String restKey(ServerPlayerEntity player) {
        String key = "rest:" + ((IEntityDataSaver) player).getPersistentData().getInt(RESTS_KEY);
        return DndRules.rests(player.getWorld()) ? key : key + "/day:" + Rests.day(player);
    }

    private static boolean used(ServerPlayerEntity player) {
        return restKey(player).equals(((IEntityDataSaver) player).getPersistentData().getString(USED_KEY));
    }

    /** 0 not a Forge Cleric, 1 ready, 2 used until the next long rest (for {@link AttunementSnapshot}). */
    public static int state(ServerPlayerEntity player) {
        if (!isForgeCleric(player))
            return 0;
        return used(player) ? 2 : 1;
    }

    /**
     * Whether the stack in inventory slot {@code slot} can take the blessing: an identified, unblessed held
     * weapon or worn armor piece below +3. The client uses it too, to show the button.
     */
    public static boolean canBless(PlayerEntity player, int slot, ItemStack stack) {
        if (stack.isEmpty() || !MagicData.isIdentified(stack) || MagicData.isForgeBlessed(stack)
                || MagicData.plus(stack) >= MagicGear.MAX_PLUS)
            return false;
        return switch (MagicGear.gearOf(stack.getItem())) {
            case MELEE, RANGED -> slot == player.getInventory().selectedSlot || slot == PlayerInventory.OFF_HAND_SLOT;
            case ARMOR -> slot >= PlayerInventory.MAIN_SIZE && slot < PlayerInventory.MAIN_SIZE + 4;
            case NONE -> false;
        };
    }

    public static boolean bless(ServerPlayerEntity player, int slot) {
        if (!isForgeCleric(player))
            return false;
        boolean rests = DndRules.rests(player.getWorld());
        if (!Progression.atAttunementTable(player)) {
            player.sendMessage(Text.literal("The Blessing of the Forge is given at an Attunement Table.")
                    .formatted(Formatting.RED), false);
            return false;
        }
        if (used(player)) {
            player.sendMessage(Text.literal(rests
                    ? "You've used the Blessing of the Forge. It returns after a long rest."
                    : "You've used the Blessing of the Forge today.").formatted(Formatting.RED), false);
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.size())
            return false;
        ItemStack stack = inventory.getStack(slot);
        if (!canBless(player, slot, stack)) {
            player.sendMessage(Text.literal("Bless an identified weapon in your hand or a piece of armor you wear,"
                    + " below +3.").formatted(Formatting.RED), false);
            return false;
        }
        String key = restKey(player);
        MagicData.setForgeBlessed(stack, true);
        NbtCompound magic = MagicData.getOrCreate(stack);
        magic.putUuid(BY_KEY, player.getUuid());
        magic.putString(REST_KEY, key);
        ((IEntityDataSaver) player).getPersistentData().putString(USED_KEY, key);
        Attunement.sync(player);
        player.sendMessage(Text.literal("Blessing of the Forge: ").formatted(Formatting.GOLD)
                .append(stack.toHoverableText())
                .append(Text.literal(" is now +" + MagicData.displayPlus(stack)
                        + (rests ? " until your next long rest." : " until tomorrow.")).formatted(Formatting.GOLD)),
                false);
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.7F, 1.3F);
        world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.2, player.getZ(), 15, 0.4, 0.3,
                0.4, 0.01);
        return true;
    }

    /** Ends stale blessings on what the player carries, and refreshes a Forge Cleric's tab once theirs is back. */
    private static void tick(MinecraftServer server, ServerPlayerEntity holder) {
        boolean changed = false;
        PlayerInventory inventory = holder.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!MagicData.isForgeBlessed(stack) || !stale(server, MagicData.get(stack)))
                continue;
            end(stack);
            changed = true;
            holder.sendMessage(Text.literal("The Blessing of the Forge on ").formatted(Formatting.GRAY)
                    .append(stack.getName()).append(Text.literal(" fades.").formatted(Formatting.GRAY)), false);
        }
        NbtCompound data = ((IEntityDataSaver) holder).getPersistentData();
        if (data.contains(USED_KEY) && !used(holder)) {
            data.remove(USED_KEY);
            changed = true;
        }
        if (changed)
            Attunement.sync(holder);
    }

    /** Whether a blessing is over: its Cleric is online and has rested since, or isn't a Forge Cleric any more. */
    private static boolean stale(MinecraftServer server, NbtCompound magic) {
        if (!magic.containsUuid(BY_KEY))
            return true; // No Cleric recorded (an old blessing): end it
        UUID by = magic.getUuid(BY_KEY);
        ServerPlayerEntity cleric = server.getPlayerManager().getPlayer(by);
        if (cleric == null)
            return false; // Can't tell yet; checked again once they're back
        return !isForgeCleric(cleric) || !magic.getString(REST_KEY).equals(restKey(cleric));
    }

    private static void end(ItemStack stack) {
        MagicData.setForgeBlessed(stack, false);
        NbtCompound magic = MagicData.get(stack);
        if (magic != null) {
            magic.remove(BY_KEY);
            magic.remove(REST_KEY);
            if (magic.isEmpty())
                stack.removeSubNbt(MagicData.KEY); // A mundane item again
        }
    }

    /** After a long rest: end this Cleric's blessings on what they carry now and show the blessing as ready. */
    public static void onLongRest(ServerPlayerEntity player) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        data.putInt(RESTS_KEY, data.getInt(RESTS_KEY) + 1);
        tick(player.getServer(), player);
        Attunement.sync(player);
    }
}
