package mattonfire.dnd.classes.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import mattonfire.dnd.magic.Attunement;
import mattonfire.dnd.magic.AttunementSnapshot;
import mattonfire.dnd.magic.Curse;
import mattonfire.dnd.magic.MagicData;
import mattonfire.dnd.magic.MagicItemLootFunction;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicTier;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * /dndmagic give &lt;player&gt; &lt;item&gt; [tier] [plus] [unidentified]
 * /dndmagic identify &lt;player&gt;   (main-hand item)
 * /dndmagic info &lt;player&gt;       (prints the main-hand item's magic data)
 *
 * /dndmagic attune &lt;player&gt;     (bonds the main-hand item: no table or channel, the other checks apply)
 * /dndmagic release &lt;player&gt; [all|&lt;n&gt;]   (the main-hand item's bond, every bond, or bond n from /dndmagic bonds;
 *                                  ignores the table and curses)
 * /dndmagic bonds &lt;player&gt;      (lists the bonds and slots)
 *
 * /dndmagic curse &lt;player&gt; &lt;curse&gt; [known]   (curses the main-hand item; unknown unless "known")
 * /dndmagic uncurse &lt;player&gt;    (lifts the main-hand item's curse; its cursed bond becomes a normal one)
 *
 * /dndmagic deck &lt;player&gt; next &lt;card&gt;   (rigs the next card of the Deck of Many Things in their main hand)
 * /dndmagic deck &lt;player&gt; clear         (lifts every lasting Deck of Many Things effect from the player)
 *
 * Given items are identified unless "unidentified" is added.
 */
public final class MagicCommand {
    private static final DynamicCommandExceptionType UNKNOWN_ITEM = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown item: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_TIER = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown tier: " + id + " (common, uncommon, rare, very_rare, legendary)"));
    private static final DynamicCommandExceptionType UNKNOWN_CURSE = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown curse: " + id + " (bloodthirst, frailty, ill_omen, beacon, sun_sick, gluttony, rings_call)"));
    private static final SimpleCommandExceptionType EMPTY_HAND = new SimpleCommandExceptionType(
            Text.literal("Hold the item in your main hand."));

    private MagicCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dndmagic")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                        .suggests((c, b) -> CommandSource.suggestIdentifiers(Registries.ITEM.getIds(), b))
                                        .executes(c -> give(c, false, false, false))
                                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                                .suggests((c, b) -> CommandSource.suggestMatching(
                                                        Arrays.stream(MagicTier.values()).map(t -> t.id), b))
                                                .executes(c -> give(c, true, false, false))
                                                .then(CommandManager.literal("unidentified")
                                                        .executes(c -> give(c, true, false, true)))
                                                .then(CommandManager.argument("plus", IntegerArgumentType.integer(0, 3))
                                                        .executes(c -> give(c, true, true, false))
                                                        .then(CommandManager.literal("unidentified")
                                                                .executes(c -> give(c, true, true, true))))))))
                .then(CommandManager.literal("identify")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MagicCommand::identify)))
                .then(CommandManager.literal("curse")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("curse", StringArgumentType.word())
                                        .suggests((c, b) -> CommandSource.suggestMatching(
                                                Arrays.stream(Curse.values()).map(k -> k.id), b))
                                        .executes(c -> curse(c, false))
                                        .then(CommandManager.literal("known").executes(c -> curse(c, true))))))
                .then(CommandManager.literal("uncurse")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MagicCommand::uncurse)))
                .then(CommandManager.literal("attune")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MagicCommand::attune)))
                .then(CommandManager.literal("release")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(c -> release(c, null))
                                .then(CommandManager.literal("all").executes(c -> release(c, -1)))
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(1))
                                        .executes(c -> release(c, IntegerArgumentType.getInteger(c, "n"))))))
                .then(CommandManager.literal("bonds")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MagicCommand::bonds)))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MagicCommand::info)))
                .then(CommandManager.literal("deck")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.literal("next")
                                        .then(CommandManager.argument("card", StringArgumentType.word())
                                                .suggests((c, b) -> CommandSource.suggestMatching(Arrays.stream(
                                                        mattonfire.dnd.magic.items.DeckCards.Card.values())
                                                        .map(mattonfire.dnd.magic.items.DeckCards.Card::id), b))
                                                .executes(MagicCommand::deckNext)))
                                .then(CommandManager.literal("clear").executes(MagicCommand::deckClear)))));
    }

    private static int give(CommandContext<ServerCommandSource> context, boolean hasTier, boolean hasPlus,
            boolean unidentified) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "item");
        Item item = Registries.ITEM.getOrEmpty(id).orElseThrow(() -> UNKNOWN_ITEM.create(id));
        if (item == Items.AIR)
            throw UNKNOWN_ITEM.create(id);
        MagicTier tier = null;
        if (hasTier) {
            String tierId = StringArgumentType.getString(context, "tier");
            tier = MagicTier.byId(tierId);
            if (tier == null)
                throw UNKNOWN_TIER.create(tierId);
        }
        @Nullable Integer plus = hasPlus ? IntegerArgumentType.getInteger(context, "plus") : Integer.valueOf(0);
        ItemStack stack = new ItemStack(item);
        if (tier != null || MagicItems.def(item) != null || unidentified)
            MagicItemLootFunction.apply(stack, tier, plus, !unidentified);
        Text name = stack.toHoverableText();
        player.getInventory().offerOrDrop(stack);
        context.getSource().sendFeedback(Text.literal("Gave ").append(name).append(" to ")
                .append(player.getDisplayName()), true);
        return 1;
    }

    private static int deckNext(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String id = StringArgumentType.getString(context, "card");
        var card = mattonfire.dnd.magic.items.DeckCards.Card.byId(id);
        if (card == null)
            throw new SimpleCommandExceptionType(Text.literal("Unknown card: " + id)).create();
        ItemStack deck = player.getMainHandStack();
        if (!mattonfire.dnd.magic.items.DeckOfManyThingsItem.isDeck(deck))
            throw new SimpleCommandExceptionType(Text.literal("Hold a Deck of Many Things in your main hand.")).create();
        mattonfire.dnd.magic.items.DeckOfManyThingsItem.rig(deck, card);
        context.getSource().sendFeedback(Text.literal("The next card of ").append(player.getDisplayName())
                .append("'s deck is ").append(card.title()), true);
        return 1;
    }

    private static int deckClear(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        mattonfire.dnd.magic.items.DeckCurses.clear(player);
        context.getSource().sendFeedback(Text.literal("Lifted the deck's effects from ").append(player.getDisplayName()),
                true);
        return 1;
    }

    private static ItemStack held(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStack stack = EntityArgumentType.getPlayer(context, "player").getMainHandStack();
        if (stack.isEmpty())
            throw EMPTY_HAND.create();
        return stack;
    }

    private static int identify(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        MagicData.setIdentified(stack, true);
        context.getSource().sendFeedback(Text.literal("Identified: ").append(stack.toHoverableText()), true);
        return 1;
    }

    private static int curse(CommandContext<ServerCommandSource> context, boolean known)
            throws CommandSyntaxException {
        ItemStack stack = held(context);
        String id = StringArgumentType.getString(context, "curse");
        Curse curse = Curse.byId(id);
        if (curse == null)
            throw UNKNOWN_CURSE.create(id);
        if (MagicItems.info(stack) == null) {
            context.getSource().sendError(Text.literal("Only magic items can be cursed (/dndmagic give it a tier)."));
            return 0;
        }
        MagicData.setCurse(stack, curse.id);
        MagicData.setCurseKnown(stack, known);
        context.getSource().sendFeedback(Text.literal("Cursed ").append(stack.toHoverableText()).append(" with ")
                .append(curse.displayName()), true);
        return 1;
    }

    private static int uncurse(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ItemStack stack = held(context);
        if (MagicData.curse(stack).isEmpty()) {
            context.getSource().sendError(Text.literal("That item isn't cursed."));
            return 0;
        }
        Attunement.uncurseBond(player, stack);
        MagicData.setCurse(stack, "");
        MagicData.setCurseKnown(stack, false);
        Attunement.removeCurseBinding(stack);
        context.getSource().sendFeedback(Text.literal("Lifted the curse on ").append(stack.toHoverableText()), true);
        return 1;
    }

    private static int attune(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ItemStack stack = held(context);
        Text refusal = Attunement.refusal(player, stack);
        if (refusal != null) {
            context.getSource().sendError(refusal);
            return 0;
        }
        Attunement.attune(player, stack);
        context.getSource().sendFeedback(Text.literal("Attuned " + player.getName().getString() + " to ")
                .append(stack.toHoverableText()), true);
        return 1;
    }

    /** @param n null for the main-hand item, -1 for all, else the 1-based index in the bonds list */
    private static int release(CommandContext<ServerCommandSource> context, @Nullable Integer n)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        List<AttunementSnapshot.Bond> bonds = Attunement.bonds(player);
        List<UUID> targets = new ArrayList<>();
        if (n == null) {
            UUID uuid = MagicData.uuid(held(context));
            if (uuid == null || Attunement.bond(player, uuid) == null) {
                context.getSource().sendError(Text.literal("That item isn't bonded to " + player.getName().getString()));
                return 0;
            }
            targets.add(uuid);
        } else if (n == -1) {
            bonds.forEach(b -> targets.add(b.uuid()));
        } else if (n <= bonds.size()) {
            targets.add(bonds.get(n - 1).uuid());
        } else {
            context.getSource().sendError(Text.literal(player.getName().getString() + " has " + bonds.size()
                    + " bonds."));
            return 0;
        }
        for (UUID uuid : targets)
            Attunement.release(player, uuid, true);
        context.getSource().sendFeedback(Text.literal("Released " + targets.size() + " bond"
                + (targets.size() == 1 ? "" : "s") + " of " + player.getName().getString()), true);
        return targets.size();
    }

    private static int bonds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        List<AttunementSnapshot.Bond> bonds = Attunement.bonds(player);
        context.getSource().sendFeedback(Text.literal(player.getName().getString() + " is attuned to "
                + bonds.size() + "/" + Attunement.slots(player) + " items" + (bonds.isEmpty() ? "." : ":")), false);
        for (int i = 0; i < bonds.size(); i++) {
            AttunementSnapshot.Bond bond = bonds.get(i);
            boolean carried = false;
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                if (bond.uuid().equals(MagicData.uuid(player.getInventory().getStack(slot))))
                    carried = true;
            }
            context.getSource().sendFeedback(Text.literal(" " + (i + 1) + ". ").append(bond.name())
                    .append(" (" + bond.item() + (carried ? ", carried" : ", missing")
                            + (bond.cursed() ? ", cursed" : "") + ")"), false);
        }
        return bonds.size();
    }

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        MagicItems.Info info = MagicItems.info(stack);
        String tier = info == null ? "none (mundane)" : info.tier().id;
        context.getSource().sendFeedback(Text.literal("Magic: ").append(stack.toHoverableText())
                .append(" tier=" + tier + " identified=" + MagicData.isIdentified(stack) + " plus="
                        + MagicData.plus(stack) + " activePlus=" + MagicData.activePlus(stack)
                        + " active=" + Attunement.isActive(EntityArgumentType.getPlayer(context, "player"), stack)
                        + " data=" + MagicData.get(stack)), false);
        return 1;
    }
}
