package mattonfire.dnd.quest;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Hooks for systems that aren't merged yet. Each one has a safe default; the owning ticket sets it.
 */
public final class QuestHooks {
    /**
     * Magic items (PR #117): rolls a magic item of a rarity ({@code common} .. {@code legendary}) for
     * the player, or returns null/empty to use the fallback loot table
     * {@code dndclasses:gameplay/quest_reward_<rarity>}.
     */
    public static BiFunction<ServerPlayerEntity, String, ItemStack> magicItem = (player, rarity) -> null;

    /**
     * Players who don't take part in quests: DM mode (PR #121, {@code DungeonMaster.isDm}) sets this
     * once both are merged. Ignored players are never added as participants and their kills, visits
     * and raids give no progress.
     */
    public static Predicate<ServerPlayerEntity> ignored = player -> false;

    private QuestHooks() {
    }
}
