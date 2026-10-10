package mattonfire.dnd.classes.Commands;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

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
 * Given items are identified unless "unidentified" is added. Later tickets add curse, uncurse, attune,
 * release and bonds.
 */
public final class MagicCommand {
    private static final DynamicCommandExceptionType UNKNOWN_ITEM = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown item: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_TIER = new DynamicCommandExceptionType(
            id -> Text.literal("Unknown tier: " + id + " (common, uncommon, rare, very_rare, legendary)"));
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
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(MagicCommand::info))));
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
        if (!player.giveItemStack(stack))
            player.dropItem(stack, false);
        context.getSource().sendFeedback(Text.literal("Gave ").append(name).append(" to ")
                .append(player.getDisplayName()), true);
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

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        MagicItems.Info info = MagicItems.info(stack);
        String tier = info == null ? "none (mundane)" : info.tier().id;
        context.getSource().sendFeedback(Text.literal("Magic: ").append(stack.toHoverableText())
                .append(" tier=" + tier + " identified=" + MagicData.isIdentified(stack) + " plus="
                        + MagicData.plus(stack) + " activePlus=" + MagicData.activePlus(stack)
                        + " data=" + MagicData.get(stack)), false);
        return 1;
    }
}
