# Alchemist
The brewer: the only class that can brew safely and use the Fast Brewing Stand, but it can't enchant.

![A Transmute cloud spreading around the Alchemist and a group of mobs](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/alchemist-transmute.png)

## How it works
- **Safe brewing:** a vanilla brewing stand explodes (power 10, no block damage) when it finishes a brew that a non-Alchemist started, i.e. a non-Alchemist had it open when the brew began and no Alchemist did. The stand breaks and drops itself and its contents. Brews started with nobody looking, such as hopper-fed ones, are safe. A death from it hints "Maybe get a alchamist to brew next time...".
- **Fast Brewing Stand:** only Alchemists can open it, and it brews 10× faster. See [Potions and brewing](../items/potions-and-brewing.md).
- **Can't enchant:** enchanting tables refuse, and enchanted books won't apply on an anvil.
- **Special: Transmute (power-up key, 9 mana):** throws a lingering flask that bursts into a cloud where it lands. The flask carries the potion in your main hand (or off hand if the main hand has none); one is used up unless you're in creative. Every effect in it gets **extra levels** (capped at level V, instant effects at II) and lasts 10 s after leaving the cloud, refreshed every second inside it. Beneficial effects go to players and your pets in the cloud; the rest go to hostile mobs and to mobs targeting a player. Instant effects hit each creature once per cloud. Undead ignore Poison and Regeneration, as in vanilla. Villagers, passive animals and other people's pets are left alone. Holding no potion (or water, awkward and other effect-less bottles) throws an **unstable brew**: one random buff (Speed, Strength, Regeneration, Resistance, Haste or Jump Boost) and one random harm (Slowness, Weakness, Poison, Wither or Glowing), both at level I plus the extra levels. It always fires.
- **Transmute ranks** (raised at the Attunement Table; rank II at class level 3, III at 6, IV at 9):

  | Rank | Radius | Cloud lasts | Extra levels |
  |------|--------|-------------|--------------|
  | I    | 3 blocks | 6 s | +1 |
  | II   | 4 blocks | 8 s | +1 |
  | III  | 5 blocks | 10 s | +2 |
  | IV   | 6 blocks | 12 s | +2 |

### Subclasses
From class level 3, choose one of these two at an Attunement Table (see [Subclasses](../systems/class-progression.md#subclasses)). Your subclass's branch opens fully. The other branch's first node stays open, but the rest of it is sealed.

| Subclass | Branch | Nodes | Feature (free, doesn't use a passive slot) |
|-|-|-|-|
| Mutagenist | Right | Iron Stomach (first node, open to both), Volatile Flask, Potent Brews | **Mutagen**: drinking any potion also gives Strength I for 10 seconds |
| Transmuter | Left | Efficient Brewer (first node, open to both), Elixir of Healing, Philosopher's Touch | **Transmuter's Eye**: Healing potions you drink heal 50% more |

How the features work:
- **Mutagen** (Mutagenist): drinking any potion with an effect also gives you Strength I for 10 s (not lengthened by Potent Brews). Water and other effectless bottles don't count, and nor do splash or lingering potions.
- **Transmuter's Eye** (Transmuter): Instant Health from a potion you drink heals 50% more (Healing: 3 hearts instead of 2; Healing II: 6 instead of 4). A Transmuter also identifies every unidentified [potion](../systems/magic-items.md#identification) that enters their inventory.

## Known limitations
- The class blurb mentions random potion backfires. They aren't implemented.

## For developers
- Explosion: `mixin/BrewingStandBlockEntityMixin` (arms the brew when it starts; `BrewingStandBlockMixin` records the last user for brewing XP). Fast stand: `Blocks/FastBrewingStandBlock`, `Entities/FastBrewingStandBlockEntity`. Enchanting: `mixin/EnchantingTableMixin`, `mixin/AnvilScreenHandlerMixin`.
- Transmute: `AlchemistSkills.transmute` (called from `PowerUpEffect` case `ALCHEMIST`), ranks in `AlchemistSkills.TRANSMUTE` (node id is still `alchemist.distill`, so saved trees keep working). The cloud is a vanilla `AreaEffectCloudEntity` with no potion, for the particles only; `AlchemistSkills.pulse` applies the effects every 20 ticks so allies and mobs get different halves.
- Devscripts: `brewing-explosion.txt`, `brewing-use-alchemist.txt`, `alchemist-ranks.txt` (Transmute).
