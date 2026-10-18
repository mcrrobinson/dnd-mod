package mattonfire.dnd.classes.Blocks.dwarven;

import net.minecraft.block.SnowBlock;

/**
 * The Treasury Coin Pile: 1-8 layers of coins that stack like snow. Unlike snow it never melts (its settings
 * don't tick randomly), and mining it gives gold nuggets, one to three per layer, not the block
 * ({@code loot_tables/blocks/treasury_coin_pile.json}). Dwarves guard it like their gold blocks
 * ({@code SettlementGrudges}).
 */
public class CoinPileBlock extends SnowBlock {
    public CoinPileBlock(Settings settings) {
        super(settings);
    }
}
