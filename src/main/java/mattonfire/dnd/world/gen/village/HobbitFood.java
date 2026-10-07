package mattonfire.dnd.world.gen.village;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;

/** What hobbits put on their plates and hang in their kitchens. */
final class HobbitFood {
    private static final Item[] PLATES = {
            Items.BREAD, Items.BREAD, Items.PUMPKIN_PIE, Items.PUMPKIN_PIE, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP,
            Items.COOKED_MUTTON, Items.COOKED_BEEF, Items.COOKED_RABBIT, Items.COOKED_SALMON, Items.COOKED_COD,
            Items.BAKED_POTATO, Items.BAKED_POTATO, Items.APPLE, Items.COOKIE, Items.MUSHROOM_STEW, Items.RABBIT_STEW,
            Items.BEETROOT_SOUP, Items.HONEY_BOTTLE, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.MELON_SLICE,
            Items.GOLDEN_CARROT, Items.CARROT, Items.BROWN_MUSHROOM
    };
    private static final Item[] HANGING = {
            Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.CHICKEN, Items.RABBIT, Items.MUTTON, Items.BREAD,
            Items.BEETROOT, Items.CARROT, Items.WHEAT, Items.SALMON, Items.COD, Items.DRIED_KELP
    };
    private static final Item[] PRODUCE = {
            Items.APPLE, Items.CARROT, Items.POTATO, Items.BEETROOT, Items.MELON_SLICE, Items.SWEET_BERRIES,
            Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.WHEAT, Items.EGG, Items.HONEYCOMB, Items.PUMPKIN_PIE, Items.BREAD
    };

    private HobbitFood() {
    }

    static ItemStack plate(Random random) {
        return new ItemStack(PLATES[random.nextInt(PLATES.length)]);
    }

    static ItemStack hanging(Random random) {
        return new ItemStack(HANGING[random.nextInt(HANGING.length)]);
    }

    static ItemStack produce(Random random) {
        return new ItemStack(PRODUCE[random.nextInt(PRODUCE.length)]);
    }
}
