package mattonfire.dnd.quest;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import mattonfire.dnd.dm.DungeonMaster;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Hooks into other systems. {@code magicItem} waits for the magic items ticket; {@code ignored} is DM mode.
 */
public final class QuestHooks {
    /**
     * Magic items (PR #117): rolls a magic item of a rarity ({@code common} .. {@code legendary}) for
     * the player, or returns null/empty to use the fallback loot table
     * {@code dndclasses:gameplay/quest_reward_<rarity>}.
     */
    public static BiFunction<ServerPlayerEntity, String, ItemStack> magicItem = (player, rarity) -> null;

    /**
     * Players who don't take part in quests: DMs in DM mode. Ignored players are never added as
     * participants and their kills, visits, raids and NPC talks give no progress.
     */
    public static Predicate<ServerPlayerEntity> ignored = DungeonMaster::isDm;

    private QuestHooks() {
    }
}
