package mattonfire.dnd.magic.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.magic.Identify;
import mattonfire.dnd.magic.RemoveCurse;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A one-use spell scroll, read with right-click.
 * <ul>
 * <li>{@link Spell#IDENTIFY}: identifies the unidentified magic item in the other hand (the offhand when the
 * scroll is in the main hand).</li>
 * <li>{@link Spell#REMOVE_CURSE}: Remove Curse on yourself ({@link RemoveCurse#scroll}).</li>
 * </ul>
 * The scroll is only used up when it does something.
 */
public class ScrollItem extends Item {
    public enum Spell {
        IDENTIFY("identify"),
        REMOVE_CURSE("remove_curse");

        public final String id;

        Spell(String id) {
            this.id = id;
        }
    }

    private final Spell spell;

    public ScrollItem(Spell spell, Settings settings) {
        super(settings);
        this.spell = spell;
    }

    public Spell spell() {
        return spell;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack scroll = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(scroll, true);
        boolean used = switch (spell) {
            case IDENTIFY -> identify(server, hand);
            case REMOVE_CURSE -> RemoveCurse.scroll(server);
        };
        if (!used)
            return TypedActionResult.fail(scroll);
        if (!player.getAbilities().creativeMode)
            scroll.decrement(1);
        player.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(scroll, false);
    }

    private static boolean identify(ServerPlayerEntity player, Hand hand) {
        ItemStack target = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
        if (!Identify.isUnidentified(target)) {
            player.sendMessage(Text.translatable("item.dndclasses.scroll_of_identify.how").formatted(Formatting.GRAY),
                    true);
            return false;
        }
        Identify.reveal(player, target, "scroll");
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dndclasses.scroll_of_" + spell.id + ".tooltip").formatted(Formatting.GRAY));
    }
}
