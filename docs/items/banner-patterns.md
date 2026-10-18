# Racial banner patterns
Eight banner patterns, one for each racial home and the goblins, so a home's banners show from a distance whose they are. Each is a banner pattern item for the loom, like vanilla's Skull Charge, and works on banners and shields. The homes hang banners with their own pattern, their kin merchants sell the pattern item, and the Goblin Warlord drops the goblin skull.

![The eight patterns on wall banners, each in its home's colours, with matching shields in frames under them](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-banners/racial-banners-wall.png)

## How it works
| Pattern | Design | Hung at | Banner |
|-|-|-|-|
| **Bloodtusk** | two crossed curved tusks under a handprint | [Orc Stronghold](../structures/orc-strongholds.md): the gatehouse towers, the Great Longhouse and the Fighting Pit's war drum | black on red (the war banners also have a black border) |
| **Clan Anvil** | a hammer over an anvil in a hexagon | [Dwarven Fortress](../structures/dwarven-fortresses.md): the gate and the great hall | gold on the gate's colour; in the hall, each bay's banners in the hall's other colour (red and gold banners get the anvil in gold and red) |
| **Sylvan Leaf** | a single leaf rising from a crescent moon | [Elven Enclave](../structures/elven-enclaves.md): the Moonwell's four corner banners | cyan on white |
| **Three Breaths** | three horned dragon heads meeting in a triangle | [Dragon Temple](../structures/dragon-temples.md): the sanctum's three breath altars | gold on red, light blue and blue |
| **Infernal Pact** | a pentagram in a ring, with horns | [Brimstone Sanctum](../structures/brimstone-sanctums.md): either side of the Pact Hall's altar | red on black |
| **Gnome Cog** | a cog with a magnifying glass | [Cogsdeep](../structures/cogsdeep.md): over the observatory door | gold on blue |
| **Green Dragon** | a curled green dragon, wing raised | [Hobbit village](../structures/hobbit-villages.md): beside the Green Dragon inn's door | green on yellow |
| **Goblin Skull** | a crude grinning goblin skull with long ears | [Goblin camp](../structures/goblin-camps.md): the gate posts, and the Warlord's standard on the chief's tent when a Warlord leads the camp | lime on black (the standard also has a red border) |

- In the loom, put a banner, a dye and the pattern item in: the pattern is the one choice, as with vanilla's pattern items. The item isn't used up.
- Patterned banners also go on a shield at the crafting table, as usual.
- A banner's tooltip names the layer, e.g. "Black Bloodtusk".
- Only banners placed when a structure generates get the patterns: structures generated before this update keep their plain banners.

![The loom with a white banner, red dye and the Bloodtusk pattern, and the banner it makes](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/racial-banners/racial-banners-loom.png)

## Where to find it
| Pattern | Where |
|-|-|
| Bloodtusk | The Orc Quartermaster sells it for 3 emeralds (he only trades with Half-Orcs and the Blood-Sworn) |
| Clan Anvil | The mountain dwarves barter it for gold: weight 4 of 159 in `gameplay/dwarf_barter` (about 1 barter in 40), 4 of 171 in the Honored table |
| Sylvan Leaf | The Elf Speaker sells it for 3 emeralds |
| Three Breaths | The Dragonborn Elder sells it for 3 emeralds |
| Infernal Pact | The Tiefling Pactbroker sells it for 3 emeralds |
| Gnome Cog | Every Gnome Tinker sells it for 3 emeralds |
| Green Dragon | The innkeeper sells it for 3 emeralds |
| Goblin Skull | The Goblin Warlord always drops one when a player kills it |

Prices are before the reputation tier and kin discounts (kin pay 25% less). The offer is added to the end of the merchant's list, also for merchants that already traded before this update. All eight are in the **DnD Building Blocks** creative tab.

## For developers
- `classes/Registry/ModBannerPatterns`: the patterns. Each `Pattern` registers a `BannerPattern` (`dndclasses:<name>`, short id `dnd_<name>` in banner NBT) and a `BannerPatternItem` (`dndclasses:<name>_banner_pattern`, uncommon, stacks to 1) tied to the banner pattern tag `dndclasses:pattern_item/<name>` (`data/dndclasses/tags/banner_pattern/pattern_item/`). `Pattern.in(color)` makes a `Layer`; `new Layer("bo", color)` is a vanilla one. `decorate(world, pos, layers...)` sets the patterns of a banner just placed.
- `ModPiece.Builder.banner(x, y, z, facing, color, layers...)` hangs a patterned wall banner; `patterns(x, y, z, layers...)` decorates a standing banner placed with `set`. The goblin camp calls `ModBannerPatterns.decorate` itself.
- `entity/RacialBannerOffers`: which merchant sells which pattern (`PRICE` = 3). `NpcMerchant` adds the offer when it makes a merchant's offers and when it loads saved ones, if they don't have it.
- Drops: `loot_tables/entities/goblin_warlord.json` (last pool), `loot_tables/gameplay/dwarf_barter.json` and `dwarf_barter_honored.json`.
- Textures: `tools/banner_pattern_textures.py` draws each emblem from shapes and writes `textures/entity/banner/<name>.png` (64x64; the front face x 1-20, y 1-40 is drawn mirrored by the game, so the emblem goes on it mirrored, and as drawn on the back face x 22-41), `textures/entity/shield/<name>.png` (front face x 1-12, y 1-22) and the item's `textures/item/<name>_banner_pattern.png`. The banner and shield atlases pick up any `entity/banner/` and `entity/shield/` texture in every namespace, so there's no atlas file.
- Lang: `item.dndclasses.<name>_banner_pattern` ("Banner Pattern"), `.desc` (the pattern's name) and `block.minecraft.banner.dndclasses.<name>.<color>` for the banner tooltip.
- Devscripts: `racial-banners.txt` (the wall of banners and shields, the loom, every merchant's offer, a fresh Pactbroker's offers, the Warlord's drop, 400 rolls of each dwarf barter table); `racial-banners-locate.txt`, `racial-banners-pieces.txt` and `racial-banners-tour.txt` find fresh structures in the dev world and screenshot their banners.
