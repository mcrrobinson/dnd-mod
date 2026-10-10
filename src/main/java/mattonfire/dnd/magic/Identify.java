package mattonfire.dnd.magic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.AlchemistSkills;
import mattonfire.dnd.classes.Progression.Classes.BardSkills;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Identifying magic items (design section 3.5). An unidentified item works as its base item only; identifying it
 * wakes its magic and reveals any curse. Ways to identify:
 * <ul>
 * <li>Wizards, Artificers and Lore Bards: every item that enters their inventory (checked once a second);
 * Transmuter Alchemists: potions</li>
 * <li>a Scroll of Identify ({@link ScrollItem}), read with the item in the offhand, or spent from the inventory by
 * the Attunement Table's Identify button</li>
 * <li>a short rest: one item</li>
 * <li>the Hobbit Tavern innkeeper, for emeralds by tier (until a sage NPC exists)</li>
 * <li>attuning ({@link Attunement}) and a curse binding ({@link Curse})</li>
 * </ul>
 */
public final class Identify {
    public static final Identifier C2S_IDENTIFY = new Identifier(DnDClasses.MOD_ID, "identify_item");

    private Identify() {
    }

    /** What a class identifies just by carrying it. */
    public enum Lore {
        NONE,
        POTIONS,
        ALL
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_IDENTIFY, (server, player, handler, buf, sender) -> {
            int slot = buf.readVarInt();
            server.execute(() -> identifyAtTable(player, slot));
        });
        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind == RestKind.SHORT)
                shortRest(player);
        });
    }

    public static Lore lore(PlayerEntity player) {
        DndCharacter c = Progression.classOf(player);
        if (c == DndCharacter.WIZARD || c == DndCharacter.ARTIFICER)
            return Lore.ALL;
        if (c == DndCharacter.BARD && Progression.current(player).hasSubclass(BardSkills.LORE))
            return Lore.ALL;
        if (c == DndCharacter.ALCHEMIST && Progression.current(player).hasSubclass(AlchemistSkills.TRANSMUTER))
            return Lore.POTIONS;
        return Lore.NONE;
    }

    public static boolean isUnidentified(ItemStack stack) {
        return !stack.isEmpty() && !MagicData.isIdentified(stack) && MagicItems.info(stack) != null;
    }

    /** "Frostbrand (Very Rare, cursed: Bloodthirst)", or "+2 Iron Sword (Rare)". */
    public static MutableText summary(ItemStack stack) {
        MagicItems.Info info = MagicItems.info(stack);
        MutableText text = Text.empty().append(stack.toHoverableText());
        if (info == null)
            return text;
        MutableText detail = Text.literal(" (").append(info.tier().displayName());
        Curse curse = Curse.byId(MagicData.curse(stack));
        if (curse != null)
            detail.append(", ").append(Text.translatable("magic.dndclasses.curse.cursed", curse.displayName())
                    .formatted(Formatting.RED));
        return text.append(detail.append(")").formatted(Formatting.GRAY));
    }

    /**
     * Identifies the stack and reveals its curse, with a chat line {@code magic.dndclasses.identify.<how>}
     * ("Your arcane training reveals: %s").
     */
    public static void reveal(ServerPlayerEntity player, ItemStack stack, String how) {
        MagicData.setIdentified(stack, true);
        if (!MagicData.curse(stack).isEmpty())
            MagicData.setCurseKnown(stack, true);
        player.sendMessage(Text.translatable("magic.dndclasses.identify." + how, summary(stack))
                .formatted(Formatting.LIGHT_PURPLE), false);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 0.8F, 1.3F);
        DnDClasses.LOGGER.info("[Identify] {} identified {} ({})", player.getEntityName(), stack.getName().getString(),
                how);
    }

    /** Once a second: Wizards, Artificers and Lore Bards (potions: Transmuters) identify what they carry. */
    static void secondTick(ServerPlayerEntity player) {
        Lore lore = lore(player);
        if (lore == Lore.NONE)
            return;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!isUnidentified(stack))
                continue;
            MagicItems.Info info = MagicItems.info(stack);
            if (lore == Lore.POTIONS && (info == null || info.kind() != MagicKind.POTION))
                continue;
            reveal(player, stack, lore == Lore.POTIONS ? "alchemy" : "arcane");
        }
    }

    /** A short rest identifies one item: the main hand's, the offhand's, else the first one carried. */
    static void shortRest(ServerPlayerEntity player) {
        ItemStack stack = firstUnidentified(player);
        if (stack != null)
            reveal(player, stack, "rest");
    }

    private static @Nullable ItemStack firstUnidentified(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        List<Integer> order = new ArrayList<>();
        order.add(inventory.selectedSlot);
        order.add(PlayerInventory.OFF_HAND_SLOT);
        for (int i = 0; i < inventory.size(); i++)
            order.add(i);
        for (int slot : order) {
            ItemStack stack = inventory.getStack(slot);
            if (isUnidentified(stack))
                return stack;
        }
        return null;
    }

    /** The table's Identify button: spends a Scroll of Identify from the inventory on the item in {@code slot}. */
    static void identifyAtTable(ServerPlayerEntity player, int slot) {
        PlayerInventory inventory = player.getInventory();
        if (!Progression.atAttunementTable(player) || slot < 0 || slot >= inventory.size())
            return;
        ItemStack stack = inventory.getStack(slot);
        if (!isUnidentified(stack))
            return;
        int scroll = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isOf(ModItems.SCROLL_OF_IDENTIFY)) {
                scroll = i;
                break;
            }
        }
        if (scroll < 0) {
            player.sendMessage(Text.translatable("magic.dndclasses.identify.need_scroll").formatted(Formatting.RED),
                    false);
            return;
        }
        if (!player.isCreative())
            inventory.getStack(scroll).decrement(1);
        reveal(player, stack, "scroll");
        Attunement.sync(player);
    }

    // ---- The innkeeper's service ----

    /** Emeralds the innkeeper (later: a sage) asks to identify an item of this tier. */
    public static int price(MagicTier tier) {
        return switch (tier) {
            case COMMON -> 2;
            case UNCOMMON -> 4;
            case RARE -> 8;
            case VERY_RARE -> 16;
            case LEGENDARY -> 32;
        };
    }

    /**
     * Sneak + right-click the innkeeper holding emeralds, with the unidentified item in the offhand. Called from
     * {@code InnkeeperEntity.interactMob} on the server.
     *
     * @return whether this was a request for the service (the click is used up), even if it was refused
     */
    public static boolean innkeeperService(ServerPlayerEntity player) {
        ItemStack emeralds = player.getMainHandStack();
        ItemStack item = player.getOffHandStack();
        if (!player.isSneaking() || !emeralds.isOf(Items.EMERALD))
            return false;
        if (!isUnidentified(item)) {
            player.sendMessage(Text.translatable("magic.dndclasses.identify.innkeeper_how").formatted(Formatting.GOLD),
                    false);
            return true;
        }
        MagicItems.Info info = MagicItems.info(item);
        int price = price(info.tier());
        if (emeralds.getCount() < price && !player.isCreative()) {
            player.sendMessage(Text.translatable("magic.dndclasses.identify.innkeeper_price", price)
                    .formatted(Formatting.RED), false);
            return true;
        }
        if (!player.isCreative())
            emeralds.decrement(price);
        reveal(player, item, "innkeeper");
        return true;
    }
}
